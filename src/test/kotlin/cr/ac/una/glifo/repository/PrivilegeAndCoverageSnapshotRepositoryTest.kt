package cr.ac.una.glifo.repository

import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.ops.entity.CoverageSnapshot
import cr.ac.una.glifo.ops.repository.CoverageSnapshotRepository
import cr.ac.una.glifo.user.repository.PrivilegeRepository
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Los dos repositorios que faltaban: privilegios (RBAC) y fotos de cobertura (delta semanal). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PrivilegeAndCoverageSnapshotRepositoryTest {

    @Autowired lateinit var privilegeRepository: PrivilegeRepository
    @Autowired lateinit var roleRepository: RoleRepository
    @Autowired lateinit var snapshotRepository: CoverageSnapshotRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var courseRepository: CourseRepository

    @Test
    fun `import sql privileges are found by name and belong to their roles`() {
        val courseWrite = assertNotNull(privilegeRepository.findByName("COURSE_WRITE"))
        assertEquals(2, privilegeRepository.findByNameIn(listOf("NOTE_READ_OWN", "COURSE_WRITE")).size)

        val teacher = assertNotNull(roleRepository.findByName("ROLE_TEACHER"))
        assertTrue(courseWrite in teacher.privileges)
    }

    @Test
    fun `coverage snapshots come back in order and the delta base is the last one before a date`() {
        val student = assertNotNull(userRepository.findByEmail("estudiante@est.una.ac.cr"))
        val course = assertNotNull(courseRepository.findByJoinCode("GLF-CALC1"))
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        listOf(14L to 40f, 7L to 55f, 0L to 68f).forEach { (daysAgo, pct) ->
            snapshotRepository.save(CoverageSnapshot().apply {
                user = student
                this.course = course
                coveragePct = pct
                takenAt = now.minus(daysAgo, ChronoUnit.DAYS)
            })
        }

        val series = snapshotRepository.findByUserIdAndCourseIdOrderByTakenAtAsc(student.id, course.id)
        assertEquals(listOf(40f, 55f, 68f), series.map { it.coveragePct })

        val weekAgo = snapshotRepository.findFirstByUserIdAndCourseIdAndTakenAtBeforeOrderByTakenAtDesc(
            student.id, course.id, now.minus(1, ChronoUnit.DAYS)
        )
        assertEquals(55f, weekAgo?.coveragePct)
    }
}
