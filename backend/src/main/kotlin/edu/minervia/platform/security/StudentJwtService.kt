package edu.minervia.platform.security

import edu.minervia.platform.config.JwtProperties
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

enum class ActorType {
    ADMIN, STUDENT
}

data class StudentTokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenJti: String,
    val refreshTokenJti: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long
)

@Service
class StudentJwtService(private val jwtProperties: JwtProperties) {

    private val logger = LoggerFactory.getLogger(StudentJwtService::class.java)
    private lateinit var secretKey: SecretKey

    @PostConstruct
    fun init() {
        if (jwtProperties.secret.isBlank()) {
            if (jwtProperties.requireSecret) {
                throw IllegalStateException("JWT secret is required but not provided.")
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

    fun generateTokenPair(studentNumber: String, studentId: Long): StudentTokenPair {
        val accessJti = UUID.randomUUID().toString()
        val refreshJti = UUID.randomUUID().toString()

        val accessToken = generateToken(
            subject = studentNumber,
            studentId = studentId,
            jti = accessJti,
            tokenType = TokenType.ACCESS,
            expiration = jwtProperties.accessTokenExpiration
        )

        val refreshToken = generateToken(
            subject = studentNumber,
            studentId = studentId,
            jti = refreshJti,
            tokenType = TokenType.REFRESH,
            expiration = jwtProperties.refreshTokenExpiration
        )

        return StudentTokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenJti = accessJti,
            refreshTokenJti = refreshJti,
            accessExpiresIn = jwtProperties.accessTokenExpiration,
            refreshExpiresIn = jwtProperties.refreshTokenExpiration
        )
    }

    private fun generateToken(
        subject: String,
        studentId: Long,
        jti: String,
        tokenType: TokenType,
        expiration: Long
    ): String {
        val now = Date()
        val expiry = Date(now.time + expiration)

        return Jwts.builder()
            .id(jti)
            .subject(subject)
            .claim("studentId", studentId)
            .claim("actor_type", ActorType.STUDENT.name)
            .claim("type", tokenType.name)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = parseToken(token)
            val actorType = claims.payload.get("actor_type", String::class.java)
            actorType == ActorType.STUDENT.name
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

    fun getStudentNumberFromToken(token: String): String? {
        return try {
            parseToken(token).payload.subject
        } catch (e: Exception) {
            null
        }
    }

    fun getStudentIdFromToken(token: String): Long? {
        return try {
            parseToken(token).payload.get("studentId", java.lang.Long::class.java)?.toLong()
        } catch (e: Exception) {
            null
        }
    }

    fun getActorTypeFromToken(token: String): ActorType? {
        return try {
            val typeStr = parseToken(token).payload.get("actor_type", String::class.java)
            ActorType.valueOf(typeStr)
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
