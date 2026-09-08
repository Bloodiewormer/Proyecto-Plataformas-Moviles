package cr.ac.una.glifo.domain.model

/**
 * The three account roles Glifo supports. Pure domain concept — drives what a user can
 * see and do across the whole app (drawer sections, home screen variant, capabilities
 * exposed by [cr.ac.una.glifo.domain.repository.AdminRepository], etc.).
 */
enum class GlifoRole {
    Student, Teacher, Admin
}
