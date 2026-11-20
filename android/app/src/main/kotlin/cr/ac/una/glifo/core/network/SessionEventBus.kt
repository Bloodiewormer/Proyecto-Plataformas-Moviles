package cr.ac.una.glifo.core.network

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SessionEvent {
    data class SessionExpired(
        val message: String = "Tu sesión expiró. Iniciá sesión de nuevo."
    ) : SessionEvent
}

/**
 * Event bus for session-level lifecycle events (such as session expiration via 401).
 * Injected as a singleton and observed by presentation layer / navigation host.
 */
@Singleton
class SessionEventBus @Inject constructor() {
    private val _events = Channel<SessionEvent>(Channel.BUFFERED)
    val events: Flow<SessionEvent> = _events.receiveAsFlow()

    fun emit(event: SessionEvent) {
        _events.trySend(event)
    }
}
