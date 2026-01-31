package edu.minervia.platform.service.ratelimit

interface RateLimitService {
    fun tryAcquire(key: String, limit: Int, windowSeconds: Int): Boolean
    fun getRemainingQuota(key: String, limit: Int, windowSeconds: Int): Int
    fun reset(key: String)
}
