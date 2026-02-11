package edu.minervia.platform.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_deliveries")
data class EmailDelivery(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "dedupe_key", nullable = false, unique = true, length = 128)
    val dedupeKey: String,

    @Column(name = "recipient_email", nullable = false)
    val recipientEmail: String,

    @Column(name = "template", nullable = false, length = 50)
    val template: String,

    @Column(name = "locale", nullable = false, length = 16)
    val locale: String = "en",

    @Column(name = "params_json", nullable = false, columnDefinition = "TEXT")
    val paramsJson: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: EmailDeliveryStatus = EmailDeliveryStatus.PENDING,

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,

    @Column(name = "next_attempt_at")
    var nextAttemptAt: LocalDateTime? = null,

    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,

    @Column(name = "provider_message_id", length = 128)
    var providerMessageId: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class EmailDeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    SUPPRESSED
}
