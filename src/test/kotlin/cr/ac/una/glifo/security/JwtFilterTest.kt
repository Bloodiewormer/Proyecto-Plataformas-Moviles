package cr.ac.una.glifo.security

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JwtFilterTest {

    private val testSigningKey = "test-only-jwt-signing-key-not-for-production-0123456789abcdef"
    private val email = "student@example.test"
    private val jwtProvider = JwtProvider(testSigningKey, 60_000)
    private lateinit var userDetailsService: UserDetailsService
    private lateinit var filter: JwtFilter

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        userDetailsService = mock(UserDetailsService::class.java)
        filter = JwtFilter(jwtProvider, userDetailsService)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `valid token authenticates an active account with current authorities`() {
        val userDetails = account(active = true)
        `when`(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails)

        val authentication = authenticate("Bearer ${validToken()}")

        assertNotNull(authentication)
        assertSame(userDetails, authentication.principal)
        assertEquals(listOf("NOTE_READ_OWN"), authentication.authorities.map { it.authority })
        assertTrue(authentication.isAuthenticated)
    }

    @Test
    fun `valid token does not authenticate a disabled account`() {
        `when`(userDetailsService.loadUserByUsername(email)).thenReturn(account(active = false))

        assertNull(authenticate("Bearer ${validToken()}"))
    }

    @Test
    fun `valid token does not authenticate a locked account`() {
        val userDetails = User.withUsername(email).password("unused")
            .authorities("NOTE_READ_OWN").accountLocked(true).build()
        `when`(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails)

        assertNull(authenticate("Bearer ${validToken()}"))
    }

    @Test
    fun `valid token does not authenticate an expired account`() {
        val userDetails = User.withUsername(email).password("unused")
            .authorities("NOTE_READ_OWN").accountExpired(true).build()
        `when`(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails)

        assertNull(authenticate("Bearer ${validToken()}"))
    }

    @Test
    fun `valid token does not authenticate expired credentials`() {
        val userDetails = User.withUsername(email).password("unused")
            .authorities("NOTE_READ_OWN").credentialsExpired(true).build()
        `when`(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails)

        assertNull(authenticate("Bearer ${validToken()}"))
    }

    @Test
    fun `request without a token does not authenticate or load an account`() {
        assertNull(authenticate())

        verifyNoInteractions(userDetailsService)
    }

    @Test
    fun `malformed token does not authenticate or load an account`() {
        assertNull(authenticate("Bearer not-a-jwt"))

        verifyNoInteractions(userDetailsService)
    }

    @Test
    fun `expired token does not authenticate or load an account`() {
        val expiredToken = JwtProvider(testSigningKey, -60_000)
            .generateToken(email, 1L, listOf("NOTE_READ_OWN"))

        assertNull(authenticate("Bearer $expiredToken"))

        verifyNoInteractions(userDetailsService)
    }

    @Test
    fun `token signed with another key does not authenticate or load an account`() {
        val foreignToken = JwtProvider("another-test-only-signing-key-not-for-production-0123456789", 60_000)
            .generateToken(email, 1L, listOf("NOTE_READ_OWN"))

        assertNull(authenticate("Bearer $foreignToken"))

        verifyNoInteractions(userDetailsService)
    }

    @Test
    fun `valid token does not authenticate a deleted account`() {
        `when`(userDetailsService.loadUserByUsername(email))
            .thenThrow(UsernameNotFoundException("Test account no longer exists"))

        assertNull(authenticate("Bearer ${validToken()}"))
    }

    private fun account(active: Boolean) = GlifoUserDetails(
        userId = 1L,
        email = email,
        passwordHash = "unused",
        authoritiesList = listOf(SimpleGrantedAuthority("NOTE_READ_OWN")),
        active = active
    )

    private fun validToken() = jwtProvider.generateToken(email, 1L, listOf("ROLE_ADMIN"))

    private fun authenticate(authorization: String? = null): Authentication? {
        val request = MockHttpServletRequest("GET", "/api/v1/notes")
        if (authorization != null) {
            request.addHeader("Authorization", authorization)
        }
        var chainInvocations = 0
        var downstreamAuthentication: Authentication? = null
        val chain = FilterChain { _, _ ->
            chainInvocations++
            downstreamAuthentication = SecurityContextHolder.getContext().authentication
        }

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertEquals(1, chainInvocations)
        return downstreamAuthentication
    }
}
