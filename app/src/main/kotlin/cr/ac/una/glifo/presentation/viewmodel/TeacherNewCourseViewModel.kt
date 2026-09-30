package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TeacherNewCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    suspend fun createCourse(name: String, code: String, term: String): Result<Course> =
        courseRepository.createCourse(name, code, term).also { result ->
            // El curso recién creado queda como activo del docente.
            if (result is Result.Success) activeCourseStore.set(result.value.id, result.value.name)
        }
}
