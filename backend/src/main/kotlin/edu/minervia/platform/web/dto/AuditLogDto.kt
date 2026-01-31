package edu.minervia.platform.web.dto

import edu.minervia.platform.domain.enums.ActorType
import edu.minervia.platform.domain.enums.AuditResult
import java.time.Instant

data class AuditLogResponse(
    val id: Long,
    val eventType: String,
    val actorType: ActorType,
    val actorId: Long?,
    val actorUsername: String?,
    val targetType: String?,
    val targetId: Long?,
    val action: String,
    val result: AuditResult,
    val errorMessage: String?,
    val oldValue: String?,
    val newValue: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val sessionId: String?,
    val createdAt: Instant
)

data class AuditLogSearchRequest(
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val eventTypes: List<String>? = null,
    val actorType: ActorType? = null,
    val actorId: Long? = null,
    val actorUsername: String? = null,
    val targetType: String? = null,
    val targetId: Long? = null,
    val action: String? = null,
    val result: AuditResult? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class AuditLogExportRequest(
    val startTime: Instant,
    val endTime: Instant,
    val eventTypes: List<String>? = null,
    val actorType: ActorType? = null,
    val actorId: Long? = null,
    val format: ExportFormat = ExportFormat.CSV
)

enum class ExportFormat {
    CSV,
    JSON
}

data class AuditStatsSummary(
    val totalLogs: Long,
    val successCount: Long,
    val failureCount: Long,
    val eventTypeCounts: Map<String, Long>,
    val actorTypeCounts: Map<ActorType, Long>,
    val period: AuditPeriod
)

data class AuditPeriod(
    val start: Instant,
    val end: Instant
)

data class IntegrityCheckResult(
    val totalChecked: Long,
    val validCount: Long,
    val invalidCount: Long,
    val invalidIds: List<Long>
)
