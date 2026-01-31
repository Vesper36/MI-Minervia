package edu.minervia.platform.service.email

import edu.minervia.platform.domain.entity.EmailSuppression
import edu.minervia.platform.domain.enums.ActorType
import edu.minervia.platform.domain.enums.EmailSuppressionReason
import edu.minervia.platform.domain.repository.EmailSuppressionRepository
import edu.minervia.platform.service.audit.AuditContext
import edu.minervia.platform.service.audit.AuditEvent
import edu.minervia.platform.service.audit.AuditLogService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale

@Service
class EmailBounceService(
    private val emailSuppressionRepository: EmailSuppressionRepository,
    private val auditLogService: AuditLogService
) {
    companion object {
        private const val SOFT_BOUNCE_THRESHOLD = 5
        private const val SOFT_BOUNCE_WINDOW_HOURS = 72L
    }

    @Transactional
    fun handleHardBounce(email: String, reason: String) {
        val normalized = normalizeEmail(email)
        val now = Instant.now()
        val suppression = emailSuppressionRepository.findByEmail(normalized).orElse(null)
            ?: EmailSuppression(email = normalized)

        suppression.reason = EmailSuppressionReason.HARD_BOUNCE
        suppression.bounceCount = (suppression.bounceCount + 1).coerceAtLeast(1)
        suppression.firstBounceAt = suppression.firstBounceAt ?: now
        suppression.lastBounceAt = now
        if (suppression.suppressedAt == null) {
            suppression.suppressedAt = now
        }

        emailSuppressionRepository.save(suppression)
        logBounceEvent(normalized, "HARD_BOUNCE", reason, suppression)
    }

    @Transactional
    fun handleSoftBounce(email: String, reason: String) {
        val normalized = normalizeEmail(email)
        val now = Instant.now()
        val windowStart = now.minus(SOFT_BOUNCE_WINDOW_HOURS, ChronoUnit.HOURS)

        val suppression = emailSuppressionRepository.findByEmail(normalized).orElse(null)
            ?: EmailSuppression(email = normalized)

        val firstBounceAt = suppression.firstBounceAt
        if (firstBounceAt == null || firstBounceAt.isBefore(windowStart)) {
            suppression.firstBounceAt = now
            suppression.bounceCount = 1
        } else {
            suppression.bounceCount += 1
        }

        suppression.reason = EmailSuppressionReason.SOFT_BOUNCE
        suppression.lastBounceAt = now

        if (suppression.bounceCount >= SOFT_BOUNCE_THRESHOLD && suppression.suppressedAt == null) {
            suppression.suppressedAt = now
        }

        emailSuppressionRepository.save(suppression)
        logBounceEvent(normalized, "SOFT_BOUNCE", reason, suppression)
    }

    @Transactional
    fun handleSpamComplaint(email: String) {
        val normalized = normalizeEmail(email)
        val now = Instant.now()
        val suppression = emailSuppressionRepository.findByEmail(normalized).orElse(null)
            ?: EmailSuppression(email = normalized)

        suppression.reason = EmailSuppressionReason.SPAM_COMPLAINT
        suppression.bounceCount = (suppression.bounceCount + 1).coerceAtLeast(1)
        suppression.firstBounceAt = suppression.firstBounceAt ?: now
        suppression.lastBounceAt = now
        if (suppression.suppressedAt == null) {
            suppression.suppressedAt = now
        }

        emailSuppressionRepository.save(suppression)
        logBounceEvent(normalized, "SPAM_COMPLAINT", null, suppression)
    }

    fun isEmailSuppressed(email: String): Boolean {
        val normalized = normalizeEmail(email)
        val suppression = emailSuppressionRepository.findByEmail(normalized).orElse(null)
        return suppression?.suppressedAt != null
    }

    @Transactional
    fun unsuppressEmail(email: String) {
        val normalized = normalizeEmail(email)
        val suppression = emailSuppressionRepository.findByEmail(normalized)
            .orElseThrow { NoSuchElementException("Suppression not found") }
        unsuppressEntity(suppression)
    }

    @Transactional
    fun unsuppressById(id: Long) {
        val suppression = emailSuppressionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Suppression not found") }
        unsuppressEntity(suppression)
    }

    fun getSuppressedEmails(pageable: Pageable): Page<EmailSuppression> {
        return emailSuppressionRepository.findAllBySuppressedAtIsNotNull(pageable)
    }

    private fun unsuppressEntity(suppression: EmailSuppression) {
        val oldSuppressedAt = suppression.suppressedAt
        suppression.suppressedAt = null
        suppression.reason = null
        suppression.bounceCount = 0
        suppression.firstBounceAt = null
        suppression.lastBounceAt = null
        emailSuppressionRepository.save(suppression)

        if (oldSuppressedAt != null) {
            logUnsuppressedEvent(suppression.email)
        }
    }

    private fun logBounceEvent(
        email: String,
        bounceType: String,
        providerReason: String?,
        suppression: EmailSuppression
    ) {
        val context = AuditContext(actorType = ActorType.SYSTEM)
        val event = AuditEvent(
            eventType = AuditLogService.EVENT_EMAIL_BOUNCE,
            action = "Email bounce: $bounceType",
            targetType = AuditLogService.TARGET_EMAIL,
            newValue = mapOf(
                "email" to email,
                "bounceType" to bounceType,
                "reason" to providerReason,
                "suppressed" to (suppression.suppressedAt != null),
                "bounceCount" to suppression.bounceCount
            )
        )
        auditLogService.logAsync(context, event)
    }

    private fun logUnsuppressedEvent(email: String) {
        val context = AuditContext(actorType = ActorType.SYSTEM)
        val event = AuditEvent(
            eventType = AuditLogService.EVENT_EMAIL_UNSUPPRESSED,
            action = "Email unsuppressed",
            targetType = AuditLogService.TARGET_EMAIL,
            newValue = mapOf("email" to email)
        )
        auditLogService.logAsync(context, event)
    }

    private fun normalizeEmail(email: String): String {
        val normalized = email.trim().lowercase(Locale.ROOT)
        require(normalized.isNotBlank()) { "Email must not be blank" }
        return normalized
    }
}
