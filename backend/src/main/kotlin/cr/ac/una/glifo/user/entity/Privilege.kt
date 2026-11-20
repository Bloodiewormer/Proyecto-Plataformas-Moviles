package cr.ac.una.glifo.user.entity

import jakarta.persistence.*
import org.hibernate.Hibernate

@Entity
@Table(name = "privileges")
class Privilege {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(nullable = false, unique = true, length = 64)
    var name: String = ""

    @Column(length = 255)
    var description: String? = null
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Privilege
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
    override fun toString(): String = "Privilege(id=$id, name='$name', description=$description)"
}
