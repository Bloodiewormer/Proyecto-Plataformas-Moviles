package cr.ac.una.glifo.core.ui.component

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import cr.ac.una.glifo.domain.model.GlifoRole

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

/**
 * Rol de la sesión iniciada. El menú lateral lo usa antes que el rol que le pasa cada pantalla,
 * así un estudiante nunca ve el menú de docente aunque llegue a una pantalla de docente.
 * null = sin sesión (previews): se usa el rol del parámetro.
 */
val LocalSessionRole = compositionLocalOf<GlifoRole?> { null }

/** Cuántas capturas faltan por sincronizar; el menú lo consulta antes de cerrar sesión, que borra los datos locales. */
val LocalPendingSyncCount = staticCompositionLocalOf<suspend () -> Int> { { 0 } }

/** Ruta que el menú lateral pide con onNavigate cuando se toca "Cambiar". */
const val COURSE_SWITCHER_ROUTE = "course_switcher"
