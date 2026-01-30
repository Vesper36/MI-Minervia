package edu.minervia.platform.service

import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.entity.RegistrationCode
import edu.minervia.platform.domain.enums.RegistrationCodeStatus
import edu.minervia.platform.domain.repository.AdminRepository
import edu.minervia.platform.domain.repository.RegistrationCodeRepository
import edu.minervia.platform.domain.repository.SystemConfigRepository
import edu.minervia.platform.web.dto.RegistrationCodeDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class RegistrationCodeService(
    private val registrationCodeRepository: RegistrationCodeRepository,
    private val adminRepository: AdminRepository,
    private val systemConfigRepository: SystemConfigRepository
) {
    private val random = SecureRandom()
    private val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    @Transactional
    fun generateCode(adminId: Long, expirationDays: Int? = null): RegistrationCodeDto {
        val admin = adminRepository.findById(adminId)
            .orElseThrow { NoSuchElementException("Admin not found") }

        val days = expirationDays ?: getDefaultExpirationDays()
        val code = generateUniqueCode()
        val expiresAt = Instant.now().plus(days.toLong(), ChronoUnit.DAYS)

        val registrationCode = RegistrationCode(
            code = code,
            createdBy = admin,
            expiresAt = expiresAt
        )

        return registrationCodeRepository.save(registrationCode).toDto()
    }

    @Transactional
    fun generateBatch(adminId: Long, count: Int, expirationDays: Int? = null): List<RegistrationCodeDto> {
        return (1..count).map { generateCode(adminId, expirationDays) }
    }

    fun verifyCode(code: String): Pair<Boolean, String?> {
        val registrationCode = registrationCodeRepository.findByCode(code.uppercase())
            .orElse(null) ?: return false to "Invalid registration code"

        return when {
            registrationCode.status == RegistrationCodeStatus.USED -> false to "Code already used"
            registrationCode.status == RegistrationCodeStatus.REVOKED -> false to "Code has been revoked"
            registrationCode.isExpired() -> false to "Code has expired"
            else -> true to null
        }
    }

    @Transactional
    fun revokeCode(codeId: Long): RegistrationCodeDto {
        val code = registrationCodeRepository.findById(codeId)
            .orElseThrow { NoSuchElementException("Code not found") }

        if (code.status != RegistrationCodeStatus.UNUSED) {
            throw IllegalArgumentException("Only unused codes can be revoked")
        }

        code.status = RegistrationCodeStatus.REVOKED
        return registrationCodeRepository.save(code).toDto()
    }

    fun getCodeById(id: Long): RegistrationCodeDto {
        return registrationCodeRepository.findById(id)
            .orElseThrow { NoSuchElementException("Code not found") }
            .toDto()
    }

    fun getAllCodes(pageable: Pageable): Page<RegistrationCodeDto> {
        return registrationCodeRepository.findAll(pageable).map { it.toDto() }
    }

    fun getCodesByStatus(status: RegistrationCodeStatus, pageable: Pageable): Page<RegistrationCodeDto> {
        return registrationCodeRepository.findAllByStatus(status, pageable).map { it.toDto() }
    }

    @Transactional
    fun markExpiredCodes(): Int {
        return registrationCodeRepository.markExpiredCodes(Instant.now())
    }

    fun getCodeByCode(code: String): RegistrationCode? {
        return registrationCodeRepository.findByCode(code.uppercase()).orElse(null)
    }

    private fun generateUniqueCode(): String {
        var code: String
        do {
            code = (1..8).map { codeChars[random.nextInt(codeChars.length)] }.joinToString("")
        } while (registrationCodeRepository.existsByCode(code))
        return code
    }

    private fun getDefaultExpirationDays(): Int {
        return systemConfigRepository.findByConfigKey("registration_code_expiry_days")
            .map { it.configValue.toIntOrNull() ?: 30 }
            .orElse(30)
    }

    private fun RegistrationCode.toDto() = RegistrationCodeDto(
        id = this.id,
        code = this.code,
        status = this.status,
        createdByUsername = this.createdBy.username,
        usedByUsername = this.usedBy?.username,
        expiresAt = this.expiresAt,
        usedAt = this.usedAt,
        createdAt = this.createdAt
    )
}
