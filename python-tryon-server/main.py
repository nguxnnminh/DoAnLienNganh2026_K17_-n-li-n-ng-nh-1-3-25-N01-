"""
Virtual Try-On Bridge v5 — HuggingFace Space (PRIMARY) + Local OOTDiffusion (FALLBACK)

Priority:
  1. HuggingFace OOTDiffusion Space API  ← primary, no local GPU needed
  2. Local OOTDiffusion (cloned repo)    ← auto-fallback when HF quota exhausted

Quota Handling:
  - Detects HF quota/rate-limit errors (429, "exceeded", "rate limit", etc.)
  - Automatically switches to local inference on quota exhaustion
  - Logs clearly with timestamps at every mode switch
  - POST /quota/reset  → retry HF (use after quota resets each month)
  - GET  /quota/status → see current backend, quota state, call counts
  - GET  /health       → full system status (both backends)

Configuration env vars:
  HF_SPACE_ID     - HuggingFace Space ID  (default: "levihsu/OOTDiffusion")
  HF_TOKEN        - HF API token          (optional, improves rate limits)
  HF_API_NAME     - Gradio API endpoint   (default: "" = auto-detect)
                    Auto-detection tries: /predict, /tryon, /run/predict
                    Override: HF_API_NAME=/predict  (or whatever the Space exposes)
                    Inspect a Space:  python -c "from gradio_client import Client;
                                      Client('levihsu/OOTDiffusion').view_api()"
  USE_HF_PRIMARY  - true/false            (default: true)
  OOTD_ROOT       - Path to cloned OOTDiffusion repo
  OOTD_CKPT       - Path to downloaded checkpoints
  GPU_ID          - GPU device index (0 = first GPU, -1 = CPU)
  MOCK_INFERENCE  - true/false (dev mode, never use in production)

Startup behavior:
  - If USE_HF_PRIMARY=true:
      * Connects to HF Space at startup (fast)
      * Loads local models in BACKGROUND thread (ready when HF quota runs out)
  - If USE_HF_PRIMARY=false:
      * Loads local models eagerly at startup (original behavior)
"""

import io
import os
import sys
import shutil
import tempfile
import time
import logging
import threading
from contextlib import asynccontextmanager
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from fastapi import FastAPI, File, Form, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse

# ── App setup ──────────────────────────────────────────────────────────────────
# Lifespan is defined here (before FastAPI()) so it can be passed to the
# constructor.  The body calls _do_startup() which is defined later in the
# file — that's fine in Python because function bodies resolve at call time.
@asynccontextmanager
async def _lifespan(_app: FastAPI):
    await _do_startup()
    yield  # application runs here; add shutdown logic after yield if needed

