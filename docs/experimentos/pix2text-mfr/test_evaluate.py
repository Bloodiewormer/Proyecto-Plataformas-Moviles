import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import Mock, patch

import numpy as np
from PIL import Image

from evaluate import MODEL_ID, MODEL_REVISION, evaluate, file_hash, preprocess, recognize, verify_model


class ExperimentTest(unittest.TestCase):
    def setUp(self):
        self.config = {"do_resize": True, "do_center_crop": False,
                       "size": {"width": 384, "height": 384}, "resample": 3,
                       "do_rescale": True, "rescale_factor": 1 / 255,
                       "do_normalize": True, "image_mean": [0.5] * 3, "image_std": [0.5] * 3}
        self.generation = {"decoder_start_token_id": 1, "eos_token_id": 2}

    def test_white_image_matches_processor_range_and_shape(self):
        pixels = preprocess(Image.new("RGB", (80, 30), "white"), self.config)
        self.assertEqual(pixels.shape, (1, 3, 384, 384))
        self.assertEqual(pixels.dtype, np.float32)
        np.testing.assert_array_equal(pixels, np.ones_like(pixels))

    def test_black_grayscale_image_converts_to_rgb(self):
        pixels = preprocess(Image.new("L", (20, 40), 0), self.config)
        np.testing.assert_array_equal(pixels, -np.ones_like(pixels))

    def test_unsupported_center_crop_is_not_silently_ignored(self):
        self.config["do_center_crop"] = True
        with self.assertRaises(ValueError):
            preprocess(Image.new("RGB", (10, 10)), self.config)

    def test_rgb_channels_are_normalized_without_reordering(self):
        pixels = preprocess(Image.new("RGB", (20, 10), (255, 0, 0)), self.config)
        np.testing.assert_array_equal(pixels[0, :, 0, 0], [1, -1, -1])

    def test_rgba_black_ink_is_composited_on_white(self):
        image = Image.new("RGBA", (2, 1), (0, 0, 0, 0))
        image.putpixel((1, 0), (0, 0, 0, 255))
        self.config["size"] = {"width": 2, "height": 1}
        pixels = preprocess(image, self.config)
        np.testing.assert_array_equal(pixels[0, :, 0, 0], [1, 1, 1])
        np.testing.assert_array_equal(pixels[0, :, 0, 1], [-1, -1, -1])

    def test_palette_transparency_is_composited_on_white(self):
        image = Image.new("P", (2, 1))
        image.putpalette([0] * 768)
        image.putdata([0, 1])
        image.info["transparency"] = 0
        self.config["size"] = {"width": 2, "height": 1}
        pixels = preprocess(image, self.config)
        np.testing.assert_array_equal(pixels[0, :, 0, 0], [1, 1, 1])
        np.testing.assert_array_equal(pixels[0, :, 0, 1], [-1, -1, -1])

    def test_asymmetric_image_preserves_spatial_and_channel_order(self):
        source = np.asarray([[[255, 0, 0], [0, 255, 0]],
                             [[0, 0, 255], [255, 255, 0]],
                             [[255, 0, 255], [0, 255, 255]]], dtype=np.uint8)
        self.config["size"] = {"width": 2, "height": 3}
        pixels = preprocess(Image.fromarray(source), self.config)
        self.assertEqual(pixels.shape, (1, 3, 3, 2))
        expected = ((source.astype(np.float64) / 255 - 0.5) / 0.5).transpose(2, 0, 1)
        np.testing.assert_allclose(pixels[0], expected, atol=1e-6)

    def test_nonuniform_image_uses_configured_bicubic_resampling(self):
        image = Image.new("RGB", (2, 3), "white")
        image.putpixel((0, 0), (0, 0, 0))
        self.config["size"] = {"width": 5, "height": 7}
        resized = np.asarray(image.resize((5, 7), Image.Resampling.BICUBIC), dtype=np.float64)
        expected = ((resized / 255 - 0.5) / 0.5).transpose(2, 0, 1)
        np.testing.assert_allclose(preprocess(image, self.config)[0], expected, atol=1e-6)

    def test_exif_orientation_is_applied_before_resize(self):
        image = Image.new("RGB", (2, 1), "red")
        image.putpixel((1, 0), (0, 0, 255))
        image.getexif()[274] = 6
        self.config["size"] = {"width": 1, "height": 2}
        pixels = preprocess(image, self.config)
        np.testing.assert_array_equal(pixels[0, :, 0, 0], [1, -1, -1])
        np.testing.assert_array_equal(pixels[0, :, 1, 0], [-1, -1, 1])

    def test_decoder_position_limit_is_checked_before_loading(self):
        with self.assertRaises(ValueError):
            evaluate(SimpleNamespace(max_tokens=1024, timeout=30, threads=2))

    def test_model_integrity_rejects_modified_cached_config(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            model_dir = root / "cache" / "model"
            model_dir.mkdir(parents=True)
            config_path = model_dir / "config.json"
            config_path.write_text("{}", encoding="utf-8")
            provenance = {"model_id": MODEL_ID, "model_revision": MODEL_REVISION,
                          "downloads": [{"file": "cache/model/config.json", "sha256": file_hash(config_path)}]}
            (root / "cache" / "provenance.json").write_text(json.dumps(provenance), encoding="utf-8")
            with patch.multiple("evaluate", ROOT=root, MODEL_DIR=model_dir, MODEL_FILES=("config.json",)):
                self.assertEqual(verify_model(), {"config.json": file_hash(config_path)})
                config_path.write_text("[]", encoding="utf-8")
                with self.assertRaises(ValueError):
                    verify_model()

    def prediction(self, generated_token, max_tokens=2, timeout=30, decoded="x"):
        with tempfile.TemporaryDirectory() as temporary:
            image_path = Path(temporary) / "sample.png"
            Image.new("RGB", (20, 10), "white").save(image_path)
            encoder = Mock()
            encoder.run.return_value = [np.zeros((1, 1, 1), dtype=np.float32)]
            decoder = Mock()
            logits = np.zeros((1, 1, 5), dtype=np.float32)
            logits[0, 0, generated_token] = 1
            decoder.run.return_value = [logits]
            tokenizer = Mock()
            tokenizer.decode.return_value = decoded
            return recognize(encoder, decoder, tokenizer, image_path, self.config, self.generation,
                             max_tokens, timeout)

    def test_end_token_marks_completion_without_inventing_confidence(self):
        result = self.prediction(2)
        self.assertEqual(result["status"], "complete")
        self.assertEqual(result["generated_tokens"], 1)
        self.assertIsNone(result["confidence"])
        self.assertIsNone(result["reference_latex"])
        self.assertFalse(result["latex_compilation_checked"])

    def test_token_limit_does_not_report_success(self):
        result = self.prediction(4, max_tokens=1)
        self.assertEqual(result["status"], "token_limit")

    def test_timeout_does_not_report_success(self):
        result = self.prediction(4, timeout=-1)
        self.assertEqual(result["status"], "time_limit")
        self.assertEqual(result["generated_tokens"], 0)

    def test_empty_completion_does_not_report_success(self):
        self.assertEqual(self.prediction(2, decoded=" ")["status"], "empty")

    def test_invalid_limits_are_rejected_before_model_load(self):
        with self.assertRaises(ValueError):
            evaluate(SimpleNamespace(max_tokens=0, timeout=30, threads=2))

    def test_failed_batch_keeps_partial_report_explicitly_incomplete(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            first_image = root / "first.png"
            second_image = root / "second.png"
            first_image.write_bytes(b"first")
            second_image.write_bytes(b"second")
            output = root / "results.json"
            arguments = SimpleNamespace(max_tokens=2, timeout=30, threads=2,
                                        images=[str(first_image), str(second_image)], output=str(output))
            with patch("evaluate.verify_model", return_value={}), \
                 patch("evaluate.ort.InferenceSession"), \
                 patch("evaluate.Tokenizer.from_file"), \
                 patch("evaluate.read_json", side_effect=[self.config, self.generation]), \
                 patch("evaluate.recognize", side_effect=[{"file": "first.png", "status": "complete"}, ValueError("bad image")]):
                with self.assertRaises(ValueError):
                    evaluate(arguments)
            report = json.loads(output.read_text(encoding="utf-8"))
            self.assertFalse(report["batch_complete"])
            self.assertEqual(report["requested_images"], ["first.png", "second.png"])
            self.assertEqual(len(report["results"]), 1)

    def test_file_hash_is_sha256(self):
        with tempfile.TemporaryDirectory() as temporary:
            image_path = Path(temporary) / "empty"
            image_path.write_bytes(b"")
            self.assertEqual(file_hash(image_path),
                             "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")


if __name__ == "__main__":
    unittest.main()
