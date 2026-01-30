package edu.minervia.platform.service

import edu.minervia.platform.config.JwtProperties
import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.repository.AdminRepository
import edu.minervia.platform.domain.repository.SystemConfigRepository
import edu.minervia.platform.security.JwtService
import edu.minervia.platform.web.dto.LoginRequest
import edu.minervia.platform.web.dto.LoginResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AuthService(
    private val adminRepository: AdminRepository,
    private val systemConfigRepository: SystemConfigRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties
) {
    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val admin = adminRepository.findByUsername(request.username)
            .orElseThrow { BadCredentialsException("Invalid username or password") }

        if (!admin.isActive) {
            throw BadCredentialsException("Account is disabled")
        }

        if (admin.isLocked()) {
            throw BadCredentialsException("Account is locked. Please try again later.")
        }

        if (!passwordEncoder.matches(request.password, admin.passwordHash)) {
            handleFailedLogin(admin)
            throw BadCredentialsException("Invalid username or password")
        }

        resetFailedAttempts(admin)

        val token = jwtService.generateToken(
            username = admin.username,
            role = admin.role.name,
            adminId = admin.id
        )

        return LoginResponse(
            token = token,
            expiresIn = jwtProperties.expiration,
            username = admin.username,
            role = admin.role.name
        )
    }

    private fun handleFailedLogin(admin: Admin) {
        val maxAttempts = getConfigValue("login_max_attempts", 5)
        val lockoutMinutes = getConfigValue("login_lockout_minutes", 30)

        admin.failedLoginAttempts++

        if (admin.failedLoginAttempts >= maxAttempts) {
            admin.lockedUntil = Instant.now().plus(lockoutMinutes.toLong(), ChronoUnit.MINUTES)
        }

        adminRepository.save(admin)
    }

    private fun resetFailedAttempts(admin: Admin) {
        if (admin.failedLoginAttempts > 0) {
            admin.failedLoginAttempts = 0
            admin.lockedUntil = null
            adminRepository.save(admin)
        }
    }

    private fun getConfigValue(key: String, default: Int): Int {
        return systemConfigRepository.findByConfigKey(key)
            .map { it.configValue.toIntOrNull() ?: default }
            .orElse(default)
    }
}
