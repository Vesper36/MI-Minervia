package edu.minervia.platform.web.controller

import edu.minervia.platform.service.AuthService
import edu.minervia.platform.web.dto.ApiResponse
import edu.minervia.platform.web.dto.LoginRequest
import edu.minervia.platform.web.dto.LoginResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
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
}
