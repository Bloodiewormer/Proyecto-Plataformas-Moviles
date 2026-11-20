# Pix2Text-MFR: local CPU experiment

This is an isolated feasibility experiment for frontend issue #38, implemented outside both application repositories. It does not implement MathOcrEngine, change Android, deploy a service, or change the issue's provider decision.

## Scope

- Model: breezedeus/pix2text-mfr-1.5, revision 1cef9f0bdcd6a4c63df7de1311fb0894593340cc.
- Public examples: breezedeus/Pix2Text, revision f52e671247c14a29aa59b44e99849e00aee984cd.
- Python 3.12.13, CPUExecutionProvider, two inference threads.
- ONNX encoder/decoder, greedy autoregressive decoding without cache, maximum 256 new tokens.
- Resize-only DeiT preprocessing follows the pinned processor configuration: RGB, 384x384 bicubic, rescale by 1/255, mean/std 0.5, channel-first float32.
- Before the pinned processor steps, this local crop adapter applies EXIF orientation and composites transparent pixels over white. These are explicit input-adapter additions, not claims about the upstream processor.
- No PyTorch, external inference API, private image upload, application dependency changes or database access.
- Downloads use public HTTPS sources. Running inference uses only local files. No remote model code is executed.
- CPU model/configuration files occupy 119,654,633 bytes. The Python environment is additional disk usage.

## Reproduce in PowerShell

Run from this directory. The environment has already been created locally; creation and installation commands are for reproduction only.

~~~powershell
uv venv --python 3.12 .venv
uv pip install --python .venv/Scripts/python.exe --requirements requirements.txt
.\.venv\Scripts\python.exe evaluate.py prepare
.\.venv\Scripts\python.exe -m unittest -v test_evaluate
.\.venv\Scripts\python.exe evaluate.py run --output results/new-run.json
~~~

For an existing local crop:

~~~powershell
.\.venv\Scripts\python.exe evaluate.py run 'C:/path/to/formula.png' --output results/my-crop.json
~~~

Choose a fresh output filename: existing result files are not overwritten. Do not place student names or sensitive information in samples intended for publication.

The prepare command downloads only fixed model files and four public example images. It verifies model revision, model-file sizes and the published LFS SHA256 for ONNX weights. JSON files and samples rely on the commit-pinned HTTPS URLs and trust on first use; their locally recorded SHA256 values do not independently authenticate the source. Inference detects model-file changes against cache/provenance.json before loading. Running prepare again regenerates that local record, so it must not be treated as an independent trust anchor. No global Python installation is modified.

The current prepare implementation was run again after review, revalidating cached files and regenerating provenance with forward-slash paths. This was not a fresh download of all files.

## Observed results

Latest recorded run after review fixes: results/public-smoke-cpu-reviewed.json. Earlier runs remain available under their original filenames.

- Four images completed generation: one printed formula and three handwritten public demos.
- Model loading: 1.524 seconds.
- Per-image end-to-end processing, excluding model loading: printed formula 7.329 seconds; handwritten examples 1.680, 3.262 and 3.413 seconds.
- Process peak working set: 272,703,488 bytes, about 260 MiB. This is Windows process memory, not total machine usage or a deployment sizing guarantee.
- Eighteen local runner unit tests passed. They cover pixel normalization/channel order, asymmetric geometry and bicubic resampling, EXIF orientation, RGBA/palette transparency, EOS/empty output, token/time limits, invalid arguments, file hashing, changed model-file rejection and incomplete batches. They do not measure OCR accuracy.
- Earlier smoke runs also completed; the latest run additionally records requested_images and batch_complete=true. Timings are individual observations under current machine load, not a benchmark distribution.

## Qualitative inspection, not an accuracy score

The three handwritten images were opened locally and compared visually with generated LaTeX.

- hw-formula1.png: the integral and its bounds appear to match the source.
- hw-formula2.png: the integral expression appears consistent at the available image resolution; the handwritten characters remain a source of ambiguity.
- hw-formula3.png: the cubic derivative term contains dy/dx in the image, but the model emits cx/dx. The output also adds a trailing period after = 0 that is not visible in the source. The remainder has not been verified against an independent reference.
- These are upstream demonstration images, not an independent test set and not our students' notes. They may overlap model development data. Do not publish a general accuracy percentage from this run.

## Limits and next decision

- status=complete means that the decoder emitted EOS, not that the formula is correct or that LaTeX compiles.
- requested_images lists the intended batch and batch_complete becomes true only after every requested image has been processed. An exception leaves any saved batch marked incomplete. This flag does not override individual token/time-limit statuses.
- max-tokens must be between 1 and 1023, reserving the start token within the pinned decoder positional limit.
- confidence is deliberately null. Token probabilities or successful parsing must not be presented as calibrated recognition confidence.
- No LaTeX compiler was available in the checked command path. Compilation, semantic equivalence and correction quality were not validated.
- Timeout is cooperative, checked between decoder invocations; an individual native inference call is not forcibly interrupted. This is an offline experiment, not an untrusted-input service.
- The preprocessing/greedy decoder is a minimal direct ONNX implementation, not a parity-certified replacement for the upstream Python package. It implements the pinned configuration only.
- Model weights, samples, environment and outputs are excluded by this directory's .gitignore. The resolved transitive environment is recorded locally in results/environment-freeze.txt, also excluded; requirements.txt pins only direct dependencies. uv pip show confirmed the installed httpx2, httpcore2 and huggingface-hub versions. These artifacts are available locally, not in an application diff; share redacted evidence explicitly if publication is later authorized. The workspace root is not a configured publication repository; do not stage it broadly. This experiment is not part of backend PR #10.
- Keep the current backend/frontend worktrees unchanged. No commit, push, issue edit, PR, deployment or dependency addition to either app was performed for this experiment.

Proposed next step: evaluate 30-50 consented real formula crops with independently transcribed references, including matrices, fractions, integrals, subscripts and difficult handwriting. Measure transcription accuracy and latency before selecting this provider for N1.5. The error observed here supports retaining rejection/uncertainty and the N2 fallback; a syntactically valid formula alone is not enough.

## Primary sources

- https://huggingface.co/breezedeus/pix2text-mfr-1.5/blob/1cef9f0bdcd6a4c63df7de1311fb0894593340cc/README.md
- https://huggingface.co/breezedeus/pix2text-mfr-1.5/blob/1cef9f0bdcd6a4c63df7de1311fb0894593340cc/preprocessor_config.json
- https://github.com/huggingface/transformers/blob/v4.52.3/src/transformers/models/deit/image_processing_deit.py
- https://github.com/breezedeus/Pix2Text/tree/f52e671247c14a29aa59b44e99849e00aee984cd/docs/examples
