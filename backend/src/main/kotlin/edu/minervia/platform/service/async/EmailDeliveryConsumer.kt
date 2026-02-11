package edu.minervia.platform.service.async

import com.fasterxml.jackson.databind.ObjectMapper
import edu.minervia.platform.domain.entity.EmailDeliveryStatus
import edu.minervia.platform.domain.repository.EmailDeliveryRepository
import edu.minervia.platform.service.email.EmailDeliveryMessage
import edu.minervia.platform.service.email.EmailDeliveryService
import edu.minervia.platform.service.email.EmailService
import edu.minervia.platform.service.email.EmailTemplate
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class EmailDeliveryConsumer(
    private val emailDeliveryRepository: EmailDeliveryRepository,
    private val emailDeliveryService: EmailDeliveryService,
    private val emailService: EmailService,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["email.deliveries.send"],
        groupId = "email-delivery-consumer"
    )
    fun consumeEmailDelivery(message: EmailDeliveryMessage, ack: Acknowledgment) {
        try {
            val delivery = emailDeliveryRepository.findById(message.deliveryId).orElse(null)
            if (delivery == null) {
                logger.warn("Email delivery not found: ${message.deliveryId}")
                ack.acknowledge()
                return
            }

            // Check if already sent (idempotency)
            if (delivery.status == EmailDeliveryStatus.SENT) {
                logger.info("Email already sent: ${message.deliveryId}")
                ack.acknowledge()
                return
            }

            // Parse params
            val params: Map<String, Any> = objectMapper.readValue(
                delivery.paramsJson,
                objectMapper.typeFactory.constructMapType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            )

            // Convert template string to enum
            val emailTemplate = EmailTemplate.entries.find { it.templateName == delivery.template }
                ?: throw IllegalArgumentException("Unknown email template: ${delivery.template}")

            // Send email
            emailService.send(
                to = delivery.recipientEmail,
                template = emailTemplate,
                params = params,
                locale = delivery.locale
            )

            // Mark as sent
            emailDeliveryService.markSent(delivery.id!!, "provider-message-id")

            logger.info("Email sent successfully: ${message.deliveryId}")
            ack.acknowledge()

        } catch (e: Exception) {
            logger.error("Failed to send email: ${message.deliveryId}", e)

            try {
                emailDeliveryService.markFailed(message.deliveryId, e.message ?: "Unknown error")
                emailDeliveryService.scheduleRetry(message.deliveryId)
            } catch (retryException: Exception) {
                logger.error("Failed to schedule retry: ${message.deliveryId}", retryException)
            }

            ack.acknowledge()
        }
    }
}
