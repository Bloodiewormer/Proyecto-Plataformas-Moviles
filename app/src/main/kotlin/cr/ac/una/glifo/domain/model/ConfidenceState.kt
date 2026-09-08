package cr.ac.una.glifo.domain.model

/**
 * The confidence tier a piece of captured content resolved at, per the escalation ladder
 * described in Glifo_Arquitectura_Estandares.md §1.3/§12.3 (N1 verified, N1.5 repaired,
 * N2 escalated, unresolved/uncertain). Pure domain concept: every layer that talks about
 * "how sure are we about this fragment/topic/note" — coverage, note content, teacher gap
 * analysis — is expressed in terms of this same enum instead of ad hoc strings.
 */
sealed class ConfidenceState {
    data object Verified : ConfidenceState()
    data object Repaired : ConfidenceState()
    data object Escalated : ConfidenceState()
    data object Uncertain : ConfidenceState()
}
