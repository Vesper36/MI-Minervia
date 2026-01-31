package edu.minervia.platform.service.audit

import edu.minervia.platform.domain.repository.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Audit alert service for detecting anomalous patterns.
 * Per spec requirements:
 * - Detect bulk ban operations (>10 bans in 1 hour)
 * - Detect anomalous login patterns (multiple IPs in short time)
 * - Alert on critical config changes
 * - Alert on integrity check failures
 */
@Service
class AuditAlertService(
    private val auditLogRepository: AuditLogRepository,
    private val alertNotificationService: AlertNotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val BULK_BAN_THRESHOLD = 10
        const val BULK_BAN_WINDOW_HOURS = 1L
        const val ANOMALOUS_LOGIN_IP_THRESHOLD = 3
        const val ANOMALOUS_LOGIN_WINDOW_MINUTES = 30L
    }

    /**
     * Check for bulk ban operations.
     * Triggers alert if any admin banned >10 students in 1 hour.
     */
    @Scheduled(fixedRate = 900_000) // Every 15 minutes
    fun checkBulkBanOperations() {
        val windowStart = Instant.now().minus(Duration.ofHours(BULK_BAN_WINDOW_HOURS))

        val bulkBans = auditLogRepository.findBulkBanActors(
            eventType = AuditLogService.EVENT_STUDENT_BANNED,
            windowStart = windowStart,
            threshold = BULK_BAN_THRESHOLD.toLong()
        )

        for (banInfo in bulkBans) {
            val actorUsername = banInfo.actorUsername ?: "unknown"
            triggerBulkBanAlert(banInfo.actorId, actorUsername, banInfo.banCount.toInt())
        }
    }

    /**
     * Check for anomalous login patterns.
     * Triggers alert if admin logs in from multiple IPs in short time.
     */
    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    fun checkAnomalousLogins() {
        val windowStart = Instant.now().minus(Duration.ofMinutes(ANOMALOUS_LOGIN_WINDOW_MINUTES))

        val anomalousLogins = auditLogRepository.findAnomalousLoginActors(
            eventType = AuditLogService.EVENT_ADMIN_LOGIN,
            windowStart = windowStart,
            threshold = ANOMALOUS_LOGIN_IP_THRESHOLD.toLong()
        )

        for (loginInfo in anomalousLogins) {
            val actorUsername = loginInfo.actorUsername ?: "unknown"
            val actorId = loginInfo.actorId

            val ipAddresses = auditLogRepository.findAllByEventTypeAndCreatedAtGreaterThanEqualAndActorId(
                AuditLogService.EVENT_ADMIN_LOGIN,
                windowStart,
                actorId
            ).mapNotNull { it.ipAddress }.distinct()

            triggerAnomalousLoginAlert(actorId, actorUsername, ipAddresses)
        }
    }

    /**
     * Trigger alert for bulk ban operation.
     */
    fun triggerBulkBanAlert(actorId: Long?, actorUsername: String, banCount: Int) {
        log.error("ALERT: Bulk ban detected - Admin {} ({}) banned {} students in {} hour(s)",
            actorUsername, actorId, banCount, BULK_BAN_WINDOW_HOURS)

        notifySuperAdmins(
            alertType = AlertType.BULK_BAN,
            message = "Admin $actorUsername banned $banCount students in the last $BULK_BAN_WINDOW_HOURS hour(s)",
            severity = AlertSeverity.HIGH,
            actorId = actorId,
            actorUsername = actorUsername
        )
    }

    /**
     * Trigger alert for anomalous login pattern.
     */
    fun triggerAnomalousLoginAlert(actorId: Long?, actorUsername: String, uniqueIps: List<String>) {
        log.error("ALERT: Anomalous login pattern - Admin {} ({}) logged in from {} different IPs in {} minutes: {}",
            actorUsername, actorId, uniqueIps.size, ANOMALOUS_LOGIN_WINDOW_MINUTES, uniqueIps)

        notifySuperAdmins(
            alertType = AlertType.ANOMALOUS_LOGIN,
            message = "Admin $actorUsername logged in from ${uniqueIps.size} different IPs: ${uniqueIps.joinToString()}",
            severity = AlertSeverity.MEDIUM,
            actorId = actorId,
            actorUsername = actorUsername
        )
    }

    /**
     * Trigger alert for config modification.
     */
    fun triggerConfigModificationAlert(adminUsername: String, configKey: String, oldValue: String?, newValue: String) {
        log.warn("ALERT: Config modified - Admin {} changed {} from '{}' to '{}'",
            adminUsername, configKey, oldValue, newValue)

        notifySuperAdmins(
            alertType = AlertType.CONFIG_MODIFIED,
            message = "Admin $adminUsername modified config '$configKey' from '$oldValue' to '$newValue'",
            severity = AlertSeverity.MEDIUM
        )
    }

    /**
     * Trigger alert for integrity check failure.
     */
    fun triggerIntegrityAlert(invalidLogIds: List<Long>) {
        log.error("CRITICAL ALERT: Audit log integrity check failed for {} entries: {}",
            invalidLogIds.size, invalidLogIds)

        notifySuperAdmins(
            alertType = AlertType.INTEGRITY_FAILURE,
            message = "Audit log integrity check failed for ${invalidLogIds.size} entries. IDs: ${invalidLogIds.joinToString()}",
            severity = AlertSeverity.CRITICAL
        )
    }

    /**
     * Placeholder for notification mechanism.
     * In production, would send to email/Slack/PagerDuty.
     */
    private fun notifySuperAdmins(
        alertType: AlertType,
        message: String,
        severity: AlertSeverity,
        actorId: Long? = null,
        actorUsername: String? = null
    ) {
        val result = alertNotificationService.sendAlert(
            alertType = alertType,
            severity = severity,
            message = message,
            actorId = actorId,
            actorUsername = actorUsername
        )

        if (result.deduplicated) {
            log.debug("Alert {} deduplicated for actor {}", alertType, actorId)
        } else if (!result.success) {
            log.error("Failed to send alert {}: {}", alertType, result.errorMessage)
        }
    }
}

enum class AlertType {
    BULK_BAN,
    ANOMALOUS_LOGIN,
    CONFIG_MODIFIED,
    INTEGRITY_FAILURE
}

enum class AlertSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}
