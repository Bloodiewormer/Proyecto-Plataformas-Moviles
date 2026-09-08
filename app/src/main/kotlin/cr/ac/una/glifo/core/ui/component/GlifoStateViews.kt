package cr.ac.una.glifo.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

/**
 * Shared full-space loading placeholder for a `StateFlow<UiState<T>>`-backed screen. Every
 * rewired screen (see `presentation.viewmodel`) uses this for `UiState.Loading`/`Empty` rather
 * than each drawing its own spinner.
 */
@Composable
fun GlifoLoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = GlifoTheme.colors.accent)
    }
}

/** Shared full-space error placeholder with an optional retry action. */
@Composable
fun GlifoErrorBox(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val colors = GlifoTheme.colors
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.alert,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = colors.textSecondary
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(14.dp))
                GlifoButton(text = "Reintentar", onClick = onRetry, small = true)
            }
        }
    }
}
