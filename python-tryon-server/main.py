"""
Virtual Try-On Server — port 8081

Two-tier inference (auto-fallback):
  1. Replicate API (cuuupid/idm-vton) — fast cloud; used while REPLICATE_API_TOKEN
     has quota. On 402/429 it flips to local (sticky) for the rest of the process.
  2. Local CatVTON (zhengchong/CatVTON, mix-48k-1024) on GPU — RTX 3050 Ti 4GB fp16,
     768x1024, VAE slicing/tiling. Default scheduler UniPC @ 20 steps.

Masking: SegFormer human-parser (mattmdjaga/segformer_b2_clothes, ATR 18-class) builds
a cloth-agnostic mask that protects face/hair/limbs/other-garment. This replaces the
original AutoMasker (DensePose+SCHP) since detectron2 won't build on Windows.

Outfit (top + bottom): parse once → derive non-overlapping upper/lower masks → run
CatVTON twice on the ORIGINAL person → composite each garment region back (NOT chaining).

Tunable via env: TRYON_STEPS, TRYON_SCHEDULER (unipc|dpm|ddim), TRYON_CFG,
TRYON_PARSER_DEVICE (cpu|cuda), TRYON_PARSER_MODEL.
"""

import io
import os
import time
import logging
import base64
import asyncio
import traceback
from concurrent.futures import ThreadPoolExecutor
from contextlib import asynccontextmanager
from pathlib import Path

# Load .env
_env_file = Path(__file__).parent / ".env"
if _env_file.exists():
    for _line in _env_file.read_text().splitlines():
        _line = _line.strip()
        if _line and not _line.startswith("#") and "=" in _line:
            _k, _v = _line.split("=", 1)
            os.environ.setdefault(_k.strip(), _v.strip())

import httpx
import replicate
from replicate.exceptions import ReplicateError
from fastapi import FastAPI, File, Form, UploadFile, HTTPException
from fastapi.responses import Response
from PIL import Image

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s - %(message)s")
log = logging.getLogger("tryon-server")

REPLICATE_API_TOKEN = os.getenv("REPLICATE_API_TOKEN", "")
REPLICATE_MODEL = "cuuupid/idm-vton:906425dbca90663ff5427624839572cc56ea7d380343d13e2a4c4b09d3f0c30f"

# CatVTON trained at 768x1024 — keep native res for fidelity (fits 3050Ti 4GB w/ fp16 + VAE slicing)
INFER_W, INFER_H = 768, 1024

# Diffusion steps. CatVTON is an inpaint-concat model → converges fast.
# With UniPC/DPM++ scheduler, 16-20 steps ≈ DDIM-25 quality. ~2.3s/step on a 3050Ti.
NUM_STEPS = int(os.getenv("TRYON_STEPS", "20"))
GUIDANCE = float(os.getenv("TRYON_CFG", "2.5"))
# Scheduler: 'ddim' (CatVTON default, safe) | 'unipc' | 'dpm' (both converge faster at low steps)
SCHEDULER = os.getenv("TRYON_SCHEDULER", "unipc").lower()
# Human-parser device: 'cpu' keeps all 4GB VRAM for diffusion (parse is one-time ~3s).
PARSER_DEVICE = os.getenv("TRYON_PARSER_DEVICE", "cpu")
PARSER_MODEL = os.getenv("TRYON_PARSER_MODEL", "mattmdjaga/segformer_b2_clothes")

_use_local_fallback = False
_catvton_pipe = None
_parser = None          # (image_processor, model) for SegFormer human parsing
_parser_failed = False
_executor = ThreadPoolExecutor(max_workers=1)


# ── CatVTON local ─────────────────────────────────────────────────────────────
CATVTON_SRC = str(Path(__file__).parent / "catvton_src")


