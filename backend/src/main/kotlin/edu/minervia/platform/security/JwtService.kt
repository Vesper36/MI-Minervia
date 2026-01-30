package edu.minervia.platform.security

import edu.minervia.platform.config.JwtProperties
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

enum class TokenType {
    ACCESS, REFRESH
}

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenJti: String,
    val refreshTokenJti: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long
)

@Service
class JwtService(private val jwtProperties: JwtProperties) {

    private val logger = LoggerFactory.getLogger(JwtService::class.java)
    private lateinit var secretKey: SecretKey

    @PostConstruct
    fun init() {
        if (jwtProperties.secret.isBlank()) {
            if (jwtProperties.requireSecret) {
                throw IllegalStateException("JWT secret is required but not provided. Set JWT_SECRET environment variable.")
            }
            logger.warn("JWT secret not configured. Using generated key for development only.")
            secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256)
        } else {
            if (jwtProperties.secret.length < 32) {
                throw IllegalStateException("JWT secret must be at least 256 bits (32 characters)")
            }
            secretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
        }
    }

    /**
     * CONSTRAINT [JWT-DUAL-TOKEN]: Generate both access and refresh tokens.
     * Access Token TTL: 30 minutes
     * Refresh Token TTL: 14 days
     */
    fun generateTokenPair(username: String, role: String, adminId: Long): TokenPair {
        val accessJti = UUID.randomUUID().toString()
        val refreshJti = UUID.randomUUID().toString()

        val accessToken = generateToken(
            username = username,
            role = role,
            adminId = adminId,
            jti = accessJti,
            tokenType = TokenType.ACCESS,
            expiration = jwtProperties.accessTokenExpiration
        )

        val refreshToken = generateToken(
            username = username,
            role = role,
            adminId = adminId,
            jti = refreshJti,
            tokenType = TokenType.REFRESH,
            expiration = jwtProperties.refreshTokenExpiration
        )

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenJti = accessJti,
            refreshTokenJti = refreshJti,
            accessExpiresIn = jwtProperties.accessTokenExpiration,
            refreshExpiresIn = jwtProperties.refreshTokenExpiration
        )
    }

    private fun generateToken(
        username: String,
        role: String,
        adminId: Long,
        jti: String,
        tokenType: TokenType,
        expiration: Long
    ): String {
        val now = Date()
        val expiry = Date(now.time + expiration)

        return Jwts.builder()
            .id(jti)
            .subject(username)
            .claim("role", role)
            .claim("adminId", adminId)
            .claim("type", tokenType.name)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
    }

    @Deprecated("Use generateTokenPair instead", ReplaceWith("generateTokenPair(username, role, adminId).accessToken"))
    fun generateToken(username: String, role: String, adminId: Long): String {
        return generateTokenPair(username, role, adminId).accessToken
    }

    fun validateToken(token: String): Boolean {
        return try {
            parseToken(token)
            true
        } catch (e: JwtException) {
            logger.debug("Invalid JWT token: {}", e.message)
            false
        } catch (e: IllegalArgumentException) {
            logger.debug("JWT token is empty: {}", e.message)
            false
        }
    }

    fun getJtiFromToken(token: String): String? {
        return try {
            parseToken(token).payload.id
        } catch (e: Exception) {
            null
        }
    }

    fun getTokenTypeFromToken(token: String): TokenType? {
        return try {
            val typeStr = parseToken(token).payload.get("type", String::class.java)
            TokenType.valueOf(typeStr)
        } catch (e: Exception) {
            null
        }
    }

    fun getUsernameFromToken(token: String): String? {
        return try {
            parseToken(token).payload.subject
        } catch (e: Exception) {
            null
        }
    }

    fun getRoleFromToken(token: String): String? {
        return try {
            parseToken(token).payload.get("role", String::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getAdminIdFromToken(token: String): Long? {
        return try {
            parseToken(token).payload.get("adminId", java.lang.Long::class.java)?.toLong()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseToken(token: String): Jws<Claims> {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
    }
}
