package cr.ac.una.glifo.user.service

import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.user.dto.UpdateRolesRequest
import cr.ac.una.glifo.user.dto.UpdateStatusRequest
import cr.ac.una.glifo.user.dto.UserAdminResponse
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {

    @Transactional(readOnly = true)
    fun getAllUsers(): List<UserAdminResponse> {
        return userRepository.findAll().map { user ->
            UserAdminResponse(
                id = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                isActive = user.isActive,
                roles = user.roles.map { it.name }
            )
        }
    }

    @Transactional
    fun updateUserRoles(userId: Long, request: UpdateRolesRequest): UserAdminResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }

        val newRoles = request.roles.map { roleName ->
            val formattedRoleName = if (roleName.startsWith("ROLE_")) roleName else "ROLE_$roleName"
            roleRepository.findByName(formattedRoleName)
                ?: throw ResourceNotFoundException("Role", formattedRoleName)
        }.toMutableSet()

        if (newRoles.isEmpty()) {
            throw IllegalArgumentException("User must have at least one assigned role")
        }

        user.roles = newRoles
        val saved = userRepository.save(user)

        return UserAdminResponse(
            id = saved.id,
            email = saved.email,
            firstName = saved.firstName,
            lastName = saved.lastName,
            isActive = saved.isActive,
            roles = saved.roles.map { it.name }
        )
    }

    @Transactional
    fun updateUserStatus(userId: Long, request: UpdateStatusRequest): UserAdminResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }

        user.isActive = request.isActive
        val saved = userRepository.save(user)

        return UserAdminResponse(
            id = saved.id,
            email = saved.email,
            firstName = saved.firstName,
            lastName = saved.lastName,
            isActive = saved.isActive,
            roles = saved.roles.map { it.name }
        )
    }
}
