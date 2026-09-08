package cr.ac.una.glifo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.core.ui.theme.ThemeManager
import cr.ac.una.glifo.core.ui.theme.ThemeMode
import cr.ac.una.glifo.presentation.navigation.GlifoNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (ThemeManager.mode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> systemDark
            }
            GlifoTheme(darkTheme = darkTheme) {
                GlifoNavHost()
            }
        }
    }
}
