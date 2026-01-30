package edu.minervia.platform.service

import edu.minervia.platform.domain.entity.EmailVerificationCode
import edu.minervia.platform.domain.entity.RegistrationApplication
import edu.minervia.platform.domain.enums.ApplicationStatus
import edu.minervia.platform.domain.repository.EmailVerificationCodeRepository
import edu.minervia.platform.domain.repository.RegistrationApplicationRepository
import edu.minervia.platform.domain.repository.SystemConfigRepository
import edu.minervia.platform.web.dto.VerifyEmailResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class EmailVerificationService(
    private val emailVerificationCodeRepository: EmailVerificationCodeRepository,
    private val registrationApplicationRepository: RegistrationApplicationRepository,
    private val systemConfigRepository: SystemConfigRepository
) {
    private val logger = LoggerFactory.getLogger(EmailVerificationService::class.java)
    private val random = SecureRandom()

    @Transactional
    fun sendVerificationCode(applicationId: Long): Boolean {
        val application = registrationApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("Application not found") }

        if (application.emailVerified) {
            throw IllegalStateException("Email already verified")
        }

        val rateLimit = getConfigValue("email_rate_limit", 5)
        val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS)
        val recentCount = emailVerificationCodeRepository.countByEmailSince(application.externalEmail, oneHourAgo)

        if (recentCount >= rateLimit) {
            throw IllegalStateException("Too many verification requests. Please try again later.")
        }

        val code = generateCode()
        val expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES)

        val verificationCode = EmailVerificationCode(
            application = application,
            code = code,
            expiresAt = expiresAt
        )

        emailVerificationCodeRepository.save(verificationCode)

        // TODO: Send email via mail service
        logger.info("Verification code {} sent to {}", code, application.externalEmail)

        return true
    }

    @Transactional
    fun verifyCode(applicationId: Long, code: String): VerifyEmailResponse {
        val application = registrationApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("Application not found") }

        if (application.emailVerified) {
            return VerifyEmailResponse(true, "Email already verified")
        }

        val verificationCode = emailVerificationCodeRepository
            .findValidCodeForApplication(applicationId, Instant.now())
            .orElse(null)
            ?: return VerifyEmailResponse(false, "No valid verification code found. Please request a new one.")

        if (verificationCode.attempts >= 5) {
            return VerifyEmailResponse(false, "Too many attempts. Please request a new code.", 0)
        }

        verificationCode.attempts++

        if (verificationCode.code != code) {
            emailVerificationCodeRepository.save(verificationCode)
            val remaining = 5 - verificationCode.attempts
            return VerifyEmailResponse(false, "Invalid code", remaining)
        }

        verificationCode.verifiedAt = Instant.now()
        emailVerificationCodeRepository.save(verificationCode)

        application.emailVerified = true
        application.status = ApplicationStatus.EMAIL_VERIFIED
        registrationApplicationRepository.save(application)

        return VerifyEmailResponse(true, "Email verified successfully")
    }

    private fun generateCode(): String {
        return (100000 + random.nextInt(900000)).toString()
    }

    private fun getConfigValue(key: String, default: Int): Int {
        return systemConfigRepository.findByConfigKey(key)
            .map { it.configValue.toIntOrNull() ?: default }
            .orElse(default)
    }
}
