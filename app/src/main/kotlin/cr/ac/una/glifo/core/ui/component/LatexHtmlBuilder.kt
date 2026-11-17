package cr.ac.una.glifo.core.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class LatexRenderMode {
    Formula,
    Mixed
}

enum class LatexTextAlign(val cssValue: String) {
    Start("left"),
    Center("center"),
    End("right")
}

sealed interface LatexSegment {
    data class Prose(val text: String) : LatexSegment
    data class Math(val tex: String, val displayMode: Boolean) : LatexSegment
}

data class LatexRenderStyle(
    val textColorCss: String,
    val fontSizeSp: Float,
    val lineHeightSp: Float,
    val textAlign: LatexTextAlign = LatexTextAlign.Center
)

object LatexHtmlBuilder {

    const val KATEX_BASE_URL = "file:///android_asset/katex/"

    // Explicit math command allowlist avoids false positives on Windows paths or normal prose.
    private val BARE_LATEX_COMMAND_REGEX = Regex(
        """\\(?:frac|dfrac|tfrac|cfrac|int|iint|iiint|oint|lim|limsup|liminf|sqrt|sum|prod|coprod|pi|theta|alpha|beta|gamma|delta|epsilon|varepsilon|zeta|eta|iota|kappa|lambda|mu|nu|xi|rho|sigma|tau|upsilon|phi|varphi|chi|psi|omega|Gamma|Delta|Theta|Lambda|Xi|Pi|Sigma|Upsilon|Phi|Psi|Omega|infty|arctan|arcsin|arccos|arcsec|arccsc|arccot|sinh|cosh|tanh|sin|cos|tan|sec|csc|cot|ln|log|exp|deg|det|dim|gcd|hom|ker|max|min|sup|inf|arg|left|right|big|Big|bigg|Bigg|cdot|cdots|ldots|vdots|ddots|times|div|pm|mp|leq|le|geq|ge|neq|ne|approx|equiv|sim|simeq|cong|propto|to|rightarrow|leftarrow|leftrightarrow|mapsto|Rightarrow|Leftarrow|Leftrightarrow|implies|iff|forall|exists|nexists|in|notin|ni|subset|subseteq|supset|supseteq|cup|cap|setminus|emptyset|varnothing|mathbb|mathcal|mathfrak|mathrm|mathbf|mathit|mathsf|mathtt|text|textbf|textit|textrm|overline|underline|overbrace|underbrace|bar|hat|vec|tilde|dot|ddot|begin|end|binom|tbinom|dbinom|partial|nabla|angle|triangle|perp|parallel|circ|degree)\b"""
    )

    private val BRACED_SCRIPT_REGEX = Regex("""[\^_]\{[^}]+\}""")
    private val CARET_EXPONENT_REGEX = Regex("""[A-Za-z0-9)\]}]\^[A-Za-z0-9(\\-]""")

    fun colorToCss(color: Color): String {
        val argb = color.toArgb()
        val a = (argb ushr 24) and 0xFF
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        return if (a == 255) {
            String.format(java.util.Locale.US, "#%02X%02X%02X", r, g, b)
        } else {
            val alpha = a / 255f
            String.format(java.util.Locale.US, "rgba(%d,%d,%d,%.3f)", r, g, b, alpha)
        }
    }

    fun containsLatex(text: String): Boolean {
        if (text.isBlank()) return false
        val delimited = parseDelimitedSegments(text)
        if (delimited.any { it is LatexSegment.Math }) return true
        return containsBareLatex(text)
    }

    fun containsBareLatex(text: String): Boolean {
        if (text.isBlank()) return false
        return BARE_LATEX_COMMAND_REGEX.containsMatchIn(text) ||
            BRACED_SCRIPT_REGEX.containsMatchIn(text) ||
            CARET_EXPONENT_REGEX.containsMatchIn(text)
    }

    fun stripOuterDelimiters(input: String): String {
        val trimmed = input.trim()
        if (trimmed.length >= 4 && trimmed.startsWith("$$") && trimmed.endsWith("$$")) {
            return trimmed.substring(2, trimmed.length - 2).trim()
        }
        if (trimmed.length >= 4 && trimmed.startsWith("\\[") && trimmed.endsWith("\\]")) {
            return trimmed.substring(2, trimmed.length - 2).trim()
        }
        if (trimmed.length >= 4 && trimmed.startsWith("\\(") && trimmed.endsWith("\\)")) {
            return trimmed.substring(2, trimmed.length - 2).trim()
        }
        if (trimmed.length >= 2 && trimmed.startsWith("$") && trimmed.endsWith("$")) {
            val inner = trimmed.substring(1, trimmed.length - 1)
            if (!hasUnescapedDollar(inner)) {
                return inner.trim()
            }
        }
        return trimmed
    }

