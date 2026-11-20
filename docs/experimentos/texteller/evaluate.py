import argparse
from collections import Counter
import hashlib
import importlib.metadata
import json
import math
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

MODEL_ID = "OleehyO/TexTeller"
MODEL_REVISION = "7b96df06b9d81cdb129c3bef68b7250bc3e2b0ea"
SOURCE_REVISION = "9b88cec77bda735aa16f9fc7e4ccb4eb1500a8b2"
MAX_NEW_TOKENS = 1023
SAMPLE_REVISION = "f52e671247c14a29aa59b44e99849e00aee984cd"
ROOT = Path(__file__).resolve().parent
MODEL_DIR = ROOT / "cache" / "model"
SAMPLE_NAMES = ("formula3.png", "hw-formula1.png", "hw-formula2.png", "hw-formula3.png")
MODEL_FILES = (
    "config.json", "generation_config.json",
    "tokenizer.json", "tokenizer_config.json", "special_tokens_map.json",
    "encoder_model.onnx", "decoder_model.onnx",
)


def read_json(file_path):
    return json.loads(file_path.read_text(encoding="utf-8"))


def file_hash(file_path):
    with file_path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def resolve_generation_config(raw_config, override=None):
    effective = dict(raw_config)
    if override is not None:
        effective["decoder_start_token_id"] = override
        source = "explicit_cli_override"
    elif effective.get("decoder_start_token_id") is not None:
        source = "generation_config.json.decoder_start_token_id"
    else:
        effective["decoder_start_token_id"] = effective["bos_token_id"]
        source = "fallback_to_generation_config.json.bos_token_id"
    return effective, source


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


def preprocess(image):
    import cv2
    import torch
    from torchvision.transforms import v2

    image = ImageOps.exif_transpose(image)
    if "A" in image.getbands() or "transparency" in image.info:
        image = Image.alpha_composite(Image.new("RGBA", image.size, "white"), image.convert("RGBA"))
    pixels = np.asarray(image.convert("RGB"))
    corners = [tuple(pixels[0, 0]), tuple(pixels[0, -1]), tuple(pixels[-1, 0]), tuple(pixels[-1, -1])]
    background = np.full_like(pixels, Counter(corners).most_common(1)[0][0])
    difference = cv2.cvtColor(cv2.absdiff(pixels, background), cv2.COLOR_BGR2GRAY)
    mask = cv2.threshold(difference, 15, 255, cv2.THRESH_BINARY)[1]
    left, top, width, height = cv2.boundingRect(mask)
    if width == 0 or height == 0:
        raise ValueError("No foreground detected in image")
    if max(width, height) > 448 * min(width, height):
        raise ValueError("Foreground aspect ratio exceeds the resize limit")
    cropped = pixels[top:top + height, left:left + width].copy(order="C")
    pipeline = v2.Compose([
        v2.ToImage(), v2.ToDtype(torch.uint8, scale=True), v2.Grayscale(),
        v2.Resize(size=447, interpolation=v2.InterpolationMode.BICUBIC, max_size=448, antialias=True),
        v2.ToDtype(torch.float32, scale=True), v2.Normalize(mean=[0.9545467], std=[0.15394445]),
    ])
    tensor = pipeline(cropped)
    tensor = v2.functional.pad(tensor, padding=[0, 0, 448 - tensor.shape[2], 448 - tensor.shape[1]])
    return np.ascontiguousarray(tensor.numpy()[None])


def prepare_inputs():
    import torch

    torch.set_num_threads(2)
    input_directory = ROOT / "cache" / "inputs"
    input_directory.mkdir(parents=True, exist_ok=True)
    manifest = {"source_revision": SOURCE_REVISION, "inputs": [],
                "packages": {name: importlib.metadata.version(name) for name in ("torch", "torchvision", "opencv-python-headless")}}
    for filename in SAMPLE_NAMES:
        image_path = ROOT / "samples" / filename
        started = time.perf_counter()
        with Image.open(image_path) as image:
            pixels = preprocess(image)
        seconds = time.perf_counter() - started
        tensor_path = input_directory / (filename + ".npy")
        np.save(tensor_path, pixels, allow_pickle=False)
        manifest["inputs"].append({"file": filename, "image_sha256": file_hash(image_path),
                                   "tensor_sha256": file_hash(tensor_path), "seconds": round(seconds, 3)})
        print(f"Prepared input: {filename}", flush=True)
    (input_directory / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")


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
        record = processor_config[image_path.name]
        if file_hash(image_path) != record["image_sha256"]:
            raise ValueError("Prepared input image has changed")
        tensor_path = ROOT / "cache" / "inputs" / (image_path.name + ".npy")
        if file_hash(tensor_path) != record["tensor_sha256"]:
            raise ValueError("Prepared input tensor has changed")
        pixels = np.load(tensor_path, allow_pickle=False)
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
            "tensor_sha256": record["tensor_sha256"],
            "latex": latex, "status": status, "generated_tokens": len(tokens) - 1,
            "seconds": round(time.perf_counter() - started, 3),
            "preprocessing_seconds": record["seconds"], "reference_latex": None,
            "confidence": None, "latex_compilation_checked": False}


