package cr.ac.una.glifo.core.pipeline

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.StringRes
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.mlkit.OcrResult
import cr.ac.una.glifo.core.mlkit.RecognizedBlock
import cr.ac.una.glifo.core.mlkit.RecognizedLine
import cr.ac.una.glifo.core.mlkit.TextRecognizerN1
import cr.ac.una.glifo.core.opencv.ImagePreprocessor
import cr.ac.una.glifo.core.opencv.PageSegmenter
import cr.ac.una.glifo.core.opencv.SegmentedRegion
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.model.RegionBounds
import cr.ac.una.glifo.domain.model.RegionInkMetrics
import cr.ac.una.glifo.domain.model.RegionKind
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object CapturePipeline {

    private const val MAX_WORKING_DIMENSION = 1600
    private const val OVERLAP_IOU_THRESHOLD = 0.50f
    private const val LINE_COVERAGE_THRESHOLD = 0.55f

    internal data class CandidateRegion(
        val bounds: RegionBounds,
        val ocr: OcrResult,
        val metrics: RegionInkMetrics? = null,
        val needsRemeasure: Boolean = false
    )

    suspend fun analyze(source: Bitmap, glossary: List<String> = emptyList()): PageAnalysis = withContext(Dispatchers.Default) {
        val startedAt = System.currentTimeMillis()

        val working = PageSegmenter.scaleToMaxDimension(source, MAX_WORKING_DIMENSION)

        val preprocessed = runCatching { ImagePreprocessor.processPipelineN0(working) }
            .getOrDefault(working)

        if (working !== source && working !== preprocessed) working.recycle()

        val ocrStartedAt = System.currentTimeMillis()

        val rawMlKitBlocks = try {
            TextRecognizerN1.recognizeBlocks(preprocessed)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            emptyList()
        }

        val ocrMillis = System.currentTimeMillis() - ocrStartedAt

        val candidates = mutableListOf<CandidateRegion>()

        if (rawMlKitBlocks.isNotEmpty()) {
            val splitBlocks = splitMixedBlocks(rawMlKitBlocks, preprocessed.width) { line ->
                val chars = line.text.count { !it.isWhitespace() }
                if (chars > 16 && line.wordCount > 3) {
                    false
                } else {
                    val extraH = min(14, (line.bounds.height * 0.45f).toInt())
                    val probeBottom = min(preprocessed.height, line.bounds.bottom + extraH)
                    val probeBounds = line.bounds.copy(height = (probeBottom - line.bounds.top).coerceAtLeast(1))
                    val m = runCatching { PageSegmenter.measureBounds(preprocessed, probeBounds) }.getOrNull()
                    m != null && m.maxStrokeMinSpanRatio < RegionClassifier.LARGE_2D_STROKE_THRESHOLD &&
                        (m.horizontalBarCount > 0 || m.tallStrokeCount > 0)
                }
            }

            val (proseBlocks, nonProseBlocks) = partitionProseAndOther(splitBlocks)
            val proseMasks = proseBlocks.flatMap { block ->
                if (block.lines.isEmpty()) listOf(block.bounds) else block.lines.map { it.bounds }
            }

            // Segment only unmasked non-prose ink so nearby text lines cannot glue into figures or formulas.
            val residualSegmented = runCatching { PageSegmenter.segmentResidual(preprocessed, proseMasks) }
                .getOrDefault(emptyList())

            proseBlocks.forEach { block ->
                candidates.add(CandidateRegion(block.bounds, block.ocr, null))
            }

            val remainingNonProse = nonProseBlocks.toMutableList()

            residualSegmented.forEach { segRegion ->
                val overlapsProse = proseBlocks.any { prose ->
                    containmentFraction(prose.bounds, segRegion.bounds) >= 0.35f ||
                        regionIoU(prose.bounds, segRegion.bounds) >= 0.35f
                }
                if (overlapsProse) return@forEach

                val matchingSnippets = remainingNonProse.filter { block ->
                    containmentFraction(block.bounds, segRegion.bounds) >= 0.55f ||
                        regionIoU(segRegion.bounds, block.bounds) >= 0.40f
                }

                val isTightSingleMatch = matchingSnippets.size == 1 && (
                    regionIoU(segRegion.bounds, matchingSnippets[0].bounds) >= 0.55f ||
                        (containmentFraction(matchingSnippets[0].bounds, segRegion.bounds) >= 0.80f &&
                            segRegion.bounds.area <= matchingSnippets[0].bounds.area * 1.35f)
                    )
                if (isTightSingleMatch) {
                    val matched = matchingSnippets[0]
                    remainingNonProse.remove(matched)
                    candidates.add(CandidateRegion(segRegion.bounds, matched.ocr, segRegion.metrics))
                } else {
                    remainingNonProse.removeAll(matchingSnippets)
                    val ocr = try {
                        TextRecognizerN1.recognizeRegion(preprocessed, segRegion.bounds)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        OcrResult.Empty
                    }
                    val mergedOcr = if (ocr.text.isBlank() && matchingSnippets.isNotEmpty()) {
                        combineBlockOcrs(matchingSnippets)
                    } else {
                        ocr
                    }
                    candidates.add(CandidateRegion(segRegion.bounds, mergedOcr, segRegion.metrics))
                }
            }

            remainingNonProse.forEach { block ->
                candidates.add(CandidateRegion(block.bounds, block.ocr, null))
            }
        } else {
            val segmented = runCatching { PageSegmenter.segment(preprocessed) }
                .getOrDefault(emptyList())
            if (segmented.isNotEmpty()) {
                segmented.forEach { region ->
                    val ocr = try {
                        TextRecognizerN1.recognizeRegion(preprocessed, region.bounds)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        OcrResult.Empty
                    }
                    candidates.add(CandidateRegion(region.bounds, ocr, region.metrics))
                }
            } else {
                val fullOcr = try {
                    TextRecognizerN1.recognize(preprocessed)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    OcrResult.Empty
                }
                candidates.add(
                    CandidateRegion(
                        RegionBounds(0, 0, preprocessed.width, preprocessed.height),
                        fullOcr,
                        null
                    )
                )
            }
        }

        val measuredCandidates = candidates.map { candidate ->
            val metrics = candidate.metrics ?: runCatching {
                PageSegmenter.measureBounds(preprocessed, candidate.bounds)
            }.getOrDefault(
                RegionInkMetrics(
                    inkDensity = 0.05f,
                    componentCount = candidate.ocr.text.count { !it.isWhitespace() },
                    aspectRatio = candidate.bounds.width.toFloat() / candidate.bounds.height.coerceAtLeast(1).toFloat(),
                    heightRatio = candidate.bounds.height.toFloat() / preprocessed.height.coerceAtLeast(1),
                    areaRatio = candidate.bounds.area.toFloat() / (preprocessed.width * preprocessed.height).coerceAtLeast(1)
                )
            )
            candidate.copy(metrics = metrics)
        }

        val mergedCandidates = mergeFragmentedMath(measuredCandidates, preprocessed.width)
            .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))

        val regions = mergedCandidates.mapIndexed { index, candidate ->
            val finalMetrics = if (candidate.metrics != null && !candidate.needsRemeasure) {
                candidate.metrics
            } else {
                runCatching { PageSegmenter.measureBounds(preprocessed, candidate.bounds) }
                    .getOrDefault(
                        candidate.metrics ?: RegionInkMetrics(
                            inkDensity = 0.05f,
                            componentCount = candidate.ocr.text.count { !it.isWhitespace() },
                            aspectRatio = candidate.bounds.width.toFloat() / candidate.bounds.height.coerceAtLeast(1).toFloat()
                        )
                    )
            }

            RegionClassifier.classify(
                index = index,
                bounds = candidate.bounds,
                metrics = finalMetrics,
                ocr = candidate.ocr,
                glossary = glossary
            )
        }

        val analysis = PageAnalysis(
            regions = regions,
            pageWidth = preprocessed.width,
            pageHeight = preprocessed.height,
            ocrMillis = ocrMillis,
            totalMillis = System.currentTimeMillis() - startedAt
        )

        if (preprocessed !== source) preprocessed.recycle()

        analysis
    }

    internal fun partitionProseAndOther(blocks: List<RecognizedBlock>): Pair<List<RecognizedBlock>, List<RecognizedBlock>> =
        blocks.partition { RegionClassifier.looksLikeProseText(it.ocr.text, it.ocr.wordCount) }

    internal fun splitMixedBlocks(
        blocks: List<RecognizedBlock>,
        pageWidth: Int,
        lineHasFormulaGeometry: (RecognizedLine) -> Boolean = { false }
    ): List<RecognizedBlock> {
        val result = mutableListOf<RecognizedBlock>()
        for (block in blocks) {
            if (block.lines.size <= 1) {
                result.add(block)
                continue
            }

            val groups = mutableListOf<MutableList<RecognizedLine>>()
            var current = mutableListOf(block.lines.first())
            for (i in 1 until block.lines.size) {
                val prev = block.lines[i - 1]
                val curr = block.lines[i]
                if (shouldSplitLines(prev, curr, pageWidth, lineHasFormulaGeometry)) {
                    groups.add(current)
                    current = mutableListOf(curr)
                } else {
                    current.add(curr)
                }
            }
            groups.add(current)

            if (groups.size == 1) {
                result.add(block)
            } else {
                groups.forEach { group ->
                    val bounds = unionBounds(group.map { it.bounds })
                    val text = group.joinToString("\n") { it.text }.trim()
                    val wordCount = group.sumOf { it.wordCount }
                    val confs = group.mapNotNull { it.confidence }
                    result.add(
                        RecognizedBlock(
                            bounds = bounds,
                            ocr = OcrResult(
                                text = text,
                                lineCount = group.size,
                                wordCount = wordCount,
                                confidence = if (confs.isEmpty()) null else confs.average().toFloat().coerceIn(0f, 1f)
                            ),
                            lines = group
                        )
                    )
                }
            }
        }
        return result
    }

    private fun shouldSplitLines(
        prev: RecognizedLine,
        curr: RecognizedLine,
        pageWidth: Int,
        lineHasFormulaGeometry: (RecognizedLine) -> Boolean
    ): Boolean {
        val prevMath = RegionClassifier.looksLikeMathText(prev.text, prev.wordCount) || lineHasFormulaGeometry(prev)
        val currMath = RegionClassifier.looksLikeMathText(curr.text, curr.wordCount) || lineHasFormulaGeometry(curr)
        val prevProse = RegionClassifier.looksLikeProseText(prev.text, prev.wordCount) && !prevMath
        val currProse = RegionClassifier.looksLikeProseText(curr.text, curr.wordCount) && !currMath

        if ((prevMath && currProse) || (prevProse && currMath)) return true

        val verticalGap = curr.bounds.top - prev.bounds.bottom
        val lineHeight = max(prev.bounds.height, curr.bounds.height).coerceAtLeast(1)
        if (verticalGap > lineHeight * 0.9f) return true

        val horizontalOverlap = min(prev.bounds.right, curr.bounds.right) - max(prev.bounds.left, curr.bounds.left)
        val maxHDisjointGap = (pageWidth * 0.08f).toInt()
        if (horizontalOverlap < -maxHDisjointGap) return true

        return false
    }

    internal fun mergeFragmentedMath(candidates: List<CandidateRegion>, pageWidth: Int): List<CandidateRegion> {
        if (candidates.size <= 1) return candidates
        val pending = candidates.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left })).toMutableList()
        var changed = true
        val maxGap = (pageWidth * 0.14f).toInt().coerceAtLeast(16)

        while (changed) {
            changed = false
            outer@ for (i in pending.indices) {
                for (j in i + 1 until pending.size) {
                    val a = pending[i]
                    val b = pending[j]
                    if (shouldMergeMathPair(a, b, maxGap)) {
                        val mergedBounds = unionBounds(listOf(a.bounds, b.bounds))
                        val ordered = if (a.bounds.left <= b.bounds.left) listOf(a, b) else listOf(b, a)
                        val combinedText = ordered.map { it.ocr.text.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
                        val confs = listOfNotNull(a.ocr.confidence, b.ocr.confidence)
                        val mergedOcr = OcrResult(
                            text = combinedText,
                            lineCount = max(a.ocr.lineCount, b.ocr.lineCount).coerceAtLeast(1),
                            wordCount = a.ocr.wordCount + b.ocr.wordCount,
                            confidence = if (confs.isEmpty()) null else confs.average().toFloat().coerceIn(0f, 1f)
                        )
                        val mergedMetrics = combineMetrics(a.metrics, b.metrics, mergedBounds)
                        pending.removeAt(j)
                        pending[i] = CandidateRegion(mergedBounds, mergedOcr, mergedMetrics, needsRemeasure = true)
                        changed = true
                        break@outer
                    }
                }
            }
        }
        return pending
    }

    private fun combineMetrics(a: RegionInkMetrics?, b: RegionInkMetrics?, bounds: RegionBounds): RegionInkMetrics? {
        if (a == null && b == null) return null
        return RegionInkMetrics(
            inkDensity = max(a?.inkDensity ?: 0f, b?.inkDensity ?: 0f),
            componentCount = (a?.componentCount ?: 0) + (b?.componentCount ?: 0),
            aspectRatio = bounds.width.toFloat() / bounds.height.coerceAtLeast(1).toFloat(),
            horizontalBarCount = (a?.horizontalBarCount ?: 0) + (b?.horizontalBarCount ?: 0),
            tallStrokeCount = (a?.tallStrokeCount ?: 0) + (b?.tallStrokeCount ?: 0),
            maxStrokeMinSpanRatio = max(a?.maxStrokeMinSpanRatio ?: 0f, b?.maxStrokeMinSpanRatio ?: 0f),
            heightRatio = max(a?.heightRatio ?: 0f, b?.heightRatio ?: 0f),
            areaRatio = (a?.areaRatio ?: 0f) + (b?.areaRatio ?: 0f)
        )
    }

    private fun shouldMergeMathPair(a: CandidateRegion, b: CandidateRegion, maxHorizontalGap: Int): Boolean {
        val mA = a.metrics
        val mB = b.metrics
        if ((mA?.maxStrokeMinSpanRatio ?: 0f) >= RegionClassifier.LARGE_2D_STROKE_THRESHOLD) return false
        if ((mB?.maxStrokeMinSpanRatio ?: 0f) >= RegionClassifier.LARGE_2D_STROKE_THRESHOLD) return false

        if (RegionClassifier.looksLikeProseText(a.ocr.text, a.ocr.wordCount)) return false
        if (RegionClassifier.looksLikeProseText(b.ocr.text, b.ocr.wordCount)) return false
        if (isStandaloneProseWord(a.ocr) || isStandaloneProseWord(b.ocr)) return false

        val yOverlap = min(a.bounds.bottom, b.bounds.bottom) - max(a.bounds.top, b.bounds.top)
        val minHeight = min(a.bounds.height, b.bounds.height).coerceAtLeast(1)
        if (yOverlap < minHeight * 0.35f) return false

        val hGap = max(0, max(a.bounds.left - b.bounds.right, b.bounds.left - a.bounds.right))
        if (hGap > maxHorizontalGap) return false

        val maxH = max(a.bounds.height, b.bounds.height)
        if (maxH > minHeight * 2.6f) return false

        val aMath = (mA?.horizontalBarCount ?: 0) > 0 || (mA?.tallStrokeCount ?: 0) > 0 ||
            RegionClassifier.looksLikeMathText(a.ocr.text, a.ocr.wordCount)
        val bMath = (mB?.horizontalBarCount ?: 0) > 0 || (mB?.tallStrokeCount ?: 0) > 0 ||
            RegionClassifier.looksLikeMathText(b.ocr.text, b.ocr.wordCount)
        val bothShortSnippets = a.ocr.text.count { !it.isWhitespace() } <= 12 &&
            b.ocr.text.count { !it.isWhitespace() } <= 12

        return aMath || bMath || bothShortSnippets
    }

    private fun isStandaloneProseWord(ocr: OcrResult): Boolean {
        val trimmed = ocr.text.trim()
        val chars = trimmed.count { !it.isWhitespace() }
        if (chars < 6 || ocr.wordCount > 2) return false
        return trimmed.all { it.isLetter() || it.isWhitespace() || it == ':' || it == '.' || it == ',' } &&
            !RegionClassifier.looksLikeMathText(trimmed, ocr.wordCount)
    }

    internal fun regionIoU(a: RegionBounds, b: RegionBounds): Float {
        val overlapLeft = maxOf(a.left, b.left)
        val overlapTop = maxOf(a.top, b.top)
        val overlapRight = minOf(a.right, b.right)
        val overlapBottom = minOf(a.bottom, b.bottom)
        if (overlapRight <= overlapLeft || overlapBottom <= overlapTop) return 0f

        val intersection = ((overlapRight - overlapLeft) * (overlapBottom - overlapTop)).toFloat()
        val union = (a.area + b.area - intersection).coerceAtLeast(1f)
        return (intersection / union).coerceIn(0f, 1f)
    }

    internal fun lineCoverageFraction(target: RegionBounds, lines: List<RecognizedLine>): Float {
        val targetArea = target.area.coerceAtLeast(1).toFloat()
        val covered = lines.sumOf { line ->
            val l = maxOf(target.left, line.bounds.left)
            val t = maxOf(target.top, line.bounds.top)
            val r = minOf(target.right, line.bounds.right)
            val b = minOf(target.bottom, line.bounds.bottom)
            if (r > l && b > t) (r - l) * (b - t) else 0
        }
        return (covered / targetArea).coerceIn(0f, 1f)
    }

    private fun containmentFraction(inner: RegionBounds, outer: RegionBounds): Float {
        val l = maxOf(inner.left, outer.left)
        val t = maxOf(inner.top, outer.top)
        val r = minOf(inner.right, outer.right)
        val b = minOf(inner.bottom, outer.bottom)
        if (r <= l || b <= t) return 0f
        return ((r - l) * (b - t)).toFloat() / inner.area.coerceAtLeast(1)
    }

    private fun combineBlockOcrs(blocks: List<RecognizedBlock>): OcrResult {
        val ordered = blocks.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
        val text = ordered.joinToString(" ") { it.ocr.text.trim() }.trim()
        val confs = ordered.mapNotNull { it.ocr.confidence }
        return OcrResult(
            text = text,
            lineCount = ordered.maxOfOrNull { it.ocr.lineCount } ?: 1,
            wordCount = ordered.sumOf { it.ocr.wordCount },
            confidence = if (confs.isEmpty()) null else confs.average().toFloat().coerceIn(0f, 1f)
        )
    }

    private fun unionBounds(bounds: List<RegionBounds>): RegionBounds {
        val left = bounds.minOf { it.left }
        val top = bounds.minOf { it.top }
        val right = bounds.maxOf { it.right }
        val bottom = bounds.maxOf { it.bottom }
        return RegionBounds(left, top, (right - left).coerceAtLeast(1), (bottom - top).coerceAtLeast(1))
    }

    enum class CaptureStepId { N0, N1, Classifier, N1_5, N2 }

    data class CaptureStepSpec(
        val id: CaptureStepId,
        val state: String,
        @StringRes val nameRes: Int,
        @StringRes val descriptionRes: Int,
        val descriptionArgs: List<Any> = emptyList()
    )

    fun stepSpecsFor(analysis: PageAnalysis): List<CaptureStepSpec> {
        val escalated = analysis.regions.count {
            it.kind == RegionKind.Drawing ||
                (it.kind == RegionKind.Text && it.ocrConfidence < RegionClassifier.TEXT_CONFIDENCE_GATE)
        }
        val seconds = String.format(Locale.US, "%.1f", analysis.ocrMillis / 1000.0)

        return listOf(
            CaptureStepSpec(
                id = CaptureStepId.N0,
                state = "done",
                nameRes = R.string.capture_step_n0_name,
                descriptionRes = R.string.capture_step_n0_desc,
                descriptionArgs = listOf(analysis.regionCount)
            ),
            CaptureStepSpec(
                id = CaptureStepId.N1,
                state = "done",
                nameRes = R.string.capture_step_n1_name,
                descriptionRes = R.string.capture_step_n1_desc,
                descriptionArgs = listOf(analysis.readCount, analysis.regionCount, seconds)
            ),
            CaptureStepSpec(
                id = CaptureStepId.Classifier,
                state = "done",
                nameRes = R.string.capture_step_classifier_name,
                descriptionRes = R.string.capture_step_classifier_desc,
                descriptionArgs = listOf(analysis.textCount, analysis.mathCount, analysis.drawingCount)
            ),
            CaptureStepSpec(
                id = CaptureStepId.N1_5,
                state = if (analysis.mathCount > 0) "now" else "pending",
                nameRes = R.string.capture_step_n15_name,
                descriptionRes = if (analysis.mathCount > 0) {
                    R.string.capture_step_n15_desc_active
                } else {
                    R.string.capture_step_n15_desc_empty
                },
                descriptionArgs = if (analysis.mathCount > 0) listOf(analysis.mathCount) else emptyList()
            ),
            CaptureStepSpec(
                id = CaptureStepId.N2,
                state = if (escalated > 0) "now" else "pending",
                nameRes = R.string.capture_step_n2_name,
                descriptionRes = if (escalated > 0) {
                    R.string.capture_step_n2_desc_active
                } else {
                    R.string.capture_step_n2_desc_idle
                },
                descriptionArgs = if (escalated > 0) listOf(escalated) else emptyList()
            )
        )
    }

    /**
     * Traduce el resultado del pipeline a la linea de tiempo que dibuja la pantalla de
     * procesamiento. Los niveles que no se llegaron a ejecutar quedan en "pending" con el
     * trabajo que les toca, para que se vea por que se activan (o por que no).
     */
    fun stepsFor(analysis: PageAnalysis, context: Context): List<CaptureStep> =
        stepSpecsFor(analysis).map { spec ->
            CaptureStep(
                state = spec.state,
                name = context.getString(spec.nameRes),
                description = if (spec.descriptionArgs.isEmpty()) {
                    context.getString(spec.descriptionRes)
                } else {
                    context.getString(spec.descriptionRes, *spec.descriptionArgs.toTypedArray())
                }
            )
        }
}
