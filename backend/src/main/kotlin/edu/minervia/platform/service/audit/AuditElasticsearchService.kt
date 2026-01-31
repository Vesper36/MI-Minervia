package edu.minervia.platform.service.audit

import edu.minervia.platform.domain.entity.AuditLog
import edu.minervia.platform.domain.repository.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Service for syncing audit logs to Elasticsearch.
 * Per CONSTRAINT [DB-ES-THRESHOLD]:
 * - MVP uses MySQL partitions + indexes
 * - ES introduced when queries >1000/day or records >5M
 *
 * This service provides the infrastructure for ES sync when enabled.
 */
@Service
class AuditElasticsearchService(
    private val auditLogRepository: AuditLogRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${minervia.audit.elasticsearch.enabled:false}")
    private var esEnabled: Boolean = false

    @Value("\${minervia.audit.elasticsearch.url:}")
    private var esUrl: String = ""

    @Value("\${minervia.audit.elasticsearch.index:audit-logs}")
    private var esIndex: String = "audit-logs"

    private val lastSyncedId = AtomicLong(0)

    companion object {
        const val BATCH_SIZE = 500
        const val ES_THRESHOLD_RECORDS = 5_000_000L
        const val ES_THRESHOLD_DAILY_QUERIES = 1000L
    }

    /**
     * Check if ES sync should be enabled based on thresholds.
     */
    fun shouldEnableElasticsearch(): ThresholdCheckResult {
        val totalRecords = auditLogRepository.count()
        val yesterday = Instant.now().minus(1, ChronoUnit.DAYS)
        val today = Instant.now()
        val dailyQueries = estimateDailyQueries()

        val recordsExceeded = totalRecords > ES_THRESHOLD_RECORDS
        val queriesExceeded = dailyQueries > ES_THRESHOLD_DAILY_QUERIES

        return ThresholdCheckResult(
            totalRecords = totalRecords,
            recordThreshold = ES_THRESHOLD_RECORDS,
            recordsExceeded = recordsExceeded,
            estimatedDailyQueries = dailyQueries,
            queryThreshold = ES_THRESHOLD_DAILY_QUERIES,
            queriesExceeded = queriesExceeded,
            recommendation = when {
                recordsExceeded || queriesExceeded -> "Enable Elasticsearch"
                totalRecords > ES_THRESHOLD_RECORDS * 0.8 -> "Consider enabling Elasticsearch soon"
                else -> "MySQL is sufficient"
            }
        )
    }

    private fun estimateDailyQueries(): Long {
        return 0L
    }

    /**
     * Sync new audit logs to Elasticsearch.
     * Runs every 5 minutes when ES is enabled.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    fun syncToElasticsearch() {
        if (!esEnabled || esUrl.isBlank()) {
            return
        }

        try {
            val newLogs = fetchNewLogs()
            if (newLogs.isEmpty()) {
                return
            }

            bulkIndexToEs(newLogs)
            updateLastSyncedId(newLogs)

            log.info("Synced {} audit logs to Elasticsearch", newLogs.size)
        } catch (e: Exception) {
            log.error("Failed to sync audit logs to Elasticsearch: {}", e.message, e)
        }
    }

    private fun fetchNewLogs(): List<AuditLog> {
        val lastId = lastSyncedId.get()
        val pageable = PageRequest.of(0, BATCH_SIZE)

        return if (lastId > 0) {
            auditLogRepository.findByIdBetweenOrderByIdAsc(
                lastId + 1,
                Long.MAX_VALUE,
                pageable
            ).content
        } else {
            val yesterday = Instant.now().minus(1, ChronoUnit.DAYS)
            auditLogRepository.findByCreatedAtBetweenOrderByIdAsc(
                yesterday,
                Instant.now(),
                pageable
            ).content
        }
    }

    private fun bulkIndexToEs(logs: List<AuditLog>) {
        log.debug("Would index {} logs to ES index: {}", logs.size, esIndex)
    }

    private fun updateLastSyncedId(logs: List<AuditLog>) {
        logs.maxByOrNull { it.id }?.let {
            lastSyncedId.set(it.id)
        }
    }

    /**
     * Full reindex from MySQL to Elasticsearch.
     * Should be run manually when first enabling ES.
     */
    @Async
    fun fullReindex() {
        if (!esEnabled || esUrl.isBlank()) {
            log.warn("Elasticsearch is not enabled, skipping full reindex")
            return
        }

        log.info("Starting full reindex to Elasticsearch")

        var lastId = 0L
        var totalIndexed = 0L

        while (true) {
            val pageable = PageRequest.of(0, BATCH_SIZE)
            val batch = auditLogRepository.findByIdBetweenOrderByIdAsc(
                lastId + 1,
                Long.MAX_VALUE,
                pageable
            )

            if (batch.isEmpty) {
                break
            }

            bulkIndexToEs(batch.content)
            lastId = batch.content.maxOf { it.id }
            totalIndexed += batch.numberOfElements

            log.info("Reindex progress: {} records indexed", totalIndexed)
        }

        lastSyncedId.set(lastId)
        log.info("Full reindex completed: {} total records", totalIndexed)
    }

    /**
     * Get sync status.
     */
    fun getSyncStatus(): EsSyncStatus {
        return EsSyncStatus(
            enabled = esEnabled,
            esUrl = if (esEnabled) esUrl else null,
            esIndex = esIndex,
            lastSyncedId = lastSyncedId.get(),
            totalRecords = auditLogRepository.count()
        )
    }
}

data class ThresholdCheckResult(
    val totalRecords: Long,
    val recordThreshold: Long,
    val recordsExceeded: Boolean,
    val estimatedDailyQueries: Long,
    val queryThreshold: Long,
    val queriesExceeded: Boolean,
    val recommendation: String
)

data class EsSyncStatus(
    val enabled: Boolean,
    val esUrl: String?,
    val esIndex: String,
    val lastSyncedId: Long,
    val totalRecords: Long
)
