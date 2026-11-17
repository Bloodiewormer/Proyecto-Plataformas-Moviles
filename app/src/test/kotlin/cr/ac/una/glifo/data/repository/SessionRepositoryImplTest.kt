package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.data.datasource.local.GlifoDatabase
import cr.ac.una.glifo.data.datasource.local.dao.StudyItemDao
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.model.QuizResultInfo
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.CourseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.Executors

class SessionRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val localUserDataCleaner = mockk<LocalUserDataCleaner>(relaxed = true)
    private lateinit var repository: SessionRepositoryImpl

    private val testUser = User(
        id = 10L,
        email = "brandon@una.ac.cr",
        fullName = "Brandon Brenes",
        role = GlifoRole.Student
    )

    @Before
    fun setUp() {
        coEvery { tokenManager.clearAll() } returns Unit
        coEvery { localUserDataCleaner.clearUserData() } returns Unit
        repository = SessionRepositoryImpl(tokenManager, localUserDataCleaner)
    }

    @Test
    fun `login stores user in memory`() {
        repository.login(testUser)
        assertEquals(testUser, repository.currentUser.value)
    }

    @Test
    fun `logout clears tokenManager, clears local user data, and resets in memory user state`() = runTest {
        repository.login(testUser)
        repository.setProfilePhotoUri("file:///test/photo.jpg")

        assertEquals(testUser, repository.currentUser.value)
        assertEquals("file:///test/photo.jpg", repository.profilePhotoUri.value)

        repository.logout()

        assertNull(repository.currentUser.value)
        assertNull(repository.profilePhotoUri.value)
        coVerify(exactly = 1) { tokenManager.clearAll() }
        coVerify(exactly = 1) { localUserDataCleaner.clearUserData() }
    }

    @Test
    fun `localUserDataCleaner clears Room off caller thread, clears active course, deletes pages dir, and clears caches`() = runTest {
        val callerThreadName = Thread.currentThread().name
        var clearTablesThreadName: String? = null
        val database = mockk<GlifoDatabase>()
        every { database.clearAllTables() } answers {
            clearTablesThreadName = Thread.currentThread().name
        }
        val activeCourseStore = mockk<ActiveCourseStore>(relaxed = true)
        val registry = SessionCacheRegistry()

        val studyRepo = StudyRepositoryImpl(
            api = mockk<GlifoApi>(),
            studyItemDao = mockk<StudyItemDao>(relaxed = true),
            courseRepository = mockk<CourseRepository>(),
            sessionCacheRegistry = registry
        )
        studyRepo.saveQuizResult(
            1L,
            QuizResultInfo(
                score = 3,
                total = 3,
                headline = "Perfect",
                subtext = "",
                review = emptyList(),
                rescheduledCount = null,
                daysUntilReturn = null
            )
        )
        assertEquals(3, (studyRepo.getQuizResult(1L) as Result.Success).value?.score)

        val coverageRepo = CoverageRepositoryImpl(
            api = mockk<GlifoApi>(),
            sessionCacheRegistry = registry
        )
        coverageRepo.setLastEvaluationIso(1L, "2026-11-17T00:00:00Z")
        assertEquals("2026-11-17T00:00:00Z", coverageRepo.getLastEvaluationIso(1L))

        val pagesDir = tempFolder.newFolder("pages")
        val pageFile = File(pagesDir, "sample.jpg").apply { writeText("img") }
        val otherDir = tempFolder.newFolder("keep")
        val otherFile = File(otherDir, "keep.txt").apply { writeText("ok") }

        val ioExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "test-io-thread") }
        try {
            val cleaner = LocalUserDataCleaner(
                database = database,
                activeCourseStore = activeCourseStore,
                sessionCacheRegistry = registry,
                pagesRootDir = pagesDir,
                ioDispatcher = ioExecutor.asCoroutineDispatcher()
            )
            val sessionRepo = SessionRepositoryImpl(tokenManager, cleaner)

            sessionRepo.logout()

            verify(exactly = 1) { activeCourseStore.clear() }
            verify(exactly = 1) { database.clearAllTables() }
            assertEquals("test-io-thread", clearTablesThreadName)
            assertNotEquals(callerThreadName, clearTablesThreadName)
            assertFalse(pageFile.exists())
            assertFalse(pagesDir.exists())
            assertTrue(otherFile.exists())
            assertNull((studyRepo.getQuizResult(1L) as Result.Success).value)
            assertNull(coverageRepo.getLastEvaluationIso(1L))
        } finally {
            ioExecutor.shutdownNow()
        }
    }
}
