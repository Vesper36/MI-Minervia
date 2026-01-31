package edu.minervia.platform.domain.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "outbox")
class Outbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "aggregate_type", nullable = false, length = 100)
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false, length = 100)
    val aggregateId: String,

    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String,

    @Column(nullable = false, columnDefinition = "JSON")
    val payload: String,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "processed_at")
    var processedAt: Instant? = null
)

@Entity
@Table(name = "outbox_dead_letter")
class OutboxDeadLetter(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "original_id", nullable = false)
    val originalId: Long,

    @Column(name = "aggregate_type", nullable = false, length = 100)
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false, length = 100)
    val aggregateId: String,

    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String,

    @Column(nullable = false, columnDefinition = "JSON")
    val payload: String,

    @Column(name = "retry_count", nullable = false)
    val retryCount: Int,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "moved_at", nullable = false, updatable = false)
    val movedAt: Instant = Instant.now()
)

@Entity
@Table(name = "task_progress")
class TaskProgress(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "application_id", nullable = false, unique = true)
    val applicationId: Long,

    @Column(nullable = false, length = 50)
    var step: String,

    @Column(nullable = false, length = 50)
    var status: String,

    @Column(name = "progress_percent", nullable = false)
    var progressPercent: Int = 0,

    @Column(length = 500)
    var message: String? = null,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Version
    @Column(nullable = false)
    var version: Long = 1,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "rate_limits")
class RateLimit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "limit_key", nullable = false, length = 255)
    val limitKey: String,

    @Column(nullable = false)
    var count: Int = 0,

    @Column(name = "window_start", nullable = false)
    val windowStart: Instant,

    @Column(name = "window_seconds", nullable = false)
    val windowSeconds: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
