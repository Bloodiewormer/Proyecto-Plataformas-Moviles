package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.repository.CourseRepository

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
