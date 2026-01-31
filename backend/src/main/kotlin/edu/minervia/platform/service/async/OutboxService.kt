package edu.minervia.platform.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import edu.minervia.platform.config.KafkaConfig
import edu.minervia.platform.domain.entity.Outbox
import edu.minervia.platform.domain.entity.OutboxDeadLetter
import edu.minervia.platform.domain.repository.OutboxDeadLetterRepository
import edu.minervia.platform.domain.repository.OutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Outbox pattern implementation per CONSTRAINT [ASYNC-OUTBOX].
 *
 * - Polls outbox table every 1 second
 * - Batch size: 500
 * - Retry: exponential backoff 1s-60s, max 10 retries
 * - Dead letter: entries with >10 retries moved to outbox_dead_letter
 */
@Service
class OutboxService(
    private val outboxRepository: OutboxRepository,
    private val deadLetterRepository: OutboxDeadLetterRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val BATCH_SIZE = 500
        const val MAX_RETRIES = 10
        const val MIN_BACKOFF_MS = 1000L
        const val MAX_BACKOFF_MS = 60000L

        const val EVENT_REGISTRATION_TASK = "REGISTRATION_TASK"
        const val AGGREGATE_APPLICATION = "APPLICATION"
    }

    /**
     * Insert outbox entry within the same transaction as the business operation.
     */
    @Transactional
    fun insertOutboxEntry(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        payload: Any
    ): Outbox {
        val payloadJson = objectMapper.writeValueAsString(payload)
        val entry = Outbox(
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            eventType = eventType,
            payload = payloadJson
        )
        return outboxRepository.save(entry)
    }

    /**
     * Poll outbox and send to Kafka.
     * Runs every 1 second per CONSTRAINT [OUTBOX-POLLER-CONFIG].
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun pollAndPublish() {
        val entries = outboxRepository.findUnprocessed(PageRequest.of(0, BATCH_SIZE))
        if (entries.isEmpty()) return

        log.debug("Processing {} outbox entries", entries.size)

        for (entry in entries) {
            try {
                if (shouldRetry(entry)) {
                    publishToKafka(entry)
                    entry.processedAt = Instant.now()
                    outboxRepository.save(entry)
                } else {
                    moveToDeadLetter(entry, "Max retries exceeded")
                }
            } catch (e: Exception) {
                log.error("Failed to publish outbox entry {}: {}", entry.id, e.message)
                handleRetry(entry, e)
            }
        }
    }

    private fun publishToKafka(entry: Outbox) {
        val topic = when (entry.eventType) {
            EVENT_REGISTRATION_TASK -> KafkaConfig.TOPIC_REGISTRATION_TASKS
            else -> throw IllegalArgumentException("Unknown event type: ${entry.eventType}")
        }

        // Use aggregateId as partition key per CONSTRAINT [KAFKA-PARTITION-KEY]
        val future = kafkaTemplate.send(topic, entry.aggregateId, entry.payload)
        future.get() // Wait for confirmation
        log.debug("Published outbox entry {} to topic {}", entry.id, topic)
    }

    private fun shouldRetry(entry: Outbox): Boolean {
        if (entry.retryCount >= MAX_RETRIES) return false
        if (entry.retryCount == 0) return true

        // Exponential backoff with jitter
        val backoffMs = calculateBackoff(entry.retryCount)
        val nextRetryTime = entry.createdAt.plusMillis(backoffMs)
        return Instant.now().isAfter(nextRetryTime)
    }

    private fun calculateBackoff(retryCount: Int): Long {
        val exponentialBackoff = MIN_BACKOFF_MS * 2.0.pow(retryCount.toDouble()).toLong()
        val cappedBackoff = min(exponentialBackoff, MAX_BACKOFF_MS)
        // Add jitter (0-500ms)
        return cappedBackoff + Random.nextLong(0, 500)
    }

    private fun handleRetry(entry: Outbox, error: Exception) {
        entry.retryCount++
        if (entry.retryCount >= MAX_RETRIES) {
            moveToDeadLetter(entry, error.message)
        } else {
            outboxRepository.save(entry)
            log.warn("Outbox entry {} retry {}/{}", entry.id, entry.retryCount, MAX_RETRIES)
        }
    }

    @Transactional
    fun moveToDeadLetter(entry: Outbox, errorMessage: String?) {
        val deadLetter = OutboxDeadLetter(
            originalId = entry.id,
            aggregateType = entry.aggregateType,
            aggregateId = entry.aggregateId,
            eventType = entry.eventType,
            payload = entry.payload,
            retryCount = entry.retryCount,
            errorMessage = errorMessage,
            createdAt = entry.createdAt
        )
        deadLetterRepository.save(deadLetter)
        outboxRepository.delete(entry)
        log.error("Moved outbox entry {} to dead letter: {}", entry.id, errorMessage)
    }

    /**
     * Cleanup processed entries older than 24 hours.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    fun cleanupProcessed() {
        val cutoff = Instant.now().minusSeconds(24 * 60 * 60)
        val deleted = outboxRepository.deleteProcessedBefore(cutoff)
        if (deleted > 0) {
            log.info("Cleaned up {} processed outbox entries", deleted)
        }
    }
}
