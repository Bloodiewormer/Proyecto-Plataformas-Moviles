package cr.ac.una.glifo.security

import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GlifoUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("User not found with email: $username")

        val authorities = mutableSetOf<SimpleGrantedAuthority>()
        user.roles.forEach { role ->
            authorities.add(SimpleGrantedAuthority(role.name))
            role.privileges.forEach { privilege ->
                authorities.add(SimpleGrantedAuthority(privilege.name))
            }
        }

        return GlifoUserDetails(
            userId = user.id,
            email = user.email,
            passwordHash = user.passwordHash,
            authoritiesList = authorities,
            active = user.isActive
        )
    }
}
