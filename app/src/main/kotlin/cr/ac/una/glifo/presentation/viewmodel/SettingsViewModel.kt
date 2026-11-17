package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.AppPreferences
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val tokenManager: TokenManager?,
    private val appPreferences: AppPreferences? = null
) : ViewModel() {

    constructor(syncRepository: SyncRepository) : this(
        syncRepository = syncRepository,
        tokenManager = null
    )

    private val _pendingCount = MutableStateFlow<UiState<Int>>(UiState.Loading)
    val pendingCount: StateFlow<UiState<Int>> = _pendingCount.asStateFlow()

    val notifySyncDone: StateFlow<Boolean> = tokenManager?.notifySyncDoneFlow
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
        ?: MutableStateFlow(true).asStateFlow()

    // Preferencias de Ajustes que sí cambian el comportamiento (ver AppPreferences).
    val rejectLowQuality: StateFlow<Boolean> = appPreferences?.rejectLowQualityFlow
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
        ?: MutableStateFlow(true).asStateFlow()

    val allowVisionEscalation: StateFlow<Boolean> = appPreferences?.allowVisionEscalationFlow
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
        ?: MutableStateFlow(true).asStateFlow()

    val syncWifiOnly: StateFlow<Boolean> = appPreferences?.syncWifiOnlyFlow
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
        ?: MutableStateFlow(false).asStateFlow()

    fun setRejectLowQuality(enabled: Boolean) {
        viewModelScope.launch { appPreferences?.setRejectLowQuality(enabled) }
    }

    fun setAllowVisionEscalation(enabled: Boolean) {
        viewModelScope.launch { appPreferences?.setAllowVisionEscalation(enabled) }
    }

    fun setSyncWifiOnly(enabled: Boolean) {
        viewModelScope.launch { appPreferences?.setSyncWifiOnly(enabled) }
    }

    init {
        viewModelScope.launch {
            _pendingCount.value = syncRepository.getPendingCount().toUiState()
        }
    }

    fun setNotifySyncDone(enabled: Boolean) {
        viewModelScope.launch {
            tokenManager?.setNotifySyncDoneEnabled(enabled)
        }
    }
}
