package edu.minervia.platform.web.controller

import edu.minervia.platform.domain.enums.ApplicationStatus
import edu.minervia.platform.security.AdminUserDetails
import edu.minervia.platform.service.EmailVerificationService
import edu.minervia.platform.service.RegistrationService
import edu.minervia.platform.web.dto.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public/registration")
class PublicRegistrationController(
    private val registrationService: RegistrationService,
    private val emailVerificationService: EmailVerificationService
) {

    @PostMapping("/start")
    fun startRegistration(
        @Valid @RequestBody request: StartRegistrationRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<RegistrationApplicationDto>> {
        val ipAddress = httpRequest.getHeader("X-Forwarded-For")?.split(",")?.first()
            ?: httpRequest.remoteAddr
        val userAgent = httpRequest.getHeader("User-Agent")

        val application = registrationService.startRegistration(request, ipAddress, userAgent)
        return ResponseEntity.ok(ApiResponse.success(application, "Registration started"))
    }

    @GetMapping("/{id}")
    fun getApplication(@PathVariable id: Long): ResponseEntity<ApiResponse<RegistrationApplicationDto>> {
        val application = registrationService.getApplication(id)
        return ResponseEntity.ok(ApiResponse.success(application))
    }

    @PostMapping("/{id}/send-verification")
    fun sendVerificationCode(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        emailVerificationService.sendVerificationCode(id)
        return ResponseEntity.ok(ApiResponse.success(Unit, "Verification code sent"))
    }

    @PostMapping("/{id}/verify-email")
    fun verifyEmail(
        @PathVariable id: Long,
        @Valid @RequestBody request: VerifyEmailRequest
    ): ResponseEntity<ApiResponse<VerifyEmailResponse>> {
        val response = emailVerificationService.verifyCode(id, request.code)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{id}/info")
    fun updateInfo(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateRegistrationInfoRequest
    ): ResponseEntity<ApiResponse<RegistrationApplicationDto>> {
        val application = registrationService.updateRegistrationInfo(id, request)
        return ResponseEntity.ok(ApiResponse.success(application))
    }

    @PostMapping("/{id}/submit")
    fun submitApplication(@PathVariable id: Long): ResponseEntity<ApiResponse<RegistrationApplicationDto>> {
        val application = registrationService.submitApplication(id)
        return ResponseEntity.ok(ApiResponse.success(application, "Application submitted for approval"))
    }
}

@RestController
@RequestMapping("/api/admin/registration-applications")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
class RegistrationApprovalController(
    private val registrationService: RegistrationService
) {

    @GetMapping
    fun getApplications(
        @RequestParam(required = false) status: ApplicationStatus?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<RegistrationApplicationDto>>> {
        val applications = if (status != null) {
            registrationService.getApplicationsByStatus(status, pageable)
        } else {
            registrationService.getPendingApplications(pageable)
        }
        return ResponseEntity.ok(ApiResponse.success(applications))
    }

    @GetMapping("/{id}")
    fun getApplication(@PathVariable id: Long): ResponseEntity<ApiResponse<RegistrationApplicationDto>> {
        val application = registrationService.getApplication(id)
        return ResponseEntity.ok(ApiResponse.success(application))
    }

    @PostMapping("/{id}/approve")
    fun approveApplication(
        @PathVariable id: Long,
        @AuthenticationPrincipal admin: AdminUserDetails
    ): ResponseEntity<ApiResponse<RegistrationApplicationDto>> {
        val application = registrationService.approveApplication(id, admin.getAdminId())
        return ResponseEntity.ok(ApiResponse.success(application, "Application approved"))
    }

    @PostMapping("/{id}/reject")
    fun rejectApplication(
        @PathVariable id: Long,
        @Valid @RequestBody request: RejectApplicationRequest,
        @AuthenticationPrincipal admin: AdminUserDetails
    ): ResponseEntity<ApiResponse<RegistrationApplicationDto>> {
        val application = registrationService.rejectApplication(id, admin.getAdminId(), request.reason)
        return ResponseEntity.ok(ApiResponse.success(application, "Application rejected"))
    }

    @PostMapping("/batch-approve")
    fun batchApprove(
        @Valid @RequestBody request: BatchApproveRequest,
        @AuthenticationPrincipal admin: AdminUserDetails
    ): ResponseEntity<ApiResponse<List<RegistrationApplicationDto>>> {
        val applications = registrationService.batchApprove(request.applicationIds, admin.getAdminId())
        return ResponseEntity.ok(ApiResponse.success(applications, "${applications.size} applications approved"))
    }
}
