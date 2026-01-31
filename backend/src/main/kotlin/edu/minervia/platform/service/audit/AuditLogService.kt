package edu.minervia.platform.service.audit

import edu.minervia.platform.domain.entity.AuditLog
import edu.minervia.platform.domain.enums.ActorType
import edu.minervia.platform.domain.enums.AuditResult
import edu.minervia.platform.domain.repository.AuditLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.CompletableFuture

data class AuditContext(
    val actorType: ActorType,
    val actorId: Long? = null,
    val actorUsername: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val sessionId: String? = null
)

data class AuditEvent(
    val eventType: String,
    val action: String,
    val targetType: String? = null,
    val targetId: Long? = null,
    val oldValue: Any? = null,
    val newValue: Any? = null,
    val result: AuditResult = AuditResult.SUCCESS,
    val errorMessage: String? = null
)

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper,
    private val hashCalculator: AuditHashCalculator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // Event types
        const val EVENT_ADMIN_LOGIN = "ADMIN_LOGIN"
        const val EVENT_ADMIN_LOGOUT = "ADMIN_LOGOUT"
        const val EVENT_LOGIN_FAILED = "LOGIN_FAILED"
        const val EVENT_STUDENT_CREATED = "STUDENT_CREATED"
        const val EVENT_STUDENT_UPDATED = "STUDENT_UPDATED"
        const val EVENT_STUDENT_BANNED = "STUDENT_BANNED"
        const val EVENT_STUDENT_UNBANNED = "STUDENT_UNBANNED"
        const val EVENT_REGISTRATION_APPROVED = "REGISTRATION_APPROVED"
        const val EVENT_REGISTRATION_REJECTED = "REGISTRATION_REJECTED"
        const val EVENT_CONFIG_MODIFIED = "CONFIG_MODIFIED"
        const val EVENT_BULK_BAN = "BULK_BAN"
        const val EVENT_IDENTITY_GENERATED = "IDENTITY_GENERATED"
        const val EVENT_CODE_GENERATED = "CODE_GENERATED"
        const val EVENT_CODE_REVOKED = "CODE_REVOKED"
        const val EVENT_EMAIL_BOUNCE = "EMAIL_BOUNCE"
        const val EVENT_EMAIL_UNSUPPRESSED = "EMAIL_UNSUPPRESSED"

        // Target types
        const val TARGET_STUDENT = "STUDENT"
        const val TARGET_APPLICATION = "APPLICATION"
        const val TARGET_CONFIG = "CONFIG"
        const val TARGET_CODE = "REGISTRATION_CODE"
        const val TARGET_ADMIN = "ADMIN"
        const val TARGET_EMAIL = "EMAIL"
    }

    @Async
    fun logAsync(context: AuditContext, event: AuditEvent): CompletableFuture<AuditLog> {
        return CompletableFuture.completedFuture(log(context, event))
    }

    fun log(context: AuditContext, event: AuditEvent): AuditLog {
        try {
            val oldValueJson = event.oldValue?.let { serializeValue(it) }
            val newValueJson = event.newValue?.let { serializeValue(it) }
            val now = Instant.now()

            val preHash = hashCalculator.calculateHash(
                eventType = event.eventType,
                actorType = context.actorType,
                actorId = context.actorId,
                action = event.action,
                result = event.result,
                targetType = event.targetType,
                targetId = event.targetId,
                oldValue = oldValueJson,
                newValue = newValueJson,
                ipAddress = context.ipAddress,
                createdAt = now
            )

            val auditLog = AuditLog(
                eventType = event.eventType,
                actorType = context.actorType,
                actorId = context.actorId,
                actorUsername = context.actorUsername,
                targetType = event.targetType,
                targetId = event.targetId,
                action = event.action,
                result = event.result,
                errorMessage = event.errorMessage,
                oldValue = oldValueJson,
                newValue = newValueJson,
                ipAddress = context.ipAddress,
                userAgent = context.userAgent,
                sessionId = context.sessionId,
                hashValue = preHash,
                createdAt = now
            )

            return auditLogRepository.save(auditLog)
        } catch (e: Exception) {
            log.error("Failed to save audit log: ${event.eventType} - ${event.action}", e)
            throw e
        }
    }

    private fun serializeValue(value: Any): String {
        return when (value) {
            is String -> value
            else -> objectMapper.writeValueAsString(value)
        }
    }

    // Convenience methods for common events
    fun logAdminLogin(adminId: Long, username: String, ipAddress: String?, userAgent: String?) {
        val context = AuditContext(
            actorType = ActorType.ADMIN,
            actorId = adminId,
            actorUsername = username,
            ipAddress = ipAddress,
            userAgent = userAgent
        )
        val event = AuditEvent(
            eventType = EVENT_ADMIN_LOGIN,
            action = "Admin login",
            targetType = TARGET_ADMIN,
            targetId = adminId
        )
        logAsync(context, event)
    }

    fun logLoginFailed(username: String, reason: String, ipAddress: String?, userAgent: String?) {
        val context = AuditContext(
            actorType = ActorType.SYSTEM,
            actorUsername = username,
            ipAddress = ipAddress,
            userAgent = userAgent
        )
        val event = AuditEvent(
            eventType = EVENT_LOGIN_FAILED,
            action = "Login failed: $reason",
            result = AuditResult.FAILURE,
            errorMessage = reason
        )
        logAsync(context, event)
    }

    fun logStudentCreated(adminId: Long, adminUsername: String, studentId: Long, studentNumber: String, ipAddress: String?) {
        val context = AuditContext(
            actorType = ActorType.ADMIN,
            actorId = adminId,
            actorUsername = adminUsername,
            ipAddress = ipAddress
        )
        val event = AuditEvent(
            eventType = EVENT_STUDENT_CREATED,
            action = "Created student account",
            targetType = TARGET_STUDENT,
            targetId = studentId,
            newValue = mapOf("studentNumber" to studentNumber)
        )
        logAsync(context, event)
    }

    fun logStudentBanned(adminId: Long, adminUsername: String, studentId: Long, reason: String?, ipAddress: String?) {
        val context = AuditContext(
            actorType = ActorType.ADMIN,
            actorId = adminId,
            actorUsername = adminUsername,
            ipAddress = ipAddress
        )
        val event = AuditEvent(
            eventType = EVENT_STUDENT_BANNED,
            action = "Banned student account",
            targetType = TARGET_STUDENT,
            targetId = studentId,
            newValue = mapOf("reason" to (reason ?: "No reason provided"))
        )
        logAsync(context, event)
    }

    fun logStudentUnbanned(adminId: Long, adminUsername: String, studentId: Long, ipAddress: String?) {
        val context = AuditContext(
            actorType = ActorType.ADMIN,
            actorId = adminId,
            actorUsername = adminUsername,
            ipAddress = ipAddress
        )
        val event = AuditEvent(
            eventType = EVENT_STUDENT_UNBANNED,
            action = "Unbanned student account",
            targetType = TARGET_STUDENT,
            targetId = studentId
        )
        logAsync(context, event)
    }

    fun logRegistrationApproved(adminId: Long, adminUsername: String, applicationId: Long, ipAddress: String?) {
        val context = AuditContext(
            actorType = ActorType.ADMIN,
            actorId = adminId,
            actorUsername = adminUsername,
            ipAddress = ipAddress
        )
        val event = AuditEvent(
            eventType = EVENT_REGISTRATION_APPROVED,
            action = "Approved registration application",
            targetType = TARGET_APPLICATION,
            targetId = applicationId
        )
        logAsync(context, event)
    }

    fun logRegistrationRejected(adminId: Long, adminUsername: String, applicationId: Long, reason: String, ipAddress: String?) {
        val context = AuditContext(
            actorType = ActorType.ADMIN,
            actorId = adminId,
            actorUsername = adminUsername,
            ipAddress = ipAddress
        )
        val event = AuditEvent(
            eventType = EVENT_REGISTRATION_REJECTED,
            action = "Rejected registration application",
            targetType = TARGET_APPLICATION,
            targetId = applicationId,
            newValue = mapOf("reason" to reason)
        )
        logAsync(context, event)
    }

    fun logConfigModified(adminId: Long, adminUsername: String, configKey: String, oldValue: String?, newValue: String, ipAddress: String?) {
        val context = AuditContext(
            actorType = ActorType.ADMIN,
            actorId = adminId,
            actorUsername = adminUsername,
            ipAddress = ipAddress
        )
        val event = AuditEvent(
            eventType = EVENT_CONFIG_MODIFIED,
            action = "Modified system config: $configKey",
            targetType = TARGET_CONFIG,
            oldValue = mapOf("key" to configKey, "value" to (oldValue ?: "")),
            newValue = mapOf("key" to configKey, "value" to newValue)
        )
        logAsync(context, event)
    }
}
