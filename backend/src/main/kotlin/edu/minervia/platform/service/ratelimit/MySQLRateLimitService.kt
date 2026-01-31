package edu.minervia.platform.service.ratelimit

import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant

/**
 * MySQL fallback rate limiter using SELECT ... FOR UPDATE for atomic updates.
 */
@Service
class MySQLRateLimitService(
    private val jdbcTemplate: JdbcTemplate
) : RateLimitService {
    private val log = LoggerFactory.getLogger(javaClass)

    private data class RateLimitRow(
        val count: Int,
        val windowStart: Instant,
        val windowSeconds: Int
    )

    @Transactional
    override fun tryAcquire(key: String, limit: Int, windowSeconds: Int): Boolean {
        val now = Instant.now()
        val row = selectForUpdate(key)

        if (row == null) {
            return insertNewWindow(key, now, windowSeconds, limit)
        }

        val windowEnd = row.windowStart.plusSeconds(row.windowSeconds.toLong())
        if (now.isAfter(windowEnd)) {
            updateWindow(key, count = 1, windowStart = now, windowSeconds = windowSeconds)
            return true
        }

        if (row.count < limit) {
            updateCount(key, row.count + 1, windowSeconds)
            return true
        }

        return false
    }

    @Transactional
    override fun getRemainingQuota(key: String, limit: Int, windowSeconds: Int): Int {
        val now = Instant.now()
        val row = selectForUpdate(key) ?: return limit

        val windowEnd = row.windowStart.plusSeconds(row.windowSeconds.toLong())
        if (now.isAfter(windowEnd)) {
            jdbcTemplate.update(DELETE_SQL, key)
            return limit
        }

        return (limit - row.count).coerceAtLeast(0)
    }

    @Transactional
    override fun reset(key: String) {
        jdbcTemplate.update(DELETE_SQL, key)
    }

    private fun selectForUpdate(key: String): RateLimitRow? {
        return try {
            jdbcTemplate.queryForObject(SELECT_FOR_UPDATE_SQL, { rs, _ ->
                RateLimitRow(
                    count = rs.getInt("count"),
                    windowStart = rs.getTimestamp("window_start").toInstant(),
                    windowSeconds = rs.getInt("window_seconds")
                )
            }, key)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    private fun insertNewWindow(key: String, now: Instant, windowSeconds: Int, limit: Int): Boolean {
        return try {
            jdbcTemplate.update(
                INSERT_SQL,
                key,
                1,
                Timestamp.from(now),
                windowSeconds
            )
            true
        } catch (e: DuplicateKeyException) {
            log.debug("Rate limit row already exists for key {}, retrying", key)
            val row = selectForUpdate(key) ?: return false
            val nowInstant = Instant.now()
            val windowEnd = row.windowStart.plusSeconds(row.windowSeconds.toLong())
            if (nowInstant.isAfter(windowEnd)) {
                updateWindow(key, count = 1, windowStart = nowInstant, windowSeconds = windowSeconds)
                true
            } else if (row.count < limit) {
                updateCount(key, row.count + 1, windowSeconds)
                true
            } else {
                false
            }
        }
    }

    private fun updateWindow(key: String, count: Int, windowStart: Instant, windowSeconds: Int) {
        jdbcTemplate.update(
            UPDATE_WINDOW_SQL,
            count,
            Timestamp.from(windowStart),
            windowSeconds,
            key
        )
    }

    private fun updateCount(key: String, count: Int, windowSeconds: Int) {
        jdbcTemplate.update(
            UPDATE_COUNT_SQL,
            count,
            windowSeconds,
            key
        )
    }

    companion object {
        private const val SELECT_FOR_UPDATE_SQL =
            "SELECT count, window_start, window_seconds FROM rate_limits WHERE limit_key = ? FOR UPDATE"
        private const val INSERT_SQL =
            "INSERT INTO rate_limits (limit_key, count, window_start, window_seconds, updated_at) VALUES (?, ?, ?, ?, NOW())"
        private const val UPDATE_WINDOW_SQL =
            "UPDATE rate_limits SET count = ?, window_start = ?, window_seconds = ?, updated_at = NOW() WHERE limit_key = ?"
        private const val UPDATE_COUNT_SQL =
            "UPDATE rate_limits SET count = ?, window_seconds = ?, updated_at = NOW() WHERE limit_key = ?"
        private const val DELETE_SQL =
            "DELETE FROM rate_limits WHERE limit_key = ?"
    }
}
