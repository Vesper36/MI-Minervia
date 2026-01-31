package edu.minervia.platform.service.async

import edu.minervia.platform.domain.entity.TaskProgress
import edu.minervia.platform.domain.repository.TaskProgressRepository
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Progress service for WebSocket updates and polling fallback.
 * Per CONSTRAINT [PROGRESS-PERSISTENCE] and [POLLING-FALLBACK].
 *
 * - Each WebSocket event is also persisted to DB
 * - Version/timestamp check prevents stale updates
 * - Supports both WS push and REST polling
 */
@Service
class TaskProgressService(
    private val taskProgressRepository: TaskProgressRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Update progress and broadcast via WebSocket.
     * Writes to DB synchronously per CONSTRAINT [PROGRESS-PERSISTENCE].
     */
    @Transactional
    fun updateProgress(
        applicationId: Long,
        step: TaskStep,
        status: TaskStatus,
        progressPercent: Int,
        message: String? = null,
        retryCount: Int = 0
    ) {
        val now = Instant.now()
        val existing = taskProgressRepository.findByApplicationId(applicationId)

        val newVersion = (existing?.version ?: 0) + 1

        if (existing != null) {
            existing.step = step.name
            existing.status = status.name
            existing.progressPercent = progressPercent
            existing.message = message
            existing.retryCount = retryCount
            existing.version = newVersion
            existing.updatedAt = now
            taskProgressRepository.save(existing)
        } else {
            val progress = TaskProgress(
                applicationId = applicationId,
                step = step.name,
                status = status.name,
                progressPercent = progressPercent,
                message = message,
                retryCount = retryCount,
                version = newVersion,
                updatedAt = now
            )
            taskProgressRepository.save(progress)
        }

        // Broadcast to WebSocket subscribers
        val event = ProgressEventMessage(
            applicationId = applicationId,
            taskId = applicationId, // Using applicationId as taskId for simplicity
            step = step,
            status = status,
            progressPercent = progressPercent,
            message = message,
            version = newVersion,
            timestamp = now
        )

        broadcastProgress(applicationId, event)
    }

    /**
     * Get current progress for polling fallback.
     * Per CONSTRAINT [POLLING-FALLBACK].
     */
    fun getProgress(applicationId: Long): ProgressEventMessage? {
        val progress = taskProgressRepository.findByApplicationId(applicationId)
            ?: return null

        return ProgressEventMessage(
            applicationId = progress.applicationId,
            taskId = progress.applicationId,
            step = TaskStep.valueOf(progress.step),
            status = TaskStatus.valueOf(progress.status),
            progressPercent = progress.progressPercent,
            message = progress.message,
            version = progress.version,
            timestamp = progress.updatedAt
        )
    }

    private fun broadcastProgress(applicationId: Long, event: ProgressEventMessage) {
        val destination = "/topic/applications/$applicationId/progress"
        try {
            messagingTemplate.convertAndSend(destination, event)
            log.debug("Broadcast progress to {}: {}% {}", destination, event.progressPercent, event.status)
        } catch (e: Exception) {
            log.warn("Failed to broadcast progress: {}", e.message)
            // Per CONSTRAINT [STOMP-SIMPLE-BROKER]: No persistence, rely on polling fallback
        }
    }
}
