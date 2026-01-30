package edu.minervia.platform.web.dto

import edu.minervia.platform.domain.enums.AdminRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class AdminDto(
    val id: Long,
    val username: String,
    val email: String,
    val role: AdminRole,
    val isActive: Boolean,
    val totpEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateAdminRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    val username: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    val role: AdminRole = AdminRole.ADMIN
)

data class UpdateAdminRoleRequest(
    val role: AdminRole
)
