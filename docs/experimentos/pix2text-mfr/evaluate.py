import argparse
import hashlib
import importlib.metadata
import json
import platform
import sys
import time
import urllib.request
from pathlib import Path

import numpy as np
import onnxruntime as ort
import psutil
from PIL import Image, ImageOps
from tokenizers import Tokenizer

MODEL_ID = "breezedeus/pix2text-mfr-1.5"
MODEL_REVISION = "1cef9f0bdcd6a4c63df7de1311fb0894593340cc"
MAX_NEW_TOKENS = 1023
SAMPLE_REVISION = "f52e671247c14a29aa59b44e99849e00aee984cd"
ROOT = Path(__file__).resolve().parent
MODEL_DIR = ROOT / "cache" / "model"
SAMPLE_NAMES = ("formula3.png", "hw-formula1.png", "hw-formula2.png", "hw-formula3.png")
MODEL_FILES = (
    "config.json", "generation_config.json", "preprocessor_config.json",
    "tokenizer.json", "tokenizer_config.json", "special_tokens_map.json",
    "encoder_model.onnx", "decoder_model.onnx",
)


def read_json(file_path):
    return json.loads(file_path.read_text(encoding="utf-8"))


def file_hash(file_path):
    with file_path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def download(url, target, expected_size=None, expected_hash=None):
    target.parent.mkdir(parents=True, exist_ok=True)
    if not target.exists():
        partial = target.with_suffix(target.suffix + ".partial")
        request = urllib.request.Request(url, headers={"User-Agent": "Glifo-MFR-local-evaluation"})
        with urllib.request.urlopen(request, timeout=120) as response, partial.open("wb") as output:
            while chunk := response.read(1024 * 1024):
                output.write(chunk)
        if expected_size is not None and partial.stat().st_size != expected_size:
            raise ValueError(f"Unexpected file size: {target.name}")
        if expected_hash and file_hash(partial) != expected_hash:
            raise ValueError(f"Unexpected SHA256: {target.name}")
        partial.replace(target)
    if expected_size is not None and target.stat().st_size != expected_size:
        raise ValueError(f"Invalid cached size: {target.name}")
    if expected_hash and file_hash(target) != expected_hash:
        raise ValueError(f"Invalid cached SHA256: {target.name}")
    return {"file": target.relative_to(ROOT).as_posix(), "url": url, "bytes": target.stat().st_size,
            "sha256": file_hash(target)}


def prepare():
    api_url = f"https://huggingface.co/api/models/{MODEL_ID}/revision/{MODEL_REVISION}?blobs=true"
    with urllib.request.urlopen(api_url, timeout=60) as response:
        metadata = json.load(response)
    if metadata["sha"] != MODEL_REVISION:
        raise ValueError("Model revision mismatch")
    entries = {entry["rfilename"]: entry for entry in metadata["siblings"]}
    provenance = {"model_id": MODEL_ID, "model_revision": MODEL_REVISION,
                  "sample_revision": SAMPLE_REVISION, "downloads": []}
    for filename in MODEL_FILES:
        entry = entries[filename]
        record = download(f"https://huggingface.co/{MODEL_ID}/resolve/{MODEL_REVISION}/{filename}",
                          MODEL_DIR / filename, entry["size"], entry.get("lfs", {}).get("sha256"))
        provenance["downloads"].append(record)
        print(f"Ready: {filename}", flush=True)
    for filename in SAMPLE_NAMES:
        url = f"https://raw.githubusercontent.com/breezedeus/Pix2Text/{SAMPLE_REVISION}/docs/examples/{filename}"
        provenance["downloads"].append(download(url, ROOT / "samples" / filename))
    (ROOT / "cache" / "provenance.json").write_text(json.dumps(provenance, indent=2), encoding="utf-8")


def preprocess(image, config):
    if not config["do_resize"] or config["do_center_crop"]:
        raise ValueError("This experiment supports the pinned model's resize-only processor")
    image = ImageOps.exif_transpose(image)
    if "A" in image.getbands() or "transparency" in image.info:
        foreground = image.convert("RGBA")
        background = Image.new("RGBA", image.size, "white")
        image = Image.alpha_composite(background, foreground).convert("RGB")
    else:
        image = image.convert("RGB")
    image = image.resize((config["size"]["width"], config["size"]["height"]), config["resample"])
    pixels = np.asarray(image, dtype=np.float32)
    if config["do_rescale"]:
        pixels = (pixels.astype(np.float64) * config["rescale_factor"]).astype(np.float32)
    if config["do_normalize"]:
        pixels = (pixels - np.asarray(config["image_mean"], dtype=np.float32)) / np.asarray(config["image_std"], dtype=np.float32)
    return np.ascontiguousarray(pixels.transpose(2, 0, 1)[None])


def verify_model():
    provenance = read_json(ROOT / "cache" / "provenance.json")
    if provenance["model_id"] != MODEL_ID or provenance["model_revision"] != MODEL_REVISION:
        raise ValueError("Unexpected model provenance")
    records = {entry["file"].replace("\\", "/"): entry for entry in provenance["downloads"]}
    hashes = {}
    for filename in MODEL_FILES:
        model_path = MODEL_DIR / filename
        digest = file_hash(model_path)
        if digest != records[model_path.relative_to(ROOT).as_posix()]["sha256"]:
            raise ValueError(f"Model file changed since preparation: {filename}")
        hashes[filename] = digest
    return hashes