def _load_catvton():
    global _catvton_pipe
    if _catvton_pipe is not None:
        return _catvton_pipe

    import sys
    import torch
    if CATVTON_SRC not in sys.path:
        sys.path.insert(0, CATVTON_SRC)

    # Faster conv autotune for fixed input size (we always run 768x1024)
    torch.backends.cudnn.benchmark = True

    from model.pipeline import CatVTONPipeline

    weights_dir = Path(__file__).parent / "weights"
    sd_path = str(weights_dir / "stable-diffusion-inpainting")
    catvton_path = str(weights_dir / "CatVTON")

    log.info("Loading CatVTON | sd=%s | attn=%s", sd_path, catvton_path)
    pipe = CatVTONPipeline(
        base_ckpt=sd_path,
        attn_ckpt=catvton_path,
        attn_ckpt_version="mix",
        weight_dtype=torch.float16,
        device="cuda",
        skip_safety_check=True,
        use_tf32=True,
    )
    # VRAM relief on 4GB: slice/tile the VAE so the 768x2048 concat decode doesn't spike OOM
    try:
        pipe.vae.enable_slicing()
        pipe.vae.enable_tiling()
    except Exception as e:
        log.warning("VAE slicing/tiling unavailable: %s", e)

    # Faster scheduler: UniPC/DPM++ reach DDIM-25 quality in ~16-20 steps (drop-in,
    # same step() API the CatVTON pipeline uses).
    if SCHEDULER in ("unipc", "dpm", "dpm++"):
        try:
            cfg = pipe.noise_scheduler.config
            if SCHEDULER == "unipc":
                from diffusers import UniPCMultistepScheduler
                pipe.noise_scheduler = UniPCMultistepScheduler.from_config(cfg)
            else:
                from diffusers import DPMSolverMultistepScheduler
                pipe.noise_scheduler = DPMSolverMultistepScheduler.from_config(
                    cfg, algorithm_type="dpmsolver++")
            log.info("Scheduler → %s", SCHEDULER)
        except Exception as e:
            log.warning("Scheduler swap failed (%s) — keeping DDIM", e)

    _catvton_pipe = pipe
    log.info("CatVTON loaded | steps=%d cfg=%.1f sched=%s parser=%s",
             NUM_STEPS, GUIDANCE, SCHEDULER, PARSER_DEVICE)
    return _catvton_pipe


# ── Human parsing (SegFormer ATR) → cloth-agnostic mask ─────────────────────────
# detectron2/DensePose won't build on Windows, so we replace CatVTON's AutoMasker
# with a SegFormer clothes-parser. Its 18 labels ARE the ATR set, so we can build a
# body-following agnostic mask that protects face/hair/limbs/other-garment.
ATR = {
    "Background": 0, "Hat": 1, "Hair": 2, "Sunglasses": 3, "Upper-clothes": 4,
    "Skirt": 5, "Pants": 6, "Dress": 7, "Belt": 8, "Left-shoe": 9, "Right-shoe": 10,
    "Face": 11, "Left-leg": 12, "Right-leg": 13, "Left-arm": 14, "Right-arm": 15,
    "Bag": 16, "Scarf": 17,
}
# Regions to REPAINT (the garment footprint) per try-on category
MASK_PARTS = {
    "upper_body": ["Upper-clothes", "Dress", "Belt"],
    "lower_body": ["Pants", "Skirt", "Dress", "Left-leg", "Right-leg"],
    "dresses":    ["Upper-clothes", "Dress", "Skirt", "Pants", "Belt", "Left-leg", "Right-leg"],
}
# Regions to FORCE-KEEP (never paint over) per category
PROTECT_PARTS = {
    "upper_body": ["Face", "Hair", "Hat", "Sunglasses", "Pants", "Skirt",
                   "Left-leg", "Right-leg", "Left-shoe", "Right-shoe", "Bag"],
    "lower_body": ["Face", "Hair", "Hat", "Sunglasses", "Upper-clothes",
                   "Left-arm", "Right-arm", "Left-shoe", "Right-shoe", "Bag", "Scarf"],
    "dresses":    ["Face", "Hair", "Hat", "Sunglasses", "Left-shoe", "Right-shoe", "Bag"],
}


def _load_parser():
    global _parser, _parser_failed
    if _parser is not None or _parser_failed:
        return _parser
    try:
        import torch
        from transformers import AutoImageProcessor, SegformerForSemanticSegmentation
        log.info("Loading human parser | %s on %s", PARSER_MODEL, PARSER_DEVICE)
        proc = AutoImageProcessor.from_pretrained(PARSER_MODEL)
        model = SegformerForSemanticSegmentation.from_pretrained(PARSER_MODEL).to(PARSER_DEVICE).eval()
        _parser = (proc, model)
        log.info("Human parser loaded")
    except Exception as e:
        log.warning("Parser load failed (%s) — falling back to rectangle masks", e)
        _parser_failed = True
    return _parser


