package edu.minervia.platform.web.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,

    @field:NotBlank(message = "Password is required")
    val password: String,

    val totpCode: String? = null
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long,
    val username: String,
    val role: String,
    val requiresTotp: Boolean = false
) {
    @Deprecated("Use accessToken instead")
    val token: String get() = accessToken

    @Deprecated("Use accessExpiresIn instead")
    val expiresIn: Long get() = accessExpiresIn
}

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errors: List<String>? = null
) {
    companion object {
        fun <T> success(data: T, message: String? = null) = ApiResponse(
            success = true,
            data = data,
            message = message
        )

        fun <T> error(message: String, errors: List<String>? = null) = ApiResponse<T>(
            success = false,
            message = message,
            errors = errors
        )
    }
}

data class StudentLoginRequest(
    @field:NotBlank(message = "Email or student number is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class StudentLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long,
    val studentNumber: String,
    val fullName: String,
    val eduEmail: String
)

data class StudentRefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

data class StudentRefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    val newPassword: String
)
