"""
One-time setup: download all OOTDiffusion checkpoints.

Run from python-tryon-server/ directory:
    python download_models.py

What it downloads:
  1. levihsu/OOTDiffusion  → OOTDiffusion/checkpoints/checkpoints/  (~8GB)
       ootd/ootd_hd/   ← half-body UNet (for tops)
       ootd/ootd_dc/   ← full-body UNet (for bottoms/dress)
       ootd/vae/       ← VAE
       humanparsing/   ← ONNX parsing models
       openpose/       ← body pose model
  2. openai/clip-vit-large-patch14  → OOTDiffusion/checkpoints/checkpoints/clip-vit-large-patch14/  (~2GB)
"""
import sys
from pathlib import Path
from huggingface_hub import snapshot_download

SCRIPT_DIR  = Path(__file__).parent
OOTD_ROOT   = SCRIPT_DIR / "OOTDiffusion"

# snapshot_download of levihsu/OOTDiffusion puts files at:
#   OOTDiffusion/checkpoints/checkpoints/ootd/
#   OOTDiffusion/checkpoints/checkpoints/humanparsing/
#   OOTDiffusion/checkpoints/checkpoints/openpose/
# (double-nested because the HF repo contains a 'checkpoints/' subfolder)
OUTER_CKPT  = OOTD_ROOT / "checkpoints"
INNER_CKPT  = OUTER_CKPT / "checkpoints"   # ← main.py's OOTD_CKPT points here
CLIP_DIR    = INNER_CKPT / "clip-vit-large-patch14"


def download(repo_id: str, local_dir: Path, desc: str):
    print(f"\n{'='*60}")
    print(f"  Downloading: {desc}")
    print(f"  Source : huggingface.co/{repo_id}")
    print(f"  Target : {local_dir}")
    print(f"{'='*60}")
    local_dir.mkdir(parents=True, exist_ok=True)
    snapshot_download(
        repo_id=repo_id,
        local_dir=str(local_dir),
        local_dir_use_symlinks=False,
    )
    print(f"  ✅ Done: {desc}\n")


if __name__ == "__main__":
    print("=" * 60)
    print("  OOTDiffusion Model Downloader")
    print(f"  Checkpoints target: {INNER_CKPT}")
    print("=" * 60)

    if not OOTD_ROOT.exists():
        print("\n❌ ERROR: OOTDiffusion repo not found.")
        print(f"   Expected at: {OOTD_ROOT}")
        print("   Run first: git clone https://github.com/levihsu/OOTDiffusion OOTDiffusion")
        sys.exit(1)

    # 1. OOTDiffusion model weights (~8GB)
    #    This downloads INTO OOTDiffusion/checkpoints/ which creates the
    #    checkpoints/checkpoints/ double-nesting (because HF repo has checkpoints/ inside it)
    download(
        repo_id="levihsu/OOTDiffusion",
        local_dir=OUTER_CKPT,
        desc="OOTDiffusion weights (~8GB)"
    )

    # 2. CLIP ViT-L/14 — required for garment visual encoding
    download(
        repo_id="openai/clip-vit-large-patch14",
        local_dir=CLIP_DIR,
        desc="CLIP ViT-L/14 (~2GB)"
    )

    # Verify expected structure exists
    print("\nVerifying checkpoint structure…")
    checks = {
        "OOTDiffusion HD UNet":  INNER_CKPT / "ootd" / "ootd_hd",
        "OOTDiffusion DC UNet":  INNER_CKPT / "ootd" / "ootd_dc",
        "Human Parsing (ONNX)":  INNER_CKPT / "humanparsing",
        "OpenPose":              INNER_CKPT / "openpose",
        "CLIP ViT":              CLIP_DIR,
    }
    all_ok = True
    for name, path in checks.items():
        ok = path.exists()
        print(f"  {'✅' if ok else '❌'} {name}: {path}")
        if not ok:
            all_ok = False

    print()
    if all_ok:
        print("✅ All models ready. Start the server:")
        print("   python main.py")
    else:
        print("❌ Some checkpoints missing. Re-run this script or check your connection.")
        sys.exit(1)
