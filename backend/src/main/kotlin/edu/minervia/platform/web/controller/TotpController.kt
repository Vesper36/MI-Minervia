package edu.minervia.platform.web.controller

import edu.minervia.platform.security.AdminUserDetails
import edu.minervia.platform.service.auth.TotpService
import edu.minervia.platform.service.auth.TotpSetupResult
import edu.minervia.platform.web.dto.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * Controller for TOTP two-factor authentication management.
 */
@RestController
@RequestMapping("/api/admin/totp")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
class TotpController(
    private val totpService: TotpService
) {

    /**
     * Get TOTP status for current admin.
     */
    @GetMapping("/status")
    fun getStatus(
        @AuthenticationPrincipal admin: AdminUserDetails
    ): ResponseEntity<ApiResponse<TotpStatusResponse>> {
        val enabled = totpService.isTotpEnabled(admin.getAdminId())
        return ResponseEntity.ok(ApiResponse.success(
            TotpStatusResponse(enabled = enabled)
        ))
    }

    /**
     * Generate TOTP secret and QR code for setup.
     */
    @PostMapping("/setup")
    fun setupTotp(
        @AuthenticationPrincipal admin: AdminUserDetails
    ): ResponseEntity<ApiResponse<TotpSetupResponse>> {
        val result = totpService.generateSecret(admin.getAdminId())
        return ResponseEntity.ok(ApiResponse.success(
            TotpSetupResponse(
                secret = result.secret,
                qrCodeUri = result.qrCodeUri,
                issuer = result.issuer,
                accountName = result.accountName
            ),
            "TOTP secret generated. Scan the QR code with your authenticator app."
        ))
    }

    /**
     * Verify TOTP code and enable 2FA.
     */
    @PostMapping("/enable")
    fun enableTotp(
        @AuthenticationPrincipal admin: AdminUserDetails,
        @Valid @RequestBody request: TotpVerifyRequest
    ): ResponseEntity<ApiResponse<TotpStatusResponse>> {
        val success = totpService.verifyAndEnable(admin.getAdminId(), request.code)
        return if (success) {
            ResponseEntity.ok(ApiResponse.success(
                TotpStatusResponse(enabled = true),
                "Two-factor authentication enabled successfully"
            ))
        } else {
            ResponseEntity.badRequest().body(ApiResponse.error("Invalid verification code"))
        }
    }

    /**
     * Disable TOTP 2FA.
     */
    @PostMapping("/disable")
    fun disableTotp(
        @AuthenticationPrincipal admin: AdminUserDetails,
        @Valid @RequestBody request: TotpVerifyRequest
    ): ResponseEntity<ApiResponse<TotpStatusResponse>> {
        val success = totpService.disable(admin.getAdminId(), request.code)
        return if (success) {
            ResponseEntity.ok(ApiResponse.success(
                TotpStatusResponse(enabled = false),
                "Two-factor authentication disabled"
            ))
        } else {
            ResponseEntity.badRequest().body(ApiResponse.error("Invalid verification code"))
        }
    }
}

data class TotpStatusResponse(
    val enabled: Boolean
)

data class TotpSetupResponse(
    val secret: String,
    val qrCodeUri: String,
    val issuer: String,
    val accountName: String
)

data class TotpVerifyRequest(
    @field:NotBlank(message = "Code is required")
    @field:Pattern(regexp = "^\\d{6}$", message = "Code must be 6 digits")
    val code: String
)
