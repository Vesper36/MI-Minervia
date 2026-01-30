package edu.minervia.platform.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SendVerificationCodeRequest(
    val applicationId: Long
)

data class VerifyEmailRequest(
    val applicationId: Long,

    @field:NotBlank(message = "Verification code is required")
    @field:Size(min = 6, max = 6, message = "Code must be 6 digits")
    val code: String
)

data class VerifyEmailResponse(
    val verified: Boolean,
    val message: String? = null,
    val attemptsRemaining: Int? = null
)
