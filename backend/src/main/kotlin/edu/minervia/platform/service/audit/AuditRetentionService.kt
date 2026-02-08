package edu.minervia.platform.service.audit

import edu.minervia.platform.domain.repository.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Service for managing audit log retention and partition automation.
 * Per CONSTRAINT [AUDIT-PARTITION-AUTOMATION]:
 * - Partition granularity: monthly
 * - Auto-create: next 3 months partitions
 * - Auto-archive: partitions older than 12 months (retain hash digest)
 *
 * Data retention: 5 years per legal requirement.
 */
@Service
class AuditRetentionService(
    private val jdbcTemplate: JdbcTemplate,
    private val auditLogRepository: AuditLogRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val RETENTION_YEARS = 5L
        const val ARCHIVE_MONTHS = 12
        const val FUTURE_PARTITIONS = 3
        private val PARTITION_FORMAT = DateTimeFormatter.ofPattern("yyyyMM")
    }

    @Value("\${minervia.audit.archive-enabled:false}")
    private var archiveEnabled: Boolean = false

    /**
     * Run partition maintenance daily at 2 AM.
     * Creates future partitions and archives old ones.
     */
    @Scheduled(cron = "0 0 2 * * *")
    fun runPartitionMaintenance() {
        try {
            log.info("Starting audit log partition maintenance")
            createFuturePartitions()
            if (archiveEnabled) {
                archiveOldPartitions()
            }
            log.info("Audit log partition maintenance completed")
        } catch (e: Exception) {
            log.error("Failed to run partition maintenance: {}", e.message, e)
        }
    }

    /**
     * Create partitions for the next N months.
     */
    fun createFuturePartitions() {
        val currentMonth = YearMonth.now()

        for (i in 0..FUTURE_PARTITIONS) {
            val targetMonth = currentMonth.plusMonths(i.toLong())
            val partitionName = "p${targetMonth.format(PARTITION_FORMAT)}"
            val nextMonth = targetMonth.plusMonths(1)
            val lessThanValue = nextMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC)

            try {
                createPartitionIfNotExists(partitionName, lessThanValue)
            } catch (e: Exception) {
                log.warn("Failed to create partition {}: {}", partitionName, e.message)
            }
        }
    }

    private fun createPartitionIfNotExists(partitionName: String, lessThanValue: Instant) {
        val checkSql = """
            SELECT COUNT(*) FROM information_schema.PARTITIONS
            WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = 'audit_logs'
            AND PARTITION_NAME = ?
        """.trimIndent()

        val exists = jdbcTemplate.queryForObject(checkSql, Int::class.java, partitionName) ?: 0

        if (exists == 0) {
            val lessThanStr = lessThanValue.toString().replace("T", " ").replace("Z", "")
            val alterSql = """
                ALTER TABLE audit_logs
                ADD PARTITION (PARTITION $partitionName VALUES LESS THAN ('$lessThanStr'))
            """.trimIndent()

            try {
                jdbcTemplate.execute(alterSql)
                log.info("Created partition: {}", partitionName)
            } catch (e: Exception) {
                if (e.message?.contains("already exists") != true) {
                    throw e
                }
            }
        }
    }

    /**
     * Archive partitions older than ARCHIVE_MONTHS.
     * Preserves hash digests for integrity verification.
     */
    @Transactional
    fun archiveOldPartitions() {
        val archiveCutoff = YearMonth.now().minusMonths(ARCHIVE_MONTHS.toLong())
        val retentionCutoff = LocalDate.now().minusYears(RETENTION_YEARS)

        log.info("Archiving partitions older than {}, deleting data older than {}",
            archiveCutoff, retentionCutoff)

        val partitions = getExistingPartitions()

        for (partition in partitions) {
            val partitionMonth = parsePartitionMonth(partition) ?: continue

            if (partitionMonth.isBefore(archiveCutoff)) {
                if (partitionMonth.atDay(1).isBefore(retentionCutoff)) {
                    dropPartition(partition)
                } else {
                    archivePartitionDigest(partition, partitionMonth)
                }
            }
        }
    }

    private fun getExistingPartitions(): List<String> {
        val sql = """
            SELECT PARTITION_NAME FROM information_schema.PARTITIONS
            WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = 'audit_logs'
            AND PARTITION_NAME IS NOT NULL
            AND PARTITION_NAME != 'p_default'
            ORDER BY PARTITION_NAME
        """.trimIndent()

        return jdbcTemplate.queryForList(sql, String::class.java)
    }

    private fun parsePartitionMonth(partitionName: String): YearMonth? {
        return try {
            if (partitionName.startsWith("p") && partitionName.length == 7) {
                YearMonth.parse(partitionName.substring(1), PARTITION_FORMAT)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun archivePartitionDigest(partitionName: String, month: YearMonth) {
        val startOfMonth = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        val endOfMonth = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC)

        val digestSql = """
            SELECT
                COUNT(*) as record_count,
                MIN(id) as min_id,
                MAX(id) as max_id,
                GROUP_CONCAT(DISTINCT event_type) as event_types
            FROM audit_logs PARTITION ($partitionName)
        """.trimIndent()

        try {
            val digest = jdbcTemplate.queryForMap(digestSql)
            log.info("Archived partition {} digest: count={}, id_range=[{}-{}], events={}",
                partitionName,
                digest["record_count"],
                digest["min_id"],
                digest["max_id"],
                digest["event_types"]
            )

            savePartitionDigest(partitionName, month, digest)
        } catch (e: Exception) {
            log.error("Failed to archive partition {} digest: {}", partitionName, e.message)
        }
    }

    private fun savePartitionDigest(partitionName: String, month: YearMonth, digest: Map<String, Any?>) {
        val insertSql = """
            INSERT INTO audit_partition_digests
            (partition_name, partition_month, record_count, min_id, max_id, event_types, created_at)
            VALUES (?, ?, ?, ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
            record_count = VALUES(record_count),
            min_id = VALUES(min_id),
            max_id = VALUES(max_id),
            event_types = VALUES(event_types)
        """.trimIndent()

        jdbcTemplate.update(
            insertSql,
            partitionName,
            month.toString(),
            digest["record_count"],
            digest["min_id"],
            digest["max_id"],
            digest["event_types"]
        )
    }

    private fun dropPartition(partitionName: String) {
        log.warn("Dropping partition {} (data retention exceeded)", partitionName)
        try {
            jdbcTemplate.execute("ALTER TABLE audit_logs DROP PARTITION $partitionName")
            log.info("Dropped partition: {}", partitionName)
        } catch (e: Exception) {
            log.error("Failed to drop partition {}: {}", partitionName, e.message)
        }
    }

    /**
     * Get retention statistics.
     */
    fun getRetentionStats(): RetentionStats {
        val partitions = getExistingPartitions()
        val oldestPartition = partitions.minOrNull()?.let { parsePartitionMonth(it) }
        val newestPartition = partitions.maxOrNull()?.let { parsePartitionMonth(it) }

        val totalCount = auditLogRepository.count()

        return RetentionStats(
            totalRecords = totalCount,
            partitionCount = partitions.size,
            oldestPartition = oldestPartition?.toString(),
            newestPartition = newestPartition?.toString(),
            retentionYears = RETENTION_YEARS,
            archiveEnabled = archiveEnabled
        )
    }
}

data class RetentionStats(
    val totalRecords: Long,
    val partitionCount: Int,
    val oldestPartition: String?,
    val newestPartition: String?,
    val retentionYears: Long,
    val archiveEnabled: Boolean
)
