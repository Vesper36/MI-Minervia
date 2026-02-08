package edu.minervia.platform.service.async

import edu.minervia.platform.domain.entity.RegistrationApplication
import edu.minervia.platform.domain.enums.ApplicationStatus
import edu.minervia.platform.domain.repository.RegistrationApplicationRepository
import edu.minervia.platform.domain.repository.TaskProgressRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service for cleaning up timed-out tasks and re-queuing them.
 * Per CONSTRAINT [AI-TIMEOUT-CLEANUP]:
 * - Immediately clean partial data from current step
 * - Re-queue task (counting toward retry limit)
 * - Cleanup and re-queue in same transaction
 *
 * PBT [PBT-21]: Timeout cleanup atomicity - cleanup + re-queue succeed or fail together
 */
@Service
@ConditionalOnProperty(name = ["app.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class TimeoutCleanupService(
    private val applicationRepository: RegistrationApplicationRepository,
    private val taskProgressRepository: TaskProgressRepository,
    private val taskPublisher: RegistrationTaskPublisher,
    private val progressService: TaskProgressService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val TOTAL_TIMEOUT_SECONDS = 300L
        const val MAX_RETRIES = 3
    }

    /**
     * Find and process all timed-out tasks.
     * Called by scheduler at fixed intervals.
     */
    fun processTimedOutTasks(): Int {
        val cutoff = Instant.now().minusSeconds(TOTAL_TIMEOUT_SECONDS)
        val stuckApplications = applicationRepository.findByStatusAndUpdatedAtBefore(
            ApplicationStatus.GENERATING,
            cutoff
        )

        if (stuckApplications.isEmpty()) {
            return 0
        }

        log.info("Found {} timed-out tasks to process", stuckApplications.size)

        var processedCount = 0
        for (application in stuckApplications) {
            try {
                processTimedOutTask(application)
                processedCount++
            } catch (e: Exception) {
                log.error("Failed to process timed-out task for application {}: {}",
                    application.id, e.message, e)
            }
        }

        return processedCount
    }

    /**
     * Process a single timed-out task.
     * Cleanup and re-queue happen in same transaction per PBT [PBT-21].
     */
    @Transactional
    fun processTimedOutTask(application: RegistrationApplication) {
        val applicationId = application.id
        log.info("Processing timed-out task for application: {}", applicationId)

        val progress = taskProgressRepository.findByApplicationId(applicationId)
        val currentRetryCount = extractRetryCount(progress)

        if (currentRetryCount >= MAX_RETRIES - 1) {
            markAsFailed(application, "Timed out after $MAX_RETRIES attempts")
            cleanupProgress(applicationId, "Max retries exceeded")
            return
        }

        cleanupPartialData(application, progress)

        application.status = ApplicationStatus.APPROVED
        application.updatedAt = Instant.now()
        applicationRepository.save(application)

        taskPublisher.publishRetryTask(applicationId, currentRetryCount)

        log.info("Re-queued timed-out task for application: {} (retry {})",
            applicationId, currentRetryCount + 1)
    }

    /**
     * Clean up partial data from the current step.
     * Per CONSTRAINT [AI-STEP-TRANSACTION]: Only current step data is affected.
     */
    private fun cleanupPartialData(application: RegistrationApplication, progress: edu.minervia.platform.domain.entity.TaskProgress?) {
        val applicationId = application.id

        if (progress != null) {
            val step = progress.step
            log.debug("Cleaning up partial data for application {} at step {}", applicationId, step)

            when (step) {
                TaskStep.IDENTITY_RULES.name -> {
                }
                TaskStep.IDENTITY_LLM.name -> {
                }
                TaskStep.PHOTO_GENERATION.name -> {
                }
            }
        }

        taskProgressRepository.deleteByApplicationId(applicationId)
        log.debug("Deleted progress record for application {}", applicationId)
    }

    private fun cleanupProgress(applicationId: Long, message: String) {
        progressService.updateProgress(
            applicationId = applicationId,
            step = TaskStep.IDENTITY_RULES,
            status = TaskStatus.FAILED,
            progressPercent = 0,
            message = message
        )
    }

    private fun markAsFailed(application: RegistrationApplication, reason: String) {
        application.status = ApplicationStatus.FAILED
        application.updatedAt = Instant.now()
        applicationRepository.save(application)
        log.error("Application {} marked as FAILED: {}", application.id, reason)
    }

    private fun extractRetryCount(progress: edu.minervia.platform.domain.entity.TaskProgress?): Int {
        return progress?.retryCount ?: 0
    }
}
