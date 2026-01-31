package edu.minervia.platform.service.ratelimit

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Circuit breaker aware fallback: Redis primary, MySQL secondary.
 */
@Service
@Primary
class RateLimitFallbackService(
    private val redisRateLimitService: RedisRateLimitService,
    private val mysqlRateLimitService: MySQLRateLimitService
) : RateLimitService {
    private val log = LoggerFactory.getLogger(javaClass)
    private val circuitBreaker = CircuitBreaker()

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    fun checkRedisHealth() {
        try {
            if (redisRateLimitService.ping()) {
                circuitBreaker.recordSuccess()
            } else {
                circuitBreaker.recordFailure()
            }
        } catch (e: Exception) {
            circuitBreaker.recordFailure()
            log.warn("Redis health check failed: {}", e.message)
        }
    }

    override fun tryAcquire(key: String, limit: Int, windowSeconds: Int): Boolean {
        if (circuitBreaker.allowRequest()) {
            try {
                val result = redisRateLimitService.tryAcquire(key, limit, windowSeconds)
                circuitBreaker.recordSuccess()
                return result
            } catch (e: Exception) {
                circuitBreaker.recordFailure()
                log.warn("Redis rate limit failed, falling back to MySQL: {}", e.message)
            }
        }
        return mysqlRateLimitService.tryAcquire(key, limit, windowSeconds)
    }

    override fun getRemainingQuota(key: String, limit: Int, windowSeconds: Int): Int {
        if (circuitBreaker.allowRequest()) {
            try {
                val remaining = redisRateLimitService.getRemainingQuota(key, limit, windowSeconds)
                circuitBreaker.recordSuccess()
                return remaining
            } catch (e: Exception) {
                circuitBreaker.recordFailure()
                log.warn("Redis quota lookup failed, falling back to MySQL: {}", e.message)
            }
        }
        return mysqlRateLimitService.getRemainingQuota(key, limit, windowSeconds)
    }

    override fun reset(key: String) {
        if (circuitBreaker.allowRequest()) {
            try {
                redisRateLimitService.reset(key)
                circuitBreaker.recordSuccess()
                return
            } catch (e: Exception) {
                circuitBreaker.recordFailure()
                log.warn("Redis reset failed, falling back to MySQL: {}", e.message)
            }
        }
        mysqlRateLimitService.reset(key)
    }

    private class CircuitBreaker(
        private val failureThreshold: Int = 1,
        private val openDurationMs: Long = 30_000
    ) {
        private val state = AtomicReference(State.CLOSED)
        private val failureCount = AtomicInteger(0)
        private val openedAt = AtomicLong(0)

        fun allowRequest(): Boolean {
            return when (state.get()) {
                State.CLOSED -> true
                State.OPEN -> {
                    val now = System.currentTimeMillis()
                    if (now - openedAt.get() >= openDurationMs) {
                        state.set(State.HALF_OPEN)
                        true
                    } else {
                        false
                    }
                }
                State.HALF_OPEN -> true
            }
        }

        fun recordSuccess() {
            failureCount.set(0)
            state.set(State.CLOSED)
        }

        fun recordFailure() {
            if (failureCount.incrementAndGet() >= failureThreshold) {
                openedAt.set(System.currentTimeMillis())
                state.set(State.OPEN)
            }
        }

        private enum class State {
            CLOSED,
            OPEN,
            HALF_OPEN
        }
    }
}
