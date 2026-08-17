package cr.ac.una.glifo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cr.ac.una.glifo.core.ui.theme.NightBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlifoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NightBackground
                ) {
                    Greeting("Glifo Ready")
                }
            }
        }
    }
}

@Composable
fun GlifoTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
fun Greeting(name: String) {
    Text(text = name)
}
