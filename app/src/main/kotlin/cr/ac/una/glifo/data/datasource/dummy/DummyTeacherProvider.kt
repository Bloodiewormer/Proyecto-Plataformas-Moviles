package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.TeacherGapTopic
import cr.ac.una.glifo.domain.model.ConfidenceState

object DummyTeacherProvider {

    // El glosario, sugerencias y temario ya vienen del backend (TeacherRepositoryImpl).

    fun getGaps(): List<TeacherGapTopic> = listOf(
        TeacherGapTopic(ConfidenceState.Escalated, "4.3 Sockets e hilos", 70f, "19/27"),
        TeacherGapTopic(ConfidenceState.Uncertain, "6.1 Seguridad y permisos", 52f, "14/27"),
        TeacherGapTopic(ConfidenceState.Repaired, "3.2 Jetpack Compose", 30f, "8/27")
    )
}
