package edu.minervia.platform.web.dto

import edu.minervia.platform.domain.enums.RegistrationCodeStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import java.time.Instant

data class RegistrationCodeDto(
    val id: Long,
    val code: String,
    val status: RegistrationCodeStatus,
    val createdByUsername: String,
    val usedByUsername: String?,
    val expiresAt: Instant,
    val usedAt: Instant?,
    val createdAt: Instant
)

data class GenerateCodeRequest(
    @field:Positive(message = "Expiration days must be positive")
    val expirationDays: Int = 30
)

data class BatchGenerateCodeRequest(
    @field:Min(1, message = "Count must be at least 1")
    @field:Max(100, message = "Count must not exceed 100")
    val count: Int,

    @field:Positive(message = "Expiration days must be positive")
    val expirationDays: Int = 30
)

data class VerifyCodeRequest(
    val code: String
)

data class VerifyCodeResponse(
    val valid: Boolean,
    val message: String? = null
)
