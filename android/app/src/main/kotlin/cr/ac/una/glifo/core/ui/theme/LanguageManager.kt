package cr.ac.una.glifo.core.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class AppLanguage(val code: String) {
    System(""),
    Spanish("es"),
    English("en")
}

/**
 * ContextWrapper that retains the enclosing Activity as baseContext so Hilt's
 * ViewModel factory can extract the Activity context, while delegating resources
 * and assets to the localized configuration.
 */
class LocalizedContextWrapper(
    base: Context,
    private val localizedContext: Context
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedContext.resources
    override fun getAssets(): AssetManager = localizedContext.assets
}

/**
 * Manages the user's application language preference, persisted in device SharedPreferences.
 * Supports switching between System default, Spanish ("es"), and English ("en").
 */
object LanguageManager {
    private const val PREFS_NAME = "glifo_prefs"
    private const val KEY_LANGUAGE = "language_mode"

    var language by mutableStateOf(AppLanguage.System)
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val saved = prefs(context).getString(KEY_LANGUAGE, AppLanguage.System.name)
        language = runCatching { AppLanguage.valueOf(saved ?: AppLanguage.System.name) }.getOrDefault(AppLanguage.System)
    }

    fun setLanguage(context: Context, newLanguage: AppLanguage) {
        language = newLanguage
        prefs(context).edit().putString(KEY_LANGUAGE, newLanguage.name).apply()
    }

    fun wrapContext(baseActivity: Context, appLanguage: AppLanguage): Context {
        val locale = when (appLanguage) {
            AppLanguage.Spanish -> Locale.forLanguageTag("es")
            AppLanguage.English -> Locale.forLanguageTag("en")
            AppLanguage.System -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val locales = Resources.getSystem().configuration.locales
                    if (locales.size() > 0) locales.get(0) else Locale.getDefault()
                } else {
                    @Suppress("DEPRECATION")
                    Resources.getSystem().configuration.locale ?: Locale.getDefault()
                }
            }
        }
        Locale.setDefault(locale)
        val config = Configuration(baseActivity.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        @Suppress("DEPRECATION")
        baseActivity.resources.updateConfiguration(config, baseActivity.resources.displayMetrics)
        // Los ViewModels y repositorios arman textos con el contexto de la aplicación (@ApplicationContext).
        // Si solo se cambiaba el de la Activity, esos textos salían en el idioma del sistema (idioma mezclado).
        val appResources = baseActivity.applicationContext.resources
        val appConfig = Configuration(appResources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appConfig.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            appConfig.locale = locale
        }
        @Suppress("DEPRECATION")
        appResources.updateConfiguration(appConfig, appResources.displayMetrics)
        val configContext = baseActivity.createConfigurationContext(config)
        return LocalizedContextWrapper(baseActivity, configContext)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
