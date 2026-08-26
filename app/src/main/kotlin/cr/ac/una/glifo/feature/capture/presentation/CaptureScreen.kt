package cr.ac.una.glifo.feature.capture.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun CaptureScreen(
    courseId: Long,
    onNavigateBack: () -> Unit,
    onCapturePhoto: () -> Unit
) {
    val colors = GlifoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Camera Viewfinder Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.surfaceHigh, colors.surface, colors.background),
                        radius = 800f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Top Camera Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                }
                Text(
                    text = "Cálculo II · página 1",
                    fontSize = 12.sp,
                    color = colors.textPrimary
                )
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Flash", tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                }
            }

            // Document Detection Overlay
            Canvas(modifier = Modifier.size(220.dp, 290.dp)) {
                val w = size.width
                val h = size.height

                // Bounding quadrilateral
                val docPath = Path().apply {
                    moveTo(16f, 24f)
                    lineTo(w - 16f, 12f)
                    lineTo(w - 6f, h - 24f)
                    lineTo(24f, h - 12f)
                    close()
                }
                drawPath(path = docPath, color = colors.accentFaint)
                drawPath(path = docPath, color = colors.accentText, style = Stroke(width = 2f))

                // Corner indicators
                val cornerStroke = Stroke(width = 4f, cap = StrokeCap.Round)
                // Top-left
                drawLine(colors.accentText, Offset(16f, 50f), Offset(16f, 24f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(colors.accentText, Offset(16f, 24f), Offset(42f, 22f), strokeWidth = 4f, cap = StrokeCap.Round)
                // Top-right
                drawLine(colors.accentText, Offset(w - 42f, 14f), Offset(w - 16f, 12f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(colors.accentText, Offset(w - 16f, 12f), Offset(w - 15f, 38f), strokeWidth = 4f, cap = StrokeCap.Round)
            }

            // Detection pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(colors.surface, RoundedCornerShape(999.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Página detectada · mantené el pulso",
                    fontSize = 11.sp,
                    color = colors.accentText
                )
            }
        }

        // Camera Bottom Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(100.dp)
                .background(colors.background)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surfaceHigh, RoundedCornerShape(7.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = "Galería", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }

            // Shutter Button
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .border(3.dp, colors.textPrimary, CircleShape)
                    .clickable(onClick = onCapturePhoto),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(colors.textPrimary, CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surfaceHigh, RoundedCornerShape(7.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("1/8", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            }
        }
    }
}
