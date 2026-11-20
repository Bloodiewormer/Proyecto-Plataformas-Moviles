import unittest
import tempfile
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import Mock, patch

import numpy as np
from PIL import Image, ImageDraw

from evaluate import SOURCE_REVISION, evaluate, file_hash, preprocess, recognize, resolve_generation_config


class TexTellerExperimentTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        import torch

        torch.set_num_threads(2)

    def test_white_image_is_rejected_instead_of_hallucinating(self):
        with self.assertRaises(ValueError):
            preprocess(Image.new("RGB", (30, 20), "white"))

    def test_tensor_shape_and_normalization(self):
        image = Image.new("RGB", (100, 40), "white")
        ImageDraw.Draw(image).line((10, 10, 90, 30), fill="black", width=3)
        pixels = preprocess(image)
        self.assertEqual(pixels.shape, (1, 1, 448, 448))
        self.assertEqual(pixels.dtype, np.float32)
        self.assertTrue(np.isfinite(pixels).all())
        self.assertLess(float(pixels.min()), -5)
        self.assertTrue(np.all(pixels[0, 0, -1] == 0))

    def test_transparent_canvas_keeps_black_ink(self):
        image = Image.new("RGBA", (60, 40), (0, 0, 0, 0))
        ImageDraw.Draw(image).line((10, 10, 50, 30), fill=(0, 0, 0, 255), width=3)
        pixels = preprocess(image)
        self.assertLess(float(pixels.min()), -5)
        self.assertEqual(pixels.shape, (1, 1, 448, 448))

    def test_palette_transparency_keeps_black_ink(self):
        image = Image.new("P", (60, 40), 0)
        image.putpalette([0] * 768)
        image.info["transparency"] = 0
        ImageDraw.Draw(image).line((10, 10, 50, 30), fill=1, width=3)
        self.assertLess(float(preprocess(image).min()), -5)

    def test_limit_is_validated_before_large_model_loading(self):
        for max_tokens in (0, 1024):
            with self.subTest(max_tokens=max_tokens), self.assertRaises(ValueError):
                evaluate(SimpleNamespace(max_tokens=max_tokens, timeout=180, threads=2))

    def test_extreme_foreground_ratio_is_rejected(self):
        image = Image.new("RGB", (902, 5), "white")
        ImageDraw.Draw(image).rectangle((1, 1, 900, 2), fill="black")
        with self.assertRaisesRegex(ValueError, "aspect ratio"):
            preprocess(image)


class DecoderAdapterTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.root_patch = patch("evaluate.ROOT", self.root)
        self.root_patch.start()
        self.addCleanup(self.root_patch.stop)
        self.image_path = self.root / "sample.png"
        Image.new("RGB", (8, 5), "white").save(self.image_path)
        self.tensor_path = self.root / "cache" / "inputs" / "sample.png.npy"
        self.tensor_path.parent.mkdir(parents=True)
        np.save(self.tensor_path, np.zeros((1, 1, 448, 448), dtype=np.float32))
        self.records = {"sample.png": {"image_sha256": file_hash(self.image_path),
                                       "tensor_sha256": file_hash(self.tensor_path), "seconds": 0}}
        self.encoder = Mock()
        self.encoder.run.return_value = [np.zeros((1, 1, 1), dtype=np.float32)]
        self.decoder = Mock()
        self.decoder.run.return_value = [np.array([[[0, 0, 1, 0]]], dtype=np.float32)]
        self.tokenizer = Mock()
        self.tokenizer.decode.return_value = "x"

    def run_recognition(self, max_tokens=2):
        return recognize(self.encoder, self.decoder, self.tokenizer, self.image_path, self.records,
                         {"decoder_start_token_id": 0, "eos_token_id": 2}, max_tokens, 1)

    def test_start_resolution_preserves_raw_config(self):
        raw = {"bos_token_id": 0, "decoder_start_token_id": None}
        effective, source = resolve_generation_config(raw)
        self.assertEqual(effective["decoder_start_token_id"], 0)
        self.assertIn("fallback", source)
        self.assertIsNone(raw["decoder_start_token_id"])
        self.assertEqual(resolve_generation_config({"bos_token_id": 0})[0]["decoder_start_token_id"], 0)

    def test_explicit_start_and_cli_override(self):
        raw = {"bos_token_id": 0, "decoder_start_token_id": 2}
        self.assertEqual(resolve_generation_config(raw)[0]["decoder_start_token_id"], 2)
        effective, source = resolve_generation_config(raw, 0)
        self.assertEqual(effective["decoder_start_token_id"], 0)
        self.assertEqual(source, "explicit_cli_override")

    def test_eos_completes_and_records_tensor_hash(self):
        result = self.run_recognition()
        self.assertEqual(result["status"], "complete")
        self.assertEqual(result["generated_tokens"], 1)
        self.assertEqual(result["tensor_sha256"], file_hash(self.tensor_path))
        np.testing.assert_array_equal(self.decoder.run.call_args.args[1]["input_ids"], [[0]])

    def test_eos_with_empty_text_is_not_success(self):
        self.tokenizer.decode.return_value = " "
        self.assertEqual(self.run_recognition()["status"], "empty")

    def test_token_limit_is_not_complete(self):
        self.decoder.run.return_value = [np.array([[[0, 0, 0, 1]]], dtype=np.float32)]
        self.assertEqual(self.run_recognition(max_tokens=1)["status"], "token_limit")

    def test_timeout_stops_before_decoder(self):
        with patch("evaluate.time.perf_counter", side_effect=[0, 2, 2]):
            self.assertEqual(self.run_recognition()["status"], "time_limit")
        self.decoder.run.assert_not_called()

    def test_changed_tensor_is_rejected(self):
        self.records["sample.png"]["tensor_sha256"] = "different"
        with self.assertRaisesRegex(ValueError, "tensor has changed"):
            self.run_recognition()
        self.encoder.run.assert_not_called()

    def test_nonfinite_timeout_is_rejected_before_model_loading(self):
        for timeout in (float("nan"), float("inf")):
            with self.subTest(timeout=timeout), self.assertRaises(ValueError):
                evaluate(SimpleNamespace(max_tokens=256, timeout=timeout, threads=2))

    def test_unprepared_image_is_rejected_before_session_creation(self):
        unknown_image = self.root / "other.png"
        Image.new("RGB", (8, 5), "white").save(unknown_image)
        manifest = {"source_revision": SOURCE_REVISION,
                    "inputs": [{"file": "sample.png", **self.records["sample.png"]}]}
        arguments = SimpleNamespace(max_tokens=256, timeout=180, threads=2,
                                    images=[str(unknown_image)], output=str(self.root / "report.json"))
        with patch("evaluate.psutil.virtual_memory", return_value=SimpleNamespace(available=3 * 1024**3)), \
             patch("evaluate.read_json", return_value=manifest), \
             patch("evaluate.ort.InferenceSession") as session:
            with self.assertRaisesRegex(ValueError, "exact prepared public images"):
                evaluate(arguments)
        session.assert_not_called()
        self.assertFalse((self.root / "report.json").exists())


if __name__ == "__main__":
    unittest.main()
