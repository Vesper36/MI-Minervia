package edu.minervia.platform.web.controller

import edu.minervia.platform.domain.enums.RegistrationCodeStatus
import edu.minervia.platform.security.AdminUserDetails
import edu.minervia.platform.service.RegistrationCodeService
import edu.minervia.platform.web.dto.*
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/registration-codes")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
class RegistrationCodeController(
    private val registrationCodeService: RegistrationCodeService
) {

    @PostMapping("/generate")
    fun generateCode(
        @AuthenticationPrincipal admin: AdminUserDetails,
        @Valid @RequestBody request: GenerateCodeRequest
    ): ResponseEntity<ApiResponse<RegistrationCodeDto>> {
        val code = registrationCodeService.generateCode(admin.getAdminId(), request.expirationDays)
        return ResponseEntity.ok(ApiResponse.success(code, "Code generated successfully"))
    }

    @PostMapping("/generate-batch")
    fun generateBatch(
        @AuthenticationPrincipal admin: AdminUserDetails,
        @Valid @RequestBody request: BatchGenerateCodeRequest
    ): ResponseEntity<ApiResponse<List<RegistrationCodeDto>>> {
        val codes = registrationCodeService.generateBatch(admin.getAdminId(), request.count, request.expirationDays)
        return ResponseEntity.ok(ApiResponse.success(codes, "${codes.size} codes generated"))
    }

    @GetMapping
    fun getAllCodes(
        @RequestParam(required = false) status: RegistrationCodeStatus?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<RegistrationCodeDto>>> {
        val codes = if (status != null) {
            registrationCodeService.getCodesByStatus(status, pageable)
        } else {
            registrationCodeService.getAllCodes(pageable)
        }
        return ResponseEntity.ok(ApiResponse.success(codes))
    }

    @GetMapping("/{id}")
    fun getCode(@PathVariable id: Long): ResponseEntity<ApiResponse<RegistrationCodeDto>> {
        val code = registrationCodeService.getCodeById(id)
        return ResponseEntity.ok(ApiResponse.success(code))
    }

    @PostMapping("/{id}/revoke")
    fun revokeCode(@PathVariable id: Long): ResponseEntity<ApiResponse<RegistrationCodeDto>> {
        val code = registrationCodeService.revokeCode(id)
        return ResponseEntity.ok(ApiResponse.success(code, "Code revoked successfully"))
    }
}

@RestController
@RequestMapping("/api/public/registration-codes")
class PublicRegistrationCodeController(
    private val registrationCodeService: RegistrationCodeService
) {

    @PostMapping("/verify")
    fun verifyCode(
        @Valid @RequestBody request: VerifyCodeRequest
    ): ResponseEntity<ApiResponse<VerifyCodeResponse>> {
        val (valid, message) = registrationCodeService.verifyCode(request.code)
        return ResponseEntity.ok(
            ApiResponse.success(VerifyCodeResponse(valid, message))
        )
    }
}
