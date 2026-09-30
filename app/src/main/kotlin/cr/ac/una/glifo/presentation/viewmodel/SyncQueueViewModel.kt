package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.SyncQueueItem
import cr.ac.una.glifo.domain.model.SyncRunSummary
import cr.ac.una.glifo.domain.model.SyncStatus
import cr.ac.una.glifo.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncQueueUiData(
    val items: List<SyncQueueItem>,
    val syncingCount: Int,
    /** Resultado de la última vez que se pidió sincronizar desde esta pantalla. */
    val lastRun: SyncRunSummary? = null
)

/** Cola offline real (#26): lo que hay en el outbox de Room y los controles para vaciarlo. */
@HiltViewModel
class SyncQueueViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<SyncQueueUiData>>(UiState.Loading)
    val state: StateFlow<UiState<SyncQueueUiData>> = _state.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private var lastRun: SyncRunSummary? = null

    init { load() }

    fun load() {
        viewModelScope.launch { refresh() }
    }

    /** Vacía la cola ahora (el worker igual lo hace solo cuando hay red). */
    fun syncNow() {
        if (_running.value) return
        viewModelScope.launch {
            _running.value = true
            (syncRepository.syncNow() as? Result.Success)?.value?.let { lastRun = it }
            refresh()
            _running.value = false
        }
    }

    fun retryFailed() {
        viewModelScope.launch {
            syncRepository.retryFailed()
            refresh()
        }
    }

    private suspend fun refresh() {
        when (val result = syncRepository.getQueueItems()) {
            is Result.Success -> _state.value = UiState.Success(
                SyncQueueUiData(
                    items = result.value,
                    syncingCount = result.value.count { it.status == SyncStatus.Syncing },
                    lastRun = lastRun
                )
            )
            is Result.Failure -> _state.value = UiState.Error(result.error)
        }
    }
}
