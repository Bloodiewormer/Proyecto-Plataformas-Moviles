package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.core.datastore.ProfilePhotoStore
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException
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
    private val profilePhotoStore = mockk<ProfilePhotoStore>()
    private val api = mockk<GlifoApi>()
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
        every { profilePhotoStore.find(any()) } returns null
        coEvery { api.getAvatar() } returns Response.error(404, "".toResponseBody())
        coEvery { api.uploadAvatar(any()) } returns Response.success(204, Unit)
        coEvery { profilePhotoStore.save(10L, "content://picker/photo") } returns "/files/profile_photos/10-1.jpg"
        repository = SessionRepositoryImpl(tokenManager, localUserDataCleaner, profilePhotoStore, { api }, CoroutineScope(Dispatchers.Unconfined))
    }

    @Test
    fun `login stores user in memory and persists user to tokenManager`() {
        repository.login(testUser)
        assertEquals(testUser, repository.currentUser.value)
        coVerify(exactly = 1) { tokenManager.saveUser(testUser) }
    }

    @Test
    fun `restoreSession restores user and activates session when token and user are present`() = runTest {
        coEvery { tokenManager.getToken() } returns "jwt-valid"
        coEvery { tokenManager.getUser() } returns testUser

        val restored = repository.restoreSession()

        assertEquals(testUser, restored)
        assertEquals(testUser, repository.currentUser.value)
    }

    @Test
    fun `restoreSession returns null and does not restore when token is missing`() = runTest {
        coEvery { tokenManager.getToken() } returns null
        coEvery { tokenManager.getUser() } returns testUser

        val restored = repository.restoreSession()

        assertNull(restored)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun `restoreSession returns null when user is missing`() = runTest {
        coEvery { tokenManager.getToken() } returns "jwt-valid"
        coEvery { tokenManager.getUser() } returns null

        val restored = repository.restoreSession()

        assertNull(restored)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun `login restores the photo saved on this device for that user`() {
        every { profilePhotoStore.find(10L) } returns "/files/profile_photos/10-1.jpg"

        repository.login(testUser)

        assertEquals("/files/profile_photos/10-1.jpg", repository.profilePhotoUri.value)
    }

    @Test
    fun `saving a photo also uploads it`() = runTest {
        val local = tempFolder.newFile("10-1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }.absolutePath
        coEvery { profilePhotoStore.save(10L, "content://picker/photo") } returns local
        repository.login(testUser)

        repository.saveProfilePhoto("content://picker/photo")

        assertEquals(local, repository.profilePhotoUri.value)
        coVerify(exactly = 1) { api.uploadAvatar(any()) }
    }

    @Test
    fun `login replaces the local photo with the one on the server`() = runTest {
        val serverBytes = byteArrayOf(9, 8, 7)
        coEvery { api.getAvatar() } returns Response.success(serverBytes.toResponseBody())
        coEvery { profilePhotoStore.saveDownloaded(10L, serverBytes) } returns "/files/profile_photos/10-2.jpg"

        repository.login(testUser)

        assertEquals("/files/profile_photos/10-2.jpg", repository.profilePhotoUri.value)
    }

    @Test
    fun `login uploads the local photo when the server has none`() = runTest {
        val local = tempFolder.newFile("10-1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }.absolutePath
        every { profilePhotoStore.find(10L) } returns local

        repository.login(testUser)

        assertEquals(local, repository.profilePhotoUri.value)
        coVerify(exactly = 1) { api.uploadAvatar(any()) }
    }

    @Test
    fun `login keeps the local photo when the server cannot be reached`() = runTest {
        every { profilePhotoStore.find(10L) } returns "/files/profile_photos/10-1.jpg"
        coEvery { api.getAvatar() } throws IOException("offline")

        repository.login(testUser)

        assertEquals("/files/profile_photos/10-1.jpg", repository.profilePhotoUri.value)
        coVerify(exactly = 0) { api.uploadAvatar(any()) }
    }

    @Test
    fun `saving a photo without a session does nothing`() = runTest {
        repository.saveProfilePhoto("content://picker/photo")

        assertNull(repository.profilePhotoUri.value)
        coVerify(exactly = 0) { profilePhotoStore.save(any(), any()) }
    }

    @Test
    fun `logout clears tokenManager, clears local user data, and resets in memory user state`() = runTest {
        repository.login(testUser)
        repository.saveProfilePhoto("content://picker/photo")

        assertEquals(testUser, repository.currentUser.value)
        assertEquals("/files/profile_photos/10-1.jpg", repository.profilePhotoUri.value)

        repository.logout()

        assertNull(repository.currentUser.value)
        assertNull(repository.profilePhotoUri.value)
        coVerify(exactly = 1) { tokenManager.clearAll() }
        coVerify(exactly = 1) { localUserDataCleaner.clearUserData() }
    }

    @Test
    fun `expireSession forgets only the token and keeps local data pending sync`() = runTest {
        repository.login(testUser)

        repository.expireSession()

        assertNull(repository.currentUser.value)
        coVerify(exactly = 1) { tokenManager.clearToken() }
        coVerify(exactly = 0) { tokenManager.clearAll() }
        coVerify(exactly = 0) { localUserDataCleaner.clearUserData() }
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
            val sessionRepo = SessionRepositoryImpl(tokenManager, cleaner, null, null, CoroutineScope(Dispatchers.Unconfined))

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
