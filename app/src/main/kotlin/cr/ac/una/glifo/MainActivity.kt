package cr.ac.una.glifo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.core.ui.theme.LanguageManager
import cr.ac.una.glifo.core.ui.theme.ThemeManager
import cr.ac.una.glifo.core.ui.theme.ThemeMode
import cr.ac.una.glifo.presentation.navigation.GlifoNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var targetDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        LanguageManager.init(this)
        enableEdgeToEdge()
        targetDestination = intent?.getStringExtra(EXTRA_DESTINATION)

        setContent {
            val currentLanguage = LanguageManager.language
            val localizedContext = remember(currentLanguage) {
                LanguageManager.wrapContext(this, currentLanguage)
            }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (ThemeManager.mode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> systemDark
            }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                GlifoTheme(darkTheme = darkTheme) {
                    GlifoNavHost(
                        targetDestination = targetDestination,
                        onClearTargetDestination = { targetDestination = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        targetDestination = intent.getStringExtra(EXTRA_DESTINATION)
    }

    companion object {
        const val EXTRA_DESTINATION = "extra_destination"
        const val EXTRA_COURSE_ID = "extra_course_id"
        const val DESTINATION_STUDY = "study"
    }
}
