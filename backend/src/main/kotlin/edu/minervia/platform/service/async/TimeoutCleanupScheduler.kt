package edu.minervia.platform.service.async

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduler for timeout cleanup tasks.
 * Runs at fixed intervals to detect and process stuck tasks.
 */
@Component
class TimeoutCleanupScheduler(
    private val timeoutCleanupService: TimeoutCleanupService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Scan for timed-out tasks every 30 seconds.
     * Per CONSTRAINT [AI-TIMEOUT-CLEANUP]: Detect tasks stuck in GENERATING state.
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 60_000)
    fun scanForTimedOutTasks() {
        try {
            val processedCount = timeoutCleanupService.processTimedOutTasks()
            if (processedCount > 0) {
                log.info("Timeout cleanup completed: {} tasks processed", processedCount)
            }
        } catch (e: Exception) {
            log.error("Error during timeout cleanup scan: {}", e.message, e)
        }
    }
}
