package cr.ac.una.glifo.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class GlifoUserDetails(
    val userId: Long,
    private val email: String,
    private val passwordHash: String,
    private val authoritiesList: Collection<GrantedAuthority>,
    private val active: Boolean
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = authoritiesList

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = active
}
