package edu.minervia.platform.security

import edu.minervia.platform.config.JwtProperties
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

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

    fun generateToken(username: String, role: String, adminId: Long): String {
        val now = Date()
        val expiry = Date(now.time + jwtProperties.expiration)

        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .claim("adminId", adminId)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
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
