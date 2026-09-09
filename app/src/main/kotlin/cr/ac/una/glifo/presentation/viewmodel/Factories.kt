package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cr.ac.una.glifo.domain.repository.AuthRepository
import cr.ac.una.glifo.domain.repository.SessionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * EntryPoint para obtener las dependencias (Repositorios) desde Hilt 
 * y poder inyectarlas manualmente en nuestros Factories.
 */
@EntryPoint
@InstallIn(ActivityComponent::class)
interface ViewModelFactoryEntryPoint {
    fun authRepository(): AuthRepository
    fun sessionRepository(): SessionRepository
    fun courseRepository(): cr.ac.una.glifo.domain.repository.CourseRepository
    fun noteRepository(): cr.ac.una.glifo.domain.repository.NoteRepository
    fun syncRepository(): cr.ac.una.glifo.domain.repository.SyncRepository
    fun teacherRepository(): cr.ac.una.glifo.domain.repository.TeacherRepository
    fun adminRepository(): cr.ac.una.glifo.domain.repository.AdminRepository
}

/**
 * Implementación manual de ViewModelProvider.Factory para LoginViewModel.
 * Esto demuestra cómo construir y proveer un ViewModel sin usar la inyección automática de Hilt.
 */
class LoginViewModelFactory(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(authRepository, sessionRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: \${modelClass.name}")
    }
}

class RegisterViewModelFactory(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel(authRepository, sessionRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

class HomeViewModelFactory(
    private val courseRepository: cr.ac.una.glifo.domain.repository.CourseRepository,
    private val noteRepository: cr.ac.una.glifo.domain.repository.NoteRepository,
    private val syncRepository: cr.ac.una.glifo.domain.repository.SyncRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(courseRepository, noteRepository, syncRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

class TeacherHomeViewModelFactory(
    private val courseRepository: cr.ac.una.glifo.domain.repository.CourseRepository,
    private val teacherRepository: cr.ac.una.glifo.domain.repository.TeacherRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherHomeViewModel::class.java)) {
            return TeacherHomeViewModel(courseRepository, teacherRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}

class AdminHomeViewModelFactory(
    private val adminRepository: cr.ac.una.glifo.domain.repository.AdminRepository,
    private val courseRepository: cr.ac.una.glifo.domain.repository.CourseRepository,
    private val syncRepository: cr.ac.una.glifo.domain.repository.SyncRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminHomeViewModel::class.java)) {
            return AdminHomeViewModel(adminRepository, courseRepository, syncRepository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}