def evaluate(arguments):
    if arguments.max_tokens < 1 or not math.isfinite(arguments.timeout) or arguments.timeout <= 0 or arguments.threads < 1:
        raise ValueError("Token, timeout and thread limits must be positive")
    if arguments.max_tokens > MAX_NEW_TOKENS:
        raise ValueError(f"This experiment uses a conservative bound of {MAX_NEW_TOKENS} new tokens")
    if psutil.virtual_memory().available < 2 * 1024 * 1024 * 1024:
        raise RuntimeError("Less than 2 GiB available; close applications before TexTeller inference")
    image_paths = [Path(image).resolve() for image in arguments.images] if arguments.images else [ROOT / "samples" / name for name in SAMPLE_NAMES]
    for image_path in image_paths:
        if not image_path.is_file():
            raise FileNotFoundError(image_path)
    output_path = Path(arguments.output).resolve()
    if output_path.exists():
        raise FileExistsError(f"Choose a new results filename: {output_path}")
    input_manifest = read_json(ROOT / "cache" / "inputs" / "manifest.json")
    if input_manifest["source_revision"] != SOURCE_REVISION:
        raise ValueError("Unexpected input preprocessing revision")
    processor_config = {record["file"]: record for record in input_manifest["inputs"]}
    for image_path in image_paths:
        record = processor_config.get(image_path.name)
        if record is None or file_hash(image_path) != record["image_sha256"]:
            raise ValueError("Only the exact prepared public images are supported")
        tensor_path = ROOT / "cache" / "inputs" / (image_path.name + ".npy")
        if file_hash(tensor_path) != record["tensor_sha256"]:
            raise ValueError("Prepared input tensor has changed")
    model_hashes = verify_model()
    settings = ort.SessionOptions()
    settings.intra_op_num_threads = arguments.threads
    settings.inter_op_num_threads = 1
    settings.execution_mode = ort.ExecutionMode.ORT_SEQUENTIAL
    settings.log_severity_level = 3
    settings.enable_cpu_mem_arena = False
    settings.enable_mem_pattern = False
    settings.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_BASIC
    started = time.perf_counter()
    encoder = ort.InferenceSession(str(MODEL_DIR / "encoder_model.onnx"), settings, providers=["CPUExecutionProvider"])
    decoder = ort.InferenceSession(str(MODEL_DIR / "decoder_model.onnx"), settings, providers=["CPUExecutionProvider"])
    load_seconds = time.perf_counter() - started
    tokenizer = Tokenizer.from_file(str(MODEL_DIR / "tokenizer.json"))
    raw_generation_config = read_json(MODEL_DIR / "generation_config.json")
    generation_config, token_source = resolve_generation_config(
        raw_generation_config, getattr(arguments, "decoder_start_token_id", None))
    report = {"model_id": MODEL_ID, "model_revision": MODEL_REVISION, "provider": "CPUExecutionProvider",
              "model_file_sha256": model_hashes, "preprocessing_source_revision": SOURCE_REVISION,
              "raw_generation_config": raw_generation_config,
              "effective_generation_config": generation_config, "decoder_start_token_source": token_source,
              "preprocessing_packages": input_manifest["packages"],
              "onnx_optimization": "ORT_ENABLE_BASIC; CPU arena and memory pattern disabled",
              "latency_excludes_separate_preprocessing": True, "upstream_postprocessing_applied": False,
              "python": platform.python_version(), "platform": platform.platform(), "threads": arguments.threads,
              "max_new_tokens": arguments.max_tokens, "per_image_timeout_seconds": arguments.timeout,
              "model_load_seconds": round(load_seconds, 3), "decoding": "greedy_without_cache",
              "sample_origin": "upstream_public_examples",
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
    subparsers.add_parser("prepare-inputs")
    runner = subparsers.add_parser("run")
    runner.add_argument("images", nargs="*")
    runner.add_argument("--output", required=True)
    runner.add_argument("--max-tokens", type=int, default=256)
    runner.add_argument("--timeout", type=float, default=180)
    runner.add_argument("--threads", type=int, default=2)
    runner.add_argument("--decoder-start-token-id", type=int, choices=(0, 2))
    arguments = parser.parse_args()
    if arguments.command == "prepare":
        prepare()
        return 0
    if arguments.command == "prepare-inputs":
        prepare_inputs()
        return 0
    return evaluate(arguments)


if __name__ == "__main__":
    sys.exit(main())
