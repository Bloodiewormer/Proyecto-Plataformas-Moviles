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
