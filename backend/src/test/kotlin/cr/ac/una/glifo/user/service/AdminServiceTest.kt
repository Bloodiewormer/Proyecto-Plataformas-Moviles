package cr.ac.una.glifo.user.service

import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.user.dto.UpdateRolesRequest
import cr.ac.una.glifo.user.dto.UpdateStatusRequest
import cr.ac.una.glifo.user.entity.Privilege
import cr.ac.una.glifo.user.entity.Role
import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class AdminServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var roleRepository: RoleRepository

    @InjectMocks
    private lateinit var adminService: AdminService

    private lateinit var studentRole: Role
    private lateinit var adminRole: Role
    private lateinit var userManagePrivilege: Privilege
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        userManagePrivilege = Privilege().apply {
            id = 1L
            name = "USER_MANAGE"
        }

        studentRole = Role().apply {
            id = 1L
            name = "ROLE_STUDENT"
        }

        adminRole = Role().apply {
            id = 3L
            name = "ROLE_ADMIN"
            privileges.add(userManagePrivilege)
        }

        testUser = User().apply {
            id = 10L
            email = "estudiante@glifo.ac.cr"
            firstName = "Carlos"
            lastName = "Mora"
            isActive = true
            roles.add(studentRole)
        }
    }

    @Test
    fun `getAllUsers returns all users mapped to response`() {
        `when`(userRepository.findAll()).thenReturn(listOf(testUser))

        val result = adminService.getAllUsers()

        assertEquals(1, result.size)
        assertEquals("estudiante@glifo.ac.cr", result[0].email)
        assertEquals("Carlos", result[0].firstName)
        assertTrue(result[0].isActive)
        assertTrue(result[0].roles.contains("ROLE_STUDENT"))
        verify(userRepository).findAll()
    }

    @Test
    fun `updateUserRoles successfully updates user roles to admin`() {
        `when`(userRepository.findById(10L)).thenReturn(Optional.of(testUser))
        `when`(roleRepository.findByName("ROLE_ADMIN")).thenReturn(adminRole)
        `when`(userRepository.save(any(User::class.java))).thenAnswer { it.arguments[0] }

        val request = UpdateRolesRequest(roles = listOf("ROLE_ADMIN"))
        val result = adminService.updateUserRoles(10L, request)

        assertEquals(10L, result.id)
        assertTrue(result.roles.contains("ROLE_ADMIN"))
        verify(userRepository).findById(10L)
        verify(userRepository).save(testUser)
    }

    @Test
    fun `updateUserRoles throws exception when user does not exist`() {
        `when`(userRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            adminService.updateUserRoles(999L, UpdateRolesRequest(listOf("ROLE_ADMIN")))
        }
    }

    @Test
    fun `updateUserRoles throws exception when role not found`() {
        `when`(userRepository.findById(10L)).thenReturn(Optional.of(testUser))
        `when`(roleRepository.findByName("ROLE_SUPERUSER")).thenReturn(null)

        assertThrows<ResourceNotFoundException> {
            adminService.updateUserRoles(10L, UpdateRolesRequest(listOf("ROLE_SUPERUSER")))
        }
    }

    @Test
    fun `updateUserStatus activates and deactivates user`() {
        `when`(userRepository.findById(10L)).thenReturn(Optional.of(testUser))
        `when`(userRepository.save(any(User::class.java))).thenAnswer { it.arguments[0] }

        val deactivateResult = adminService.updateUserStatus(10L, UpdateStatusRequest(isActive = false))
        assertFalse(deactivateResult.isActive)

        val activateResult = adminService.updateUserStatus(10L, UpdateStatusRequest(isActive = true))
        assertTrue(activateResult.isActive)

        verify(userRepository, times(2)).save(testUser)
    }
}
