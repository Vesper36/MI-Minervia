package edu.minervia.platform.web.dto

import edu.minervia.platform.domain.enums.ApplicationStatus
import edu.minervia.platform.domain.enums.IdentityType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class RegistrationApplicationDto(
    val id: Long,
    val registrationCode: String,
    val externalEmail: String,
    val emailVerified: Boolean,
    val identityType: IdentityType,
    val countryCode: String?,
    val majorId: Long?,
    val classId: Long?,
    val status: ApplicationStatus,
    val rejectionReason: String?,
    val approvedByUsername: String?,
    val approvedAt: Instant?,
    val oauthProvider: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class StartRegistrationRequest(
    @field:NotBlank(message = "Registration code is required")
    val code: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotNull(message = "Identity type is required")
    val identityType: IdentityType,

    val countryCode: String? = null
)

data class UpdateRegistrationInfoRequest(
    val majorId: Long,
    val classId: Long,
    val countryCode: String? = null
)

data class ApproveApplicationRequest(
    val applicationId: Long
)

data class RejectApplicationRequest(
    val applicationId: Long,

    @field:NotBlank(message = "Rejection reason is required")
    val reason: String
)

data class BatchApproveRequest(
    val applicationIds: List<Long>
)
