package edu.minervia.platform.service

import edu.minervia.platform.service.ratelimit.RedisRateLimitService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.data.redis.core.SessionCallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ZSetOperations

@ExtendWith(MockitoExtension::class)
class RateLimitServiceTest {

    @Mock
    private lateinit var redisTemplate: StringRedisTemplate

    @Mock
    private lateinit var zSetOps: ZSetOperations<String, String>

    private lateinit var service: RedisRateLimitService

    @BeforeEach
    fun setUp() {
        service = RedisRateLimitService(redisTemplate)
        doReturn(zSetOps).`when`(redisTemplate).opsForZSet()
    }

    @Test
    fun tryAcquire_allows_whenUnderLimit() {
        stubExecute(listOf<Any>(0L, 0L, true, true))

        val allowed = service.tryAcquire("user", 5, 60)

        assertTrue(allowed)
        verify(zSetOps, never()).remove(any<String>(), any())
    }

    @Test
    fun tryAcquire_denies_whenAtLimit_andRemovesMember() {
        stubExecute(listOf<Any>(0L, 5L, true, true))

        val allowed = service.tryAcquire("user", 5, 60)

        assertFalse(allowed)
        verify(zSetOps).remove(eq("rate_limit:user"), any())
    }

    @Test
    fun getRemainingQuota_returnsLimitMinusCount() {
        stubExecute(listOf<Any>(0L, 2L, true))

        val remaining = service.getRemainingQuota("user", 5, 60)

        assertEquals(3, remaining)
    }

    @Test
    fun getRemainingQuota_neverNegative() {
        stubExecute(listOf<Any>(0L, 10L, true))

        val remaining = service.getRemainingQuota("user", 5, 60)

        assertEquals(0, remaining)
    }

    @Test
    fun reset_deletesRedisKey() {
        service.reset("user")

        verify(redisTemplate).delete("rate_limit:user")
    }

    private fun stubExecute(result: List<Any>) {
        @Suppress("UNCHECKED_CAST")
        doReturn(result).`when`(redisTemplate)
            .execute(any<SessionCallback<List<Any>>>())
    }
}
