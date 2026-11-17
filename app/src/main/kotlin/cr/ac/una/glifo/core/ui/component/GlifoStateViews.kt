package cr.ac.una.glifo.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.R
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
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(14.dp))
                GlifoButton(text = stringResource(R.string.common_retry), onClick = onRetry, small = true)
            }
        }
    }
}

/**
 * Pantalla completa para los estados que no son "con datos" (cargando, vacío, error) de una
 * pantalla a la que se entra desde otra: barra con título y flecha para volver, y el contenido
 * debajo. Sin esto el aviso quedaba pegado arriba, encima de la barra del teléfono, y no había
 * forma de volver salvo con el botón atrás del sistema.
 */
@Composable
fun GlifoStatusScreen(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlifoTheme.colors.background)
    ) {
        GlifoTopAppBar(title = title, isBack = true, onNavigationClick = onNavigateBack)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(),
            content = content
        )
    }
}

/** Estado vacío centrado: ícono, mensaje y un botón (por lo general "Volver"). */
@Composable
fun GlifoEmptyBox(
    message: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlifoTheme.colors
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlifoButton(text = actionText, onClick = onAction, small = true)
        }
    }
}
