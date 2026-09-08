package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TeacherNewCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    suspend fun createCourse(name: String, code: String, term: String): Result<Course> =
        courseRepository.createCourse(name, code, term)
}
