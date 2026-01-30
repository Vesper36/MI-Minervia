package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.AuditLog
import edu.minervia.platform.domain.enums.ActorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    fun findAllByActorTypeAndActorId(actorType: ActorType, actorId: Long, pageable: Pageable): Page<AuditLog>
    fun findAllByTargetTypeAndTargetId(targetType: String, targetId: Long, pageable: Pageable): Page<AuditLog>
    fun findAllByEventType(eventType: String, pageable: Pageable): Page<AuditLog>
    fun findAllByCreatedAtBetween(start: Instant, end: Instant, pageable: Pageable): Page<AuditLog>
    fun findAllByAction(action: String, pageable: Pageable): Page<AuditLog>
}