app = FastAPI(title="Virtual Try-On Bridge v5 (HF+Local)", version="5.0.0", lifespan=_lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("tryon-v5")

# ── Config ─────────────────────────────────────────────────────────────────────
MOCK_MODE       = os.environ.get("MOCK_INFERENCE", "false").lower() == "true"
RESULTS_DIR     = Path("results")
RESULTS_DIR.mkdir(exist_ok=True)

# HuggingFace Space
HF_SPACE_ID     = os.environ.get("HF_SPACE_ID",    "levihsu/OOTDiffusion")
HF_TOKEN        = os.environ.get("HF_TOKEN",        None)
# Leave empty to auto-detect from the Space's available endpoints.
# The levihsu/OOTDiffusion Space uses gr.Interface which registers "/predict".
# Set HF_API_NAME env var to pin a specific name (e.g. "/tryon" for custom Spaces).
HF_API_NAME     = os.environ.get("HF_API_NAME",    "")
USE_HF_PRIMARY  = os.environ.get("USE_HF_PRIMARY", "true").lower() == "true"

# Actual API endpoints on levihsu/OOTDiffusion (verified via view_api()):
#   /process_hd — HD UNet, upper-body only, NO category param
#   /process_dc — DC UNet, upper/lower/dress, HAS category param
# These are NOT /predict or /tryon — those names don't exist on this Space.
_HF_ENDPOINT_HD = "/process_hd"  # upper body
_HF_ENDPOINT_DC = "/process_dc"  # lower body / dress

# Category string expected by /process_dc
_HF_DC_CATEGORY = {
    "upper_body": "Upper-body",
    "lower_body": "Lower-body",
    "dresses":    "Dress",
}

# Local model
_BRIDGE_DIR     = Path(__file__).parent
OOTD_ROOT       = Path(os.environ.get("OOTD_ROOT", str(_BRIDGE_DIR / "OOTDiffusion")))
OOTD_CKPT       = Path(os.environ.get(
    "OOTD_CKPT",
    str(OOTD_ROOT / "checkpoints" / "checkpoints")
))
GPU_ID          = int(os.environ.get("GPU_ID", "0"))

GARMENT_W, GARMENT_H = 512, 768
POSE_W,    POSE_H    = 384, 512

# ── Custom error types ─────────────────────────────────────────────────────────
class ModelNotLoadedError(RuntimeError):
    """OOTDiffusion repo/checkpoints not found or failed to load."""

class InferenceError(RuntimeError):
    """Model loaded but inference call failed."""

class QuotaExhaustedError(RuntimeError):
    """HuggingFace Space quota or rate limit exceeded."""


# ══════════════════════════════════════════════════════════════════════════════
# QUOTA STATE  —  tracks HF quota and active backend
# ══════════════════════════════════════════════════════════════════════════════

_quota_lock                          = threading.Lock()
_hf_quota_exhausted: bool           = False
_hf_quota_since: datetime | None    = None

# Call counters (informational, displayed in /quota/status and /health)
_stats = {
    "hf_ok":      0,   # successful HF calls
    "hf_err":     0,   # non-quota HF errors
    "hf_quota":   0,   # quota exhaustion events
    "local_ok":   0,   # successful local calls
    "local_err":  0,   # local inference errors
}


# Keywords that identify quota / rate-limit responses from HF Spaces
_QUOTA_KEYWORDS = [
    "429",
    "quota",
    "rate limit",
    "rate_limit",
    "too many requests",
    "exceeded your",
    "limit reached",
    "usage limit",
    "daily limit",
    "monthly limit",
    "free tier",
    "gpuquota",
    "resource_exhausted",
    "out of capacity",
    "no capacity",
]


def _is_quota_error(exc: Exception) -> bool:
    """Return True if the exception looks like an HF quota/rate-limit error."""
    msg = str(exc).lower()
    return any(k in msg for k in _QUOTA_KEYWORDS)


def _mark_quota_exhausted(exc: Exception):
    """Record that HF quota is exhausted and log the mode switch."""
    global _hf_quota_exhausted, _hf_quota_since
    with _quota_lock:
        _hf_quota_exhausted = True
        _hf_quota_since     = datetime.now()
        _stats["hf_quota"] += 1

    logger.warning(
        "\n"
        "╔══════════════════════════════════════════════════════════════╗\n"
        "║   ⚠️  HuggingFace QUOTA EXHAUSTED — switching to LOCAL      ║\n"
        "╠══════════════════════════════════════════════════════════════╣\n"
        "║  Time : %s                                   ║\n"
        "║  Error: %s\n"
        "║  All future requests → local OOTDiffusion inference          ║\n"
        "║  To retry HF:  POST /quota/reset                            ║\n"
        "╚══════════════════════════════════════════════════════════════╝",
        _hf_quota_since.strftime("%Y-%m-%d %H:%M:%S"),
        str(exc)[:80],
    )


def _active_backend() -> str:
    """Return which backend is currently active."""
    if MOCK_MODE:
        return "mock"
    if not USE_HF_PRIMARY:
        return "local"
    if _hf_quota_exhausted:
        return "local (hf_quota_exhausted)"
    return "huggingface"


# ══════════════════════════════════════════════════════════════════════════════
# HUGGINGFACE SPACE BACKEND
# ══════════════════════════════════════════════════════════════════════════════

_hf_client        = None
_hf_client_lock   = threading.Lock()
_hf_connect_error = None   # cached connection error


def _get_hf_client():
    """
    Return a cached gradio_client.Client connected to HF_SPACE_ID.
    Thread-safe, lazy-initialized.
    """
    global _hf_client, _hf_connect_error

    if _hf_client is not None:
        return _hf_client
    if _hf_connect_error is not None:
        raise _hf_connect_error

    with _hf_client_lock:
        if _hf_client is not None:
            return _hf_client
        try:
            from gradio_client import Client
            logger.info("🌐 Connecting to HuggingFace Space: %s …", HF_SPACE_ID)
            kwargs = {"hf_token": HF_TOKEN} if HF_TOKEN else {}
            _hf_client = Client(HF_SPACE_ID, **kwargs)
            logger.info("✅ HuggingFace Space connected: %s", HF_SPACE_ID)
            return _hf_client
        except ImportError:
            err = ImportError("gradio_client not installed. Run: pip install gradio_client")
            _hf_connect_error = err
            raise err
        except Exception as e:
            err = RuntimeError(f"Failed to connect to HF Space '{HF_SPACE_ID}': {e}")
            _hf_connect_error = err
            logger.error("❌ HF Space connection failed: %s", e)
            raise err


def _extract_hf_gallery(result) -> str:
    """
    Extract the first image filepath from a Gradio Gallery output.

    The Space returns: List[Dict(image: filepath, caption: str | None)]
    Older gradio_client versions return: List[Tuple(filepath, caption)]
    Handle both, plus bare filepath strings.
    """
    if not result:
        raise InferenceError("HF Space returned an empty gallery")

    item = result[0]

    if isinstance(item, dict):
        path = item.get("image") or item.get("path") or item.get("name")
    elif isinstance(item, (list, tuple)):
        path = item[0]
    elif isinstance(item, str):
        path = item
    else:
        raise InferenceError(f"Unexpected gallery item type: {type(item)!r}")

    if not path:
        raise InferenceError("HF Space gallery item has no image path")

    return str(path)


def _run_hf_ootd(
    person_path: str,
    garment_path: str,
    garment_description: str = "a garment",   # kept for API compat, unused by the Space
    category: str = "upper_body",
    denoise_steps: int = 20,
    seed: int = -1,
) -> str:
    """
    Run OOTDiffusion via HuggingFace Space API.
    Returns local path to the saved result PNG.

    Actual Space endpoints (verified via view_api()):

      /process_hd  — HD UNet, upper-body only:
        vton_img, garm_img, n_samples, n_steps, image_scale, seed
        → Gallery[Dict(image, caption)]

      /process_dc  — DC UNet, upper / lower / dress:
        vton_img, garm_img, category, n_samples, n_steps, image_scale, seed
        category: 'Upper-body' | 'Lower-body' | 'Dress'
        → Gallery[Dict(image, caption)]

    Routing:
      upper_body → /process_hd   (better quality for tops)
      lower_body → /process_dc   (DC UNet required for bottoms)
      dresses    → /process_dc
    """
    try:
        from gradio_client import handle_file
    except ImportError:
        raise ImportError("gradio_client not installed. Run: pip install gradio_client")

    client = _get_hf_client()
    _seed  = seed if seed >= 0 else int(time.time() * 1000) % 2147483647

    # Shared params for both endpoints
    common = dict(
        vton_img=handle_file(person_path),
        garm_img=handle_file(garment_path),
        n_samples=1,
        n_steps=float(denoise_steps),   # Gradio slider expects float
        image_scale=2.0,
        seed=float(_seed),
    )

    try:
        if category == "upper_body":
            logger.info("🌐 HF /process_hd | steps=%d | seed=%d", denoise_steps, _seed)
            result = client.predict(**common, api_name=_HF_ENDPOINT_HD)
        else:
            hf_cat = _HF_DC_CATEGORY.get(category, "Lower-body")
            logger.info("🌐 HF /process_dc | category=%s | steps=%d | seed=%d",
                        hf_cat, denoise_steps, _seed)
            result = client.predict(**common, category=hf_cat, api_name=_HF_ENDPOINT_DC)

    except Exception as e:
        if _is_quota_error(e):
            raise QuotaExhaustedError(str(e)) from e
        with _quota_lock:
            _stats["hf_err"] += 1
        raise InferenceError(f"HF Space inference failed: {e}") from e

    # Parse Gallery output and copy to our results dir
    hf_img_path = _extract_hf_gallery(result)
    out_path    = str(RESULTS_DIR / f"hf_{category}_{int(time.time() * 1000)}.png")
    shutil.copy2(hf_img_path, out_path)

    with _quota_lock:
        _stats["hf_ok"] += 1

    logger.info("✅ HF Space complete → %s", out_path)
    return out_path


def _run_hf_outfit(
    person_path: str,
    top_path: str,
    bottom_path: str,
    denoise_steps: int = 20,
    seed: int = -1,
) -> str:
    """
    Full outfit via HF Space:
      Step 1 — /process_hd: person + top    → result_upper
      Step 2 — /process_dc: result_upper + bottom → final outfit
    """
    logger.info("🌐 HF Space outfit — step 1/2: upper garment (/process_hd)…")
    result_upper = _run_hf_ootd(
        person_path=person_path,
        garment_path=top_path,
        category="upper_body",
        denoise_steps=denoise_steps,
        seed=seed,
    )

    logger.info("🌐 HF Space outfit — step 2/2: lower garment on upper result…")
    result_outfit = None
    try:
        result_outfit = _run_hf_ootd(
            person_path=result_upper,
            garment_path=bottom_path,
            garment_description="lower body garment",
            category="lower_body",
            denoise_steps=denoise_steps,
            seed=(seed + 1) if seed >= 0 else -1,
        )
    finally:
        # Clean up intermediate result regardless of success/failure
        try:
            os.unlink(result_upper)
        except OSError:
            pass

    return result_outfit


# ══════════════════════════════════════════════════════════════════════════════
# LOCAL OOTDIFFUSION BACKEND  (unchanged from v4)
# ══════════════════════════════════════════════════════════════════════════════

_ootd_model         = None
_openpose_model     = None
_parsing_model      = None
_model_load_err     = None

# Used when HF is primary — local models load in background so they're
# ready when quota runs out (avoids a 60-second cold-start on first fallback)
_local_ready_event  = threading.Event()
_local_loading      = False


def _add_ootd_to_path():
    """Insert OOTDiffusion directories into sys.path so imports work."""
    run_dir  = str(OOTD_ROOT / "run")
    ootd_dir = str(OOTD_ROOT)
    for d in [ootd_dir, run_dir]:
        if d not in sys.path:
            sys.path.insert(0, d)


def _load_models():
    """
    Load OOTDiffusion + OpenPose + HumanParsing models.
    Results are cached globally — only called once per process.
    Raises ModelNotLoadedError with a clear message if anything is missing.
    """
    global _ootd_model, _openpose_model, _parsing_model, _model_load_err

    if _ootd_model is not None:
        return  # Already loaded

    if _model_load_err is not None:
        raise _model_load_err  # Don't retry a failed load

    if MOCK_MODE:
        logger.info("MOCK_INFERENCE=true — skipping model load")
        return

    if not OOTD_ROOT.exists():
        err = ModelNotLoadedError(
            f"OOTDiffusion repo not found at {OOTD_ROOT}. "
            f"Run: git clone https://github.com/levihsu/OOTDiffusion {OOTD_ROOT}"
        )
        _model_load_err = err
        raise err

    if not OOTD_CKPT.exists() or not (OOTD_CKPT / "ootd").exists():
        err = ModelNotLoadedError(
            f"OOTDiffusion checkpoints not found at {OOTD_CKPT}. "
            "Expected: {OOTD_CKPT}/ootd/, /humanparsing/, /openpose/. "
            "Run: python download_models.py"
        )
        _model_load_err = err
        raise err

    try:
        _add_ootd_to_path()
        _patch_checkpoint_paths()

        logger.info("📦 Loading OOTDiffusion models from %s (GPU %d)…", OOTD_ROOT, GPU_ID)

        from preprocess.openpose.run_openpose import OpenPose
        from preprocess.humanparsing.run_parsing import Parsing
        from ootd.inference_ootd_hd import OOTDiffusionHD

        _openpose_model = OpenPose(GPU_ID)
        _parsing_model  = Parsing(GPU_ID)
        _ootd_model     = OOTDiffusionHD(GPU_ID)

        # ── VRAM optimizations (tiered for RTX 3050 Ti / 4 GB) ───────────────
        # OOTDiffusion HD at FP16 needs ~4.3 GB without xformers.
        # With xformers + VAE slicing the footprint drops to ~3.0 GB — fits in 4 GB.
        # Tiers applied in order (each is additive):
        #   1. FP16          — mandatory for 4 GB, halves VRAM vs FP32
        #   2. xformers      — -25 % VRAM, +speed (best option if available)
        #      OR attention_slicing(1)  — slower but universally supported
        #   3. VAE slicing   — reduces decode peak, essentially free
        #   4. VAE tiling    — extra safety for large resolutions
        #   5. model_cpu_offload — last resort if xformers unavailable & VRAM < 4.5 GB
        try:
            import torch

            # Detect GPU capabilities
            if torch.cuda.is_available() and GPU_ID >= 0:
                props   = torch.cuda.get_device_properties(GPU_ID)
                vram_gb = props.total_memory / 1024 ** 3
                logger.info("🖥️  GPU: %s | VRAM: %.1f GB", props.name, vram_gb)
            else:
                vram_gb = 0.0

            # cuDNN auto-tuner — speeds up repeated same-size inferences
            torch.backends.cudnn.benchmark = True

            if hasattr(_ootd_model, "pipe"):
                pipe = _ootd_model.pipe

                # Tier 1 — FP16
                try:
                    pipe.to("cuda", torch_dtype=torch.float16)
                    logger.info("✅ FP16 enabled")
                except Exception as e:
                    logger.warning("FP16 failed: %s", e)

                # Tier 2 — xformers (preferred)
                xformers_ok = False
                try:
                    pipe.enable_xformers_memory_efficient_attention()
                    logger.info("✅ xformers memory-efficient attention enabled")
                    xformers_ok = True
                except Exception:
                    logger.warning("xformers unavailable — using attention slicing")
                    try:
                        # slice_size=1 is the most aggressive (lowest VRAM peak)
                        pipe.enable_attention_slicing(slice_size=1)
                        logger.info("✅ Attention slicing (slice_size=1) enabled")
                    except Exception as e:
                        logger.warning("Attention slicing failed: %s", e)

                # Tier 3 — VAE slicing (reduces decode peak, no quality loss)
                try:
                    pipe.enable_vae_slicing()
                    logger.info("✅ VAE slicing enabled")
                except AttributeError:
                    pass  # diffusers < 0.18, skip silently

                # Tier 4 — VAE tiling (extra guard for large resolutions)
                try:
                    pipe.enable_vae_tiling()
                    logger.info("✅ VAE tiling enabled")
                except AttributeError:
                    pass  # diffusers < 0.20, skip silently

                # Tier 5 — model CPU offload (last resort: VRAM < 4.5 GB + no xformers)
                # Keeps ~2.5 GB VRAM but is 20-30 % slower per inference.
                if 0 < vram_gb < 4.5 and not xformers_ok:
                    try:
                        pipe.enable_model_cpu_offload(gpu_id=GPU_ID)
                        logger.info(
                            "✅ Model CPU offload enabled "
                            "(VRAM %.1f GB < 4.5 GB, no xformers)", vram_gb,
                        )
                    except Exception as e:
                        logger.warning("CPU offload failed: %s", e)

                # Report final VRAM reservation
                if vram_gb > 0:
                    reserved = torch.cuda.memory_reserved(GPU_ID) / 1024 ** 3
                    logger.info(
                        "📊 VRAM reserved after optimizations: %.2f / %.1f GB (%.0f %%)",
                        reserved, vram_gb, reserved / vram_gb * 100,
                    )

        except Exception as opt_err:
            logger.warning("VRAM optimization error (inference will still proceed): %s", opt_err)

        _local_ready_event.set()
        logger.info("✅ Local OOTDiffusion models loaded — local fallback READY")

    except Exception as e:
        import traceback
        logger.error("Full traceback:\n%s", traceback.format_exc())
        err = ModelNotLoadedError(f"Failed to load OOTDiffusion models: {e}")
        _model_load_err = err
        _local_ready_event.set()  # unblock any waiters even on failure
        raise err


def _patch_checkpoint_paths():
    """
    OOTDiffusion inference modules hardcode checkpoint paths as '../checkpoints/...'
    Patch those globals to point to our actual OOTD_CKPT location.
    """
    import importlib.util

    ootd_hd_path = str(OOTD_ROOT / "ootd" / "inference_ootd_hd.py")
    if not Path(ootd_hd_path).exists():
        raise ModelNotLoadedError(
            f"inference_ootd_hd.py not found at {ootd_hd_path}. "
            "Is the OOTDiffusion repo cloned correctly?"
        )

    clip_candidates = [
        OOTD_CKPT / "clip-vit-large-patch14",
        OOTD_ROOT / "checkpoints" / "clip-vit-large-patch14",
    ]
    clip_path = next((p for p in clip_candidates if p.exists()), clip_candidates[0])

    # HD model (upper body)
    spec = importlib.util.spec_from_file_location("ootd.inference_ootd_hd", ootd_hd_path)
    mod  = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    mod.VIT_PATH   = str(clip_path)
    mod.VAE_PATH   = str(OOTD_CKPT / "ootd")
    mod.UNET_PATH  = str(OOTD_CKPT / "ootd" / "ootd_hd" / "checkpoint-36000")
    mod.MODEL_PATH = str(OOTD_CKPT / "ootd")
    sys.modules["ootd.inference_ootd_hd"] = mod
    logger.info("Patched HD paths → VIT=%s | UNET=%s", clip_path, OOTD_CKPT / "ootd" / "ootd_hd")

    # DC model (lower body / full outfit)
    ootd_dc_path = str(OOTD_ROOT / "ootd" / "inference_ootd_dc.py")
    if Path(ootd_dc_path).exists():
        spec_dc = importlib.util.spec_from_file_location("ootd.inference_ootd_dc", ootd_dc_path)
        mod_dc  = importlib.util.module_from_spec(spec_dc)
        spec_dc.loader.exec_module(mod_dc)
        mod_dc.VIT_PATH   = str(clip_path)
        mod_dc.VAE_PATH   = str(OOTD_CKPT / "ootd")
        mod_dc.UNET_PATH  = str(OOTD_CKPT / "ootd" / "ootd_dc" / "checkpoint-36000")
        mod_dc.MODEL_PATH = str(OOTD_CKPT / "ootd")
        sys.modules["ootd.inference_ootd_dc"] = mod_dc
        logger.info("Patched DC paths → UNET=%s", OOTD_CKPT / "ootd" / "ootd_dc")


def _run_local_ootd(
    person_path: str,
    garment_path: str,
    category: str,
    model_type: str = "hd",
    num_steps: int  = 20,
    image_scale: float = 2.0,
    seed: int = -1,
) -> str:
    """
    Run local OOTDiffusion inference.
    category: 'upper_body' | 'lower_body' | 'dresses'
    model_type: 'hd' (upper only) | 'dc' (lower/dress)
    """
    _load_models()

    from utils_ootd import get_mask_location

    _util_to_model = {
        "upper_body": "upperbody",
        "lower_body": "lowerbody",
        "dresses":    "dress",
    }
    if category not in _util_to_model:
        raise InferenceError(f"Invalid category '{category}'. Use: upper_body | lower_body | dresses")
    model_category = _util_to_model[category]

    if model_type == "hd" and category != "upper_body":
        raise InferenceError(
            "model_type='hd' only supports category='upper_body'. "
            "Use model_type='dc' for lower_body or dresses."
        )

    logger.info(
        "🖥️  Local OOTDiffusion | model_type=%s | category=%s | steps=%d | scale=%.1f | seed=%d",
        model_type, category, num_steps, image_scale, seed
    )

    try:
        cloth_img = Image.open(garment_path).convert("RGB").resize(
            (GARMENT_W, GARMENT_H), Image.LANCZOS
        )
        model_img = Image.open(person_path).convert("RGB").resize(
            (GARMENT_W, GARMENT_H), Image.LANCZOS
        )

        logger.info("Running OpenPose…")
        keypoints = _openpose_model(model_img.resize((POSE_W, POSE_H)))

        logger.info("Running HumanParsing…")
        model_parse, _ = _parsing_model(model_img.resize((POSE_W, POSE_H)))

        mask, mask_gray = get_mask_location(model_type, category, model_parse, keypoints)
        mask      = mask.resize((GARMENT_W, GARMENT_H), Image.NEAREST)
        mask_gray = mask_gray.resize((GARMENT_W, GARMENT_H), Image.NEAREST)

        masked_vton = Image.composite(mask_gray, model_img, mask)

        logger.info("Running OOTDiffusion denoising (%d steps)…", num_steps)
        images = _ootd_model(
            model_type=model_type,
            category=model_category,
            image_garm=cloth_img,
            image_vton=masked_vton,
            mask=mask,
            image_ori=model_img,
            num_samples=1,
            num_steps=num_steps,
            image_scale=image_scale,
            seed=seed,
        )

        if not images:
            raise InferenceError("OOTDiffusion returned empty image list")

        result_path = str(RESULTS_DIR / f"local_{category}_{int(time.time() * 1000)}.png")
        images[0].save(result_path, "PNG")

        with _quota_lock:
            _stats["local_ok"] += 1

        logger.info("✅ Local inference complete → %s", result_path)

        try:
            import torch
            torch.cuda.empty_cache()
        except Exception:
            pass

        return result_path

    except (ModelNotLoadedError, InferenceError):
        raise
    except Exception as e:
        with _quota_lock:
            _stats["local_err"] += 1
        raise InferenceError(f"Local OOTDiffusion inference failed: {e}") from e


def _run_outfit_local(
    person_path: str,
    top_path: str,
    bottom_path: str,
    num_steps: int = 20,
    image_scale: float = 2.0,
    seed: int = -1,
) -> str:
    """
    Full outfit via two sequential local OOTDiffusion calls.
    Upper result feeds as person image for lower — same GPU, same session.
    """
    _load_models()

    logger.info("🖥️  Local outfit — step 1/2: upper garment (HD)…")
    result_upper = _run_local_ootd(
        person_path=person_path,
        garment_path=top_path,
        category="upper_body",
        model_type="hd",
        num_steps=num_steps,
        image_scale=image_scale,
        seed=seed,
    )

    logger.info("🖥️  Local outfit — step 2/2: lower garment (DC) on upper result…")
    result_outfit = None
    try:
        result_outfit = _run_local_ootd(
            person_path=result_upper,
            garment_path=bottom_path,
            category="lower_body",
            model_type="dc",
            num_steps=num_steps,
            image_scale=image_scale,
            seed=seed + 1,
        )
    finally:
        try:
            os.unlink(result_upper)
        except OSError:
            pass

    return result_outfit


# ══════════════════════════════════════════════════════════════════════════════
# SMART ROUTING  —  HF primary → local fallback
# ══════════════════════════════════════════════════════════════════════════════

def _wait_for_local_ready(timeout: float = 120.0):
    """
    If local models are loading in background, wait up to `timeout` seconds.
    Raises ModelNotLoadedError if they never finished loading.
    """
    global _local_loading
    if _local_loading and not _local_ready_event.is_set():
        logger.info("⏳ Waiting for local models to finish loading (HF quota just hit)…")
        _local_ready_event.wait(timeout=timeout)

    if _model_load_err is not None:
        raise _model_load_err


def _local_is_available() -> bool:
    """True if local OOTDiffusion is loaded or can be loaded (repo exists)."""
    return _ootd_model is not None or OOTD_ROOT.exists()


def _route_ootd(
    person_path: str,
    garment_path: str,
    garment_description: str = "a garment",
    category: str = "upper_body",
    denoise_steps: int = 20,
    seed: int = -1,
) -> tuple[str, str]:
    """
    Smart-route a single-garment try-on request.
    Returns (result_path, backend_name).

    Routing logic:
      1. MOCK_MODE → offline mock
      2. USE_HF_PRIMARY=true AND quota not exhausted → try HF
           QuotaExhaustedError  → mark quota permanently, fall through to local
           ImportError          → gradio_client missing, hard-fail (can't fix at runtime)
           InferenceError / any → log + fall through to local if available, else re-raise
      3. Local (primary when USE_HF_PRIMARY=false, or fallback after any HF failure)
    """
    if MOCK_MODE:
        return _generate_mock(person_path, garment_path), "mock"

    if USE_HF_PRIMARY and not _hf_quota_exhausted:
        try:
            result = _run_hf_ootd(
                person_path=person_path,
                garment_path=garment_path,
                garment_description=garment_description,
                category=category,
                denoise_steps=denoise_steps,
                seed=seed,
            )
            return result, "huggingface"

        except QuotaExhaustedError as qe:
            _mark_quota_exhausted(qe)
            # Fall through to local permanently

        except ImportError as ie:
            # gradio_client not installed — cannot use HF at all
            raise InferenceError(
                f"gradio_client is not installed. Run: pip install gradio_client\n{ie}"
            ) from ie

        except Exception as hf_err:
            # Any other HF failure (wrong API name, network error, model error, etc.)
            # Fall back to local if available; otherwise surface the error.
            if _local_is_available():
                logger.warning(
                    "⚠️  HF Space failed — falling back to local for this request.\n"
                    "    Reason: %s", hf_err,
                )
                with _quota_lock:
                    _stats["hf_err"] += 1
                # Fall through to local below
            else:
                raise InferenceError(
                    f"HF Space failed and no local OOTDiffusion available: {hf_err}"
                ) from hf_err

    # ── Local inference (fallback or primary) ──────────────────────────────────
    if _hf_quota_exhausted or USE_HF_PRIMARY:
        _wait_for_local_ready()
        if _hf_quota_exhausted:
            logger.info(
                "🔄 LOCAL FALLBACK | HF quota exhausted since %s",
                _hf_quota_since.strftime("%Y-%m-%d %H:%M:%S") if _hf_quota_since else "unknown",
            )

    model_type = "hd" if category == "upper_body" else "dc"
    result = _run_local_ootd(
        person_path=person_path,
        garment_path=garment_path,
        category=category,
        model_type=model_type,
        num_steps=denoise_steps,
        seed=seed,
    )
    return result, "local"


def _route_outfit(
    person_path: str,
    top_path: str,
    bottom_path: str,
    denoise_steps: int = 20,
    seed: int = -1,
) -> tuple[str, str]:
    """
    Smart-route a full outfit (top + bottom) request.
    Returns (result_path, backend_name).
    Same fallback logic as _route_ootd.
    """
    if MOCK_MODE:
        return _generate_mock(person_path, top_path, bottom_path), "mock"

    if USE_HF_PRIMARY and not _hf_quota_exhausted:
        try:
            result = _run_hf_outfit(
                person_path=person_path,
                top_path=top_path,
                bottom_path=bottom_path,
                denoise_steps=denoise_steps,
                seed=seed,
            )
            return result, "huggingface"

        except QuotaExhaustedError as qe:
            _mark_quota_exhausted(qe)
            # Fall through to local permanently

        except ImportError as ie:
            raise InferenceError(
                f"gradio_client is not installed. Run: pip install gradio_client\n{ie}"
            ) from ie

        except Exception as hf_err:
            if _local_is_available():
                logger.warning(
                    "⚠️  HF Space failed (outfit) — falling back to local for this request.\n"
                    "    Reason: %s", hf_err,
                )
                with _quota_lock:
                    _stats["hf_err"] += 1
                # Fall through to local below
            else:
                raise InferenceError(
                    f"HF Space failed and no local OOTDiffusion available: {hf_err}"
                ) from hf_err

    # ── Local inference ────────────────────────────────────────────────────────
    if _hf_quota_exhausted or USE_HF_PRIMARY:
        _wait_for_local_ready()
        if _hf_quota_exhausted:
            logger.info(
                "🔄 LOCAL FALLBACK (outfit) | HF quota exhausted since %s",
                _hf_quota_since.strftime("%Y-%m-%d %H:%M:%S") if _hf_quota_since else "unknown",
            )

    result = _run_outfit_local(
        person_path=person_path,
        top_path=top_path,
        bottom_path=bottom_path,
        num_steps=denoise_steps,
        seed=seed,
    )
    return result, "local"


# ══════════════════════════════════════════════════════════════════════════════
# GARMENT UTILITIES
# ══════════════════════════════════════════════════════════════════════════════

def _remove_background(image_path: str) -> Image.Image:
    try:
        from rembg import remove
        with open(image_path, "rb") as f:
            img_bytes = f.read()
        return Image.open(io.BytesIO(remove(img_bytes))).convert("RGBA")
    except ImportError:
        logger.warning("rembg not installed — skipping background removal")
        return Image.open(image_path).convert("RGBA")
    except Exception as e:
        logger.warning("Background removal failed (%s) — using original", e)
        return Image.open(image_path).convert("RGBA")


def _normalize_garment(image_path: str) -> str:
    """Remove background + letterbox to GARMENT_W×GARMENT_H on white canvas."""
    rgba = _remove_background(image_path)
    canvas = Image.new("RGB", (GARMENT_W, GARMENT_H), (255, 255, 255))
    rgba.thumbnail((GARMENT_W, GARMENT_H), Image.LANCZOS)
    ox = (GARMENT_W - rgba.width)  // 2
    oy = (GARMENT_H - rgba.height) // 2
    canvas.paste(rgba, (ox, oy), rgba.split()[3])
    out = tempfile.NamedTemporaryFile(delete=False, suffix="_normalized.png")
    canvas.save(out.name, "PNG")
    out.close()
    logger.info("Garment normalized → %dx%d at %s", GARMENT_W, GARMENT_H, out.name)
    return out.name


def _save_upload(upload: UploadFile, suffix: str = ".jpg") -> str:
    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
    shutil.copyfileobj(upload.file, tmp)
    tmp.close()
    return tmp.name


# ══════════════════════════════════════════════════════════════════════════════
# MOCK  (dev only — MOCK_INFERENCE=true)
# ══════════════════════════════════════════════════════════════════════════════

def _generate_mock(person_path: str, *garment_paths: str) -> str:
    """
    Offline mock for development only.
    NEVER called automatically as fallback — only when MOCK_INFERENCE=true.
    """
    person = Image.open(person_path).convert("RGB")
    draw   = ImageDraw.Draw(person)
    for i, gp in enumerate(garment_paths):
        garment = Image.open(gp).convert("RGB")
        thumb   = garment.resize((person.width // 4, person.height // 4))
        person.paste(thumb, (person.width - thumb.width - 10, 10 + i * (thumb.height + 5)))
    draw.text((10, person.height - 30), "MOCK TRY-ON v5", fill=(255, 80, 80))
    out = str(RESULTS_DIR / f"mock_{int(time.time())}.png")
    person.save(out)
    logger.warning("⚠️  Mock result generated (MOCK_INFERENCE=true)")
    return out


# ══════════════════════════════════════════════════════════════════════════════
# STARTUP
# ══════════════════════════════════════════════════════════════════════════════

async def _do_startup():
    """Called by the lifespan context manager at application startup."""
    global _local_loading

    if MOCK_MODE:
        logger.info("🟡 MOCK_INFERENCE=true — all model loading skipped")
        return

    if USE_HF_PRIMARY:
        # Connect to HF Space (fast — just HTTP handshake)
        try:
            _get_hf_client()
            logger.info("🌐 HF Space ready as PRIMARY backend")
        except Exception as e:
            logger.warning(
                "⚠️  HF Space connection failed at startup (%s). "
                "Will retry on first request. Local fallback will be used if HF unavailable.", e
            )

        # Load local models in background so they're ready when quota runs out.
        # This avoids a 60-second cold-start on the FIRST fallback request.
        if OOTD_ROOT.exists():
            logger.info(
                "📦 Starting background local model loading "
                "(will be ready when HF quota runs out)…"
            )
            _local_loading = True

            def _bg_load():
                global _local_loading
                try:
                    _load_models()
                except ModelNotLoadedError as e:
                    logger.warning("⚠️  Local fallback will be unavailable: %s", e)
                finally:
                    _local_loading = False

            bg = threading.Thread(target=_bg_load, daemon=True, name="local-model-loader")
            bg.start()
        else:
            logger.warning(
                "Local OOTDiffusion repo not found at %s. "
                "Only HF Space will be available (no local fallback).", OOTD_ROOT
            )
    else:
        # Local primary mode: eager load (fail fast)
        logger.info("🖥️  USE_HF_PRIMARY=false — loading local models eagerly…")
        try:
            _load_models()
        except ModelNotLoadedError as e:
            logger.error("❌ STARTUP FAILED — local models not ready: %s", e)
            logger.error(
                "Fix: clone repo and download checkpoints, then restart.\n"
                "  git clone https://github.com/levihsu/OOTDiffusion %s\n"
                "  python download_models.py", OOTD_ROOT
            )


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINTS
# ══════════════════════════════════════════════════════════════════════════════

@app.get("/health")
async def health():
    """Full system status: both backends, quota state, call counts."""
    hf_client_ok = _hf_client is not None
    local_ready  = _ootd_model is not None

    return {
        "status":       "ok" if (hf_client_ok or local_ready or MOCK_MODE) else "degraded",
        "version":      "5.0",
        "active_backend": _active_backend(),

        # ── HuggingFace backend ──
        "huggingface": {
            "space_id":        HF_SPACE_ID,
            "enabled":         USE_HF_PRIMARY,
            "client_ready":    hf_client_ok,
            "quota_exhausted": _hf_quota_exhausted,
            "quota_since":     _hf_quota_since.isoformat() if _hf_quota_since else None,
            "connect_error":   str(_hf_connect_error) if _hf_connect_error else None,
        },

        # ── Local backend ──
        "local": {
            "ootd_root":   str(OOTD_ROOT),
            "checkpoints": str(OOTD_CKPT),
            "model_ready": local_ready,
            "loading":     _local_loading,
            "load_error":  str(_model_load_err) if _model_load_err else None,
        },

        # ── Call stats ──
        "stats": _stats,

        "mock_mode": MOCK_MODE,
    }


@app.get("/quota/status")
async def quota_status():
    """Current quota state and backend selection."""
    return {
        "active_backend":       _active_backend(),
        "hf_space_id":          HF_SPACE_ID,
        "use_hf_primary":       USE_HF_PRIMARY,
        "hf_quota_exhausted":   _hf_quota_exhausted,
        "hf_quota_since":       _hf_quota_since.isoformat() if _hf_quota_since else None,
        "local_model_ready":    _ootd_model is not None,
        "local_model_loading":  _local_loading,
        "stats":                _stats,
        "hint": (
            "POST /quota/reset to retry HuggingFace (use when your monthly quota resets)"
            if _hf_quota_exhausted else
            "HuggingFace quota is OK"
        ),
    }


@app.post("/quota/reset")
async def quota_reset():
    """
    Reset the quota-exhausted flag so the next request retries HuggingFace.
    Use this when your HF Space quota has reset (typically monthly).
    """
    global _hf_quota_exhausted, _hf_quota_since, _hf_client, _hf_connect_error

    if not USE_HF_PRIMARY:
        return JSONResponse(
            status_code=400,
            content={"error": "USE_HF_PRIMARY=false — HF Space is disabled, nothing to reset"}
        )

    old_state = _hf_quota_exhausted
    with _quota_lock:
        _hf_quota_exhausted = False
        _hf_quota_since     = None
        # Also reset cached client so a fresh connection is made
        _hf_client          = None
        _hf_connect_error   = None

    logger.info(
        "🔄 Quota flag reset by /quota/reset (was: %s → now: False). "
        "Next request will try HuggingFace again.", old_state
    )

    return {
        "reset": True,
        "previous_quota_exhausted": old_state,
        "active_backend": _active_backend(),
        "message": "HuggingFace will be tried on the next request",
    }


@app.post("/preprocess/garment")
async def preprocess_garment(garment_image: UploadFile = File(...)):
    """Remove background + normalize garment to GARMENT_W×GARMENT_H white canvas."""
    raw_path = _save_upload(garment_image, ".png")
    try:
        normalized = _normalize_garment(raw_path)
        return FileResponse(normalized, media_type="image/png", filename="garment_processed.png")
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Garment preprocessing failed: {e}")
    finally:
        try:
            os.unlink(raw_path)
        except OSError:
            pass


@app.post("/tryon")
async def tryon(
    person_image:        UploadFile = File(...),
    garment_image:       UploadFile = File(...),
    garment_description: str        = Form("a garment"),
    category:            str        = Form("upper_body"),
    denoise_steps:       int        = Form(20),
    seed:                int        = Form(-1),
):
    """
    Single-garment try-on.
    Tries HuggingFace Space first; falls back to local on quota exhaustion.
    category: 'upper_body' | 'lower_body' | 'dresses'
    """
    person_path  = _save_upload(person_image)
    garment_path = _save_upload(garment_image)
    result_path  = None

    try:
        result_path, backend = _route_ootd(
            person_path=person_path,
            garment_path=garment_path,
            garment_description=garment_description,
            category=category,
            denoise_steps=denoise_steps,
            seed=seed,
        )
        logger.info("✅ /tryon complete | backend=%s | result=%s", backend, result_path)

        from starlette.background import BackgroundTask

        def _cleanup():
            for p in [person_path, garment_path, result_path]:
                try:
                    if p and os.path.exists(p):
                        os.unlink(p)
                except OSError:
                    pass

        response = FileResponse(
            result_path,
            media_type="image/png",
            filename="tryon_result.png",
            background=BackgroundTask(_cleanup),
        )
        response.headers["X-Backend"] = backend
        return response

    except ModelNotLoadedError as e:
        logger.error("Local model not loaded: %s", e)
        raise HTTPException(
            status_code=503,
            detail={
                "error":   "Local OOTDiffusion model is not loaded",
                "reason":  str(e),
                "fix":     f"Clone repo to {OOTD_ROOT} and run download_models.py, then restart.",
                "backend": "local",
            },
        )
    except InferenceError as e:
        logger.error("Inference failed: %s", e)
        raise HTTPException(
            status_code=500,
            detail={"error": "Inference failed", "reason": str(e)},
        )
    except Exception as e:
        logger.exception("Unexpected error in /tryon")
        raise HTTPException(status_code=500, detail={"error": "Unexpected error", "reason": str(e)})
    finally:
        if result_path is None:
            for p in [person_path, garment_path]:
                try:
                    os.unlink(p)
                except OSError:
                    pass


@app.post("/tryon/outfit")
async def tryon_outfit(
    person_image:         UploadFile = File(...),
    top_garment_image:    UploadFile = File(...),
    bottom_garment_image: UploadFile = File(...),
    top_description:      str        = Form("a top garment"),
    bottom_description:   str        = Form("a bottom garment"),
    denoise_steps:        int        = Form(20),
    seed:                 int        = Form(-1),
):
    """
    Full outfit try-on (upper + lower).
    Tries HuggingFace Space first; falls back to local on quota exhaustion.

    Pipeline:
      1. Normalize both garments (rembg + letterbox)
      2. HF Space OR local: person + top → result_upper
      3. HF Space OR local: result_upper + bottom → final outfit
    """
    person_path     = _save_upload(person_image)
    top_raw_path    = _save_upload(top_garment_image)
    bottom_raw_path = _save_upload(bottom_garment_image)
    top_path        = None
    bottom_path     = None
    result_path     = None

    try:
        logger.info("Normalizing garments…")
        top_path    = _normalize_garment(top_raw_path)
        bottom_path = _normalize_garment(bottom_raw_path)

        result_path, backend = _route_outfit(
            person_path=person_path,
            top_path=top_path,
            bottom_path=bottom_path,
            denoise_steps=denoise_steps,
            seed=seed,
        )
        logger.info("✅ /tryon/outfit complete | backend=%s | result=%s", backend, result_path)

        from starlette.background import BackgroundTask

        cleanup_inputs = [p for p in [
            person_path, top_raw_path, bottom_raw_path, top_path, bottom_path
        ] if p]

        def _cleanup():
            for p in cleanup_inputs:
                try:
                    if os.path.exists(p):
                        os.unlink(p)
                except OSError:
                    pass
            try:
                if result_path and os.path.exists(result_path):
                    os.unlink(result_path)
            except OSError:
                pass

        response = FileResponse(
            result_path,
            media_type="image/png",
            filename="outfit_result.png",
            background=BackgroundTask(_cleanup),
        )
        response.headers["X-Backend"] = backend
        return response

    except ModelNotLoadedError as e:
        logger.error("Local model not loaded: %s", e)
        raise HTTPException(
            status_code=503,
            detail={
                "error":   "Local OOTDiffusion model is not loaded",
                "reason":  str(e),
                "fix":     f"Clone repo to {OOTD_ROOT} and run download_models.py, then restart.",
                "backend": "local",
            },
        )
    except InferenceError as e:
        logger.error("Inference failed: %s", e)
        raise HTTPException(
            status_code=500,
            detail={"error": "Inference failed", "reason": str(e)},
        )
    except Exception as e:
        logger.exception("Unexpected error in /tryon/outfit")
        raise HTTPException(status_code=500, detail={"error": "Unexpected error", "reason": str(e)})
    finally:
        if result_path is None:
            for p in [person_path, top_raw_path, bottom_raw_path, top_path, bottom_path]:
                if p:
                    try:
                        os.unlink(p)
                    except OSError:
                        pass


# ── Main ───────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", "8081"))
    mode = "MOCK" if MOCK_MODE else (
        f"HF_PRIMARY={'quota_exhausted' if _hf_quota_exhausted else HF_SPACE_ID}"
        if USE_HF_PRIMARY else "LOCAL_ONLY"
    )
    logger.info(
        "Starting Virtual Try-On Bridge v5 | port=%d | mode=%s | ootd_root=%s",
        port, mode, OOTD_ROOT
    )
    uvicorn.run(app, host="0.0.0.0", port=port)