package cr.ac.una.glifo.core.ui.component

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun LatexFormula(
    latex: String,
    modifier: Modifier = Modifier,
    mode: LatexRenderMode = LatexRenderMode.Formula,
    displayMode: Boolean = mode == LatexRenderMode.Formula,
    textColor: Color = GlifoTheme.colors.textPrimary,
    fontSize: TextUnit = 17.sp,
    lineHeight: TextUnit = (fontSize.value * 1.45f).sp,
    textAlign: TextAlign = TextAlign.Center,
    allowHorizontalScroll: Boolean = false,
    fallbackFontFamily: FontFamily = FontFamily.Monospace,
    fallbackFontWeight: FontWeight = FontWeight.Medium,
    contentDescription: String = latex
) {
    val isPreview = LocalInspectionMode.current
    val fontScale = LocalDensity.current.fontScale
    val cssTextAlign = when (textAlign) {
        TextAlign.Start, TextAlign.Left -> LatexTextAlign.Start
        TextAlign.End, TextAlign.Right -> LatexTextAlign.End
        else -> LatexTextAlign.Center
    }

    val segments = remember(latex, mode, displayMode) {
        LatexHtmlBuilder.parseSegments(latex, mode, displayMode)
    }

    val cssColor = remember(textColor) { LatexHtmlBuilder.colorToCss(textColor) }
    val effectiveFontSize = fontSize.value * fontScale
    val effectiveLineHeight = lineHeight.value * fontScale

    var hasRenderError by remember(latex, mode, displayMode) { mutableStateOf(false) }
    var measuredHeightDp by remember(latex, mode, displayMode) { mutableStateOf<Dp?>(null) }

    val shouldFallback = isPreview || LatexHtmlBuilder.shouldFallbackToPlainText(
        rawText = latex,
        hasRenderError = hasRenderError,
        segments = segments
    )

    Box(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.contentDescription = contentDescription
        },
        contentAlignment = when (cssTextAlign) {
            LatexTextAlign.Start -> Alignment.CenterStart
            LatexTextAlign.End -> Alignment.CenterEnd
            LatexTextAlign.Center -> Alignment.Center
        }
    ) {
        if (shouldFallback) {
            Text(
                text = latex,
                fontFamily = fallbackFontFamily,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = fallbackFontWeight,
                color = textColor,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
            )
        } else {
            val generationCounter = remember { AtomicInteger(0) }
            val currentGeneration = remember(segments, cssColor, effectiveFontSize, effectiveLineHeight, cssTextAlign) {
                generationCounter.incrementAndGet()
            }
            val html = remember(currentGeneration) {
                val style = LatexRenderStyle(
                    textColorCss = cssColor,
                    fontSizeSp = effectiveFontSize,
                    lineHeightSp = effectiveLineHeight,
                    textAlign = cssTextAlign
                )
                LatexHtmlBuilder.buildHtmlDocument(segments, style, currentGeneration)
            }
            val onRenderedCallback by rememberUpdatedState { id: Int, heightPx: Int ->
                if (id == generationCounter.get() && heightPx > 0) {
                    measuredHeightDp = heightPx.coerceIn(20, 1200).dp
                }
            }
            val onErrorCallback by rememberUpdatedState { id: Int ->
                if (id == generationCounter.get()) {
                    hasRenderError = true
                }
            }
            var activeWebView by remember { mutableStateOf<FormulaWebView?>(null) }
            val minFallbackHeight = remember(effectiveLineHeight) { (effectiveLineHeight + 8f).dp }

            DisposableEffect(Unit) {
                onDispose {
                    activeWebView?.let { wv ->
                        wv.stopLoading()
                        wv.removeJavascriptInterface(BRIDGE_NAME)
                        wv.destroy()
                    }
                    activeWebView = null
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(measuredHeightDp ?: minFallbackHeight),
                factory = { context ->
                    createFormulaWebView(
                        context = context,
                        allowHorizontalScroll = allowHorizontalScroll,
                        onRendered = { id, heightPx -> onRenderedCallback(id, heightPx) },
                        onError = { id -> onErrorCallback(id) }
                    ).also { activeWebView = it }
                },
                update = { webView ->
                    webView.touchScrollEnabled = allowHorizontalScroll
                    webView.isClickable = allowHorizontalScroll
                    if (webView.lastHtml != html || webView.lastGeneration != currentGeneration) {
                        webView.lastHtml = html
                        webView.lastGeneration = currentGeneration
                        webView.loadDataWithBaseURL(
                            LatexHtmlBuilder.KATEX_BASE_URL,
                            html,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createFormulaWebView(
    context: Context,
    allowHorizontalScroll: Boolean,
    onRendered: (renderId: Int, heightPx: Int) -> Unit,
    onError: (renderId: Int) -> Unit
): FormulaWebView {
    val mainHandler = Handler(Looper.getMainLooper())
    val webView = FormulaWebView(context)
    webView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    webView.overScrollMode = View.OVER_SCROLL_NEVER
    webView.isVerticalScrollBarEnabled = false
    webView.isHorizontalScrollBarEnabled = false
    webView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    webView.touchScrollEnabled = allowHorizontalScroll
    webView.isClickable = allowHorizontalScroll
    webView.isFocusable = false
    webView.isFocusableInTouchMode = false
    webView.isLongClickable = false

    webView.settings.apply {
        javaScriptEnabled = true
        allowFileAccess = true
        allowContentAccess = false
        // Enforce strictly offline rendering with bundled KaTeX assets only.
        blockNetworkLoads = true
        loadsImagesAutomatically = false
        useWideViewPort = true
        loadWithOverviewMode = false
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
    }

    webView.addJavascriptInterface(
        GlifoLatexJsBridge(
            onRendered = { id, heightPx -> mainHandler.post { onRendered(id, heightPx) } },
            onError = { id -> mainHandler.post { onError(id) } }
        ),
        BRIDGE_NAME
    )

    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            if (request?.isForMainFrame == true) {
                mainHandler.post { onError(webView.lastGeneration) }
            }
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            mainHandler.post { onError(webView.lastGeneration) }
            return true
        }
    }

    return webView
}

private class FormulaWebView(context: Context) : WebView(context) {
    var touchScrollEnabled: Boolean = false
    var lastHtml: String? = null
    var lastGeneration: Int = 0

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean =
        if (touchScrollEnabled) super.onInterceptTouchEvent(ev) else false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean =
        if (touchScrollEnabled) super.onTouchEvent(event) else false
}

private class GlifoLatexJsBridge(
    private val onRendered: (renderId: Int, heightPx: Int) -> Unit,
    private val onError: (renderId: Int) -> Unit
) {
    @JavascriptInterface
    fun onRendered(renderId: Int, heightPx: Int) {
        onRendered.invoke(renderId, heightPx)
    }

    @JavascriptInterface
    fun onError(renderId: Int, @Suppress("UNUSED_PARAMETER") message: String?) {
        onError.invoke(renderId)
    }
}

private const val BRIDGE_NAME = "GlifoLatex"
