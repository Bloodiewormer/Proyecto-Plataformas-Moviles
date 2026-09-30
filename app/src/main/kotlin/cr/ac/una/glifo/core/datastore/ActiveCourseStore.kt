package cr.ac.una.glifo.core.datastore

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** El curso activo: su id para las rutas y su nombre para mostrarlo sin pedirlo a la red. */
data class ActiveCourse(val id: Long, val name: String?)

/**
 * Último curso que eligió el usuario: es el "curso activo" del inicio y del menú (notas,
 * cobertura, estudio, cámara, docente). Es una preferencia de la app, no un dato del
 * servidor, así que va en SharedPreferences (Contexto_Curso.md §8.1).
 */
@Singleton
class ActiveCourseStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _activeCourse = MutableStateFlow(read())
    val activeCourse: StateFlow<ActiveCourse?> = _activeCourse.asStateFlow()

    private val _activeCourseId = MutableStateFlow(_activeCourse.value?.id)
    val activeCourseId: StateFlow<Long?> = _activeCourseId.asStateFlow()

    /**
     * Cambia el curso activo. Sin [name] se conserva el que ya había si es el mismo curso
     * (por ejemplo, al abrirlo desde una pantalla que todavía no cargó el detalle).
     */
    fun set(courseId: Long, name: String? = null) {
        val current = _activeCourse.value
        val resolvedName = name ?: current?.name?.takeIf { current.id == courseId }
        val next = ActiveCourse(courseId, resolvedName)
        if (current == next) return
        prefs.edit()
            .putLong(KEY_COURSE_ID, courseId)
            .putString(KEY_COURSE_NAME, resolvedName)
            .apply()
        _activeCourse.value = next
        _activeCourseId.value = courseId
    }

    /** Al cerrar sesión: el siguiente usuario no hereda el curso del anterior. */
    fun clear() {
        prefs.edit().remove(KEY_COURSE_ID).remove(KEY_COURSE_NAME).apply()
        _activeCourse.value = null
        _activeCourseId.value = null
    }

    private fun read(): ActiveCourse? {
        val id = prefs.getLong(KEY_COURSE_ID, NONE).takeIf { it != NONE } ?: return null
        return ActiveCourse(id, prefs.getString(KEY_COURSE_NAME, null))
    }

    private companion object {
        const val PREFS_NAME = "glifo_active_course"
        const val KEY_COURSE_ID = "course_id"
        const val KEY_COURSE_NAME = "course_name"
        const val NONE = -1L
    }
}
