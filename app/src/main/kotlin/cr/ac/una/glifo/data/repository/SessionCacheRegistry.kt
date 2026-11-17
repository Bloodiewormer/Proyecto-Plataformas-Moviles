package cr.ac.una.glifo.data.repository

import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCacheRegistry @Inject constructor() {
    private val clearActions = CopyOnWriteArrayList<() -> Unit>()

    fun register(clearAction: () -> Unit) {
        clearActions += clearAction
    }

    fun clearAll() {
        clearActions.forEach { it() }
    }
}
