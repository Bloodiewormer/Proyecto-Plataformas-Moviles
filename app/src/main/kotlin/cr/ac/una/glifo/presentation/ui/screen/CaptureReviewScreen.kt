package cr.ac.una.glifo.presentation.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.CaptureReviewViewModel
import cr.ac.una.glifo.presentation.viewmodel.toMessage

@Composable
fun CaptureReviewScreen(
    photoPath: String? = null,
    onNavigateBack: () -> Unit,
    onRetakePhoto: () -> Unit,
    onProcessAnyway: () -> Unit,
    viewModel: CaptureReviewViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsState()

    androidx.compose.runtime.LaunchedEffect(photoPath) {
        viewModel.load(photoPath)
    }

    val bitmap = remember(photoPath) {
        photoPath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = stringResource(R.string.capture_review_title),
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // Real captured photo when available, otherwise a placeholder.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(colors.surfaceHigh, RoundedCornerShape(7.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(7.dp))
                    .clip(RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.capture_review_photo_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = stringResource(R.string.capture_review_preview_placeholder),
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = uiState) {
                is UiState.Loading -> GlifoLoadingBox()
                is UiState.Empty -> GlifoLoadingBox()
                is UiState.Error -> GlifoErrorBox(state.error.toMessage(context), onRetry = viewModel::load)
                is UiState.Success -> {
                    val checks = state.data.checks
                    val hasWarning = state.data.hasWarning

                    if (hasWarning) {
                        // Warning Card
                        GlifoCard(
                            backgroundColor = colors.alertFaint,
                            borderColor = colors.alertLine
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = colors.alert, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.capture_review_blur_warning_title), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            }
                            Text(
                                text = stringResource(R.string.capture_review_blur_warning_description),
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    // Quality Diagnostics
                    Text(
                        text = stringResource(R.string.capture_review_quality_section),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                    )

                    GlifoCard {
                        checks.forEach { check ->
                            val barColor = if (check.isWarning) colors.alert else colors.verified
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (check.isWarning) Icons.Default.Warning else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = barColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(text = check.label, fontSize = 13.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.surfaceHigh)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(check.percent / 100f)
                                            .background(barColor)
                                    )
                                }
                                Text(
                                    text = check.scoreLabel,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    GlifoButton(
                        text = stringResource(R.string.capture_review_retake),
                        icon = Icons.Default.CameraAlt,
                        onClick = onRetakePhoto
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    GlifoButton(
                        text = if (hasWarning) {
                            stringResource(R.string.capture_review_process_anyway)
                        } else {
                            stringResource(R.string.capture_review_process)
                        },
                        variant = GlifoButtonVariant.Text,
                        onClick = onProcessAnyway
                    )
                }
            }
        }
    }
}