def recognize(encoder, decoder, tokenizer, image_path, processor_config, generation_config, max_tokens, timeout):
    started = time.perf_counter()
    with Image.open(image_path) as image:
        image_size = list(image.size)
        pixels = preprocess(image, processor_config)
    encoded = encoder.run(None, {"pixel_values": pixels})[0]
    tokens = [generation_config["decoder_start_token_id"]]
    status = "token_limit"
    for step in range(max_tokens):
        if time.perf_counter() - started > timeout:
            status = "time_limit"
            break
        logits = decoder.run(None, {"input_ids": np.asarray([tokens], dtype=np.int64),
                                    "encoder_hidden_states": encoded})[0]
        token = int(np.argmax(logits[0, -1]))
        tokens.append(token)
        if token == generation_config["eos_token_id"]:
            status = "complete"
            break
    latex = tokenizer.decode(tokens, skip_special_tokens=True)
    if status == "complete" and not latex.strip():
        status = "empty"
    return {"file": image_path.name, "sha256": file_hash(image_path), "image_size": image_size,
            "latex": latex, "status": status, "generated_tokens": len(tokens) - 1,
            "seconds": round(time.perf_counter() - started, 3), "reference_latex": None,
            "confidence": None, "latex_compilation_checked": False}


def evaluate(arguments):
    if arguments.max_tokens < 1 or arguments.timeout <= 0 or arguments.threads < 1:
        raise ValueError("Token, timeout and thread limits must be positive")
    if arguments.max_tokens > MAX_NEW_TOKENS:
        raise ValueError(f"The pinned decoder supports at most {MAX_NEW_TOKENS} new tokens")
    if psutil.virtual_memory().available < 512 * 1024 * 1024:
        raise RuntimeError("Less than 512 MiB available; close applications before inference")
    image_paths = [Path(image).resolve() for image in arguments.images] if arguments.images else [ROOT / "samples" / name for name in SAMPLE_NAMES]
    for image_path in image_paths:
        if not image_path.is_file():
            raise FileNotFoundError(image_path)
    output_path = Path(arguments.output).resolve()
    if output_path.exists():
        raise FileExistsError(f"Choose a new results filename: {output_path}")
    model_hashes = verify_model()
    settings = ort.SessionOptions()
    settings.intra_op_num_threads = arguments.threads
    settings.inter_op_num_threads = 1
    settings.execution_mode = ort.ExecutionMode.ORT_SEQUENTIAL
    settings.log_severity_level = 3
    started = time.perf_counter()
    encoder = ort.InferenceSession(str(MODEL_DIR / "encoder_model.onnx"), settings, providers=["CPUExecutionProvider"])
    decoder = ort.InferenceSession(str(MODEL_DIR / "decoder_model.onnx"), settings, providers=["CPUExecutionProvider"])
    load_seconds = time.perf_counter() - started
    tokenizer = Tokenizer.from_file(str(MODEL_DIR / "tokenizer.json"))
    processor_config = read_json(MODEL_DIR / "preprocessor_config.json")
    generation_config = read_json(MODEL_DIR / "generation_config.json")
    report = {"model_id": MODEL_ID, "model_revision": MODEL_REVISION, "provider": "CPUExecutionProvider",
              "model_file_sha256": model_hashes,
              "python": platform.python_version(), "platform": platform.platform(), "threads": arguments.threads,
              "max_new_tokens": arguments.max_tokens, "per_image_timeout_seconds": arguments.timeout,
              "model_load_seconds": round(load_seconds, 3), "decoding": "greedy_without_cache",
              "sample_origin": "user_supplied_local" if arguments.images else "upstream_public_examples",
              "accuracy_measured": False, "not_a_calibrated_confidence": True,
              "packages": {name: importlib.metadata.version(name) for name in ("numpy", "onnxruntime", "pillow", "psutil", "tokenizers")},
              "requested_images": [image_path.name for image_path in image_paths],
              "batch_complete": False, "results": []}
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    for image_path in image_paths:
        result = recognize(encoder, decoder, tokenizer, image_path, processor_config, generation_config,
                           arguments.max_tokens, arguments.timeout)
        report["results"].append(result)
        memory = psutil.Process().memory_info()
        report["process_rss_bytes"] = memory.rss
        report["process_peak_working_set_bytes"] = getattr(memory, "peak_wset", None)
        output_path.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
        print(json.dumps(result, ensure_ascii=True), flush=True)
    report["batch_complete"] = True
    output_path.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"Results: {output_path}", flush=True)
    return 0 if all(result["status"] == "complete" for result in report["results"]) else 2


def main():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("prepare")
    runner = subparsers.add_parser("run")
    runner.add_argument("images", nargs="*")
    runner.add_argument("--output", required=True)
    runner.add_argument("--max-tokens", type=int, default=256)
    runner.add_argument("--timeout", type=float, default=30)
    runner.add_argument("--threads", type=int, default=2)
    arguments = parser.parse_args()
    if arguments.command == "prepare":
        prepare()
        return 0
    return evaluate(arguments)


if __name__ == "__main__":
    sys.exit(main())
