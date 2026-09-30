package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.repository.CourseRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Curso activo del docente: el que eligió en el selector o, la primera vez, el primero de
 * sus cursos (que queda guardado como activo). Null si no tiene cursos o no hay conexión.
 */
internal suspend fun ActiveCourseStore.resolveTeacherCourseId(courseRepository: CourseRepository): Long? =
    activeCourseId.value
        ?: (courseRepository.getTeacherCourses() as? Result.Success)?.value
            ?.firstOrNull()
            ?.also { set(it.id, it.name) }
            ?.id

/**
 * Carga con el curso activo ahora y otra vez cada vez que cambie; una carga en curso del
 * curso anterior se cancela. [load] recibe null si todavía no hay curso elegido.
 */
internal fun ViewModel.followActiveCourse(store: ActiveCourseStore, load: suspend (Long?) -> Unit) {
    viewModelScope.launch { store.activeCourseId.collectLatest { load(it) } }
}