def _human_parse(person_img: Image.Image):
    """Return an HxW uint8 label map (ATR classes) aligned to person_img, or None."""
    parser = _load_parser()
    if parser is None:
        return None
    import torch
    proc, model = parser
    inputs = proc(images=person_img, return_tensors="pt").to(PARSER_DEVICE)
    with torch.no_grad():
        logits = model(**inputs).logits  # [1, C, h/4, w/4]
    up = torch.nn.functional.interpolate(
        logits, size=(person_img.height, person_img.width), mode="bilinear", align_corners=False
    )
    return up.argmax(dim=1)[0].to("cpu").numpy().astype("uint8")


def _rect_mask(w: int, h: int, category: str) -> Image.Image:
    """Last-resort fallback when the parser is unavailable."""
    from PIL import ImageDraw, ImageFilter
    mask = Image.new("L", (w, h), 0)
    draw = ImageDraw.Draw(mask)
    if category == "upper_body":
        draw.rectangle([int(w * 0.12), int(h * 0.18), int(w * 0.88), int(h * 0.60)], fill=255)
    elif category == "lower_body":
        draw.rectangle([int(w * 0.15), int(h * 0.48), int(w * 0.85), int(h * 0.97)], fill=255)
    else:
        draw.rectangle([int(w * 0.12), int(h * 0.18), int(w * 0.88), int(h * 0.97)], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(radius=8))
    return mask.point(lambda x: 255 if x > 64 else 0)


def _parts_to_mask(parse, names) -> "np.ndarray":
    import numpy as np
    m = np.zeros(parse.shape, dtype=np.uint8)
    for n in names:
        m |= (parse == ATR[n]).astype(np.uint8)
    return m


def _hull_mask(mask_area):
    """Convex hull per blob to fill gaps inside the garment region.
    Inlined from CatVTON's cloth_masker so our mask path needs no detectron2/SCHP."""
    import numpy as np, cv2
    _, binary = cv2.threshold(mask_area, 127, 255, cv2.THRESH_BINARY)
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    out = np.zeros_like(mask_area)
    for c in contours:
        hull = cv2.convexHull(c)
        out = cv2.fillPoly(np.zeros_like(mask_area), [hull], 255) | out
    return out


def _agnostic_mask(parse, category: str) -> Image.Image:
    """Body-following inpaint mask from a parse map: garment region (convex-hulled,
    dilated) minus the protected regions (face/hair/limbs/other-garment)."""
    import numpy as np, cv2

    cat = category if category in MASK_PARTS else "upper_body"
    h, w = parse.shape
    k = max(w, h) // 100
    k = k + 1 if k % 2 == 0 else k
    kernel = np.ones((k, k), np.uint8)

    garment = _parts_to_mask(parse, MASK_PARTS[cat])
    if garment.sum() < (w * h) * 0.005:           # parser found ~nothing → fall back
        return _rect_mask(w, h, category)

    mask = _hull_mask(garment * 255) // 255        # fill gaps inside the garment blob
    mask = cv2.dilate(mask, kernel, iterations=2)  # grow a touch past the seam

    protect = _parts_to_mask(parse, PROTECT_PARTS[cat])
    protect = cv2.dilate(protect, kernel, iterations=1)
    mask = mask & (~protect.astype(bool)).astype(np.uint8)

    blur = max(w, h) // 80
    blur = blur + 1 if blur % 2 == 0 else blur
    mask = cv2.GaussianBlur(mask * 255, (blur, blur), 0)
    mask[mask < 64] = 0
    mask[mask >= 64] = 255
    return Image.fromarray(mask.astype(np.uint8), mode="L")


def _build_mask(person_proc: Image.Image, category: str) -> Image.Image:
    """Parse + agnostic mask for a single garment; rectangle fallback on any failure."""
    try:
        parse = _human_parse(person_proc)
        if parse is not None:
            return _agnostic_mask(parse, category)
    except Exception as e:
        log.warning("Mask build failed (%s) — using rectangle", e)
    return _rect_mask(person_proc.width, person_proc.height, category)


