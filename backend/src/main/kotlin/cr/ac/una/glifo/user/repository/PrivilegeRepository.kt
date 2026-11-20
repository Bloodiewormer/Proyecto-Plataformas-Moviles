package cr.ac.una.glifo.user.repository

import cr.ac.una.glifo.user.entity.Privilege
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PrivilegeRepository : JpaRepository<Privilege, Long> {
    fun findByName(name: String): Privilege?
    fun findByNameIn(names: Collection<String>): List<Privilege>
}
