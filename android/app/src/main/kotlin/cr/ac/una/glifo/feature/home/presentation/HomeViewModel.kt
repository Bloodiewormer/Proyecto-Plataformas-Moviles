package cr.ac.una.glifo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.core.network.GlifoApi
import cr.ac.una.glifo.core.network.dto.CourseResponse
import cr.ac.una.glifo.core.network.dto.CreateCourseRequest
import cr.ac.una.glifo.core.network.dto.JoinCourseRequest
import cr.ac.una.glifo.feature.home.domain.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val role: UserRole = UserRole.STUDENT,
    val userEmail: String = "",
    val courses: List<CourseResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val glifoApi: GlifoApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeUserSession()
    }

    private fun observeUserSession() {
        viewModelScope.launch {
            tokenManager.rolesFlow.collect { roles ->
                val derivedRole = UserRole.fromRoles(roles)
                _uiState.update { it.copy(role = derivedRole) }
                loadCourses()
            }
        }
    }

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = glifoApi.getCourses()
                _uiState.update {
                    it.copy(
                        courses = response.data ?: emptyList(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                // If offline or network error, keep current state or show message without crash
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "No se pudieron cargar los cursos"
                    )
                }
            }
        }
    }

    fun createCourse(name: String, code: String, term: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val response = glifoApi.createCourse(CreateCourseRequest(name = name, code = code, term = term))
                if (response.data != null) {
                    _uiState.update {
                        it.copy(
                            courses = it.courses + response.data,
                            isActionLoading = false,
                            successMessage = "Curso ${response.data.name} creado exitosamente"
                        )
                    }
                    onComplete(true)
                } else {
                    _uiState.update { it.copy(isActionLoading = false, errorMessage = "No se pudo crear el curso") }
                    onComplete(false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isActionLoading = false, errorMessage = e.message ?: "Error al crear curso") }
                onComplete(false)
            }
        }
    }

    fun joinCourse(joinCode: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val response = glifoApi.joinCourse(JoinCourseRequest(joinCode = joinCode.trim()))
                if (response.data != null) {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = "Te has unido al curso correctamente"
                        )
                    }
                    loadCourses()
                    onComplete(true)
                } else {
                    _uiState.update { it.copy(isActionLoading = false, errorMessage = "Código inválido o curso no encontrado") }
                    onComplete(false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isActionLoading = false, errorMessage = e.message ?: "Error al unirse al curso") }
                onComplete(false)
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
