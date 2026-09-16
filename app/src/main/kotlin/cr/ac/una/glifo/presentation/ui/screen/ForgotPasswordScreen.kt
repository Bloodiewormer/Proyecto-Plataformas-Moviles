package cr.ac.una.glifo.presentation.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.ui.component.GlifoBanner
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoTextField
import cr.ac.una.glifo.core.ui.component.GlifoTopBar
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onSendLink: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var bannerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { bannerVisible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlifoTheme.colors.background)
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
            Text(
                text = stringResource(R.string.forgot_password_description),
                color = GlifoTheme.colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            GlifoTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.forgot_password_field_email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlifoButton(
                text = stringResource(R.string.forgot_password_button_send),
                enabled = email.isNotBlank(),
                onClick = onSendLink
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = bannerVisible, enter = fadeIn(tween(400))) {
                GlifoBanner(
                    text = stringResource(R.string.forgot_password_banner),
                    icon = Icons.Outlined.Info
                )
            }
        }
    }
}
