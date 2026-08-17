package cr.ac.una.glifo.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun GlifoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp).fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GlifoTheme.colors.accent,
            contentColor = GlifoTheme.colors.onAccent,
            disabledContainerColor = GlifoTheme.colors.surfaceHigh,
            disabledContentColor = GlifoTheme.colors.textSecondary
        )
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GlifoOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp).fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GlifoTheme.colors.accent),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = GlifoTheme.colors.accentText,
            disabledContentColor = GlifoTheme.colors.textSecondary
        )
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlifoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GlifoTheme.colors.accent,
                unfocusedBorderColor = GlifoTheme.colors.border,
                focusedLabelColor = GlifoTheme.colors.accentText,
                unfocusedLabelColor = GlifoTheme.colors.textSecondary,
                focusedTextColor = GlifoTheme.colors.textPrimary,
                unfocusedTextColor = GlifoTheme.colors.textPrimary,
                errorBorderColor = GlifoTheme.colors.alert,
                errorLabelColor = GlifoTheme.colors.alert,
                errorTextColor = GlifoTheme.colors.textPrimary,
                focusedContainerColor = GlifoTheme.colors.surfaceHigh,
                unfocusedContainerColor = GlifoTheme.colors.surfaceHigh
            )
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = GlifoTheme.colors.alert,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlifoTopBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(text = title, color = GlifoTheme.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium) },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Text("←", color = GlifoTheme.colors.textPrimary, fontSize = 20.sp)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GlifoTheme.colors.surface
        )
    )
}

@Composable
fun GlifoBottomBar(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToStudy: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    NavigationBar(
        containerColor = GlifoTheme.colors.surface,
        contentColor = GlifoTheme.colors.textPrimary
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = onNavigateToHome,
            icon = { Text("🏠", color = if (currentRoute == "home") GlifoTheme.colors.accentText else GlifoTheme.colors.textSecondary) },
            label = { Text("Inicio") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GlifoTheme.colors.accentText,
                unselectedIconColor = GlifoTheme.colors.textSecondary,
                selectedTextColor = GlifoTheme.colors.accentText,
                unselectedTextColor = GlifoTheme.colors.textSecondary,
                indicatorColor = GlifoTheme.colors.surfaceHigh
            )
        )
        NavigationBarItem(
            selected = currentRoute == "study",
            onClick = onNavigateToStudy,
            icon = { Text("🧠", color = if (currentRoute == "study") GlifoTheme.colors.accentText else GlifoTheme.colors.textSecondary) },
            label = { Text("Estudio") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GlifoTheme.colors.accentText,
                unselectedIconColor = GlifoTheme.colors.textSecondary,
                selectedTextColor = GlifoTheme.colors.accentText,
                unselectedTextColor = GlifoTheme.colors.textSecondary,
                indicatorColor = GlifoTheme.colors.surfaceHigh
            )
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = onNavigateToSettings,
            icon = { Text("⚙️", color = if (currentRoute == "settings") GlifoTheme.colors.accentText else GlifoTheme.colors.textSecondary) },
            label = { Text("Ajustes") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GlifoTheme.colors.accentText,
                unselectedIconColor = GlifoTheme.colors.textSecondary,
                selectedTextColor = GlifoTheme.colors.accentText,
                unselectedTextColor = GlifoTheme.colors.textSecondary,
                indicatorColor = GlifoTheme.colors.surfaceHigh
            )
        )
    }
}

@Composable
fun GlifoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val mod = modifier.fillMaxWidth()
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = mod,
            shape = RoundedCornerShape(16.dp),
            color = GlifoTheme.colors.surface,
            border = BorderStroke(1.dp, GlifoTheme.colors.border)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(
            modifier = mod,
            shape = RoundedCornerShape(16.dp),
            color = GlifoTheme.colors.surface,
            border = BorderStroke(1.dp, GlifoTheme.colors.border)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun GlifoEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📭", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = GlifoTheme.colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = GlifoTheme.colors.textSecondary)
    }
}
