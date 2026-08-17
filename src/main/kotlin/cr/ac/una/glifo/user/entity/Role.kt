package cr.ac.una.glifo.user.entity

import jakarta.persistence.*

@Entity(name = "roles")
class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    var name: String = ""
    var description: String? = null
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_privileges",
        joinColumns = [JoinColumn(name = "role_id")],
        inverseJoinColumns = [JoinColumn(name = "privilege_id")]
    )
    var privileges: MutableSet<Privilege> = mutableSetOf()
    
    override fun equals(other: Any?): Boolean = other is Role && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
}
