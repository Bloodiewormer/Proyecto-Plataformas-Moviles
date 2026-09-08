package cr.ac.una.glifo.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.domain.model.ConfidenceState

@Composable
fun GlifoConfidenceChip(
    state: ConfidenceState,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    val colors = GlifoTheme.colors
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        when (state) {
            is ConfidenceState.Verified -> {
                drawCircle(
                    color = colors.verified,
                    radius = w / 2f,
                    center = Offset(w / 2f, h / 2f)
                )
            }
            is ConfidenceState.Repaired -> {
                drawRoundRect(
                    color = colors.repaired,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
            is ConfidenceState.Escalated -> {
                val path = Path().apply {
                    moveTo(w / 2f, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = path, color = colors.escalated)
            }
            is ConfidenceState.Uncertain -> {
                drawCircle(
                    color = colors.uncertain,
                    radius = (w / 2f) - 1.5f,
                    center = Offset(w / 2f, h / 2f),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                    )
                )
            }
        }
    }
}
