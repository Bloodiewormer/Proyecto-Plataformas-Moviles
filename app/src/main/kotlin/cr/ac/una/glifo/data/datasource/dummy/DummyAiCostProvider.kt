package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.AiCostBreakdownRow
import cr.ac.una.glifo.domain.model.AiCostPaidCall
import cr.ac.una.glifo.domain.model.AiCostStats
import cr.ac.una.glifo.domain.model.ConfidenceState
import cr.ac.una.glifo.domain.model.GlifoRole

object DummyAiCostProvider {
    private val breakdown = listOf(
        AiCostBreakdownRow(ConfidenceState.Verified, "N1 · ML Kit local", 68f, "612"),
        AiCostBreakdownRow(ConfidenceState.Repaired, "N1.5 · SimpleTex gratis", 13f, "117"),
        AiCostBreakdownRow(ConfidenceState.Escalated, "N2 · visión, en lote", 16f, "144"),
        AiCostBreakdownRow(ConfidenceState.Uncertain, "Sin resolver", 3f, "27")
    )

    private val paidCalls = listOf(
        AiCostPaidCall("IA-00 · reparación por visión", "31"),
        AiCostPaidCall("IA-01 · reconstrucción", "47"),
        AiCostPaidCall("IA-02 · flashcards y quizzes", "9"),
        AiCostPaidCall("IA-05 · adjudicación semántica", "6")
    )

    fun getStats(role: GlifoRole): AiCostStats {
        val isAdmin = role == GlifoRole.Admin
        val isTeacher = role == GlifoRole.Teacher
        return AiCostStats(
            periodLabel = when {
                isAdmin -> "Agosto · Sistema completo · 142 usuarios"
                isTeacher -> "Agosto · EIF411 · 27 estudiantes"
                else -> "Agosto · Cálculo II"
            },
            headlinePercent = if (isAdmin) 64 else 81,
            isSystemWide = isAdmin,
            quotaUsed = if (isAdmin) 1842 else null,
            quotaTotal = if (isAdmin) 2900 else null,
            breakdown = breakdown,
            paidCalls = paidCalls,
            footerNote = "Sin la escalera, 47 páginas habrían requerido 47 llamadas de visión de página completa."
        )
    }
}
