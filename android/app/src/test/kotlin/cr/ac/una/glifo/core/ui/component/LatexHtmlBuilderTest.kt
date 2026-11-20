package cr.ac.una.glifo.core.ui.component

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexHtmlBuilderTest {

    @Test
    fun `colorToCss formats opaque and translucent colors`() {
        assertEquals("#D7D1B9", LatexHtmlBuilder.colorToCss(Color(0xFFD7D1B9)))
        assertEquals("#2E3B4B", LatexHtmlBuilder.colorToCss(Color(0xFF2E3B4B)))
        assertEquals("rgba(255,211,114,0.502)", LatexHtmlBuilder.colorToCss(Color(0x80FFD372)))
    }

    @Test
    fun `escapeHtml escapes HTML special characters`() {
        val input = """<script>"a" & 'b'</script>"""
        val expected = "&lt;script&gt;&quot;a&quot; &amp; &#39;b&#39;&lt;/script&gt;"
        assertEquals(expected, LatexHtmlBuilder.escapeHtml(input))
    }

    @Test
    fun `escapeJsonString escapes backslashes quotes and script breakout sequences`() {
        val raw = """\frac{1}{2}</script><img onerror="alert(1)"> & test"""
        val escaped = LatexHtmlBuilder.escapeJsonString(raw)

        assertTrue(escaped.startsWith("\""))
        assertTrue(escaped.endsWith("\""))
        assertTrue(escaped.contains("""\\frac{1}{2}"""))
        assertFalse(escaped.contains("</script>"))
        assertTrue(escaped.contains("""\u003c/script\u003e"""))
        assertTrue(escaped.contains("""\u0026"""))
    }

    @Test
    fun `stripOuterDelimiters removes balanced math delimiters only`() {
        assertEquals("""\frac{1}{2}""", LatexHtmlBuilder.stripOuterDelimiters("""  $$\frac{1}{2}$$  """))
        assertEquals("""\int_0^\pi \sec x\,dx""", LatexHtmlBuilder.stripOuterDelimiters("""\[\int_0^\pi \sec x\,dx\]"""))
        assertEquals("""\arctan(x)""", LatexHtmlBuilder.stripOuterDelimiters("""\(\arctan(x)\)"""))
        assertEquals("""\sqrt{x^2 + 1}""", LatexHtmlBuilder.stripOuterDelimiters("""$\sqrt{x^2 + 1}$"""))
        assertEquals("""\lim_{x\to\infty} f(x)""", LatexHtmlBuilder.stripOuterDelimiters("""\lim_{x\to\infty} f(x)"""))
        assertEquals("\$a\$ + \$b\$", LatexHtmlBuilder.stripOuterDelimiters("\$a\$ + \$b\$"))
    }

    @Test
    fun `containsLatex detects math commands delimiters and scripts without false positives`() {
        assertTrue(LatexHtmlBuilder.containsLatex("""\frac{1}{2}"""))
        assertTrue(LatexHtmlBuilder.containsLatex("""\int_{0}^{\infty} \frac{\arctan(x)}{\sqrt{x^2+\pi}} dx"""))
        assertTrue(LatexHtmlBuilder.containsLatex("""\lim_{x \to 0} \sec(x) \left[ x_n^2 \right]"""))
        assertTrue(LatexHtmlBuilder.containsLatex("Find \$x + 1\$"))
        assertTrue(LatexHtmlBuilder.containsLatex("""Evaluate \(\pi r^2\)"""))
        assertTrue(LatexHtmlBuilder.containsLatex("""x^{n+1}"""))
        assertTrue(LatexHtmlBuilder.containsLatex("""a_{i,j}"""))
        assertTrue(LatexHtmlBuilder.containsLatex("""x^2 + y^2"""))

        assertFalse(LatexHtmlBuilder.containsLatex(""))
        assertFalse(LatexHtmlBuilder.containsLatex("   "))
        assertFalse(LatexHtmlBuilder.containsLatex("What is the derivative of sine?"))
        assertFalse(LatexHtmlBuilder.containsLatex("True"))
        assertFalse(LatexHtmlBuilder.containsLatex("The price is $5 and tax is $2."))
        assertFalse(LatexHtmlBuilder.containsLatex("""C:\Users\docs\notes.txt"""))
        assertFalse(LatexHtmlBuilder.containsLatex("snake_case_identifier"))
    }

    @Test
    fun `parseSegments handles formula and mixed modes`() {
        val formulaSegments = LatexHtmlBuilder.parseSegments("""$$\frac{1}{2}$$""", LatexRenderMode.Formula)
        assertEquals(
            listOf(LatexSegment.Math(tex = """\frac{1}{2}""", displayMode = true)),
            formulaSegments
        )

        val mixedDelimited = LatexHtmlBuilder.parseSegments(
            input = "Compute \$\\int_0^\\pi \\sec^2(x)\\,dx\$ and \\[\\lim_{n\\to\\infty} \\frac{1}{n}\\]",
            mode = LatexRenderMode.Mixed
        )
        assertEquals(
            listOf(
                LatexSegment.Prose("Compute "),
                LatexSegment.Math("""\int_0^\pi \sec^2(x)\,dx""", displayMode = false),
                LatexSegment.Prose(" and "),
                LatexSegment.Math("""\lim_{n\to\infty} \frac{1}{n}""", displayMode = true)
            ),
            mixedDelimited
        )

        val mixedBare = LatexHtmlBuilder.parseSegments("""\arctan(\sqrt{3})""", LatexRenderMode.Mixed)
        assertEquals(
            listOf(LatexSegment.Math("""\arctan(\sqrt{3})""", displayMode = false)),
            mixedBare
        )

        val mixedProse = LatexHtmlBuilder.parseSegments("""Just plain text \$5""", LatexRenderMode.Mixed)
        assertEquals(listOf(LatexSegment.Prose("""Just plain text \$5""")), mixedProse)
    }

    @Test
    fun `shouldFallbackToPlainText decides based on blankness error state and math segments`() {
        val mathSegments = listOf(LatexSegment.Math("""\frac{1}{2}""", displayMode = true))
        val proseSegments = listOf(LatexSegment.Prose("Plain text"))

        assertTrue(LatexHtmlBuilder.shouldFallbackToPlainText("", hasRenderError = false, segments = mathSegments))
        assertTrue(LatexHtmlBuilder.shouldFallbackToPlainText("   ", hasRenderError = false, segments = mathSegments))
        assertTrue(LatexHtmlBuilder.shouldFallbackToPlainText("""\frac{1}{2}""", hasRenderError = true, segments = mathSegments))
        assertTrue(LatexHtmlBuilder.shouldFallbackToPlainText("Plain text", hasRenderError = false, segments = proseSegments))
        assertFalse(LatexHtmlBuilder.shouldFallbackToPlainText("""\frac{1}{2}""", hasRenderError = false, segments = mathSegments))
    }

    @Test
    fun `buildHtmlDocument builds offline theme-aware template with KaTeX assets and error bridge`() {
        val formula = """\int_{0}^{\infty} \frac{\arctan(x)\sec(x)}{\sqrt{x^2 + \pi}} \left[ x_n^2 \right] dx"""
        val segments = LatexHtmlBuilder.parseSegments(formula, LatexRenderMode.Formula)
        val style = LatexRenderStyle(
            textColorCss = "#D7D1B9",
            fontSizeSp = 20f,
            lineHeightSp = 29f,
            textAlign = LatexTextAlign.Center
        )

        val html = LatexHtmlBuilder.buildHtmlDocument(segments = segments, style = style, renderId = 42)

        assertTrue(html.contains("""<link rel="stylesheet" href="katex.min.css">"""))
        assertTrue(html.contains("""<script src="katex.min.js"></script>"""))
        assertFalse(html.contains("http://"))
        assertFalse(html.contains("https://"))
        assertTrue(html.contains("color: #D7D1B9;"))
        assertTrue(html.contains("font-size: 20.00px;"))
        assertTrue(html.contains("text-align: center;"))
        assertTrue(html.contains("var renderId = 42;"))
        assertTrue(html.contains("throwOnError: true"))
        assertTrue(html.contains("window.GlifoLatex.onRendered(renderId, h)"))
        assertTrue(html.contains("window.GlifoLatex.onError(renderId,"))
        assertTrue(html.contains("""\\int_{0}^{\\infty}"""))
        assertTrue(html.contains("""\\arctan(x)"""))
    }
}
