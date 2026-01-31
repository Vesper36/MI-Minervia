package edu.minervia.platform.service.audit

import com.fasterxml.jackson.databind.ObjectMapper
import edu.minervia.platform.domain.entity.AuditNotification
import edu.minervia.platform.domain.entity.NotificationStatus
import edu.minervia.platform.domain.enums.AdminRole
import edu.minervia.platform.domain.repository.AdminRepository
import edu.minervia.platform.domain.repository.AuditNotificationRepository
import edu.minervia.platform.service.email.EmailService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class EmailAlertNotificationService(
    private val emailService: EmailService,
    private val adminRepository: AdminRepository,
    private val auditNotificationRepository: AuditNotificationRepository,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : AlertNotificationService {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DEDUP_KEY_PREFIX = "alert"
        private val DEDUP_WINDOW = Duration.ofHours(1)
    }

    @Transactional
    override fun sendAlert(
        alertType: AlertType,
        severity: AlertSeverity,
        message: String,
        actorId: Long?,
        actorUsername: String?
    ): AlertNotificationResult {
        val dedupKey = buildDedupKey(alertType, actorId)

        if (isDuplicate(dedupKey)) {
            log.debug("Alert deduplicated: {} for actor {}", alertType, actorId)
            return AlertNotificationResult(
                success = true,
                deduplicated = true
            )
        }

        val superAdmins = adminRepository.findAllByRole(AdminRole.SUPER_ADMIN)
            .filter { it.isActive }

        if (superAdmins.isEmpty()) {
            log.warn("No active super admins found to receive alert: {}", alertType)
            return AlertNotificationResult(
                success = false,
                errorMessage = "No active super admins configured"
            )
        }

        val recipientEmails = superAdmins.mapNotNull { it.email }
        if (recipientEmails.isEmpty()) {
            log.warn("No super admin emails found for alert: {}", alertType)
            return AlertNotificationResult(
                success = false,
                errorMessage = "No super admin emails configured"
            )
        }

        val notification = AuditNotification(
            alertType = alertType.name,
            severity = severity.name,
            message = message,
            recipients = objectMapper.writeValueAsString(recipientEmails)
        )

        val savedNotification = auditNotificationRepository.save(notification)

        return try {
            val result = emailService.sendAlertEmail(
                to = recipientEmails,
                alertType = alertType.name,
                severity = severity.name,
                message = message
            )

            if (result.success) {
                savedNotification.sendStatus = NotificationStatus.SENT
                savedNotification.sentAt = Instant.now()
                auditNotificationRepository.save(savedNotification)

                log.info("Alert sent successfully: {} to {} recipients", alertType, recipientEmails.size)
                AlertNotificationResult(
                    success = true,
                    notificationId = savedNotification.id,
                    recipientCount = recipientEmails.size
                )
            } else {
                savedNotification.sendStatus = NotificationStatus.FAILED
                savedNotification.errorMessage = result.errorMessage
                savedNotification.retryCount = 1
                auditNotificationRepository.save(savedNotification)

                log.error("Failed to send alert {}: {}", alertType, result.errorMessage)
                AlertNotificationResult(
                    success = false,
                    notificationId = savedNotification.id,
                    errorMessage = result.errorMessage
                )
            }
        } catch (e: Exception) {
            savedNotification.sendStatus = NotificationStatus.FAILED
            savedNotification.errorMessage = e.message
            savedNotification.retryCount = 1
            auditNotificationRepository.save(savedNotification)

            log.error("Exception sending alert {}: {}", alertType, e.message, e)
            AlertNotificationResult(
                success = false,
                notificationId = savedNotification.id,
                errorMessage = e.message
            )
        }
    }

    private fun buildDedupKey(alertType: AlertType, actorId: Long?): String {
        return "$DEDUP_KEY_PREFIX:${alertType.name}:${actorId ?: "system"}"
    }

    private fun tryAcquireDedupLock(key: String): Boolean {
        return try {
            val acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", DEDUP_WINDOW)
            acquired == true
        } catch (e: Exception) {
            log.warn("Redis unavailable for dedup lock, proceeding with alert: {}", e.message)
            true
        }
    }

    private fun isDuplicate(key: String): Boolean {
        return !tryAcquireDedupLock(key)
    }
}
