package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.AuditLog
import edu.minervia.platform.domain.enums.ActorType
import edu.minervia.platform.domain.enums.AuditResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    fun findAllByActorTypeAndActorId(actorType: ActorType, actorId: Long, pageable: Pageable): Page<AuditLog>
    fun findAllByTargetTypeAndTargetId(targetType: String, targetId: Long, pageable: Pageable): Page<AuditLog>
    fun findAllByEventType(eventType: String, pageable: Pageable): Page<AuditLog>
    fun findAllByCreatedAtBetween(start: Instant, end: Instant, pageable: Pageable): Page<AuditLog>
    fun findAllByAction(action: String, pageable: Pageable): Page<AuditLog>

    fun countByCreatedAtBetween(start: Instant, end: Instant): Long
    fun countByResultAndCreatedAtBetween(result: AuditResult, start: Instant, end: Instant): Long

    fun findByIdBetweenOrderByIdAsc(startId: Long, endId: Long, pageable: Pageable): Slice<AuditLog>
    fun findByCreatedAtBetweenOrderByIdAsc(start: Instant, end: Instant, pageable: Pageable): Slice<AuditLog>

    @Query(
        """
        select a.eventType as eventType, count(a) as count
        from AuditLog a
        where a.createdAt between :start and :end
        group by a.eventType
        """
    )
    fun findEventTypeCounts(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<EventTypeCount>

    @Query(
        """
        select a.actorType as actorType, count(a) as count
        from AuditLog a
        where a.createdAt between :start and :end
        group by a.actorType
        """
    )
    fun findActorTypeCounts(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<ActorTypeCount>

    @Query(
        """
        select a.actorId as actorId, a.actorUsername as actorUsername, count(a) as banCount
        from AuditLog a
        where a.eventType = :eventType
          and a.createdAt >= :windowStart
        group by a.actorId, a.actorUsername
        having count(a) > :threshold
        """
    )
    fun findBulkBanActors(
        @Param("eventType") eventType: String,
        @Param("windowStart") windowStart: Instant,
        @Param("threshold") threshold: Long
    ): List<BulkBanActorCount>

    @Query(
        """
        select a.actorId as actorId, a.actorUsername as actorUsername,
               count(distinct a.ipAddress) as uniqueIpCount
        from AuditLog a
        where a.eventType = :eventType
          and a.createdAt >= :windowStart
          and a.ipAddress is not null
        group by a.actorId, a.actorUsername
        having count(distinct a.ipAddress) >= :threshold
        """
    )
    fun findAnomalousLoginActors(
        @Param("eventType") eventType: String,
        @Param("windowStart") windowStart: Instant,
        @Param("threshold") threshold: Long
    ): List<AnomalousLoginActorCount>

    fun findAllByEventTypeAndCreatedAtGreaterThanEqualAndActorId(
        eventType: String,
        createdAt: Instant,
        actorId: Long?
    ): List<AuditLog>
}

interface EventTypeCount {
    val eventType: String
    val count: Long
}

interface ActorTypeCount {
    val actorType: ActorType
    val count: Long
}

interface BulkBanActorCount {
    val actorId: Long?
    val actorUsername: String?
    val banCount: Long
}

interface AnomalousLoginActorCount {
    val actorId: Long?
    val actorUsername: String?
    val uniqueIpCount: Long
}
