package edu.minervia.platform.service

import edu.minervia.platform.domain.entity.RegistrationApplication
import edu.minervia.platform.domain.enums.ApplicationStatus
import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.RegistrationCodeStatus
import edu.minervia.platform.domain.repository.AdminRepository
import edu.minervia.platform.domain.repository.RegistrationApplicationRepository
import edu.minervia.platform.domain.repository.RegistrationCodeRepository
import edu.minervia.platform.domain.repository.SystemConfigRepository
import edu.minervia.platform.service.async.RegistrationTaskPublisher
import edu.minervia.platform.service.email.EmailService
import edu.minervia.platform.web.dto.RegistrationApplicationDto
import edu.minervia.platform.web.dto.StartRegistrationRequest
import edu.minervia.platform.web.dto.UpdateRegistrationInfoRequest
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class RegistrationService(
    private val registrationApplicationRepository: RegistrationApplicationRepository,
    private val registrationCodeRepository: RegistrationCodeRepository,
    private val registrationCodeService: RegistrationCodeService,
    private val adminRepository: AdminRepository,
    private val systemConfigRepository: SystemConfigRepository,
    private val emailService: EmailService,
    private val taskPublisher: RegistrationTaskPublisher
) {
    private val log = LoggerFactory.getLogger(RegistrationService::class.java)

    @Transactional
    fun startRegistration(
        request: StartRegistrationRequest,
        ipAddress: String?,
        userAgent: String?
    ): RegistrationApplicationDto {
        // Check IP rate limit
        val ipRateLimit = getConfigValue("ip_rate_limit", 3)
        val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS)

        if (ipAddress != null) {
            val ipCount = registrationApplicationRepository.countByIpAddressSince(ipAddress, oneHourAgo)
            if (ipCount >= ipRateLimit) {
                throw IllegalStateException("Too many registration attempts. Please try again later.")
            }
        }

        // Check email rate limit (24 hours)
        val oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS)
        if (registrationApplicationRepository.existsByExternalEmailAndCreatedAtAfter(request.email, oneDayAgo)) {
            throw IllegalStateException("A registration with this email already exists")
        }

        // Atomically claim the registration code to prevent concurrent use
        val registrationCode = registrationCodeService.getCodeByCode(request.code)
            ?: throw IllegalArgumentException("Registration code not found")

        if (!registrationCode.isValid()) {
            throw IllegalArgumentException("Registration code is invalid or already used")
        }

        // Atomic update: only claim if still UNUSED
        val claimed = registrationCodeRepository.claimCode(registrationCode.id, Instant.now())
        if (claimed == 0) {
            throw IllegalArgumentException("Registration code has already been used")
        }

        // Validate country code for international students
        if (request.identityType == IdentityType.INTERNATIONAL && request.countryCode.isNullOrBlank()) {
            throw IllegalArgumentException("Country code is required for international students")
        }

        val countryCode = if (request.identityType == IdentityType.LOCAL) "PL" else request.countryCode

        val application = RegistrationApplication(
            registrationCode = registrationCode,
            externalEmail = request.email,
            identityType = request.identityType,
            countryCode = countryCode,
            ipAddress = ipAddress,
            userAgent = userAgent
        )

        return registrationApplicationRepository.save(application).toDto()
    }

    @Transactional
    fun updateRegistrationInfo(applicationId: Long, request: UpdateRegistrationInfoRequest): RegistrationApplicationDto {
        val application = registrationApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("Application not found") }

        if (application.status != ApplicationStatus.EMAIL_VERIFIED) {
            throw IllegalStateException("Email must be verified before updating info")
        }

        application.majorId = request.majorId
        application.classId = request.classId
        if (request.countryCode != null) {
            application.countryCode = request.countryCode
        }
        application.status = ApplicationStatus.INFO_SELECTED

        return registrationApplicationRepository.save(application).toDto()
    }

    @Transactional
    fun submitApplication(applicationId: Long): RegistrationApplicationDto {
        val application = registrationApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("Application not found") }

        if (application.status != ApplicationStatus.INFO_SELECTED) {
            throw IllegalStateException("Application info must be completed before submission")
        }

        application.status = ApplicationStatus.PENDING_APPROVAL
        return registrationApplicationRepository.save(application).toDto()
    }

    fun getApplication(applicationId: Long): RegistrationApplicationDto {
        return registrationApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("Application not found") }
            .toDto()
    }

    fun getPendingApplications(pageable: Pageable): Page<RegistrationApplicationDto> {
        return registrationApplicationRepository.findPendingApplications(pageable).map { it.toDto() }
    }

    fun getApplicationsByStatus(status: ApplicationStatus, pageable: Pageable): Page<RegistrationApplicationDto> {
        return registrationApplicationRepository.findAllByStatus(status, pageable).map { it.toDto() }
    }

    @Transactional
    fun approveApplication(applicationId: Long, adminId: Long): RegistrationApplicationDto {
        val application = registrationApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("Application not found") }

        if (application.status != ApplicationStatus.PENDING_APPROVAL) {
            throw IllegalStateException("Application is not pending approval")
        }

        val admin = adminRepository.findById(adminId)
            .orElseThrow { NoSuchElementException("Admin not found") }

        application.status = ApplicationStatus.APPROVED
        application.approvedBy = admin
        application.approvedAt = Instant.now()

        application.status = ApplicationStatus.GENERATING
        val savedApplication = registrationApplicationRepository.save(application)

        taskPublisher.publishRegistrationTask(savedApplication)

        return savedApplication.toDto()
    }

    @Transactional
    fun rejectApplication(applicationId: Long, adminId: Long, reason: String): RegistrationApplicationDto {
        val application = registrationApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("Application not found") }

        if (application.status != ApplicationStatus.PENDING_APPROVAL) {
            throw IllegalStateException("Application is not pending approval")
        }

        val admin = adminRepository.findById(adminId)
            .orElseThrow { NoSuchElementException("Admin not found") }

        application.status = ApplicationStatus.REJECTED
        application.approvedBy = admin
        application.approvedAt = Instant.now()
        application.rejectionReason = reason

        val savedApplication = registrationApplicationRepository.save(application)

        sendRejectionEmailIfNeeded(savedApplication, reason)

        return savedApplication.toDto()
    }

    @Transactional
    fun batchApprove(applicationIds: List<Long>, adminId: Long): List<RegistrationApplicationDto> {
        return applicationIds.map { approveApplication(it, adminId) }
    }

    private fun getConfigValue(key: String, default: Int): Int {
        return systemConfigRepository.findByConfigKey(key)
            .map { it.configValue.toIntOrNull() ?: default }
            .orElse(default)
    }

    private fun sendRejectionEmailIfNeeded(application: RegistrationApplication, reason: String) {
        if (application.rejectionEmailSentAt != null) {
            log.info("Rejection email already sent for application {}", application.id)
            return
        }

        val locale = application.countryCode?.let { mapCountryToLocale(it) } ?: "en"
        val result = emailService.sendRejectionEmail(
            to = application.externalEmail,
            applicantName = "Applicant",
            reason = reason,
            locale = locale
        )

        if (result.success) {
            application.rejectionEmailSentAt = Instant.now()
            registrationApplicationRepository.save(application)
            log.info("Rejection email sent for application {}", application.id)
        } else {
            log.warn("Failed to send rejection email for application {}: {}", application.id, result.errorMessage)
        }
    }

    private fun mapCountryToLocale(countryCode: String): String = when (countryCode.uppercase()) {
        "PL" -> "pl"
        "CN" -> "zh-CN"
        else -> "en"
    }

    private fun RegistrationApplication.toDto() = RegistrationApplicationDto(
        id = this.id,
        registrationCode = this.registrationCode.code,
        externalEmail = this.externalEmail,
        emailVerified = this.emailVerified,
        identityType = this.identityType,
        countryCode = this.countryCode,
        majorId = this.majorId,
        classId = this.classId,
        status = this.status,
        rejectionReason = this.rejectionReason,
        approvedByUsername = this.approvedBy?.username,
        approvedAt = this.approvedAt,
        oauthProvider = this.oauthProvider,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
