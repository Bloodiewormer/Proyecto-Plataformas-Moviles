package cr.ac.una.glifo.repository

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.Enrollment
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NotePage
import cr.ac.una.glifo.note.repository.NotePageRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaRepositoryAndEntityIntegrationTest {

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var courseRepository: CourseRepository

    @Autowired
    private lateinit var syllabusTopicRepository: SyllabusTopicRepository

    @Autowired
    private lateinit var enrollmentRepository: EnrollmentRepository

    @Autowired
    private lateinit var noteRepository: NoteRepository

    @Autowired
    private lateinit var notePageRepository: NotePageRepository

    @Test
    fun `debe persistir usuario con relacion ManyToMany a roles cargados de import_sql`() {
        val studentRole = roleRepository.findByName("ROLE_STUDENT")
        assertNotNull(studentRole, "El rol ROLE_STUDENT debe existir desde import.sql")

        val newUser = User().apply {
            email = "test_lab4@una.cr"
            passwordHash = "hash12345"
            firstName = "Test"
            lastName = "Estudiante"
            roles.add(studentRole!!)
        }

        val savedUser = userRepository.save(newUser)
        assertNotNull(savedUser.id)
        assertTrue(savedUser.id > 0)

        val retrieved = userRepository.findByEmail("test_lab4@una.cr")
        assertNotNull(retrieved)
        assertEquals("Test", retrieved?.firstName)
        assertEquals(1, retrieved?.roles?.size)
        assertEquals("ROLE_STUDENT", retrieved?.roles?.first()?.name)

        // Verificación de toString seguro (sin StackOverflowError)
        assertFalse(retrieved.toString().isEmpty())
        assertTrue(retrieved.toString().contains("test_lab4@una.cr"))
    }

    @Test
    fun `debe persistir curso con relacion OneToMany a SyllabusTopics ordenados`() {
        val teacher = userRepository.findByEmail("docente@una.ac.cr")
            ?: userRepository.findAll().first()

        val course = Course().apply {
            owner = teacher
            code = "EIF411"
            name = "Diseño y Programación de Plataformas Móviles"
            term = "II-2026"
            joinCode = "UNA-EIF411"
        }

        val savedCourse = courseRepository.save(course)
        assertTrue(savedCourse.id > 0)

        val topic1 = SyllabusTopic().apply {
            this.course = savedCourse
            code = "T1"
            title = "Arquitectura y Fundamentos de Móviles"
            orderIndex = 1
        }
        val topic2 = SyllabusTopic().apply {
            this.course = savedCourse
            code = "T2"
            title = "Programación Distribuida y Persistencia"
            orderIndex = 2
        }

        savedCourse.addSyllabusTopic(topic1)
        savedCourse.addSyllabusTopic(topic2)
        syllabusTopicRepository.save(topic1)
        syllabusTopicRepository.save(topic2)

        val topics = syllabusTopicRepository.findByCourseIdOrderByOrderIndex(savedCourse.id)
        assertEquals(2, topics.size)
        assertEquals("T1", topics[0].code)
        assertEquals("T2", topics[1].code)
    }

    @Test
    fun `debe persistir matricula Enrollment relacionando User y Course`() {
        val student = userRepository.findByEmail("estudiante@est.una.ac.cr")
            ?: userRepository.findAll().first()

        val teacher = userRepository.findByEmail("docente@una.ac.cr")
            ?: student

        val course = courseRepository.save(Course().apply {
            owner = teacher
            code = "MAT101"
            name = "Cálculo I"
            term = "II-2026"
            joinCode = "MAT101-TST"
        })

        val enrollment = Enrollment().apply {
            this.user = student
            this.course = course
            this.status = "ACTIVE"
        }
        enrollmentRepository.save(enrollment)

        assertTrue(enrollmentRepository.existsByUserIdAndCourseId(student.id, course.id))
        val enrollments = enrollmentRepository.findByUserId(student.id)
        assertTrue(enrollments.any { it.course?.id == course.id })
    }

    @Test
    fun `debe persistir Note y NotePages en cascada mediante relacion OneToMany y ManyToOne`() {
        val student = userRepository.findByEmail("estudiante@est.una.ac.cr")
            ?: userRepository.findAll().first()

        val course = courseRepository.save(Course().apply {
            owner = student
            code = "EIF411"
            name = "Móviles"
            term = "II-2026"
            joinCode = "TST-NOTE"
        })

        val note = Note().apply {
            this.user = student
            this.course = course
            this.title = "Apuntes Sesión 5 - Persistencia"
            this.classDate = LocalDate.now()
            this.status = "READY"
        }

        val page1 = NotePage().apply {
            this.pageIndex = 0
            this.perceptualHash = "a1b2c3d4e5f60718"
            this.storageUri = "file:///storage/notes/page_0.jpg"
            this.levelReached = "N3"
            this.overallConfidence = 0.94f
        }

        val page2 = NotePage().apply {
            this.pageIndex = 1
            this.perceptualHash = "f6e5d4c3b2a18170"
            this.storageUri = "file:///storage/notes/page_1.jpg"
            this.levelReached = "N2"
            this.overallConfidence = 0.88f
        }

        note.addPage(page1)
        note.addPage(page2)

        val savedNote = noteRepository.save(note)
        assertTrue(savedNote.id > 0)

        // Cascada: las páginas se deben haber persistido automáticamente
        val pages = notePageRepository.findByNoteIdOrderByPageIndex(savedNote.id)
        assertEquals(2, pages.size)
        assertEquals("a1b2c3d4e5f60718", pages[0].perceptualHash)
        assertEquals("f6e5d4c3b2a18170", pages[1].perceptualHash)

        // Verificación de consultas derivadas del repositorio
        assertTrue(notePageRepository.existsByNoteIdAndPerceptualHash(savedNote.id, "a1b2c3d4e5f60718"))
        assertNotNull(notePageRepository.findByPerceptualHash("f6e5d4c3b2a18170"))
        assertEquals(2L, notePageRepository.countByNoteId(savedNote.id))

        // Verificación de que toString() no produce recursión circular
        assertDoesNotThrow {
            val noteString = savedNote.toString()
            val pageString = pages[0].toString()
            assertTrue(noteString.contains("Apuntes Sesión 5"))
            assertTrue(pageString.contains("a1b2c3d4e5f60718"))
        }
    }

    @Test
    fun `debe eliminar pagina huerfana de la base de datos al removerla de Note por orphanRemoval`() {
        val student = userRepository.findByEmail("estudiante@est.una.ac.cr")
            ?: userRepository.findAll().first()

        val course = courseRepository.save(Course().apply {
            owner = student
            code = "EIF411-ORPHAN"
            name = "Móviles Orphan Test"
            term = "II-2026"
            joinCode = "TST-ORPHAN"
        })

        val note = Note().apply {
            this.user = student
            this.course = course
            this.title = "Nota para prueba de orphanRemoval"
            this.classDate = LocalDate.now()
            this.status = "DRAFT"
        }

        val page1 = NotePage().apply {
            this.pageIndex = 0
            this.perceptualHash = "1111222233334444"
            this.storageUri = "file:///storage/notes/p1.jpg"
            this.levelReached = "N0"
            this.overallConfidence = 0.9f
        }

        val page2 = NotePage().apply {
            this.pageIndex = 1
            this.perceptualHash = "5555666677778888"
            this.storageUri = "file:///storage/notes/p2.jpg"
            this.levelReached = "N1"
            this.overallConfidence = 0.85f
        }

        note.addPage(page1)
        note.addPage(page2)

        val savedNote = noteRepository.save(note)
        entityManager.flush()
        entityManager.clear()

        // 1. Verificar que inicialmente se persistieron 2 páginas
        assertEquals(2L, notePageRepository.countByNoteId(savedNote.id))

        // 2. Volver a cargar la nota desde el repositorio
        val loadedNote = noteRepository.findById(savedNote.id).orElseThrow()
        assertEquals(2, loadedNote.pages.size)

        // 3. Remover la primera página de la colección
        val pageToRemove = loadedNote.pages.first()
        loadedNote.removePage(pageToRemove)

        // 4. Guardar, flush() y clear() del EntityManager
        noteRepository.save(loadedNote)
        entityManager.flush()
        entityManager.clear()

        // 5. Verificar que en BD queda exactamente 1 página para esa nota
        assertEquals(1L, notePageRepository.countByNoteId(savedNote.id))
        val remainingPages = notePageRepository.findByNoteIdOrderByPageIndex(savedNote.id)
        assertEquals(1, remainingPages.size)
        assertEquals("5555666677778888", remainingPages.first().perceptualHash)
    }
}
