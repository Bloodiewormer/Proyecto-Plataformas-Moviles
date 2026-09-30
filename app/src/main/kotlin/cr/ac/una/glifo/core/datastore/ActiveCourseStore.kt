package cr.ac.una.glifo.core.datastore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Último curso que abrió el estudiante: es el "curso activo" del inicio y del menú (notas,
 * cobertura, estudio, cámara). Es una preferencia de la app, no un dato del servidor, así
 * que va en SharedPreferences (Contexto_Curso.md §8.1).
 */
@Singleton
class ActiveCourseStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _activeCourseId = MutableStateFlow(prefs.getLong(KEY_COURSE_ID, NONE).takeIf { it != NONE })
    val activeCourseId: StateFlow<Long?> = _activeCourseId.asStateFlow()

    fun set(courseId: Long) {
        if (_activeCourseId.value == courseId) return
        prefs.edit().putLong(KEY_COURSE_ID, courseId).apply()
        _activeCourseId.value = courseId
    }

    /** Al cerrar sesión: el siguiente usuario no hereda el curso del anterior. */
    fun clear() {
        prefs.edit().remove(KEY_COURSE_ID).apply()
        _activeCourseId.value = null
    }

    private companion object {
        const val PREFS_NAME = "glifo_active_course"
        const val KEY_COURSE_ID = "course_id"
        const val NONE = -1L
    }
}
