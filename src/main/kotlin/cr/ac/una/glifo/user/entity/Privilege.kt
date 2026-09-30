package cr.ac.una.glifo.user.entity

import jakarta.persistence.*

@Entity
@Table(name = "privileges")
class Privilege {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable = false, unique = true, length = 50)
    var name: String = ""

    @Column(length = 255)
    var description: String? = null
    
    override fun equals(other: Any?): Boolean = other is Privilege && id != 0L && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Privilege(id=$id, name='$name', description=$description)"
}
