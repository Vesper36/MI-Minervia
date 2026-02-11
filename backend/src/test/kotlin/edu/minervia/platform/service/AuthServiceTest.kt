package edu.minervia.platform.service

import edu.minervia.platform.config.JwtProperties
import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.entity.SystemConfig
import edu.minervia.platform.domain.enums.AdminRole
import edu.minervia.platform.domain.repository.AdminRepository
import edu.minervia.platform.domain.repository.SystemConfigRepository
import edu.minervia.platform.security.JwtService
import edu.minervia.platform.security.TokenPair
import edu.minervia.platform.security.TokenRevocationService
import edu.minervia.platform.security.TokenType
import edu.minervia.platform.service.auth.TotpService
import edu.minervia.platform.web.dto.LoginRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.anyLong
import org.mockito.kotlin.anyString
import org.mockito.kotlin.eq
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var adminRepository: AdminRepository

    @Mock
    private lateinit var systemConfigRepository: SystemConfigRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtService: JwtService

    @Mock
    private lateinit var tokenRevocationService: TokenRevocationService

    @Mock
    private lateinit var totpService: TotpService

    private lateinit var jwtProperties: JwtProperties
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        jwtProperties = JwtProperties()
        authService = AuthService(
            adminRepository = adminRepository,
            systemConfigRepository = systemConfigRepository,
            passwordEncoder = passwordEncoder,
            jwtService = jwtService,
            jwtProperties = jwtProperties,
            tokenRevocationService = tokenRevocationService,
            totpService = totpService
        )
    }

    @Test
    fun login_success_withoutTotp_resetsFailedAttempts_andReturnsTokens() {
        val admin = admin(
            username = "user",
            passwordHash = "hash",
            failedLoginAttempts = 2,
            lockedUntil = Instant.now().plusSeconds(60),
            totpEnabled = false
        )
        `when`(adminRepository.findByUsername("user")).thenReturn(Optional.of(admin))
        `when`(passwordEncoder.matches("pw", admin.passwordHash)).thenReturn(true)
        val tokenPair = TokenPair(
            accessToken = "access",
            refreshToken = "refresh",
            accessTokenJti = "ajti",
            refreshTokenJti = "rjti",
            accessExpiresIn = 1000,
            refreshExpiresIn = 2000
        )
        `when`(jwtService.generateTokenPair(eq("user"), eq(admin.role.name), eq(admin.id))).thenReturn(tokenPair)

        val response = authService.login(LoginRequest(username = "user", password = "pw"))

        assertEquals("access", response.accessToken)
        assertEquals("refresh", response.refreshToken)
        assertFalse(response.requiresTotp)
        assertEquals(0, admin.failedLoginAttempts)
        assertNull(admin.lockedUntil)
        verify(adminRepository).save(admin)
        verify(jwtService).generateTokenPair("user", admin.role.name, admin.id)
    }

    @Test
    fun login_totpEnabled_withoutCode_requiresTotp_andSkipsTokenIssue() {
        val admin = admin(username = "user", passwordHash = "hash", totpEnabled = true)
        `when`(adminRepository.findByUsername("user")).thenReturn(Optional.of(admin))
        `when`(passwordEncoder.matches("pw", admin.passwordHash)).thenReturn(true)

        val response = authService.login(LoginRequest(username = "user", password = "pw"))

        assertTrue(response.requiresTotp)
        assertEquals("", response.accessToken)
        assertEquals("", response.refreshToken)
        verify(jwtService, never()).generateTokenPair(any<String>(), any<String>(), any<Long>())
        verify(adminRepository, never()).save(admin)
    }

    @Test
    fun login_invalidPassword_incrementsFailedAttempts_andLocksWhenMaxReached() {
        val admin = admin(username = "user", passwordHash = "hash", failedLoginAttempts = 0)
        `when`(adminRepository.findByUsername("user")).thenReturn(Optional.of(admin))
        `when`(passwordEncoder.matches("bad", admin.passwordHash)).thenReturn(false)
        `when`(systemConfigRepository.findByConfigKey("login_max_attempts"))
            .thenReturn(Optional.of(SystemConfig(configKey = "login_max_attempts", configValue = "1")))
        `when`(systemConfigRepository.findByConfigKey("login_lockout_minutes"))
            .thenReturn(Optional.of(SystemConfig(configKey = "login_lockout_minutes", configValue = "30")))

        assertThrows(BadCredentialsException::class.java) {
            authService.login(LoginRequest(username = "user", password = "bad"))
        }

        assertEquals(1, admin.failedLoginAttempts)
        assertNotNull(admin.lockedUntil)
        verify(adminRepository).save(admin)
    }

    @Test
    fun login_invalidTotpCode_throws_andIncrementsFailedAttempts() {
        val admin = admin(username = "user", passwordHash = "hash", totpEnabled = true)
        `when`(adminRepository.findByUsername("user")).thenReturn(Optional.of(admin))
        `when`(passwordEncoder.matches("pw", admin.passwordHash)).thenReturn(true)
        `when`(totpService.verifyCode(admin.id, "123456")).thenReturn(false)
        `when`(systemConfigRepository.findByConfigKey("login_max_attempts"))
            .thenReturn(Optional.of(SystemConfig(configKey = "login_max_attempts", configValue = "5")))
        `when`(systemConfigRepository.findByConfigKey("login_lockout_minutes"))
            .thenReturn(Optional.of(SystemConfig(configKey = "login_lockout_minutes", configValue = "30")))

        assertThrows(BadCredentialsException::class.java) {
            authService.login(LoginRequest(username = "user", password = "pw", totpCode = "123456"))
        }

        assertEquals(1, admin.failedLoginAttempts)
        verify(adminRepository).save(admin)
    }

    @Test
    fun refreshToken_invalidToken_throws() {
        `when`(jwtService.validateToken("bad")).thenReturn(false)

        assertThrows(BadCredentialsException::class.java) {
            authService.refreshToken("bad")
        }
    }

    @Test
    fun refreshToken_success_revokesOld_andReturnsNewPair() {
        val admin = admin(username = "user", passwordHash = "hash", isActive = true)
        `when`(jwtService.validateToken("rt")).thenReturn(true)
        `when`(jwtService.getTokenTypeFromToken("rt")).thenReturn(TokenType.REFRESH)
        `when`(jwtService.getJtiFromToken("rt")).thenReturn("oldJti")
        `when`(tokenRevocationService.isRevoked("oldJti")).thenReturn(false)
        `when`(jwtService.getUsernameFromToken("rt")).thenReturn("user")
        `when`(adminRepository.findByUsername("user")).thenReturn(Optional.of(admin))

        val tokenPair = TokenPair(
            accessToken = "newAccess",
            refreshToken = "newRefresh",
            accessTokenJti = "newAjti",
            refreshTokenJti = "newRjti",
            accessExpiresIn = 111,
            refreshExpiresIn = 222
        )
        `when`(jwtService.generateTokenPair("user", admin.role.name, admin.id)).thenReturn(tokenPair)

        val response = authService.refreshToken("rt")

        assertEquals("newAccess", response.accessToken)
        assertEquals("newRefresh", response.refreshToken)
        verify(tokenRevocationService).revokeRefreshToken("oldJti")
        verify(jwtService).generateTokenPair("user", admin.role.name, admin.id)
    }

    @Test
    fun logout_revokesAccess_andRefreshTokens_whenJtiPresent() {
        `when`(jwtService.getJtiFromToken("access")).thenReturn("ajti")
        `when`(jwtService.getJtiFromToken("refresh")).thenReturn("rjti")

        authService.logout("access", "refresh")

        verify(tokenRevocationService).revokeAccessToken("ajti")
        verify(tokenRevocationService).revokeRefreshToken("rjti")
    }

    private fun admin(
        username: String,
        passwordHash: String,
        failedLoginAttempts: Int = 0,
        lockedUntil: Instant? = null,
        totpEnabled: Boolean = false,
        isActive: Boolean = true
    ): Admin {
        return Admin(
            id = 1L,
            username = username,
            email = "$username@example.com",
            passwordHash = passwordHash,
            role = AdminRole.ADMIN,
            isActive = isActive,
            failedLoginAttempts = failedLoginAttempts,
            lockedUntil = lockedUntil,
            totpEnabled = totpEnabled
        )
    }
}
