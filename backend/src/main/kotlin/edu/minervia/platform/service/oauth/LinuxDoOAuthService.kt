package edu.minervia.platform.service.oauth

import edu.minervia.platform.domain.entity.RegistrationApplication
import edu.minervia.platform.domain.repository.RegistrationApplicationRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.time.Instant

/**
 * Service for handling Linux.do OAuth authentication.
 * Implements OAuth 2.0 authorization code flow.
 */
@Service
class LinuxDoOAuthService(
    private val applicationRepository: RegistrationApplicationRepository,
    private val restTemplate: RestTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${minervia.oauth.linuxdo.client-id:}")
    private lateinit var clientId: String

    @Value("\${minervia.oauth.linuxdo.client-secret:}")
    private lateinit var clientSecret: String

    @Value("\${minervia.oauth.linuxdo.redirect-uri:}")
    private lateinit var redirectUri: String

    @Value("\${minervia.oauth.linuxdo.authorize-url:https://connect.linux.do/oauth2/authorize}")
    private lateinit var authorizeUrl: String

    @Value("\${minervia.oauth.linuxdo.token-url:https://connect.linux.do/oauth2/token}")
    private lateinit var tokenUrl: String

    @Value("\${minervia.oauth.linuxdo.userinfo-url:https://connect.linux.do/api/user}")
    private lateinit var userinfoUrl: String

    companion object {
        const val PROVIDER_NAME = "linux.do"
    }

    /**
     * Generate OAuth authorization URL with state parameter.
     */
    fun getAuthorizationUrl(applicationId: Long): String {
        val state = generateState(applicationId)
        return "$authorizeUrl?" +
            "client_id=$clientId&" +
            "redirect_uri=$redirectUri&" +
            "response_type=code&" +
            "scope=read&" +
            "state=$state"
    }

    /**
     * Handle OAuth callback and link user to application.
     */
    fun handleCallback(code: String, state: String): OAuthResult {
        val applicationId = parseState(state)
            ?: return OAuthResult.failure("Invalid state parameter")

        val application = applicationRepository.findById(applicationId).orElse(null)
            ?: return OAuthResult.failure("Application not found")

        if (application.oauthUserId != null) {
            return OAuthResult.failure("Application already linked to OAuth account")
        }

        val tokenResponse = exchangeCodeForToken(code)
            ?: return OAuthResult.failure("Failed to exchange code for token")

        val userInfo = fetchUserInfo(tokenResponse.accessToken)
            ?: return OAuthResult.failure("Failed to fetch user info")

        val existingApp = applicationRepository.findAll()
            .find { it.oauthProvider == PROVIDER_NAME && it.oauthUserId == userInfo.userId }

        if (existingApp != null && existingApp.id != applicationId) {
            return OAuthResult.failure("This Linux.do account is already linked to another application")
        }

        linkOAuthToApplication(application, userInfo)

        return OAuthResult.success(
            applicationId = applicationId,
            userId = userInfo.userId,
            username = userInfo.username
        )
    }

    private fun exchangeCodeForToken(code: String): TokenResponse? {
        return try {
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
            }

            val body = LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("code", code)
                add("client_id", clientId)
                add("client_secret", clientSecret)
                add("redirect_uri", redirectUri)
            }

            val request = HttpEntity(body, headers)
            val response = restTemplate.postForEntity(tokenUrl, request, Map::class.java)

            if (response.statusCode.is2xxSuccessful && response.body != null) {
                val responseBody = response.body as Map<*, *>
                TokenResponse(
                    accessToken = responseBody["access_token"] as String,
                    tokenType = responseBody["token_type"] as? String ?: "Bearer",
                    expiresIn = (responseBody["expires_in"] as? Number)?.toLong() ?: 3600
                )
            } else {
                null
            }
        } catch (e: Exception) {
            log.error("Failed to exchange code for token: {}", e.message)
            null
        }
    }

    private fun fetchUserInfo(accessToken: String): LinuxDoUserInfo? {
        return try {
            val headers = HttpHeaders().apply {
                setBearerAuth(accessToken)
            }

            val request = HttpEntity<Void>(headers)
            val response = restTemplate.exchange(
                userinfoUrl,
                HttpMethod.GET,
                request,
                Map::class.java
            )

            if (response.statusCode.is2xxSuccessful && response.body != null) {
                val body = response.body as Map<*, *>
                LinuxDoUserInfo(
                    userId = body["id"]?.toString() ?: body["user_id"]?.toString() ?: "",
                    username = body["username"]?.toString() ?: body["name"]?.toString() ?: "",
                    email = body["email"]?.toString()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            log.error("Failed to fetch user info: {}", e.message)
            null
        }
    }

    private fun linkOAuthToApplication(application: RegistrationApplication, userInfo: LinuxDoUserInfo) {
        application.oauthProvider = PROVIDER_NAME
        application.oauthUserId = userInfo.userId
        application.updatedAt = Instant.now()
        applicationRepository.save(application)

        log.info("Linked Linux.do user {} to application {}",
            userInfo.username, application.id)
    }

    private fun generateState(applicationId: Long): String {
        return "$applicationId:${System.currentTimeMillis()}"
    }

    private fun parseState(state: String): Long? {
        return try {
            state.split(":").firstOrNull()?.toLong()
        } catch (e: Exception) {
            null
        }
    }
}

data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long
)

data class LinuxDoUserInfo(
    val userId: String,
    val username: String,
    val email: String?
)

sealed class OAuthResult {
    data class Success(
        val applicationId: Long,
        val userId: String,
        val username: String
    ) : OAuthResult()

    data class Failure(val error: String) : OAuthResult()

    companion object {
        fun success(applicationId: Long, userId: String, username: String) =
            Success(applicationId, userId, username)

        fun failure(error: String) = Failure(error)
    }
}
