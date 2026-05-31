"""
Download CatVTON weights - resumable, no symlinks.
Run: python download_models.py
"""
import os
import sys
from pathlib import Path

os.environ["HF_HUB_DISABLE_SYMLINKS_WARNING"] = "1"

BASE_DIR = Path(__file__).parent

# SD Inpainting base model
SD_DIR = BASE_DIR / "weights" / "stable-diffusion-inpainting"
# CatVTON attention weights
CATVTON_DIR = BASE_DIR / "weights" / "CatVTON"
# VAE
VAE_DIR = BASE_DIR / "weights" / "sd-vae-ft-mse"


def download_file(repo_id, filename, local_dir, token=None):
    from huggingface_hub import hf_hub_download
    dest = Path(local_dir) / filename
    if dest.exists() and dest.stat().st_size > 1024:
        print(f"  skip {filename}")
        return
    dest.parent.mkdir(parents=True, exist_ok=True)
    print(f"  downloading {filename}...")
    hf_hub_download(
        repo_id=repo_id,
        filename=filename,
        local_dir=str(local_dir),
        local_dir_use_symlinks=False,
        token=token,
    )
    print(f"  done {filename}")


def download():
    try:
        from huggingface_hub import hf_hub_download, list_repo_files
    except ImportError:
        print("pip install huggingface_hub")
        sys.exit(1)

    token = os.getenv("HF_TOKEN")

    # ── 1. VAE (~335MB) ──────────────────────────────────────────────
    print("\n[1/3] VAE (stabilityai/sd-vae-ft-mse) ~335MB")
    for f in ["config.json", "diffusion_pytorch_model.safetensors"]:
        try:
            download_file("stabilityai/sd-vae-ft-mse", f, VAE_DIR, token)
        except Exception as e:
            print(f"  WARN {f}: {e}")

    # ── 2. SD Inpainting UNet + scheduler (~3.4GB) ────────────────────
    print("\n[2/3] SD Inpainting (runwayml/stable-diffusion-inpainting) ~3.4GB")
    sd_files = [
        "scheduler/scheduler_config.json",
        "unet/config.json",
        "unet/diffusion_pytorch_model.bin",
    ]
    for f in sd_files:
        try:
            download_file("runwayml/stable-diffusion-inpainting", f, SD_DIR, token)
        except Exception as e:
            print(f"  WARN {f}: {e}")

    # ── 3. CatVTON attention weights (~900MB) ─────────────────────────
    print("\n[3/3] CatVTON (zhengchong/CatVTON) ~900MB")
    try:
        all_files = list(list_repo_files("zhengchong/CatVTON", token=token))
    except Exception as e:
        print(f"Cannot list CatVTON files: {e}")
        sys.exit(1)

    ok = skip = fail = 0
    for i, f in enumerate(all_files, 1):
        try:
            dest = CATVTON_DIR / f
            if dest.exists() and dest.stat().st_size > 100:
                print(f"  [{i}/{len(all_files)}] skip {f}")
                skip += 1
                continue
            download_file("zhengchong/CatVTON", f, CATVTON_DIR, token)
            ok += 1
        except KeyboardInterrupt:
            print(f"\nInterrupted at {i}/{len(all_files)}. Run again to resume.")
            sys.exit(0)
        except Exception as e:
            print(f"  FAIL {f}: {e}")
            fail += 1

    print(f"\nDone: {ok} downloaded, {skip} skipped, {fail} failed")
    if fail == 0:
        print("All weights ready.")


if __name__ == "__main__":
    download()
