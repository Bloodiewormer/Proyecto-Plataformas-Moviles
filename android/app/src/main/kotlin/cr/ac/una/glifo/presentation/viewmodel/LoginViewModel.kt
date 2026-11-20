package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.DemoAccountHint
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.AuthRepository
import cr.ac.una.glifo.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel internal constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val retryDelayMs: Long
) : ViewModel() {

    @Inject
    constructor(
        authRepository: AuthRepository,
        sessionRepository: SessionRepository
    ) : this(
        authRepository = authRepository,
        sessionRepository = sessionRepository,
        retryDelayMs = RETRY_DELAY_MS
    )

    private val _demoAccountHints = MutableStateFlow<List<DemoAccountHint>>(emptyList())
    val demoAccountHints: StateFlow<List<DemoAccountHint>> = _demoAccountHints.asStateFlow()

    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()

    init {
        // En el APK de release las cuentas demo no se muestran (salvo que se pida en local.properties).
        if (cr.ac.una.glifo.BuildConfig.SHOW_DEMO_ACCOUNTS) viewModelScope.launch {
            val result = authRepository.getDemoAccountHints()
            if (result is Result.Success) _demoAccountHints.value = result.value
        }
    }

    /** Logs in and, on success, stores the user in [SessionRepository]. Caller drives loading UI. */
    suspend fun login(email: String, password: String): Result<User> {
        val firstAttempt = authRepository.login(email, password)
        val finalResult = if (firstAttempt is Result.Failure && firstAttempt.error is AppError.NoConnection) {
            _isRetrying.value = true
            try {
                if (retryDelayMs > 0L) delay(retryDelayMs)
                authRepository.login(email, password)
            } finally {
                _isRetrying.value = false
            }
        } else {
            firstAttempt
        }
        if (finalResult is Result.Success) sessionRepository.login(finalResult.value)
        return finalResult
    }

    private companion object {
        const val RETRY_DELAY_MS = 1_500L
    }
}
