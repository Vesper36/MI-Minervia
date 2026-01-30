package edu.minervia.platform.web.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class LoginResponse(
    val token: String,
    val expiresIn: Long,
    val username: String,
    val role: String
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
