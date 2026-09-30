package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Expone el curso activo al NavHost para que las rutas por curso no usen un id fijo. */
@HiltViewModel
class ActiveCourseViewModel @Inject constructor(
    private val store: ActiveCourseStore
) : ViewModel() {
    val activeCourseId: StateFlow<Long?> = store.activeCourseId

    fun select(courseId: Long) = store.set(courseId)
}
