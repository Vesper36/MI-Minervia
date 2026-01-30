package edu.minervia.platform.web.controller

import edu.minervia.platform.service.AuthService
import edu.minervia.platform.web.dto.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(ApiResponse.success(response))
        } catch (e: BadCredentialsException) {
            ResponseEntity.status(401).body(ApiResponse.error(e.message ?: "Authentication failed"))
        }
    }

    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<RefreshTokenResponse>> {
        return try {
            val response = authService.refreshToken(request.refreshToken)
            ResponseEntity.ok(ApiResponse.success(response))
        } catch (e: BadCredentialsException) {
            ResponseEntity.status(401).body(ApiResponse.error(e.message ?: "Token refresh failed"))
        }
    }

    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody(required = false) body: LogoutRequest?
    ): ResponseEntity<ApiResponse<Unit>> {
        val accessToken = authHeader.removePrefix("Bearer ").trim()
        authService.logout(accessToken, body?.refreshToken)
        return ResponseEntity.ok(ApiResponse.success(Unit, "Logged out successfully"))
    }
}

data class LogoutRequest(
    val refreshToken: String? = null
)