    fun parseSegments(
        input: String,
        mode: LatexRenderMode,
        displayModeInFormula: Boolean = true
    ): List<LatexSegment> {
        if (input.isBlank()) return emptyList()
        return when (mode) {
            LatexRenderMode.Formula -> {
                val stripped = stripOuterDelimiters(input)
                if (stripped.isBlank()) emptyList()
                else listOf(LatexSegment.Math(tex = stripped, displayMode = displayModeInFormula))
            }
            LatexRenderMode.Mixed -> {
                val delimited = parseDelimitedSegments(input)
                if (delimited.any { it is LatexSegment.Math }) {
                    delimited
                } else if (containsBareLatex(input)) {
                    listOf(LatexSegment.Math(tex = input.trim(), displayMode = false))
                } else {
                    listOf(LatexSegment.Prose(input))
                }
            }
        }
    }

    fun shouldFallbackToPlainText(
        rawText: String,
        hasRenderError: Boolean,
        segments: List<LatexSegment>
    ): Boolean {
        if (rawText.isBlank() || hasRenderError) return true
        return segments.none { it is LatexSegment.Math }
    }

    fun escapeHtml(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun escapeJsonString(text: String): String {
        val sb = StringBuilder(text.length + 2)
        sb.append('"')
        for (ch in text) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                // Escape HTML-sensitive characters so embedded JSON cannot break out of <script>.
                '<' -> sb.append("\\u003c")
                '>' -> sb.append("\\u003e")
                '&' -> sb.append("\\u0026")
                '\u2028' -> sb.append("\\u2028")
                '\u2029' -> sb.append("\\u2029")
                else -> {
                    if (ch.code < 0x20) {
                        sb.append(String.format(java.util.Locale.US, "\\u%04x", ch.code))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    fun buildSegmentsJson(segments: List<LatexSegment>): String {
        val sb = StringBuilder("[")
        segments.forEachIndexed { index, segment ->
            if (index > 0) sb.append(',')
            when (segment) {
                is LatexSegment.Prose -> {
                    sb.append("{\"t\":\"p\",\"v\":")
                    sb.append(escapeJsonString(segment.text))
                    sb.append('}')
                }
                is LatexSegment.Math -> {
                    sb.append("{\"t\":\"m\",\"d\":")
                    sb.append(if (segment.displayMode) "true" else "false")
                    sb.append(",\"v\":")
                    sb.append(escapeJsonString(segment.tex))
                    sb.append('}')
                }
            }
        }
        sb.append(']')
        return sb.toString()
    }

    fun buildHtmlDocument(
        segments: List<LatexSegment>,
        style: LatexRenderStyle,
        renderId: Int
    ): String {
        val segmentsJson = buildSegmentsJson(segments)
        val lineHeightRatio = if (style.fontSizeSp > 0f) {
            (style.lineHeightSp / style.fontSizeSp).coerceIn(1.1f, 2.2f)
        } else {
            1.45f
        }
        val fontSizeCss = String.format(java.util.Locale.US, "%.2fpx", style.fontSizeSp)
        val lineHeightCss = String.format(java.util.Locale.US, "%.2f", lineHeightRatio)

        return """
            |<!DOCTYPE html>
            |<html>
            |<head>
            |<meta charset="utf-8">
            |<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            |<link rel="stylesheet" href="katex.min.css">
            |<style>
            |html, body {
            |  margin: 0;
            |  padding: 0;
            |  background: transparent;
            |  color: ${style.textColorCss};
            |  font-family: sans-serif;
            |  font-size: $fontSizeCss;
            |  line-height: $lineHeightCss;
            |  overflow-x: auto;
            |  overflow-y: hidden;
            |  -webkit-text-size-adjust: 100%;
            |}
            |#formula-root {
            |  display: block;
            |  width: 100%;
            |  box-sizing: border-box;
            |  padding: 4px 2px;
            |  text-align: ${style.textAlign.cssValue};
            |  word-wrap: break-word;
            |}
            |.katex-display {
            |  margin: 0.25em 0 !important;
            |  overflow-x: auto;
            |  overflow-y: hidden;
            |}
            |.katex {
            |  font-size: 1.08em;
            |}
            |</style>
            |</head>
            |<body>
            |<div id="formula-root"></div>
            |<script src="katex.min.js"></script>
            |<script>
            |(function() {
            |  var renderId = $renderId;
            |  var segments = $segmentsJson;
            |  var root = document.getElementById("formula-root");
            |  function reportHeight() {
            |    if (!window.GlifoLatex || !root) return;
            |    var rect = root.getBoundingClientRect();
            |    var h = Math.ceil(Math.max(rect.height || 0, root.scrollHeight || 0));
            |    window.GlifoLatex.onRendered(renderId, h);
            |  }
            |  function fail(err) {
            |    if (window.GlifoLatex) {
            |      window.GlifoLatex.onError(renderId, err ? String(err) : "render_error");
            |    }
            |  }
            |  try {
            |    if (typeof katex === "undefined") {
            |      fail("katex_unavailable");
            |      return;
            |    }
            |    for (var i = 0; i < segments.length; i++) {
            |      var seg = segments[i];
            |      if (seg.t === "p") {
            |        var proseEl = document.createElement("span");
            |        proseEl.textContent = seg.v;
            |        root.appendChild(proseEl);
            |      } else {
            |        var mathEl = document.createElement(seg.d ? "div" : "span");
            |        katex.render(seg.v, mathEl, {
            |          displayMode: !!seg.d,
            |          throwOnError: true,
            |          strict: false,
            |          output: "html"
            |        });
            |        root.appendChild(mathEl);
            |      }
            |    }
            |    reportHeight();
            |    if (document.fonts && document.fonts.ready) {
            |      document.fonts.ready.then(function() {
            |        requestAnimationFrame(reportHeight);
            |      });
            |    }
            |    if (typeof ResizeObserver !== "undefined") {
            |      new ResizeObserver(function() {
            |        reportHeight();
            |      }).observe(root);
            |    }
            |  } catch (e) {
            |    fail(e && e.message ? e.message : e);
            |  }
            |})();
            |</script>
            |</body>
            |</html>
        """.trimMargin()
    }

    private fun parseDelimitedSegments(input: String): List<LatexSegment> {
        val segments = mutableListOf<LatexSegment>()
        val proseBuffer = StringBuilder()
        var i = 0
        val n = input.length

        fun flushProse() {
            if (proseBuffer.isNotEmpty()) {
                segments += LatexSegment.Prose(proseBuffer.toString())
                proseBuffer.clear()
            }
        }

        while (i < n) {
            if (input[i] == '\\' && i + 1 < n) {
                val next = input[i + 1]
                if (next == '$') {
                    proseBuffer.append('$')
                    i += 2
                    continue
                }
                if (next == '[' || next == '(') {
                    val closing = if (next == '[') "\\]" else "\\)"
                    val endIdx = findClosingDelimiter(input, i + 2, closing)
                    if (endIdx != -1) {
                        val content = input.substring(i + 2, endIdx).trim()
                        if (content.isNotEmpty()) {
                            flushProse()
                            segments += LatexSegment.Math(content, displayMode = next == '[')
                            i = endIdx + 2
                            continue
                        }
                    }
                }
            }

            if (input[i] == '$') {
                if (i + 1 < n && input[i + 1] == '$') {
                    val endIdx = findClosingDelimiter(input, i + 2, "$$")
                    if (endIdx != -1) {
                        val content = input.substring(i + 2, endIdx).trim()
                        if (content.isNotEmpty()) {
                            flushProse()
                            segments += LatexSegment.Math(content, displayMode = true)
                            i = endIdx + 2
                            continue
                        }
                    }
                } else {
                    val endIdx = findClosingSingleDollar(input, i + 1)
                    if (endIdx != -1) {
                        val content = input.substring(i + 1, endIdx).trim()
                        if (content.isNotEmpty()) {
                            flushProse()
                            segments += LatexSegment.Math(content, displayMode = false)
                            i = endIdx + 1
                            continue
                        }
                    }
                }
            }

            proseBuffer.append(input[i])
            i++
        }

        flushProse()
        return segments
    }

    private fun findClosingDelimiter(input: String, startIndex: Int, closing: String): Int {
        var i = startIndex
        while (i <= input.length - closing.length) {
            if (input[i] == '\\' && closing == "$$") {
                i += 2
                continue
            }
            if (input.regionMatches(i, closing, 0, closing.length)) {
                return i
            }
            i++
        }
        return -1
    }

    private fun findClosingSingleDollar(input: String, startIndex: Int): Int {
        if (startIndex >= input.length || input[startIndex].isWhitespace()) return -1
        var i = startIndex
        while (i < input.length) {
            val ch = input[i]
            if (ch == '\\') {
                i += 2
                continue
            }
            if (ch == '$' && (i + 1 >= input.length || input[i + 1] != '$')) {
                val prevIsWhitespace = i > startIndex && input[i - 1].isWhitespace()
                val nextIsDigit = i + 1 < input.length && input[i + 1].isDigit()
                if (!prevIsWhitespace && !nextIsDigit) {
                    return i
                }
                return -1
            }
            i++
        }
        return -1
    }

    private fun hasUnescapedDollar(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\') {
                i += 2
                continue
            }
            if (text[i] == '$') return true
            i++
        }
        return false
    }
}
