# TexTeller: isolated CPU experiment

Purpose: test the second OCR candidate for N1.5 against the same four public images used for Pix2Text-MFR. This is not a production integration or closure of issue #38.

## Fixed inputs and implementation

- Model repository: OleehyO/TexTeller, revision 7b96df06b9d81cdb129c3bef68b7250bc3e2b0ea. The pinned model card announces TexTeller 2.0; this experiment must not be described as a certified test of TexTeller 3.0.
- Preprocessing source: OleehyO/TexTeller, code revision 9b88cec77bda735aa16f9fc7e4ccb4eb1500a8b2, texteller/utils/image.py and texteller/constants.py.
- Public images: breezedeus/Pix2Text revision f52e671247c14a29aa59b44e99849e00aee984cd, same filenames and bytes as the Pix2Text experiment.
- Preprocessing uses OpenCV corner-background trimming (threshold 15), torchvision grayscale, aspect-preserving bicubic resize with size 447 and max_size 448, normalization mean 0.9545467/std 0.15394445 and bottom/right padding to 448x448 after normalization.
- EXIF orientation and white compositing for transparent inputs are explicit local adapter additions. A writable crop copy avoids passing read-only NumPy storage to torch.
- PyTorch and torchvision CPU are used only in the separate prepare-inputs process. Inference reads precomputed NumPy tensors, verifies their hashes and does not import torch.
- Direct ONNX encoder/decoder execution: CPUExecutionProvider, two threads, greedy decoding without cache, maximum 256 new tokens. The runner resolves the start token from generation_config.json, falling back to its bos_token_id (0), rather than overriding it from the nested decoder config (2). Both variants were investigated; neither establishes package parity.
- ORT_ENABLE_BASIC, CPU arena disabled and memory patterns disabled reduce inference memory pressure. Pix2Text used its default ONNX optimizations: timings are not a controlled comparison of equivalent runtime settings.
- The cooperative per-image timeout is 180 seconds, checked between decoder calls. This is longer than the Pix2Text experiment's 30 seconds; incomplete/time-limited outputs must remain explicit.
- The output is raw tokenizer LaTeX. Upstream style removal and formatting/postprocessing are not applied. Full package parity has not been certified.

## Reproduce in PowerShell

Run from this directory. Environments, model files, tensors, samples and outputs are local and ignored by Git.

~~~powershell
uv venv --python 3.12 .venv
uv pip install --python .venv/Scripts/python.exe --requirements requirements.txt
uv pip install --python .venv/Scripts/python.exe --requirements requirements-preprocess.txt
.\.venv\Scripts\python.exe evaluate.py prepare
.\.venv\Scripts\python.exe -m unittest -v test_evaluate
.\.venv\Scripts\python.exe evaluate.py prepare-inputs
.\.venv\Scripts\python.exe evaluate.py run --output results/new-run.json --max-tokens 256 --timeout 180 --threads 2
~~~

Use a fresh output path. The current prepared-input command supports the fixed public sample set only; this is not yet a general upload endpoint or an Android feature. Do not run preprocessing and large-model inference concurrently on a memory-constrained machine.

Model downloads are pinned to a commit and use HTTPS. Published LFS sizes and SHA256 values are verified; JSON/sample hashes are local trust-on-first-use records, not independent authentication. Inference verifies model fingerprints and the image/tensor hashes recorded during preparation.

## Evidence and limits

- Local results are stored under results; input preprocessing timings and fingerprints are in cache/inputs/manifest.json, download provenance in cache/provenance.json.
- batch_complete indicates whether all requested images were processed, not recognition correctness. Per-image EOS completion is not LaTeX compilation or semantic validation.
- confidence is null; no calibrated recognition confidence or quantitative accuracy is inferred from these public demonstrations.
- Per-image seconds in the inference report exclude the separate preprocessing process. preprocessing_seconds is recorded separately; its first image includes lazy torchvision initialization. Model load time and process peak working set describe only the ONNX inference process, not the total environment or preparation process.
- Five initial preprocessing/limit unit tests cover blank input rejection, shape and normalization, transparency and the generation bound. Model correctness is exercised only by the smoke run, not by these tests.
- The samples come from Pix2Text's public demonstrations and are not an independent balanced benchmark of the two models. No general accuracy percentage or production/provider decision should be made from four images.
- No private images, external inference calls, paid services, database operations, application edits, commits or GitHub mutations are part of this experiment.

## Adapter diagnostic

The first run, results/public-smoke-cpu.json, forced decoder_start_token_id=2 from config.json.decoder. It reached EOS on three handwritten images but timed out on the printed formula after 180.080 s. Its third handwritten output omitted the two-equation system. This is diagnostic evidence for that adapter configuration, not a certified upstream result or a general model-quality conclusion.

The pinned generation_config.json instead has bos_token_id=0 and no decoder_start_token_id. The upstream inference source constructs a GenerationConfig with tokenizer BOS/EOS/PAD but no explicit decoder start. Transformers generation resolution depends on the loaded model generation configuration as well as version. The second run uses the pinned generation-file fallback (0), records the effective configuration explicitly, and remains a direct ONNX experiment, not a verified reproduction of the full package.

