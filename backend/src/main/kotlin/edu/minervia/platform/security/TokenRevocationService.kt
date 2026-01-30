package edu.minervia.platform.security

import edu.minervia.platform.config.JwtProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * CONSTRAINT [JWT-REVOCATION-LIST]: Redis revocation list storing only jti.
 * Key format: revoked:jwt:{jti}
 * TTL matches token expiration for automatic cleanup.
 */
@Service
class TokenRevocationService(
    private val redisTemplate: StringRedisTemplate,
    private val jwtProperties: JwtProperties
) {
    private val logger = LoggerFactory.getLogger(TokenRevocationService::class.java)

    companion object {
        private const val REVOCATION_KEY_PREFIX = "revoked:jwt:"
    }

    fun revokeAccessToken(jti: String, remainingTtlMs: Long? = null) {
        val ttl = remainingTtlMs ?: jwtProperties.accessTokenExpiration
        setRevocation(jti, ttl)
        logger.debug("Revoked access token: {}", jti)
    }

    fun revokeRefreshToken(jti: String, remainingTtlMs: Long? = null) {
        val ttl = remainingTtlMs ?: jwtProperties.refreshTokenExpiration
        setRevocation(jti, ttl)
        logger.debug("Revoked refresh token: {}", jti)
    }

    private fun setRevocation(jti: String, ttlMs: Long) {
        if (ttlMs <= 0) return
        val key = "$REVOCATION_KEY_PREFIX$jti"
        redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(ttlMs))
    }

    fun isRevoked(jti: String): Boolean {
        val key = "$REVOCATION_KEY_PREFIX$jti"
        return redisTemplate.hasKey(key) == true
    }

    fun revokeTokenPair(accessJti: String?, refreshJti: String?) {
        accessJti?.let { revokeAccessToken(it) }
        refreshJti?.let { revokeRefreshToken(it) }
    }
}
