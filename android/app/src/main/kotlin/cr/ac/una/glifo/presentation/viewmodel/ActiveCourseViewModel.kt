package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import cr.ac.una.glifo.core.datastore.ActiveCourse
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Expone el curso activo al NavHost: sus rutas por curso y lo que muestra el menú lateral. */
@HiltViewModel
class ActiveCourseViewModel @Inject constructor(
    store: ActiveCourseStore
) : ViewModel() {
    val activeCourseId: StateFlow<Long?> = store.activeCourseId
    val activeCourse: StateFlow<ActiveCourse?> = store.activeCourse
}
