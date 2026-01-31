package edu.minervia.platform.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import edu.minervia.platform.domain.entity.RegistrationApplication
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for publishing registration tasks using the transactional outbox pattern.
 * Per CONSTRAINT [ASYNC-OUTBOX]:
 * - Approval operation and outbox insert happen in same transaction
 * - Independent poller publishes to Kafka
 * - Kafka unavailability doesn't block approval
 */
@Service
class RegistrationTaskPublisher(
    private val outboxService: OutboxService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Publish a registration task after approval.
     * Must be called within the same transaction as the approval operation.
     */
    @Transactional
    fun publishRegistrationTask(application: RegistrationApplication) {
        val taskMessage = RegistrationTaskMessage(
            applicationId = application.id,
            retryCount = 0,
            maxRetries = 3
        )

        outboxService.insertOutboxEntry(
            aggregateType = OutboxService.AGGREGATE_APPLICATION,
            aggregateId = application.id.toString(),
            eventType = OutboxService.EVENT_REGISTRATION_TASK,
            payload = taskMessage
        )

        log.info("Published registration task to outbox for application: {}", application.id)
    }

    /**
     * Publish a retry task for failed generation.
     */
    @Transactional
    fun publishRetryTask(applicationId: Long, currentRetryCount: Int) {
        val taskMessage = RegistrationTaskMessage(
            applicationId = applicationId,
            retryCount = currentRetryCount + 1,
            maxRetries = 3
        )

        outboxService.insertOutboxEntry(
            aggregateType = OutboxService.AGGREGATE_APPLICATION,
            aggregateId = applicationId.toString(),
            eventType = OutboxService.EVENT_REGISTRATION_TASK,
            payload = taskMessage
        )

        log.info("Published retry task to outbox for application: {} (retry {})",
            applicationId, currentRetryCount + 1)
    }
}
