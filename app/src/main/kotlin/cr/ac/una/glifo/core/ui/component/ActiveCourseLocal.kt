package cr.ac.una.glifo.core.ui.component

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Curso activo tal como lo muestra la interfaz: el nombre y la acción de cambiarlo. Lo provee
 * el NavHost una sola vez, así el menú lateral de cualquier pantalla muestra el mismo curso y
 * su botón "Cambiar" abre el selector, sin que cada pantalla tenga que pasarlo.
 */
data class ActiveCourseUi(
    /** null mientras no se eligió ningún curso. */
    val name: String?,
    val onChangeCourse: () -> Unit
)

val LocalActiveCourse = staticCompositionLocalOf { ActiveCourseUi(name = null, onChangeCourse = {}) }

/** Ruta que el menú lateral pide con onNavigate cuando se toca "Cambiar". */
const val COURSE_SWITCHER_ROUTE = "course_switcher"
