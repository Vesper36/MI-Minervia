package edu.minervia.platform.security

import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.enums.AdminRole
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class AdminUserDetails(
    private val admin: Admin
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()
        authorities.add(SimpleGrantedAuthority("ROLE_${admin.role.name}"))
        if (admin.role == AdminRole.SUPER_ADMIN) {
            authorities.add(SimpleGrantedAuthority("ROLE_ADMIN"))
        }
        return authorities
    }

    override fun getPassword(): String = admin.passwordHash

    override fun getUsername(): String = admin.username

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = !admin.isLocked()

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = admin.isActive

    fun getAdminId(): Long = admin.id

    fun getRole(): AdminRole = admin.role

    fun getAdmin(): Admin = admin
}
