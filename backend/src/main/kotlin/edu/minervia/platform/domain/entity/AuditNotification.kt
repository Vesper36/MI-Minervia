package edu.minervia.platform.domain.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "audit_notifications")
class AuditNotification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "alert_type", nullable = false, length = 50)
    val alertType: String,

    @Column(nullable = false, length = 20)
    val severity: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,

    @Column(nullable = false, columnDefinition = "JSON")
    val recipients: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false)
    var sendStatus: NotificationStatus = NotificationStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "sent_at")
    var sentAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

enum class NotificationStatus {
    PENDING, SENT, FAILED
}
