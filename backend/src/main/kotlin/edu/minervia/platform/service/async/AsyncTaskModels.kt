package edu.minervia.platform.service.async

import java.time.Instant
import java.util.UUID

/**
 * Task status enum following design spec.
 */
enum class TaskStatus {
    PENDING,
    GENERATING_IDENTITY,
    GENERATING_PHOTOS,
    COMPLETED,
    FAILED
}

/**
 * Task step for individual transaction boundaries
 * per CONSTRAINT [AI-STEP-TRANSACTION]
 */
enum class TaskStep {
    IDENTITY_RULES,
    IDENTITY_LLM,
    PHOTO_GENERATION
}

/**
 * Registration task message sent to Kafka.
 * Uses applicationId as partition key per CONSTRAINT [KAFKA-PARTITION-KEY].
 */
data class RegistrationTaskMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val applicationId: Long,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val createdAt: Instant = Instant.now(),
    val lastAttemptAt: Instant? = null
)

/**
 * Progress event message for WebSocket/polling updates.
 */
data class ProgressEventMessage(
    val applicationId: Long,
    val taskId: Long,
    val step: TaskStep,
    val status: TaskStatus,
    val progressPercent: Int,
    val message: String?,
    val version: Long,
    val timestamp: Instant = Instant.now()
)

/**
 * Outbox entry for transactional outbox pattern
 * per CONSTRAINT [ASYNC-OUTBOX].
 */
data class OutboxEntry(
    val id: Long = 0,
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val payload: String,
    val retryCount: Int = 0,
    val createdAt: Instant = Instant.now(),
    val processedAt: Instant? = null
)
