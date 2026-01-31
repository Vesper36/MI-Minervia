package edu.minervia.platform.web.controller

import edu.minervia.platform.service.audit.AuditLogQueryService
import edu.minervia.platform.web.dto.*
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
class AuditLogController(
    private val auditLogQueryService: AuditLogQueryService,
    private val objectMapper: ObjectMapper
) {

    @GetMapping
    fun searchLogs(
        @RequestParam(required = false) startTime: String?,
        @RequestParam(required = false) endTime: String?,
        @RequestParam(required = false) eventTypes: List<String>?,
        @RequestParam(required = false) actorType: String?,
        @RequestParam(required = false) actorId: Long?,
        @RequestParam(required = false) actorUsername: String?,
        @RequestParam(required = false) targetType: String?,
        @RequestParam(required = false) targetId: Long?,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) result: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Map<String, Any>> {
        val request = AuditLogSearchRequest(
            startTime = startTime?.let { parseInstant(it) },
            endTime = endTime?.let { parseInstant(it) },
            eventTypes = eventTypes,
            actorType = actorType?.let { edu.minervia.platform.domain.enums.ActorType.valueOf(it) },
            actorId = actorId,
            actorUsername = actorUsername,
            targetType = targetType,
            targetId = targetId,
            action = action,
            result = result?.let { edu.minervia.platform.domain.enums.AuditResult.valueOf(it) },
            page = page,
            size = size
        )

        val results = auditLogQueryService.search(request)

        return ResponseEntity.ok(
            mapOf(
                "content" to results.content,
                "totalElements" to results.totalElements,
                "totalPages" to results.totalPages,
                "page" to results.number,
                "size" to results.size
            )
        )
    }

    @GetMapping("/{id}")
    fun getLogById(@PathVariable id: Long): ResponseEntity<AuditLogResponse> {
        val log = auditLogQueryService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(log)
    }

    @GetMapping("/export")
    fun exportLogs(
        @RequestParam startTime: String,
        @RequestParam endTime: String,
        @RequestParam(required = false) eventTypes: List<String>?,
        @RequestParam(required = false) actorType: String?,
        @RequestParam(required = false) actorId: Long?,
        @RequestParam(defaultValue = "CSV") format: String,
        response: HttpServletResponse
    ) {
        val exportFormat = ExportFormat.valueOf(format.uppercase())
        val request = AuditLogExportRequest(
            startTime = parseInstant(startTime),
            endTime = parseInstant(endTime),
            eventTypes = eventTypes,
            actorType = actorType?.let { edu.minervia.platform.domain.enums.ActorType.valueOf(it) },
            actorId = actorId,
            format = exportFormat
        )

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

        response.characterEncoding = Charsets.UTF_8.name()

        when (exportFormat) {
            ExportFormat.CSV -> {
                response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=audit_logs_$timestamp.csv"
                )
                response.contentType = "text/csv"
                auditLogQueryService.writeCsvExport(request, response.outputStream)
            }
            ExportFormat.JSON -> {
                response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=audit_logs_$timestamp.json"
                )
                response.contentType = MediaType.APPLICATION_JSON_VALUE

                val generator = objectMapper.factory.createGenerator(response.outputStream)
                val writer = objectMapper.writerWithDefaultPrettyPrinter()
                generator.writeStartArray()
                auditLogQueryService.streamLogs(request) { log ->
                    writer.writeValue(generator, log)
                }
                generator.writeEndArray()
                generator.flush()
                generator.close()
            }
        }
    }

    @GetMapping("/stats")
    fun getStats(
        @RequestParam startTime: String,
        @RequestParam endTime: String
    ): ResponseEntity<AuditStatsSummary> {
        val stats = auditLogQueryService.getStatsSummary(
            parseInstant(startTime),
            parseInstant(endTime)
        )
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/integrity-check")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun checkIntegrity(
        @RequestParam startId: Long,
        @RequestParam endId: Long
    ): ResponseEntity<IntegrityCheckResult> {
        val result = auditLogQueryService.verifyIntegrity(startId, endId)
        return ResponseEntity.ok(result)
    }

    private fun parseInstant(value: String): Instant {
        return try {
            Instant.parse(value)
        } catch (e: Exception) {
            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toInstant(ZoneOffset.UTC)
        }
    }
}