def _catvton_infer(
    person_img: Image.Image,
    garment_img: Image.Image,
    category: str,
    num_steps: int = NUM_STEPS,
    person_proc: Image.Image = None,
    mask: Image.Image = None,
):
    """Run one CatVTON pass. Returns (result_img, person_proc, mask).
    person_proc/mask may be passed in to reuse across an outfit's two passes."""
    import torch
    import sys
    if CATVTON_SRC not in sys.path:
        sys.path.insert(0, CATVTON_SRC)
    from utils import resize_and_crop

    pipe = _load_catvton()
    if person_proc is None:
        person_proc = resize_and_crop(person_img.convert("RGB"), (INFER_W, INFER_H))
    if mask is None:
        mask = _build_mask(person_proc, category)

    result = pipe(
        image=person_proc,
        condition_image=garment_img.convert("RGB"),  # pipe pads to ratio internally
        mask=mask,
        num_inference_steps=num_steps,
        guidance_scale=GUIDANCE,
        height=INFER_H,
        width=INFER_W,
        generator=torch.Generator(device="cuda").manual_seed(42),
    )
    out = result[0] if isinstance(result, list) else result
    return out, person_proc, mask


def _local_tryon_sync(
    person_bytes: bytes,
    garment_bytes: bytes,
    category: str = "upper_body",
    description: str = "",
) -> bytes:
    person_img = Image.open(io.BytesIO(person_bytes)).convert("RGB")
    garment_img = Image.open(io.BytesIO(garment_bytes)).convert("RGB")

    log.info("CatVTON local | category=%s | steps=%d", category, NUM_STEPS)
    t0 = time.time()
    result_img, _, _ = _catvton_infer(person_img, garment_img, category)
    log.info("CatVTON done in %.1fs", time.time() - t0)

    buf = io.BytesIO()
    result_img.save(buf, format="JPEG", quality=90)
    return buf.getvalue()


def _local_outfit_sync(
    person_bytes: bytes,
    top_bytes: bytes,
    bottom_bytes: bytes,
    top_desc: str = "",
    bottom_desc: str = "",
) -> bytes:
    """Full outfit, the CORRECT way: parse the person ONCE, run two CatVTON passes
    on the ORIGINAL person (upper + lower), then composite each garment region back
    onto the crisp original. No chaining → no JPEG/denoise damage, no overlap."""
    import sys
    from PIL import ImageFilter
    if CATVTON_SRC not in sys.path:
        sys.path.insert(0, CATVTON_SRC)
    from utils import resize_and_crop

    person_img = Image.open(io.BytesIO(person_bytes)).convert("RGB")
    top_img = Image.open(io.BytesIO(top_bytes)).convert("RGB")
    bottom_img = Image.open(io.BytesIO(bottom_bytes)).convert("RGB")

    person_proc = resize_and_crop(person_img, (INFER_W, INFER_H))
    # Parse once, derive both non-overlapping masks (upper protects legs, lower protects arms)
    t0 = time.time()
    upper_mask = _build_mask(person_proc, "upper_body")
    lower_mask = _build_mask(person_proc, "lower_body")
    log.info("Outfit masks ready in %.1fs", time.time() - t0)

    t1 = time.time()
    res_up, _, _ = _catvton_infer(person_img, top_img, "upper_body",
                                  person_proc=person_proc, mask=upper_mask)
    log.info("CatVTON upper done in %.1fs", time.time() - t1)
    t2 = time.time()
    res_lo, _, _ = _catvton_infer(person_img, bottom_img, "lower_body",
                                  person_proc=person_proc, mask=lower_mask)
    log.info("CatVTON lower done in %.1fs", time.time() - t2)

    # Composite: take only each garment region (feathered) onto the original person
    final = person_proc.copy()
    final = Image.composite(res_up, final, upper_mask.filter(ImageFilter.GaussianBlur(4)))
    final = Image.composite(res_lo, final, lower_mask.filter(ImageFilter.GaussianBlur(4)))
    log.info("Outfit composited | total %.1fs", time.time() - t0)

    buf = io.BytesIO()
    final.save(buf, format="JPEG", quality=90)
    return buf.getvalue()


