package edu.minervia.platform.web.controller

import edu.minervia.platform.service.oauth.LinuxDoOAuthService
import edu.minervia.platform.service.oauth.OAuthResult
import edu.minervia.platform.web.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

/**
 * Controller for OAuth authentication flows.
 */
@RestController
@RequestMapping("/api/public/oauth")
class OAuthController(
    private val linuxDoOAuthService: LinuxDoOAuthService
) {

    /**
     * Initiate OAuth flow for Linux.do.
     * Returns the authorization URL to redirect the user to.
     */
    @GetMapping("/linuxdo/authorize")
    fun initiateLinuxDoAuth(
        @RequestParam applicationId: Long
    ): ResponseEntity<ApiResponse<OAuthAuthorizeResponse>> {
        val authUrl = linuxDoOAuthService.getAuthorizationUrl(applicationId)
        return ResponseEntity.ok(ApiResponse.success(
            OAuthAuthorizeResponse(authorizationUrl = authUrl)
        ))
    }

    /**
     * Handle OAuth callback from Linux.do.
     * This endpoint is called by Linux.do after user authorization.
     */
    @GetMapping("/linuxdo/callback")
    fun handleLinuxDoCallback(
        @RequestParam code: String,
        @RequestParam state: String,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false, name = "error_description") errorDescription: String?
    ): ResponseEntity<*> {
        if (error != null) {
            val redirectUrl = "/registration/oauth-error?error=${error}&description=${errorDescription ?: ""}"
            return ResponseEntity.status(302).location(URI.create(redirectUrl)).build<Void>()
        }

        return when (val result = linuxDoOAuthService.handleCallback(code, state)) {
            is OAuthResult.Success -> {
                val redirectUrl = "/registration/${result.applicationId}/oauth-success?username=${result.username}"
                ResponseEntity.status(302).location(URI.create(redirectUrl)).build<Void>()
            }
            is OAuthResult.Failure -> {
                val redirectUrl = "/registration/oauth-error?error=${result.error}"
                ResponseEntity.status(302).location(URI.create(redirectUrl)).build<Void>()
            }
        }
    }

    /**
     * API endpoint for SPA to handle callback (alternative to redirect).
     */
    @PostMapping("/linuxdo/callback")
    fun handleLinuxDoCallbackApi(
        @RequestBody request: OAuthCallbackRequest
    ): ResponseEntity<ApiResponse<OAuthCallbackResponse>> {
        if (request.error != null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(request.error, request.errorDescription)
            )
        }

        return when (val result = linuxDoOAuthService.handleCallback(request.code!!, request.state!!)) {
            is OAuthResult.Success -> {
                ResponseEntity.ok(ApiResponse.success(
                    OAuthCallbackResponse(
                        applicationId = result.applicationId,
                        userId = result.userId,
                        username = result.username,
                        linked = true
                    ),
                    "OAuth account linked successfully"
                ))
            }
            is OAuthResult.Failure -> {
                ResponseEntity.badRequest().body(
                    ApiResponse.error<OAuthCallbackResponse>(result.error)
                )
            }
        }
    }
}

data class OAuthAuthorizeResponse(
    val authorizationUrl: String
)

data class OAuthCallbackRequest(
    val code: String?,
    val state: String?,
    val error: String?,
    val errorDescription: String?
)

data class OAuthCallbackResponse(
    val applicationId: Long,
    val userId: String,
    val username: String,
    val linked: Boolean
)
