package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.ui.component.GlifoBanner
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoTopBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

/**
 * "Olvidé mi contraseña". El backend no envía correos, así que la pantalla ya no simula
 * que manda un enlace: explica qué hacer. Con sesión iniciada se cambia en Perfil.
 */
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onSendLink: () -> Unit
) {
    val colors = GlifoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopBar(
            title = stringResource(R.string.forgot_password_title),
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            GlifoBanner(
                text = stringResource(R.string.forgot_password_not_available),
                icon = Icons.Outlined.Info
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.forgot_password_how_to),
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            GlifoButton(
                text = stringResource(R.string.forgot_password_back_to_login),
                onClick = onSendLink
            )
        }
    }
}
