package edu.minervia.platform.service.audit

import edu.minervia.platform.domain.entity.AuditLog
import edu.minervia.platform.domain.repository.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Slice
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

/**
 * Audit log integrity verification service.
 * Per spec: Audit logs must be tamper-proof with hash verification.
 */
@Service
class AuditIntegrityService(
    private val auditLogRepository: AuditLogRepository,
    private val alertService: AuditAlertService,
    private val hashCalculator: AuditHashCalculator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val INTEGRITY_PAGE_SIZE = 500
    }

    /**
     * Verify integrity of audit logs in a given range.
     * Returns list of log IDs with failed integrity checks.
     */
    fun verifyIntegrity(startId: Long, endId: Long): List<Long> {
        val invalidIds = mutableListOf<Long>()
        var page = 0
        var slice: Slice<AuditLog>

        do {
            val pageable = PageRequest.of(
                page,
                INTEGRITY_PAGE_SIZE,
                Sort.by(Sort.Direction.ASC, "id")
            )
            slice = auditLogRepository.findByIdBetweenOrderByIdAsc(startId, endId, pageable)

            for (auditLog in slice.content) {
                if (auditLog.hashValue != null) {
                    val expectedHash = hashCalculator.calculateHash(auditLog)
                    if (expectedHash != auditLog.hashValue) {
                        invalidIds.add(auditLog.id)
                        log.error(
                            "Audit log integrity check failed for ID {}: expected {}, got {}",
                            auditLog.id,
                            expectedHash,
                            auditLog.hashValue
                        )
                    }
                } else {
                    invalidIds.add(auditLog.id)
                    log.warn("Audit log ID {} has null hash value", auditLog.id)
                }
            }

            page++
        } while (slice.hasNext())

        return invalidIds
    }

    /**
     * Periodic integrity check - runs daily.
     */
    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily
    fun scheduledIntegrityCheck() {
        log.info("Starting scheduled audit log integrity check")

        val now = Instant.now()
        val oneDayAgo = now.minus(Duration.ofDays(1))
        val totalRecent = auditLogRepository.countByCreatedAtBetween(oneDayAgo, now)

        if (totalRecent == 0L) {
            log.info("No audit logs to verify")
            return
        }

        val invalidIds = verifyIntegrityByCreatedAt(oneDayAgo, now)

        if (invalidIds.isNotEmpty()) {
            log.error("Audit log integrity check found {} invalid entries: {}", invalidIds.size, invalidIds)
            alertService.triggerIntegrityAlert(invalidIds)
        } else {
            log.info("Audit log integrity check passed for {} entries", totalRecent)
        }
    }

    private fun verifyIntegrityByCreatedAt(start: Instant, end: Instant): List<Long> {
        val invalidIds = mutableListOf<Long>()
        var page = 0
        var slice: Slice<AuditLog>

        do {
            val pageable = PageRequest.of(
                page,
                INTEGRITY_PAGE_SIZE,
                Sort.by(Sort.Direction.ASC, "id")
            )
            slice = auditLogRepository.findByCreatedAtBetweenOrderByIdAsc(start, end, pageable)

            for (auditLog in slice.content) {
                if (auditLog.hashValue != null) {
                    val expectedHash = hashCalculator.calculateHash(auditLog)
                    if (expectedHash != auditLog.hashValue) {
                        invalidIds.add(auditLog.id)
                        log.error(
                            "Audit log integrity check failed for ID {}: expected {}, got {}",
                            auditLog.id,
                            expectedHash,
                            auditLog.hashValue
                        )
                    }
                } else {
                    invalidIds.add(auditLog.id)
                    log.warn("Audit log ID {} has null hash value", auditLog.id)
                }
            }

            page++
        } while (slice.hasNext())

        return invalidIds
    }
}
