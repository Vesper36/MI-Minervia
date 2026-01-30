package edu.minervia.platform.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class SystemConfigDto(
    val id: Long,
    val key: String,
    val value: String,
    val description: String?,
    val updatedBy: String?,
    val updatedAt: Instant
)

data class UpdateConfigRequest(
    @field:NotBlank
    @field:Size(max = 5000)
    val value: String,

    @field:Size(max = 500)
    val description: String? = null
)

data class CreateConfigRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val key: String,

    @field:NotBlank
    @field:Size(max = 5000)
    val value: String,

    @field:Size(max = 500)
    val description: String? = null
)
