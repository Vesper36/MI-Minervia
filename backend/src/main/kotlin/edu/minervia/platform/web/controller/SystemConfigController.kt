package edu.minervia.platform.web.controller

import edu.minervia.platform.security.AdminUserDetails
import edu.minervia.platform.service.SystemConfigService
import edu.minervia.platform.web.dto.*
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/config")
@PreAuthorize("hasRole('SUPER_ADMIN')")
class SystemConfigController(
    private val systemConfigService: SystemConfigService
) {
    @GetMapping
    fun getAllConfigs(): ResponseEntity<ApiResponse<List<SystemConfigDto>>> {
        val configs = systemConfigService.getAllConfigs()
        return ResponseEntity.ok(ApiResponse.success(configs))
    }

    @GetMapping("/{key}")
    fun getConfig(@PathVariable key: String): ResponseEntity<ApiResponse<SystemConfigDto>> {
        val config = systemConfigService.getConfig(key)
        return ResponseEntity.ok(ApiResponse.success(config))
    }

    @PatchMapping("/{key}")
    fun updateConfig(
        @PathVariable key: String,
        @Valid @RequestBody request: UpdateConfigRequest,
        @AuthenticationPrincipal admin: AdminUserDetails
    ): ResponseEntity<ApiResponse<SystemConfigDto>> {
        val config = systemConfigService.updateConfig(key, request, admin.getAdminId())
        return ResponseEntity.ok(ApiResponse.success(config, "Config updated"))
    }

    @PostMapping
    fun createConfig(
        @Valid @RequestBody request: CreateConfigRequest,
        @AuthenticationPrincipal admin: AdminUserDetails
    ): ResponseEntity<ApiResponse<SystemConfigDto>> {
        val config = systemConfigService.createConfig(
            key = request.key,
            value = request.value,
            description = request.description,
            adminId = admin.getAdminId()
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(config, "Config created"))
    }
}