After changing the crop to a writable copy, all five tests passed again and prepare-inputs reproduced all four tensor SHA256 values exactly. A first attempt to load the model was safely refused with less than 2 GiB available RAM; it was retried only after the memory guard passed, without changing its threshold or terminating other applications.

## Observed local results

The completed BOS=0 run is results/public-smoke-cpu-bos.json: batch_complete=true and four EOS-complete outputs. Model loading took 4.910 s; peak ONNX-process working set was 1,448,357,888 bytes (about 1.35 GiB). The seven downloaded model/config/tokenizer files total 1,253,912,588 bytes, excluding the environment.

| Public image | TexTeller BOS=0 seconds | Pix2Text seconds | Visual observation on TexTeller |
| --- | ---: | ---: | --- |
| formula3.png (printed) | 75.184 | 7.329 | Full-looking expression; no independent transcription or compilation check |
| hw-formula1.png | 18.000 | 1.680 | Uppercase S transcribed as lowercase s |
| hw-formula2.png | 38.781 | 3.262 | Plausible expression, not scored against an independent reference |
| hw-formula3.png | 12.822 | 3.413 | Two equations collapsed into one; first derivative changed to second derivative |

Pix2Text figures come from ../pix2text-mfr/results/public-smoke-cpu-reviewed.json and use the exact same image SHA256 values. Its process peak was 272,703,488 bytes and model/config files totaled 119,654,633 bytes. Runtime optimization and timeout settings differ; these are observations on this machine, not a controlled speed ranking or accuracy benchmark.

For hw-formula3, TexTeller emitted only \[\frac{d^{2}y}{dx^{2}}+y=3\], whereas the image shows a system starting with dy/dx + y = 3 and a second nonlinear equation. EOS completion did not catch that omission. Pix2Text preserved the two-equation structure but also had recognition errors documented in its own README. Neither output is acceptable as an automatically trusted transcription.

These observations do not justify integrating this TexTeller adapter or closing #38. The practical next gate is a small consented set of real formula crops with independently checked transcriptions, followed by package-parity verification for any selected candidate. No claim about TexTeller 3.0 follows from the older model artifact tested here.

## Independent review and final local checks

Claude completed a static adapter review while the BOS batch was still running. Its statement that the fourth output was pending reflects that earlier snapshot; the writer subsequently verified batch_complete=true and inspected the final image directly. The reviewer did not view images or execute tests, and its review does not certify recognition quality.

Corrections after review: raw and effective generation configuration are now separate, with an explicit decoder_start_token_source. An optional --decoder-start-token-id 0 or 2 permits reproducing both configurations. Reports now include each tensor hash. Unsupported or changed inputs are rejected before loading ONNX sessions. Nonfinite timeouts and extreme crop aspect ratios are rejected; 1023 tokens is documented as a conservative experimental bound.

Fifteen unit tests passed on the final test file: preprocessing, token resolution without mutating raw configuration, explicit override, EOS, empty output, token/time limits, altered tensor and unsupported-image rejection. A fresh real-model single-image run using the final runner completed hw-formula1 in 20.298 s with the same transcription as before, raw config without decoder_start_token_id, effective start=0, explicit fallback source and tensor SHA256. This is results/reviewed-single-cpu.json. The full four-image measurement was not rerun after these reporting/validation corrections; its original files are preserved unmodified. There is no second independent review of these corrections.

The first run was produced by an earlier script revision and did not store token choice or tensor hashes in its JSON. The writer observed matching tensor hashes before/after writable-copy, but the original manifest was not archived: this claim is not independently auditable from the first report. The BOS report field generation_config contains a derived start=0, not the verbatim generation_config.json; only the final single-image report separates raw and effective values. Do not infer provenance retroactively from the old schema.

With batch_complete=false, a report may represent a running or interrupted process; the file alone does not distinguish them. No general user-upload path, upstream PyTorch/ONNX parity, or colored-image preprocessing parity is certified. The model/config pairing and source versions remain experimental.

Preserved local report SHA256 fingerprints (files remain ignored and are not independently authenticated by these hashes):

- results/public-smoke-cpu.json: 93e3578a91ad097ea64378736ce2634b4efb1559a94c519c99585292cf2476ef
- results/public-smoke-cpu-bos.json: 0b1d28a8fbb515634c627c3fb116635a6920aa23cc8a7b216149abdd91b8af05
- results/reviewed-single-cpu.json: 2f3eb95f6989775f939dde5160d7106717afcf3c1de90c9426ccd8e84b107cc3

## Primary sources

- https://huggingface.co/OleehyO/TexTeller/tree/7b96df06b9d81cdb129c3bef68b7250bc3e2b0ea
- https://github.com/OleehyO/TexTeller/blob/9b88cec77bda735aa16f9fc7e4ccb4eb1500a8b2/texteller/utils/image.py
- https://github.com/OleehyO/TexTeller/blob/9b88cec77bda735aa16f9fc7e4ccb4eb1500a8b2/texteller/constants.py
- https://github.com/OleehyO/TexTeller/blob/9b88cec77bda735aa16f9fc7e4ccb4eb1500a8b2/texteller/api/inference.py
- https://github.com/huggingface/transformers/blob/v4.41.2/src/transformers/generation/utils.py (_prepare_generation_config, _prepare_special_tokens and _get_decoder_start_token_id; inspected as source, not executed here)
