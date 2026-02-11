package edu.minervia.platform.service.email

import com.fasterxml.jackson.databind.ObjectMapper
import edu.minervia.platform.domain.entity.EmailDelivery
import edu.minervia.platform.domain.entity.EmailDeliveryStatus
import edu.minervia.platform.domain.repository.EmailDeliveryRepository
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

@Service
class EmailDeliveryService(
    private val emailDeliveryRepository: EmailDeliveryRepository,
    private val kafkaTemplate: KafkaTemplate<String, EmailDeliveryMessage>,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val KAFKA_TOPIC = "email.deliveries.send"
        private const val INITIAL_DELAY_MS = 1000L
        private const val MULTIPLIER = 2.0
        private const val MAX_DELAY_MS = 8000L
        private const val MAX_ATTEMPTS = 3
        private const val JITTER = 0.2
    }

    @Transactional
    fun createDelivery(
        recipient: String,
        template: String,
        params: Map<String, String>,
        locale: String = "en",
        eventType: String,
        entityId: Long
    ): EmailDelivery {
        val dedupeKey = generateDedupeKey(eventType, entityId, recipient, template)

        // Check if already exists
        val existing = emailDeliveryRepository.findByDedupeKey(dedupeKey)
        if (existing != null) {
            return existing
        }

        val paramsJson = objectMapper.writeValueAsString(params)

        val delivery = EmailDelivery(
            dedupeKey = dedupeKey,
            recipientEmail = recipient,
            template = template,
            locale = locale,
            paramsJson = paramsJson,
            status = EmailDeliveryStatus.PENDING
        )

        return emailDeliveryRepository.save(delivery)
    }

    fun sendAsync(deliveryId: Long) {
        val message = EmailDeliveryMessage(deliveryId)
        kafkaTemplate.send(KAFKA_TOPIC, deliveryId.toString(), message)
    }

    @Transactional
    fun markSent(deliveryId: Long, providerMessageId: String) {
        val delivery = emailDeliveryRepository.findById(deliveryId).orElseThrow()
        delivery.status = EmailDeliveryStatus.SENT
        delivery.providerMessageId = providerMessageId
        delivery.updatedAt = LocalDateTime.now()
        emailDeliveryRepository.save(delivery)
    }

    @Transactional
    fun markFailed(deliveryId: Long, error: String) {
        val delivery = emailDeliveryRepository.findById(deliveryId).orElseThrow()
        delivery.status = EmailDeliveryStatus.FAILED
        delivery.lastError = error
        delivery.attemptCount++
        delivery.updatedAt = LocalDateTime.now()
        emailDeliveryRepository.save(delivery)
    }

    @Transactional
    fun scheduleRetry(deliveryId: Long) {
        val delivery = emailDeliveryRepository.findById(deliveryId).orElseThrow()

        if (delivery.attemptCount >= MAX_ATTEMPTS) {
            // Move to DLT
            return
        }

        val delayMs = calculateBackoff(delivery.attemptCount)
        val nextAttempt = LocalDateTime.now().plusNanos(delayMs * 1_000_000)

        delivery.status = EmailDeliveryStatus.PENDING
        delivery.nextAttemptAt = nextAttempt
        delivery.updatedAt = LocalDateTime.now()

        emailDeliveryRepository.save(delivery)
    }

    private fun generateDedupeKey(
        eventType: String,
        entityId: Long,
        recipient: String,
        template: String
    ): String {
        return "$eventType:$entityId:$recipient:$template"
    }

    private fun calculateBackoff(attemptCount: Int): Long {
        val baseDelay = INITIAL_DELAY_MS * MULTIPLIER.pow(attemptCount.toDouble())
        val cappedDelay = min(baseDelay, MAX_DELAY_MS.toDouble())
        val jitterFactor = 1.0 + Random.nextDouble(-JITTER, JITTER)
        return (cappedDelay * jitterFactor).toLong()
    }
}

data class EmailDeliveryMessage(
    val deliveryId: Long
)
