package edu.minervia.platform.web.controller

import edu.minervia.platform.domain.enums.RegistrationCodeStatus
import edu.minervia.platform.security.AdminUserDetails
import edu.minervia.platform.service.RegistrationCodeService
import edu.minervia.platform.web.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Registration Codes", description = "Registration code generation and management")
class RegistrationCodeController(
    private val registrationCodeService: RegistrationCodeService
) {

    @PostMapping("/generate")
    @Operation(summary = "Generate registration code", description = "Generate a single registration code")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Code generated successfully"),
        SwaggerApiResponse(responseCode = "401", description = "Unauthorized"),
        SwaggerApiResponse(responseCode = "403", description = "Forbidden")
    ])
    fun generateCode(
        @AuthenticationPrincipal admin: AdminUserDetails,
        @Valid @RequestBody request: GenerateCodeRequest
    ): ResponseEntity<ApiResponse<RegistrationCodeDto>> {
        val code = registrationCodeService.generateCode(admin.getAdminId(), request.expirationDays)
        return ResponseEntity.ok(ApiResponse.success(code, "Code generated successfully"))
    }

    @PostMapping("/generate-batch")
    @Operation(summary = "Batch generate codes", description = "Generate multiple registration codes (max 100)")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Codes generated successfully"),
        SwaggerApiResponse(responseCode = "400", description = "Invalid count (must be 1-100)")
    ])
    fun generateBatch(
        @AuthenticationPrincipal admin: AdminUserDetails,
        @Valid @RequestBody request: BatchGenerateCodeRequest
    ): ResponseEntity<ApiResponse<List<RegistrationCodeDto>>> {
        val codes = registrationCodeService.generateBatch(admin.getAdminId(), request.count, request.expirationDays)
        return ResponseEntity.ok(ApiResponse.success(codes, "${codes.size} codes generated"))
    }

    @GetMapping
    @Operation(summary = "List registration codes", description = "Get paginated list of registration codes")
    fun getAllCodes(
        @Parameter(description = "Filter by status") @RequestParam(required = false) status: RegistrationCodeStatus?,
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
    @Operation(summary = "Get registration code", description = "Get registration code by ID")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Code found"),
        SwaggerApiResponse(responseCode = "404", description = "Code not found")
    ])
    fun getCode(@Parameter(description = "Code ID") @PathVariable id: Long): ResponseEntity<ApiResponse<RegistrationCodeDto>> {
        val code = registrationCodeService.getCodeById(id)
        return ResponseEntity.ok(ApiResponse.success(code))
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Revoke registration code", description = "Revoke an active registration code")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Code revoked successfully"),
        SwaggerApiResponse(responseCode = "404", description = "Code not found"),
        SwaggerApiResponse(responseCode = "400", description = "Code already used or revoked")
    ])
    fun revokeCode(@Parameter(description = "Code ID") @PathVariable id: Long): ResponseEntity<ApiResponse<RegistrationCodeDto>> {
        val code = registrationCodeService.revokeCode(id)
        return ResponseEntity.ok(ApiResponse.success(code, "Code revoked successfully"))
    }
}

@RestController
@RequestMapping("/api/public/registration-codes")
@Tag(name = "Registration Codes", description = "Public registration code verification")
class PublicRegistrationCodeController(
    private val registrationCodeService: RegistrationCodeService
) {

    @PostMapping("/verify")
    @Operation(summary = "Verify registration code", description = "Check if a registration code is valid")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Verification result returned")
    ])
    @SecurityRequirements
    fun verifyCode(
        @Valid @RequestBody request: VerifyCodeRequest
    ): ResponseEntity<ApiResponse<VerifyCodeResponse>> {
        val (valid, message) = registrationCodeService.verifyCode(request.code)
        return ResponseEntity.ok(
            ApiResponse.success(VerifyCodeResponse(valid, message))
        )
    }
}
