package edu.minervia.platform.domain.entity

import edu.minervia.platform.domain.enums.EmailSuppressionReason
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "email_suppression",
    indexes = [Index(name = "idx_email_suppression_email", columnList = "email")]
)
class EmailSuppression(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "reason")
    var reason: EmailSuppressionReason? = null,

    @Column(name = "bounce_count", nullable = false)
    var bounceCount: Int = 0,

    @Column(name = "first_bounce_at")
    var firstBounceAt: Instant? = null,

    @Column(name = "last_bounce_at")
    var lastBounceAt: Instant? = null,

    @Column(name = "suppressed_at")
    var suppressedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    fun isSuppressed(): Boolean = suppressedAt != null
}
