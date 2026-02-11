package edu.minervia.platform.service

import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.entity.RegistrationCode
import edu.minervia.platform.domain.entity.SystemConfig
import edu.minervia.platform.domain.enums.AdminRole
import edu.minervia.platform.domain.enums.RegistrationCodeStatus
import edu.minervia.platform.domain.repository.AdminRepository
import edu.minervia.platform.domain.repository.RegistrationCodeRepository
import edu.minervia.platform.domain.repository.SystemConfigRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyString
import org.mockito.kotlin.eq
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class RegistrationCodeServiceTest {

    @Mock
    private lateinit var registrationCodeRepository: RegistrationCodeRepository

    @Mock
    private lateinit var adminRepository: AdminRepository

    @Mock
    private lateinit var systemConfigRepository: SystemConfigRepository

    private lateinit var service: RegistrationCodeService

    @BeforeEach
    fun setUp() {
        service = RegistrationCodeService(
            registrationCodeRepository = registrationCodeRepository,
            adminRepository = adminRepository,
            systemConfigRepository = systemConfigRepository
        )
    }

    @Test
    fun generateCode_usesDefaultExpiry_andPersistsUnusedCode() {
        val admin = admin()
        `when`(adminRepository.findById(1L)).thenReturn(Optional.of(admin))
        `when`(systemConfigRepository.findByConfigKey("registration_code_expiry_days"))
            .thenReturn(Optional.of(SystemConfig(configKey = "registration_code_expiry_days", configValue = "10")))
        `when`(registrationCodeRepository.existsByCode(anyString())).thenReturn(false)
        `when`(registrationCodeRepository.save(any(RegistrationCode::class.java)))
            .thenAnswer { it.arguments[0] as RegistrationCode }

        val start = Instant.now()
        val dto = service.generateCode(1L, null)
        val end = Instant.now()

        val captor = ArgumentCaptor.forClass(RegistrationCode::class.java)
        verify(registrationCodeRepository).save(captor.capture())
        val saved = captor.value

        assertEquals(RegistrationCodeStatus.UNUSED, saved.status)
        assertEquals(admin, saved.createdBy)
        assertTrue(saved.code.length == 8)
        assertTrue(saved.code.all { codeChars.contains(it) })
        val expectedStart = start.plus(10, ChronoUnit.DAYS)
        val expectedEnd = end.plus(10, ChronoUnit.DAYS)
        assertFalse(saved.expiresAt.isBefore(expectedStart))
        assertFalse(saved.expiresAt.isAfter(expectedEnd))
        assertEquals(saved.code, dto.code)
        assertEquals(admin.username, dto.createdByUsername)
    }

    @Test
    fun generateCode_adminNotFound_throws() {
        `when`(adminRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows(NoSuchElementException::class.java) {
            service.generateCode(1L, null)
        }
    }

    @Test
    fun verifyCode_invalid_returnsMessage() {
        `when`(registrationCodeRepository.findByCode("INVALID")).thenReturn(Optional.empty())

        val result = service.verifyCode("invalid")

        assertFalse(result.first)
        assertEquals("Invalid registration code", result.second)
    }

    @Test
    fun verifyCode_expired_returnsExpiredMessage() {
        val admin = admin()
        val code = RegistrationCode(
            code = "ABCDEFGH",
            createdBy = admin,
            expiresAt = Instant.now().minusSeconds(10),
            status = RegistrationCodeStatus.UNUSED
        )
        `when`(registrationCodeRepository.findByCode(eq("ABCDEFGH"))).thenReturn(Optional.of(code))

        val result = service.verifyCode("abcdefgh")

        assertFalse(result.first)
        assertEquals("Code has expired", result.second)
    }

    @Test
    fun revokeCode_nonUnused_throws() {
        val admin = admin()
        val code = RegistrationCode(
            id = 5L,
            code = "ABCDEFGH",
            createdBy = admin,
            expiresAt = Instant.now().plusSeconds(60),
            status = RegistrationCodeStatus.USED
        )
        `when`(registrationCodeRepository.findById(5L)).thenReturn(Optional.of(code))

        assertThrows(IllegalArgumentException::class.java) {
            service.revokeCode(5L)
        }
    }

    @Test
    fun revokeCode_unused_marksRevoked_andSaves() {
        val admin = admin()
        val code = RegistrationCode(
            id = 5L,
            code = "ABCDEFGH",
            createdBy = admin,
            expiresAt = Instant.now().plusSeconds(60),
            status = RegistrationCodeStatus.UNUSED
        )
        `when`(registrationCodeRepository.findById(5L)).thenReturn(Optional.of(code))
        `when`(registrationCodeRepository.save(code)).thenReturn(code)

        val dto = service.revokeCode(5L)

        assertEquals(RegistrationCodeStatus.REVOKED, code.status)
        assertEquals(RegistrationCodeStatus.REVOKED, dto.status)
        verify(registrationCodeRepository).save(code)
    }

    private fun admin(): Admin {
        return Admin(
            id = 1L,
            username = "admin",
            email = "admin@example.com",
            passwordHash = "hash",
            role = AdminRole.ADMIN
        )
    }

    companion object {
        private const val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
