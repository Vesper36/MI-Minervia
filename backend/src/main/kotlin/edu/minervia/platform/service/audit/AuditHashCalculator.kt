package edu.minervia.platform.service.audit

import edu.minervia.platform.domain.entity.AuditLog
import edu.minervia.platform.domain.enums.ActorType
import edu.minervia.platform.domain.enums.AuditResult
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Instant

@Component
class AuditHashCalculator {

    fun calculateHash(
        eventType: String,
        actorType: ActorType,
        actorId: Long?,
        action: String,
        result: AuditResult,
        targetType: String?,
        targetId: Long?,
        oldValue: String?,
        newValue: String?,
        ipAddress: String?,
        createdAt: Instant
    ): String {
        val normalizedCreatedAt = normalizeTimestamp(createdAt)

        val data = buildString {
            append(eventType)
            append(actorType)
            append(actorId ?: "")
            append(action)
            append(result)
            append(targetType ?: "")
            append(targetId ?: "")
            append(oldValue ?: "")
            append(newValue ?: "")
            append(ipAddress ?: "")
            append(normalizedCreatedAt.epochSecond)
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun calculateHash(auditLog: AuditLog): String {
        return calculateHash(
            eventType = auditLog.eventType,
            actorType = auditLog.actorType,
            actorId = auditLog.actorId,
            action = auditLog.action,
            result = auditLog.result,
            targetType = auditLog.targetType,
            targetId = auditLog.targetId,
            oldValue = auditLog.oldValue,
            newValue = auditLog.newValue,
            ipAddress = auditLog.ipAddress,
            createdAt = auditLog.createdAt
        )
    }

    private fun normalizeTimestamp(instant: Instant): Instant {
        return Instant.ofEpochSecond(instant.epochSecond)
    }
}
