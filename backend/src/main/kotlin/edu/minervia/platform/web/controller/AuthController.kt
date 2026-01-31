package edu.minervia.platform.web.controller

import edu.minervia.platform.service.AuthService
import edu.minervia.platform.web.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, logout, and token management")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    @Operation(
        summary = "Admin login",
        description = "Authenticate admin user and obtain JWT tokens"
    )
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Login successful"),
        SwaggerApiResponse(responseCode = "401", description = "Invalid credentials"),
        SwaggerApiResponse(responseCode = "400", description = "Validation error")
    ])
    @SecurityRequirements
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(ApiResponse.success(response))
        } catch (e: BadCredentialsException) {
            ResponseEntity.status(401).body(ApiResponse.error(e.message ?: "Authentication failed"))
        }
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Exchange refresh token for new access and refresh tokens"
    )
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        SwaggerApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    ])
    @SecurityRequirements
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<RefreshTokenResponse>> {
        return try {
            val response = authService.refreshToken(request.refreshToken)
            ResponseEntity.ok(ApiResponse.success(response))
        } catch (e: BadCredentialsException) {
            ResponseEntity.status(401).body(ApiResponse.error(e.message ?: "Token refresh failed"))
        }
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout",
        description = "Invalidate current access token and optionally refresh token"
    )
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Logged out successfully")
    ])
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