async def _local_tryon_async(person_bytes, garment_bytes, category="upper_body", description=""):
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(
        _executor, _local_tryon_sync, person_bytes, garment_bytes, category, description
    )


async def _local_outfit_async(person_bytes, top_bytes, bottom_bytes, top_desc="", bottom_desc=""):
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(
        _executor, _local_outfit_sync, person_bytes, top_bytes, bottom_bytes, top_desc, bottom_desc
    )


def _composite_outfit_bytes(upper_bytes: bytes, lower_bytes: bytes) -> bytes:
    """Merge two FINISHED single-garment images (e.g. from Replicate) into one outfit:
    base = upper try-on (new top + original bottom); paste the new bottom region from
    the lower try-on using its parse mask."""
    import sys
    from PIL import ImageFilter
    if CATVTON_SRC not in sys.path:
        sys.path.insert(0, CATVTON_SRC)
    from utils import resize_and_crop
    up = resize_and_crop(Image.open(io.BytesIO(upper_bytes)).convert("RGB"), (INFER_W, INFER_H))
    lo = resize_and_crop(Image.open(io.BytesIO(lower_bytes)).convert("RGB"), (INFER_W, INFER_H))
    final = up
    try:
        parse = _human_parse(lo)
        if parse is not None:
            lower_mask = _agnostic_mask(parse, "lower_body")
            final = Image.composite(lo, up, lower_mask.filter(ImageFilter.GaussianBlur(4)))
    except Exception as e:
        log.warning("Cloud outfit composite failed (%s) — returning upper", e)
    buf = io.BytesIO()
    final.save(buf, format="JPEG", quality=90)
    return buf.getvalue()


# ── Replicate API ─────────────────────────────────────────────────────────────
class QuotaExceededError(Exception):
    pass


async def _replicate_tryon(person_bytes, garment_bytes, category="upper_body", description=""):
    if not REPLICATE_API_TOKEN:
        raise ValueError("No API token")

    person_uri = f"data:{_mime(person_bytes)};base64,{base64.b64encode(person_bytes).decode()}"
    garment_uri = f"data:{_mime(garment_bytes)};base64,{base64.b64encode(garment_bytes).decode()}"

    input_data = {
        "human_img": person_uri,
        "garm_img": garment_uri,
        "garment_des": description or "clothing item",
        "category": category if category in ("upper_body", "lower_body", "dresses") else "upper_body",
        "is_checked": True,
        "is_checked_crop": False,
        "denoise_steps": 20,
        "seed": 42,
    }

    log.info("Replicate IDM-VTON | category=%s", category)
    t0 = time.time()
    try:
        output = await asyncio.get_event_loop().run_in_executor(
            None, lambda: replicate.run(REPLICATE_MODEL, input=input_data)
        )
        log.info("Replicate done in %.1fs", time.time() - t0)
        if isinstance(output, list):
            output = output[0]
        result_url = getattr(output, "url", None) or str(output)
        async with httpx.AsyncClient(timeout=120) as client:
            resp = await client.get(result_url)
            resp.raise_for_status()
            return resp.content
    except ReplicateError as e:
        msg = str(e)
        if any(x in msg for x in ("quota", "limit", "402", "429", "credit", "Insufficient")):
            log.warning("Replicate quota exhausted")
            raise QuotaExceededError(msg)
        raise


def _mime(data: bytes) -> str:
    if data[:4] == b"\x89PNG": return "image/png"
    if data[:2] == b"\xff\xd8": return "image/jpeg"
    return "image/jpeg"


# ── Dispatcher ────────────────────────────────────────────────────────────────
async def run_tryon(person_bytes, garment_bytes, category="upper_body", description=""):
    global _use_local_fallback
    if REPLICATE_API_TOKEN and not _use_local_fallback:
        try:
            return await _replicate_tryon(person_bytes, garment_bytes, category, description)
        except QuotaExceededError:
            _use_local_fallback = True
        except Exception as e:
            log.error("Replicate error: %s - trying local", e)
    return await _local_tryon_async(person_bytes, garment_bytes, category, description)



