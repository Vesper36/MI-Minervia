package edu.minervia.platform.web.controller

import edu.minervia.platform.service.StudentAuthService
import edu.minervia.platform.web.dto.ApiResponse
import edu.minervia.platform.web.dto.StudentLoginRequest
import edu.minervia.platform.web.dto.StudentLoginResponse
import edu.minervia.platform.web.dto.StudentRefreshTokenRequest
import edu.minervia.platform.web.dto.StudentRefreshTokenResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/student/auth")
class StudentAuthController(
    private val studentAuthService: StudentAuthService
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: StudentLoginRequest): ResponseEntity<ApiResponse<StudentLoginResponse>> {
        val response = studentAuthService.login(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: StudentRefreshTokenRequest): ResponseEntity<ApiResponse<StudentRefreshTokenResponse>> {
        val response = studentAuthService.refreshToken(request.refreshToken)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody(required = false) body: Map<String, String>?
    ): ResponseEntity<ApiResponse<Unit>> {
        val accessToken = authHeader.removePrefix("Bearer ")
        val refreshToken = body?.get("refreshToken")
        studentAuthService.logout(accessToken, refreshToken)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }
}
