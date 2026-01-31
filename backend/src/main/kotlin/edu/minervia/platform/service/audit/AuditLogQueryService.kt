package edu.minervia.platform.service.audit

import edu.minervia.platform.domain.entity.AuditLog
import edu.minervia.platform.domain.enums.ActorType
import edu.minervia.platform.domain.enums.AuditResult
import edu.minervia.platform.domain.repository.AuditLogRepository
import edu.minervia.platform.web.dto.*
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.security.MessageDigest
import java.time.Instant

@Service
class AuditLogQueryService(
    private val auditLogRepository: AuditLogRepository,
    private val hashCalculator: AuditHashCalculator
) {
    companion object {
        private const val EXPORT_BATCH_SIZE = 1000
        private const val INTEGRITY_PAGE_SIZE = 500
    }

    fun search(request: AuditLogSearchRequest): Page<AuditLogResponse> {
        val pageable = PageRequest.of(
            request.page,
            request.size.coerceIn(1, 100),
            Sort.by(Sort.Direction.DESC, "createdAt")
        )

        val spec = buildSpecification(request)
        return auditLogRepository.findAll(spec, pageable).map { it.toResponse() }
    }

    fun findById(id: Long): AuditLogResponse? {
        return auditLogRepository.findById(id).orElse(null)?.toResponse()
    }

    fun streamLogs(
        request: AuditLogExportRequest,
        batchSize: Int = EXPORT_BATCH_SIZE,
        consumer: (AuditLog) -> Unit
    ) {
        val spec = buildSpecification(
            AuditLogSearchRequest(
                startTime = request.startTime,
                endTime = request.endTime,
                eventTypes = request.eventTypes,
                actorType = request.actorType,
                actorId = request.actorId
            )
        )

        var page = 0
        var pageResult: Page<AuditLog>

        do {
            val pageable = PageRequest.of(
                page,
                batchSize.coerceAtLeast(1),
                Sort.by(Sort.Direction.ASC, "createdAt", "id")
            )
            pageResult = auditLogRepository.findAll(spec, pageable)
            pageResult.content.forEach(consumer)
            page++
        } while (pageResult.hasNext())
    }

    fun writeCsvExport(
        request: AuditLogExportRequest,
        outputStream: OutputStream,
        batchSize: Int = EXPORT_BATCH_SIZE
    ) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
        writer.append("id,event_type,actor_type,actor_id,actor_username,target_type,target_id,action,result,error_message,ip_address,created_at")
        writer.newLine()

        streamLogs(request, batchSize) { log ->
            writeCsvRow(log, writer)
        }

        writer.flush()
    }

    fun getStatsSummary(startTime: Instant, endTime: Instant): AuditStatsSummary {
        val totalLogs = auditLogRepository.countByCreatedAtBetween(startTime, endTime)
        val successCount = auditLogRepository.countByResultAndCreatedAtBetween(
            AuditResult.SUCCESS,
            startTime,
            endTime
        )
        val failureCount = auditLogRepository.countByResultAndCreatedAtBetween(
            AuditResult.FAILURE,
            startTime,
            endTime
        )

        val eventTypeCounts = auditLogRepository.findEventTypeCounts(startTime, endTime)
            .associate { it.eventType to it.count }
        val actorTypeCounts = auditLogRepository.findActorTypeCounts(startTime, endTime)
            .associate { it.actorType to it.count }

        return AuditStatsSummary(
            totalLogs = totalLogs,
            successCount = successCount,
            failureCount = failureCount,
            eventTypeCounts = eventTypeCounts,
            actorTypeCounts = actorTypeCounts,
            period = AuditPeriod(startTime, endTime)
        )
    }

    fun verifyIntegrity(startId: Long, endId: Long): IntegrityCheckResult {
        val invalidIds = mutableListOf<Long>()
        var validCount = 0L
        var totalChecked = 0L
        var page = 0
        var slice: Slice<AuditLog>

        do {
            val pageable = PageRequest.of(
                page,
                INTEGRITY_PAGE_SIZE,
                Sort.by(Sort.Direction.ASC, "id")
            )
            slice = auditLogRepository.findByIdBetweenOrderByIdAsc(startId, endId, pageable)

            for (log in slice.content) {
                totalChecked++
                if (log.hashValue != null) {
                    val expectedHash = hashCalculator.calculateHash(log)
                    if (expectedHash != log.hashValue) {
                        invalidIds.add(log.id)
                    } else {
                        validCount++
                    }
                } else {
                    invalidIds.add(log.id)
                }
            }
            page++
        } while (slice.hasNext())

        return IntegrityCheckResult(
            totalChecked = totalChecked,
            validCount = validCount,
            invalidCount = invalidIds.size.toLong(),
            invalidIds = invalidIds
        )
    }

    private fun writeCsvRow(log: AuditLog, writer: BufferedWriter) {
        writer.append(log.id.toString()).append(',')
        writer.append(escapeCsv(log.eventType)).append(',')
        writer.append(log.actorType.toString()).append(',')
        writer.append(log.actorId?.toString() ?: "").append(',')
        writer.append(escapeCsv(log.actorUsername ?: "")).append(',')
        writer.append(escapeCsv(log.targetType ?: "")).append(',')
        writer.append(log.targetId?.toString() ?: "").append(',')
        writer.append(escapeCsv(log.action)).append(',')
        writer.append(log.result.toString()).append(',')
        writer.append(escapeCsv(log.errorMessage ?: "")).append(',')
        writer.append(escapeCsv(log.ipAddress ?: "")).append(',')
        writer.append(log.createdAt.toString())
        writer.newLine()
    }

    private fun buildSpecification(request: AuditLogSearchRequest): Specification<AuditLog> {
        return Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            request.startTime?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), it))
            }

            request.endTime?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), it))
            }

            request.eventTypes?.takeIf { it.isNotEmpty() }?.let {
                predicates.add(root.get<String>("eventType").`in`(it))
            }

            request.actorType?.let {
                predicates.add(cb.equal(root.get<ActorType>("actorType"), it))
            }

            request.actorId?.let {
                predicates.add(cb.equal(root.get<Long>("actorId"), it))
            }

            request.actorUsername?.let {
                predicates.add(cb.like(root.get("actorUsername"), "%$it%"))
            }

            request.targetType?.let {
                predicates.add(cb.equal(root.get<String>("targetType"), it))
            }

            request.targetId?.let {
                predicates.add(cb.equal(root.get<Long>("targetId"), it))
            }

            request.action?.let {
                predicates.add(cb.like(root.get("action"), "%$it%"))
            }

            request.result?.let {
                predicates.add(cb.equal(root.get<AuditResult>("result"), it))
            }

            cb.and(*predicates.toTypedArray())
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun AuditLog.toResponse() = AuditLogResponse(
        id = id,
        eventType = eventType,
        actorType = actorType,
        actorId = actorId,
        actorUsername = actorUsername,
        targetType = targetType,
        targetId = targetId,
        action = action,
        result = result,
        errorMessage = errorMessage,
        oldValue = oldValue,
        newValue = newValue,
        ipAddress = ipAddress,
        userAgent = userAgent,
        sessionId = sessionId,
        createdAt = createdAt
    )
}