# ── FastAPI ───────────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    log.info("Try-On server starting on port 8081")
    log.info("Replicate token: %s", "SET" if REPLICATE_API_TOKEN else "NOT SET - local only")
    if not REPLICATE_API_TOKEN:
        _executor.submit(_load_catvton)
        _executor.submit(_load_parser)
    yield
    _executor.shutdown(wait=False)


app = FastAPI(title="Virtual Try-On Server", version="3.0", lifespan=lifespan)


@app.get("/health")
async def health():
    return {
        "status": "online",
        "mode": "local" if _use_local_fallback else ("api" if REPLICATE_API_TOKEN else "local_only"),
        "model": "IDM-VTON (Replicate)" if (REPLICATE_API_TOKEN and not _use_local_fallback) else "CatVTON (local)",
    }


@app.post("/tryon")
async def tryon_single(
    person_image: UploadFile = File(...),
    garment_image: UploadFile = File(...),
    category: str = Form("upper_body"),
    garment_description: str = Form(""),
):
    person_bytes = await person_image.read()
    garment_bytes = await garment_image.read()
    if not person_bytes or not garment_bytes:
        raise HTTPException(400, "Empty image")
    log.info("/tryon | category=%s | person=%dKB | garment=%dKB",
             category, len(person_bytes) // 1024, len(garment_bytes) // 1024)
    try:
        result = await run_tryon(person_bytes, garment_bytes, category, garment_description)
    except Exception as e:
        log.error("Tryon error:\n%s", traceback.format_exc())
        raise HTTPException(503, str(e))
    return Response(content=result, media_type="image/jpeg")


@app.post("/tryon/outfit")
async def tryon_outfit(
    person_image: UploadFile = File(...),
    top_garment_image: UploadFile = File(...),
    bottom_garment_image: UploadFile = File(...),
    top_description: str = Form(""),
    bottom_description: str = Form(""),
):
    person_bytes = await person_image.read()
    top_bytes = await top_garment_image.read()
    bottom_bytes = await bottom_garment_image.read()
    if not person_bytes or not top_bytes or not bottom_bytes:
        raise HTTPException(400, "Empty image")
    log.info("/tryon/outfit | person=%dKB | top=%dKB | bottom=%dKB",
             len(person_bytes) // 1024, len(top_bytes) // 1024, len(bottom_bytes) // 1024)

    global _use_local_fallback
    try:
        if REPLICATE_API_TOKEN and not _use_local_fallback:
            try:
                upper_result, lower_result = await asyncio.gather(
                    _replicate_tryon(person_bytes, top_bytes, "upper_body", top_description),
                    _replicate_tryon(person_bytes, bottom_bytes, "lower_body", bottom_description),
                )
                outfit_result = await asyncio.get_event_loop().run_in_executor(
                    _executor, _composite_outfit_bytes, upper_result, lower_result
                )
            except QuotaExceededError:
                _use_local_fallback = True
                outfit_result = await _local_outfit_async(
                    person_bytes, top_bytes, bottom_bytes, top_description, bottom_description)
            except Exception as e:
                log.error("Replicate outfit error: %s - trying local", e)
                outfit_result = await _local_outfit_async(
                    person_bytes, top_bytes, bottom_bytes, top_description, bottom_description)
        else:
            # Local: two passes on the ORIGINAL person + composite (no chaining)
            log.info("Local outfit: upper + lower from original → composite")
            outfit_result = await _local_outfit_async(
                person_bytes, top_bytes, bottom_bytes, top_description, bottom_description)
    except Exception as e:
        log.error("Outfit error:\n%s", traceback.format_exc())
        raise HTTPException(503, str(e))

    log.info("Outfit done | %dKB", len(outfit_result) // 1024)
    return Response(content=outfit_result, media_type="image/jpeg")


@app.post("/preprocess/garment")
async def preprocess_garment(garment_image: UploadFile = File(...)):
    data = await garment_image.read()
    if not data:
        raise HTTPException(400, "Empty image")
    try:
        from rembg import remove
        result = remove(data)
        return Response(content=result, media_type="image/png")
    except Exception as e:
        log.error("rembg failed: %s", e)
        return Response(content=data, media_type="image/png")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8081, log_level="info")
