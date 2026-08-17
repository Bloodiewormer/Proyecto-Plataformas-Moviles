package cr.ac.una.glifo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.navigation.GlifoNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlifoTheme {
                GlifoNavHost()
            }
        }
    }
}
