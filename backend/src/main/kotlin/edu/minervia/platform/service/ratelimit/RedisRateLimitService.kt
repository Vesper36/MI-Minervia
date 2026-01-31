package edu.minervia.platform.service.ratelimit

import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.SessionCallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

/**
 * Redis sliding window rate limiter using MULTI/EXEC.
 * Key format: rate_limit:{key}
 */
@Service
class RedisRateLimitService(
    private val redisTemplate: StringRedisTemplate
) : RateLimitService {

    companion object {
        private const val KEY_PREFIX = "rate_limit:"
    }

    fun ping(): Boolean {
        val response = redisTemplate.execute { connection -> connection.ping() }
        return response != null
    }

    override fun tryAcquire(key: String, limit: Int, windowSeconds: Int): Boolean {
        val redisKey = "$KEY_PREFIX$key"
        val nowMillis = System.currentTimeMillis()
        val windowStartMillis = nowMillis - windowSeconds * 1000L
        val member = "$nowMillis:${UUID.randomUUID()}"

        val results = redisTemplate.execute(object : SessionCallback<List<Any>> {
            override fun <K, V> execute(operations: RedisOperations<K, V>): List<Any> {
                @Suppress("UNCHECKED_CAST")
                val ops = operations as RedisOperations<String, String>
                val zset = ops.opsForZSet()
                ops.multi()
                zset.removeRangeByScore(redisKey, 0.0, windowStartMillis.toDouble())
                zset.size(redisKey)
                zset.add(redisKey, member, nowMillis.toDouble())
                ops.expire(redisKey, Duration.ofSeconds(windowSeconds.toLong()))
                return ops.exec() ?: emptyList()
            }
        }) ?: emptyList()

        val currentCount = (results.getOrNull(1) as? Long) ?: 0L
        val allowed = currentCount < limit.toLong()
        if (!allowed) {
            redisTemplate.opsForZSet().remove(redisKey, member)
        }
        return allowed
    }

    override fun getRemainingQuota(key: String, limit: Int, windowSeconds: Int): Int {
        val redisKey = "$KEY_PREFIX$key"
        val nowMillis = System.currentTimeMillis()
        val windowStartMillis = nowMillis - windowSeconds * 1000L

        val results = redisTemplate.execute(object : SessionCallback<List<Any>> {
            override fun <K, V> execute(operations: RedisOperations<K, V>): List<Any> {
                @Suppress("UNCHECKED_CAST")
                val ops = operations as RedisOperations<String, String>
                val zset = ops.opsForZSet()
                ops.multi()
                zset.removeRangeByScore(redisKey, 0.0, windowStartMillis.toDouble())
                zset.size(redisKey)
                ops.expire(redisKey, Duration.ofSeconds(windowSeconds.toLong()))
                return ops.exec() ?: emptyList()
            }
        }) ?: emptyList()

        val currentCount = (results.getOrNull(1) as? Long) ?: 0L
        return (limit - currentCount.toInt()).coerceAtLeast(0)
    }

    override fun reset(key: String) {
        redisTemplate.delete("$KEY_PREFIX$key")
    }
}
