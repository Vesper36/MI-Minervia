package edu.minervia.platform.service

import edu.minervia.platform.config.JwtProperties
import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.repository.AdminRepository
import edu.minervia.platform.domain.repository.SystemConfigRepository
import edu.minervia.platform.security.JwtService
import edu.minervia.platform.security.TokenRevocationService
import edu.minervia.platform.security.TokenType
import edu.minervia.platform.service.auth.TotpService
import edu.minervia.platform.web.dto.LoginRequest
import edu.minervia.platform.web.dto.LoginResponse
import edu.minervia.platform.web.dto.RefreshTokenResponse
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
    private val jwtProperties: JwtProperties,
    private val tokenRevocationService: TokenRevocationService,
    private val totpService: TotpService
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

        if (admin.totpEnabled) {
            if (request.totpCode.isNullOrBlank()) {
                return LoginResponse(
                    accessToken = "",
                    refreshToken = "",
                    accessExpiresIn = 0,
                    refreshExpiresIn = 0,
                    username = admin.username,
                    role = admin.role.name,
                    requiresTotp = true
                )
            }

            if (!totpService.verifyCode(admin.id, request.totpCode)) {
                handleFailedLogin(admin)
                throw BadCredentialsException("Invalid TOTP code")
            }
        }

        resetFailedAttempts(admin)

        val tokenPair = jwtService.generateTokenPair(
            username = admin.username,
            role = admin.role.name,
            adminId = admin.id
        )

        return LoginResponse(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            accessExpiresIn = tokenPair.accessExpiresIn,
            refreshExpiresIn = tokenPair.refreshExpiresIn,
            username = admin.username,
            role = admin.role.name,
            requiresTotp = false
        )
    }

    fun refreshToken(refreshToken: String): RefreshTokenResponse {
        if (!jwtService.validateToken(refreshToken)) {
            throw BadCredentialsException("Invalid refresh token")
        }

        val tokenType = jwtService.getTokenTypeFromToken(refreshToken)
        if (tokenType != TokenType.REFRESH) {
            throw BadCredentialsException("Token is not a refresh token")
        }

        val jti = jwtService.getJtiFromToken(refreshToken)
        if (jti != null && tokenRevocationService.isRevoked(jti)) {
            throw BadCredentialsException("Refresh token has been revoked")
        }

        val username = jwtService.getUsernameFromToken(refreshToken)
            ?: throw BadCredentialsException("Invalid refresh token")

        val admin = adminRepository.findByUsername(username)
            .orElseThrow { BadCredentialsException("User not found") }

        if (!admin.isActive) {
            throw BadCredentialsException("Account is disabled")
        }

        // Revoke old refresh token (one-time use)
        jti?.let { tokenRevocationService.revokeRefreshToken(it) }

        val tokenPair = jwtService.generateTokenPair(
            username = admin.username,
            role = admin.role.name,
            adminId = admin.id
        )

        return RefreshTokenResponse(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            accessExpiresIn = tokenPair.accessExpiresIn,
            refreshExpiresIn = tokenPair.refreshExpiresIn
        )
    }

    /**
     * CONSTRAINT [JWT-REVOCATION-TRIGGERS]: Logout triggers token revocation.
     */
    fun logout(accessToken: String, refreshToken: String?) {
        jwtService.getJtiFromToken(accessToken)?.let {
            tokenRevocationService.revokeAccessToken(it)
        }
        refreshToken?.let { rt ->
            jwtService.getJtiFromToken(rt)?.let {
                tokenRevocationService.revokeRefreshToken(it)
            }
        }
    }

    /**
     * CONSTRAINT [JWT-REVOCATION-TRIGGERS]: Password reset triggers token revocation.
     * Revokes both access and refresh tokens to force full re-login.
     */
    @Transactional
    fun changePassword(adminId: Long, currentPassword: String, newPassword: String, currentAccessJti: String?, currentRefreshJti: String?) {
        val admin = adminRepository.findById(adminId)
            .orElseThrow { BadCredentialsException("Admin not found") }

        if (!passwordEncoder.matches(currentPassword, admin.passwordHash)) {
            throw BadCredentialsException("Current password is incorrect")
        }

        admin.passwordHash = passwordEncoder.encode(newPassword)
        adminRepository.save(admin)

        // Revoke both tokens to force full re-login
        currentAccessJti?.let { tokenRevocationService.revokeAccessToken(it) }
        currentRefreshJti?.let { tokenRevocationService.revokeRefreshToken(it) }
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
