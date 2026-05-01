"""
Python Bridge Server for Virtual Try-On.

Bridges Spring Boot ↔ Hugging Face Space (yisol/IDM-VTON).
This server does NOT need a GPU — it only forwards requests.

Usage:
    pip install -r requirements.txt
    python main.py
"""

import os
import shutil
import tempfile
import time
import logging
from pathlib import Path

from fastapi import FastAPI, File, Form, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse

# ── App setup ────────────────────────────────────────────
app = FastAPI(title="Virtual Try-On Bridge", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("tryon-bridge")

# ── Config ───────────────────────────────────────────────
HF_SPACE_ID = os.environ.get("HF_SPACE_ID", "yisol/IDM-VTON")
HF_TOKEN = os.environ.get("HF_TOKEN", None)
MOCK_MODE = os.environ.get("MOCK_INFERENCE", "false").lower() == "true"
RESULTS_DIR = Path("results")
RESULTS_DIR.mkdir(exist_ok=True)


def _save_upload(upload: UploadFile, suffix: str = ".jpg") -> str:
    """Save an uploaded file to a temp path and return it."""
    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
    shutil.copyfileobj(upload.file, tmp)
    tmp.close()
    return tmp.name


def _generate_mock(person_path: str, garment_path: str) -> str:
    """Return the person image as-is (mock / offline demo)."""
    from PIL import Image, ImageDraw, ImageFont

    person = Image.open(person_path).convert("RGB")
    garment = Image.open(garment_path).convert("RGB")

    # Overlay a small garment thumbnail on the corner + watermark
    garment_thumb = garment.resize((person.width // 4, person.height // 4))
    person.paste(garment_thumb, (person.width - garment_thumb.width - 10, 10))

    draw = ImageDraw.Draw(person)
    draw.text(
        (10, person.height - 30),
        "MOCK TRY-ON — HF Space unavailable",
        fill=(255, 80, 80),
    )

    result_path = str(RESULTS_DIR / f"mock_{int(time.time())}.png")
    person.save(result_path)
    return result_path


def _generate_hf(person_path: str, garment_path: str,
                 garment_des: str, category: str,
                 denoise_steps: int, seed: int) -> str:
    """Call the real IDM-VTON Hugging Face Space via Gradio Client."""
    from gradio_client import Client, handle_file

    # Authenticate with HF if token is available
    if HF_TOKEN:
        try:
            from huggingface_hub import login
            login(token=HF_TOKEN, add_to_git_credential=False)
            logger.info("Authenticated with HuggingFace token")
        except Exception as e:
            logger.warning("HF login failed: %s", e)

    # Map category for IDM-VTON
    # IDM-VTON expects: "upper_body", "lower_body", or "dresses"
    is_checked = True   # Use auto-crop
    is_checked_crop = False

    max_retries = 2
    for attempt in range(max_retries + 1):
        client = None
        try:
            logger.info("Attempt %d/%d — Connecting to HF Space: %s (token=%s, category=%s)",
                        attempt + 1, max_retries + 1, HF_SPACE_ID,
                        "YES" if HF_TOKEN else "NO", category)
            client = Client(HF_SPACE_ID)

            logger.info("Calling /tryon endpoint (this may take 60-180s)...")
            result = client.predict(
                dict={"background": handle_file(person_path), "layers": [], "composite": None},
                garm_img=handle_file(garment_path),
                garment_des=garment_des,
                is_checked=is_checked,
                is_checked_crop=is_checked_crop,
                denoise_steps=denoise_steps,
                seed=seed,
                api_name="/tryon",
            )

            # ── Extract the FIRST output image path only ──
            result_path = _extract_first_image(result)
            logger.info("Try-on complete: %s", result_path)

            # Copy result to our results dir so it persists
            final_path = str(RESULTS_DIR / f"tryon_{int(time.time())}.png")
            shutil.copy2(str(result_path), final_path)
            return final_path

        except Exception as e:
            logger.warning("Attempt %d failed: %s", attempt + 1, str(e))
            if attempt < max_retries:
                logger.info("Retrying in 5 seconds...")
                time.sleep(5)
            else:
                # All retries failed — fallback to mock
                logger.error("All %d attempts failed. Falling back to MOCK mode.", max_retries + 1)
                return _generate_mock(person_path, garment_path)
        finally:
            # ── Close client to stop heartbeat threads ──
            if client is not None:
                try:
                    client.close()
                except Exception:
                    pass


def _extract_first_image(result) -> str:
    """Recursively extract the first file path from a Gradio result."""
    if isinstance(result, str):
        return result
    if isinstance(result, Path):
        return str(result)
    if isinstance(result, dict):
        # Some Gradio results come as {"path": "...", "url": "...", ...}
        if "path" in result:
            return str(result["path"])
        if "url" in result:
            return str(result["url"])
    if isinstance(result, (list, tuple)) and len(result) > 0:
        return _extract_first_image(result[0])
    return str(result)


# ── Endpoints ────────────────────────────────────────────

@app.get("/health")
async def health():
    return {"status": "ok", "mode": "mock" if MOCK_MODE else "huggingface",
            "hf_space": HF_SPACE_ID}


@app.post("/tryon")
async def tryon(
    person_image: UploadFile = File(...),
    garment_image: UploadFile = File(...),
    garment_description: str = Form("a garment"),
    category: str = Form("upper_body"),
    denoise_steps: int = Form(30),
    seed: int = Form(42),
):
    """
    Generate a virtual try-on image.
    - person_image: full-body photo of the person
    - garment_image: product garment image (flat-lay or ghost mannequin)
    - category: upper_body / lower_body / full_body
    """
    person_path = _save_upload(person_image)
    garment_path = _save_upload(garment_image)
    result_path = None

    try:
        if MOCK_MODE:
            logger.info("MOCK mode — returning composite image")
            result_path = _generate_mock(person_path, garment_path)
        else:
            result_path = _generate_hf(
                person_path, garment_path,
                garment_description, category,
                denoise_steps, seed,
            )

        from starlette.background import BackgroundTask

        def _cleanup():
            """Delete temp files after response is sent."""
            for p in [person_path, garment_path, result_path]:
                try:
                    if p and os.path.exists(p):
                        os.unlink(p)
                        logger.debug("Cleaned up: %s", p)
                except OSError:
                    pass

        return FileResponse(
            result_path,
            media_type="image/png",
            filename="tryon_result.png",
            background=BackgroundTask(_cleanup),
        )

    except Exception as e:
        logger.exception("Try-on failed")
        # Clean up on error
        for p in [person_path, garment_path]:
            try:
                os.unlink(p)
            except OSError:
                pass
        if result_path:
            try:
                os.unlink(result_path)
            except OSError:
                pass
        raise HTTPException(status_code=500, detail=f"Try-on failed: {str(e)}")


@app.post("/preprocess/garment")
async def preprocess_garment(garment_image: UploadFile = File(...)):
    """
    Basic garment preprocessing: validate and save.
    In a production system this would do background removal via rembg,
    but for the HF Space approach the model handles it internally.
    """
    garment_path = _save_upload(garment_image, ".png")

    try:
        from PIL import Image

        img = Image.open(garment_path).convert("RGB")
        logger.info("Garment image: %dx%d", img.width, img.height)

        # Just validate it's a real image — HF Space handles preprocessing
        result_path = str(RESULTS_DIR / f"garment_{int(time.time())}.png")
        img.save(result_path, "PNG")

        return FileResponse(
            result_path,
            media_type="image/png",
            filename="garment_processed.png",
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid image: {str(e)}")
    finally:
        try:
            os.unlink(garment_path)
        except OSError:
            pass


@app.post("/tryon/outfit")
async def tryon_outfit(
    person_image: UploadFile = File(...),
    garment_images: list[UploadFile] = File(...),
    garment_descriptions: str = Form(""),
    garment_categories: str = Form(""),
    denoise_steps: int = Form(30),
    seed: int = Form(42),
):
    """
    Generate a full outfit try-on by chaining multiple garments sequentially.
    - person_image: full-body photo
    - garment_images: list of garment images (applied in order: top → bottom → etc.)
    - garment_descriptions: comma-separated descriptions (optional)
    - garment_categories: comma-separated categories e.g. "upper_body,lower_body" (optional)
    """
    person_path = _save_upload(person_image)
    garment_paths = [_save_upload(g) for g in garment_images]
    descriptions = [d.strip() for d in garment_descriptions.split(",")] if garment_descriptions else []
    categories = [c.strip() for c in garment_categories.split(",")] if garment_categories else []
    temp_files = [person_path] + garment_paths
    result_path = None

    try:
        current_person = person_path

        for i, g_path in enumerate(garment_paths):
            desc = descriptions[i] if i < len(descriptions) else "a garment"
            category = categories[i] if i < len(categories) else "upper_body"
            logger.info("Outfit step %d/%d: applying garment (category=%s)", i + 1, len(garment_paths), category)

            if MOCK_MODE:
                step_result = _generate_mock(current_person, g_path)
            else:
                step_result = _generate_hf(
                    current_person, g_path, desc, category,
                    denoise_steps, seed,
                )

            # Track for cleanup
            temp_files.append(step_result)

            # Use this result as person for next step
            if current_person != person_path:
                pass  # Already tracked
            current_person = step_result

        result_path = current_person

        from starlette.background import BackgroundTask

        def _cleanup():
            for p in set(temp_files):
                try:
                    if p and os.path.exists(p):
                        os.unlink(p)
                except OSError:
                    pass

        return FileResponse(
            result_path,
            media_type="image/png",
            filename="outfit_result.png",
            background=BackgroundTask(_cleanup),
        )

    except Exception as e:
        logger.exception("Outfit try-on failed")
        for p in set(temp_files):
            try:
                if p and os.path.exists(p):
                    os.unlink(p)
            except OSError:
                pass
        raise HTTPException(status_code=500, detail=f"Outfit try-on failed: {str(e)}")


# ── Main ─────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", "8081"))
    logger.info("Starting Try-On Bridge on port %d (mode=%s)",
                port, "MOCK" if MOCK_MODE else "HuggingFace")
    uvicorn.run(app, host="0.0.0.0", port=port)
