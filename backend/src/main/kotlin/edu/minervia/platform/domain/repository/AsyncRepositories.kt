package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.Outbox
import edu.minervia.platform.domain.entity.OutboxDeadLetter
import edu.minervia.platform.domain.entity.TaskProgress
import edu.minervia.platform.domain.entity.RateLimit
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface OutboxRepository : JpaRepository<Outbox, Long> {
    @Query("SELECT o FROM Outbox o WHERE o.processedAt IS NULL ORDER BY o.createdAt ASC")
    fun findUnprocessed(pageable: Pageable): List<Outbox>

    @Modifying
    @Query("DELETE FROM Outbox o WHERE o.processedAt IS NOT NULL AND o.processedAt < :before")
    fun deleteProcessedBefore(before: Instant): Int
}

@Repository
interface OutboxDeadLetterRepository : JpaRepository<OutboxDeadLetter, Long> {
    fun findByAggregateTypeAndAggregateId(aggregateType: String, aggregateId: String): List<OutboxDeadLetter>
}

@Repository
interface TaskProgressRepository : JpaRepository<TaskProgress, Long> {
    fun findByApplicationId(applicationId: Long): TaskProgress?

    @Modifying
    @Query("UPDATE TaskProgress t SET t.step = :step, t.status = :status, t.progressPercent = :progress, t.message = :message, t.updatedAt = :now WHERE t.applicationId = :appId AND t.version < :newVersion")
    fun updateIfNewer(
        appId: Long,
        step: String,
        status: String,
        progress: Int,
        message: String?,
        newVersion: Long,
        now: Instant
    ): Int

    @Query("SELECT t FROM TaskProgress t WHERE t.status IN :statuses AND t.updatedAt < :cutoff")
    fun findStuckTasks(statuses: List<String>, cutoff: Instant): List<TaskProgress>

    @Modifying
    @Query("DELETE FROM TaskProgress t WHERE t.applicationId = :applicationId")
    fun deleteByApplicationId(applicationId: Long): Int
}

@Repository
interface RateLimitRepository : JpaRepository<RateLimit, Long> {
    fun findByLimitKeyAndWindowStart(limitKey: String, windowStart: Instant): RateLimit?

    @Modifying
    @Query("DELETE FROM RateLimit r WHERE r.windowStart < :cutoff")
    fun deleteExpiredBefore(cutoff: Instant): Int
}
