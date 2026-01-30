package edu.minervia.platform.security

import edu.minervia.platform.domain.repository.AdminRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AdminUserDetailsService(
    private val adminRepository: AdminRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val admin = adminRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("Admin not found: $username") }
        return AdminUserDetails(admin)
    }
}
