package edu.minervia.platform.domain.entity

import edu.minervia.platform.domain.enums.ActorType
import edu.minervia.platform.domain.enums.AuditResult
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "audit_logs")
class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    val actorType: ActorType,

    @Column(name = "actor_id")
    val actorId: Long? = null,

    @Column(name = "actor_username", length = 100)
    val actorUsername: String? = null,

    @Column(name = "target_type", length = 50)
    val targetType: String? = null,

    @Column(name = "target_id")
    val targetId: Long? = null,

    @Column(nullable = false, length = 100)
    val action: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val result: AuditResult,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(name = "old_value", columnDefinition = "JSON")
    val oldValue: String? = null,

    @Column(name = "new_value", columnDefinition = "JSON")
    val newValue: String? = null,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    val userAgent: String? = null,

    @Column(name = "session_id", length = 100)
    val sessionId: String? = null,

    @Column(name = "hash_value", length = 64)
    val hashValue: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
