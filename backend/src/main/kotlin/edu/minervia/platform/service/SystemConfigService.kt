package edu.minervia.platform.service

import edu.minervia.platform.domain.entity.SystemConfig
import edu.minervia.platform.domain.repository.AdminRepository
import edu.minervia.platform.domain.repository.SystemConfigRepository
import edu.minervia.platform.web.dto.SystemConfigDto
import edu.minervia.platform.web.dto.UpdateConfigRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SystemConfigService(
    private val systemConfigRepository: SystemConfigRepository,
    private val adminRepository: AdminRepository
) {
    companion object {
        const val DAILY_EMAIL_LIMIT = "daily_email_limit"
        const val REGISTRATION_CODE_VALIDITY_DAYS = "registration_code_validity_days"
        const val JWT_ACCESS_TOKEN_MINUTES = "jwt_access_token_minutes"
        const val JWT_REFRESH_TOKEN_DAYS = "jwt_refresh_token_days"
        const val EMAIL_VERIFICATION_EXPIRY_MINUTES = "email_verification_expiry_minutes"
        const val MAX_LOGIN_ATTEMPTS = "max_login_attempts"
        const val LOGIN_LOCKOUT_MINUTES = "login_lockout_minutes"
    }

    fun getAllConfigs(): List<SystemConfigDto> {
        return systemConfigRepository.findAll().map { it.toDto() }
    }

    fun getConfig(key: String): SystemConfigDto {
        return systemConfigRepository.findByConfigKey(key)
            .orElseThrow { NoSuchElementException("Config not found: $key") }
            .toDto()
    }

    fun getConfigValue(key: String, default: String): String {
        return systemConfigRepository.findByConfigKey(key)
            .map { it.configValue }
            .orElse(default)
    }

    fun getConfigValueAsInt(key: String, default: Int): Int {
        return systemConfigRepository.findByConfigKey(key)
            .map { it.configValue.toIntOrNull() ?: default }
            .orElse(default)
    }

    @Transactional
    fun updateConfig(key: String, request: UpdateConfigRequest, adminId: Long): SystemConfigDto {
        val config = systemConfigRepository.findByConfigKey(key)
            .orElseThrow { NoSuchElementException("Config not found: $key") }

        val admin = adminRepository.findById(adminId)
            .orElseThrow { NoSuchElementException("Admin not found") }

        config.configValue = request.value
        request.description?.let { config.description = it }
        config.updatedBy = admin

        return systemConfigRepository.save(config).toDto()
    }

    @Transactional
    fun createConfig(key: String, value: String, description: String?, adminId: Long): SystemConfigDto {
        if (systemConfigRepository.existsByConfigKey(key)) {
            throw IllegalArgumentException("Config already exists: $key")
        }

        val admin = adminRepository.findById(adminId)
            .orElseThrow { NoSuchElementException("Admin not found") }

        val config = SystemConfig(
            configKey = key,
            configValue = value,
            description = description,
            updatedBy = admin
        )

        return systemConfigRepository.save(config).toDto()
    }

    private fun SystemConfig.toDto() = SystemConfigDto(
        id = this.id,
        key = this.configKey,
        value = this.configValue,
        description = this.description,
        updatedBy = this.updatedBy?.username,
        updatedAt = this.updatedAt
    )
}
