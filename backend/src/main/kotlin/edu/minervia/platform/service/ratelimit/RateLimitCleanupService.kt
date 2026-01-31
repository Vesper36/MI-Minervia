package edu.minervia.platform.service.ratelimit

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RateLimitCleanupService(
    private val jdbcTemplate: JdbcTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun cleanupExpiredWindows() {
        val deleted = jdbcTemplate.update(
            "DELETE FROM rate_limits WHERE DATE_ADD(window_start, INTERVAL window_seconds SECOND) < NOW()"
        )
        if (deleted > 0) {
            log.info("Cleaned up {} expired rate limit windows", deleted)
        }
    }
}
