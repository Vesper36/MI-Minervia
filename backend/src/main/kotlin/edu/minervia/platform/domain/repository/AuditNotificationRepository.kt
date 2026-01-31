package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.AuditNotification
import edu.minervia.platform.domain.entity.NotificationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface AuditNotificationRepository : JpaRepository<AuditNotification, Long> {
    fun findAllBySendStatus(status: NotificationStatus): List<AuditNotification>
    fun findAllByAlertTypeAndCreatedAtAfter(alertType: String, after: Instant): List<AuditNotification>
}
