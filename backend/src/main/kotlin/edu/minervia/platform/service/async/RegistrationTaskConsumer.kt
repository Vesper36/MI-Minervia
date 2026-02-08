package edu.minervia.platform.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import edu.minervia.platform.config.KafkaConfig
import edu.minervia.platform.domain.entity.RegistrationApplication
import edu.minervia.platform.domain.enums.ApplicationStatus
import edu.minervia.platform.domain.repository.RegistrationApplicationRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Kafka consumer for registration tasks.
 * Implements CONSTRAINT [ASYNC-IDEMPOTENCY] using applicationId as idempotent key.
 * Implements CONSTRAINT [AI-STEP-TRANSACTION] with independent transactions per step.
 */
@Service
@ConditionalOnProperty(name = ["app.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class RegistrationTaskConsumer(
    private val applicationRepository: RegistrationApplicationRepository,
    private val stepExecutor: RegistrationStepExecutor,
    private val progressService: TaskProgressService,
    private val taskPublisher: RegistrationTaskPublisher,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MAX_RETRIES = 3
        const val LLM_TIMEOUT_MS = 60_000L
        const val PHOTO_TIMEOUT_MS = 180_000L
        const val TOTAL_TIMEOUT_MS = 300_000L
        const val MIN_BACKOFF_MS = 1000L
        const val MAX_BACKOFF_MS = 8000L
    }

    @KafkaListener(
        topics = [KafkaConfig.TOPIC_REGISTRATION_TASKS],
        groupId = "\${spring.kafka.consumer.group-id}"
    )
    fun consumeRegistrationTask(record: ConsumerRecord<String, String>, ack: Acknowledgment) {
        val startTime = System.currentTimeMillis()

        // Safe key parsing inside try block
        val applicationId: Long
        try {
            val key = record.key()
            if (key.isNullOrBlank()) {
                log.error("Invalid record key: null or blank")
                ack.acknowledge()
                return
            }
            applicationId = key.toLongOrNull() ?: run {
                log.error("Invalid record key format: {}", key)
                ack.acknowledge()
                return
            }
        } catch (e: Exception) {
            log.error("Failed to parse record key: {}", e.message)
            ack.acknowledge()
            return
        }

        log.info("Processing registration task for application: {}", applicationId)

        try {
            // Idempotency check: skip if already completed
            val application = applicationRepository.findById(applicationId).orElse(null)
            if (application == null) {
                log.warn("Application {} not found, skipping", applicationId)
                ack.acknowledge()
                return
            }

            if (application.status == ApplicationStatus.COMPLETED) {
                log.info("Application {} already completed, skipping", applicationId)
                ack.acknowledge()
                return
            }

            if (application.status == ApplicationStatus.REJECTED) {
                log.info("Application {} rejected, skipping", applicationId)
                ack.acknowledge()
                return
            }

            // Parse task message
            val taskMessage = objectMapper.readValue(record.value(), RegistrationTaskMessage::class.java)

            // Check retry count
            if (taskMessage.retryCount >= MAX_RETRIES) {
                log.error("Application {} exceeded max retries ({})", applicationId, taskMessage.retryCount)
                markAsFailed(application, "Max retries exceeded after ${taskMessage.retryCount} attempts")
                ack.acknowledge()
                return
            }

            // Execute steps with independent transactions (via stepExecutor)
            executeIdentityGeneration(application, startTime, taskMessage.retryCount)

            ack.acknowledge()
            log.info("Completed registration task for application: {}", applicationId)

        } catch (e: Exception) {
            log.error("Failed to process registration task for application {}: {}", applicationId, e.message, e)
            handleTaskFailure(applicationId, e)
            ack.acknowledge()
        }
    }

    /**
     * Execute identity generation with step-by-step transactions.
     * Per CONSTRAINT [AI-STEP-TRANSACTION].
     * Uses RegistrationStepExecutor to ensure @Transactional works correctly.
     */
    private fun executeIdentityGeneration(application: RegistrationApplication, startTime: Long, currentRetryCount: Int) {
        val applicationId = application.id

        try {
            // Step 1: Generate identity using rules
            progressService.updateProgress(
                applicationId = applicationId,
                step = TaskStep.IDENTITY_RULES,
                status = TaskStatus.GENERATING_IDENTITY,
                progressPercent = 10,
                message = "Generating identity information...",
                retryCount = currentRetryCount
            )

            checkTimeout(startTime, TOTAL_TIMEOUT_MS, "Identity rules generation")
            stepExecutor.executeIdentityRulesStep(application)

            progressService.updateProgress(
                applicationId = applicationId,
                step = TaskStep.IDENTITY_RULES,
                status = TaskStatus.GENERATING_IDENTITY,
                progressPercent = 30,
                message = "Basic identity generated",
                retryCount = currentRetryCount
            )

            // Step 2: LLM polish
            progressService.updateProgress(
                applicationId = applicationId,
                step = TaskStep.IDENTITY_LLM,
                status = TaskStatus.GENERATING_IDENTITY,
                progressPercent = 40,
                message = "Enhancing profile with AI...",
                retryCount = currentRetryCount
            )

            checkTimeout(startTime, TOTAL_TIMEOUT_MS, "LLM polish")
            stepExecutor.executeLlmPolishStep(application)

            progressService.updateProgress(
                applicationId = applicationId,
                step = TaskStep.IDENTITY_LLM,
                status = TaskStatus.GENERATING_IDENTITY,
                progressPercent = 70,
                message = "Profile enhanced",
                retryCount = currentRetryCount
            )

            // Step 3: Photo generation
            progressService.updateProgress(
                applicationId = applicationId,
                step = TaskStep.PHOTO_GENERATION,
                status = TaskStatus.GENERATING_PHOTOS,
                progressPercent = 80,
                message = "Generating photos...",
                retryCount = currentRetryCount
            )

            checkTimeout(startTime, TOTAL_TIMEOUT_MS, "Photo generation")
            stepExecutor.executePhotoGenerationStep(application)

            // Mark as completed
            markAsCompleted(application)

            progressService.updateProgress(
                applicationId = applicationId,
                step = TaskStep.PHOTO_GENERATION,
                status = TaskStatus.COMPLETED,
                progressPercent = 100,
                message = "Registration completed",
                retryCount = currentRetryCount
            )

        } catch (e: TimeoutException) {
            log.error("Task timed out for application {}: {}", applicationId, e.message)
            // Requeue with incremented retry count
            if (currentRetryCount < MAX_RETRIES - 1) {
                taskPublisher.publishRetryTask(applicationId, currentRetryCount)
                log.info("Requeued application {} for retry (attempt {})", applicationId, currentRetryCount + 1)
            } else {
                markAsFailed(application, "Timed out after ${MAX_RETRIES} attempts")
            }
            throw e
        }
    }

    private fun checkTimeout(startTime: Long, limitMs: Long, operation: String) {
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed > limitMs) {
            throw TimeoutException("$operation timed out after ${elapsed}ms (limit: ${limitMs}ms)")
        }
    }

    @Transactional
    fun markAsFailed(application: RegistrationApplication, reason: String) {
        application.status = ApplicationStatus.FAILED
        applicationRepository.save(application)
        log.error("Application {} marked as FAILED: {}", application.id, reason)
    }

    @Transactional
    fun markAsCompleted(application: RegistrationApplication) {
        application.status = ApplicationStatus.COMPLETED
        applicationRepository.save(application)
        log.info("Application {} marked as COMPLETED", application.id)
    }

    private fun handleTaskFailure(applicationId: Long, exception: Exception) {
        try {
            val application = applicationRepository.findById(applicationId).orElse(null)
            if (application != null && application.status != ApplicationStatus.COMPLETED) {
                progressService.updateProgress(
                    applicationId = applicationId,
                    step = TaskStep.IDENTITY_RULES,
                    status = TaskStatus.FAILED,
                    progressPercent = 0,
                    message = "Generation failed: ${exception.message}"
                )
                markAsFailed(application, exception.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            log.error("Failed to handle task failure for application {}: {}", applicationId, e.message)
        }
    }
}

class TimeoutException(message: String) : RuntimeException(message)
