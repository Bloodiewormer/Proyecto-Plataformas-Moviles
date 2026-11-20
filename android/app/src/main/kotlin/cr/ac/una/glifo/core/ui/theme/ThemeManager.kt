package cr.ac.una.glifo.core.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { Light, Dark, System }

/**
 * Where the user's dark/light preference lives — "vive en SharedPreferences, no en
 * una tabla de configuración" per the settings spec, same reasoning applied here.
 * Purely a device-local UI preference, not something a future backend would ever own.
 */
object ThemeManager {
    private const val PREFS_NAME = "glifo_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    var mode by mutableStateOf(ThemeMode.System)
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val saved = prefs(context).getString(KEY_THEME_MODE, ThemeMode.System.name)
        mode = runCatching { ThemeMode.valueOf(saved ?: ThemeMode.System.name) }.getOrDefault(ThemeMode.System)
    }

    fun setMode(context: Context, newMode: ThemeMode) {
        mode = newMode
        prefs(context).edit().putString(KEY_THEME_MODE, newMode.name).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
