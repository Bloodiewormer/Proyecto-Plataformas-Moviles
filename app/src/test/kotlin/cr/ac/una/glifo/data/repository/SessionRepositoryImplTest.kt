package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SessionRepositoryImplTest {

    private val tokenManager = mockk<TokenManager>(relaxed = true)
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
        repository = SessionRepositoryImpl(tokenManager)
    }

    @Test
    fun `login stores user in memory`() {
        repository.login(testUser)
        assertEquals(testUser, repository.currentUser.value)
    }

    @Test
    fun `logout clears token from tokenManager and resets in memory user state`() = runTest {
        repository.login(testUser)
        repository.setProfilePhotoUri("file:///test/photo.jpg")

        assertEquals(testUser, repository.currentUser.value)
        assertEquals("file:///test/photo.jpg", repository.profilePhotoUri.value)

        repository.logout()

        assertNull(repository.currentUser.value)
        assertNull(repository.profilePhotoUri.value)
        coVerify(exactly = 1) { tokenManager.clearAll() }
    }
}
