package edu.minervia.platform.service

import com.fasterxml.jackson.databind.ObjectMapper
import edu.minervia.platform.domain.entity.EmailDelivery
import edu.minervia.platform.domain.entity.EmailDeliveryStatus
import edu.minervia.platform.domain.repository.EmailDeliveryRepository
import edu.minervia.platform.service.email.EmailDeliveryMessage
import edu.minervia.platform.service.email.EmailDeliveryService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class EmailDeliveryServiceTest {

    @Mock
    private lateinit var emailDeliveryRepository: EmailDeliveryRepository

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, EmailDeliveryMessage>

    @Mock
    private lateinit var objectMapper: ObjectMapper

    private lateinit var emailDeliveryService: EmailDeliveryService

    private val recipient = "test@example.com"
    private val template = "welcome"
    private val locale = "en"
    private val eventType = "REGISTRATION"
    private val entityId = 1L
    private val deliveryId = 100L

    @BeforeEach
    fun setUp() {
        emailDeliveryService = EmailDeliveryService(
            emailDeliveryRepository,
            kafkaTemplate,
            objectMapper
        )
    }

    @Test
    fun `createDelivery should create new delivery when dedupe key does not exist`() {
        // Given
        val params = mapOf("name" to "John", "code" to "ABC123")
        val paramsJson = """{"name":"John","code":"ABC123"}"""
        val dedupeKey = "$eventType:$entityId:$recipient:$template"

        whenever(emailDeliveryRepository.findByDedupeKey(dedupeKey)).thenReturn(null)
        whenever(objectMapper.writeValueAsString(params)).thenReturn(paramsJson)

        val savedDelivery = EmailDelivery(
            id = deliveryId,
            dedupeKey = dedupeKey,
            recipientEmail = recipient,
            template = template,
            locale = locale,
            paramsJson = paramsJson,
            status = EmailDeliveryStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(emailDeliveryRepository.save(any())).thenReturn(savedDelivery)

        // When
        val result = emailDeliveryService.createDelivery(
            recipient = recipient,
            template = template,
            params = params,
            locale = locale,
            eventType = eventType,
            entityId = entityId
        )

        // Then
        assertNotNull(result)
        assertEquals(deliveryId, result.id)
        assertEquals(recipient, result.recipientEmail)
        assertEquals(template, result.template)
        assertEquals(EmailDeliveryStatus.PENDING, result.status)

        verify(emailDeliveryRepository).findByDedupeKey(dedupeKey)
        verify(objectMapper).writeValueAsString(params)
        verify(emailDeliveryRepository).save(argThat {
            this.dedupeKey == dedupeKey &&
            this.recipientEmail == recipient &&
            this.template == template &&
            this.status == EmailDeliveryStatus.PENDING
        })
    }

    @Test
    fun `createDelivery should return existing delivery when dedupe key exists`() {
        // Given
        val params = mapOf("name" to "John")
        val dedupeKey = "$eventType:$entityId:$recipient:$template"

        val existingDelivery = EmailDelivery(
            id = deliveryId,
            dedupeKey = dedupeKey,
            recipientEmail = recipient,
            template = template,
            locale = locale,
            paramsJson = "{}",
            status = EmailDeliveryStatus.SENT,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(emailDeliveryRepository.findByDedupeKey(dedupeKey)).thenReturn(existingDelivery)

        // When
        val result = emailDeliveryService.createDelivery(
            recipient = recipient,
            template = template,
            params = params,
            locale = locale,
            eventType = eventType,
            entityId = entityId
        )

        // Then
        assertNotNull(result)
        assertEquals(deliveryId, result.id)
        assertEquals(EmailDeliveryStatus.SENT, result.status)

        verify(emailDeliveryRepository).findByDedupeKey(dedupeKey)
        verify(objectMapper, never()).writeValueAsString(any())
        verify(emailDeliveryRepository, never()).save(any())
    }

    @Test
    fun `sendAsync should send message to Kafka`() {
        // Given
        val message = EmailDeliveryMessage(deliveryId)

        // When
        emailDeliveryService.sendAsync(deliveryId)

        // Then
        verify(kafkaTemplate).send(
            eq("email.deliveries.send"),
            eq(deliveryId.toString()),
            argThat { this.deliveryId == deliveryId }
        )
    }

    @Test
    fun `markSent should update delivery status to SENT`() {
        // Given
        val providerMessageId = "msg-12345"

        val delivery = EmailDelivery(
            id = deliveryId,
            dedupeKey = "key",
            recipientEmail = recipient,
            template = template,
            locale = locale,
            paramsJson = "{}",
            status = EmailDeliveryStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(emailDeliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery))
        whenever(emailDeliveryRepository.save(any())).thenAnswer { it.arguments[0] as EmailDelivery }

        // When
        emailDeliveryService.markSent(deliveryId, providerMessageId)

        // Then
        verify(emailDeliveryRepository).findById(deliveryId)
        verify(emailDeliveryRepository).save(argThat {
            this.status == EmailDeliveryStatus.SENT &&
            this.providerMessageId == providerMessageId
        })
    }

    @Test
    fun `markSent should throw exception when delivery not found`() {
        // Given
        whenever(emailDeliveryRepository.findById(deliveryId)).thenReturn(Optional.empty())

        // When & Then
        assertThrows(NoSuchElementException::class.java) {
            emailDeliveryService.markSent(deliveryId, "msg-123")
        }

        verify(emailDeliveryRepository).findById(deliveryId)
        verify(emailDeliveryRepository, never()).save(any())
    }

    @Test
    fun `markFailed should update delivery status to FAILED and increment attempt count`() {
        // Given
        val error = "SMTP connection failed"

        val delivery = EmailDelivery(
            id = deliveryId,
            dedupeKey = "key",
            recipientEmail = recipient,
            template = template,
            locale = locale,
            paramsJson = "{}",
            status = EmailDeliveryStatus.PENDING,
            attemptCount = 1,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(emailDeliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery))
        whenever(emailDeliveryRepository.save(any())).thenAnswer { it.arguments[0] as EmailDelivery }

        // When
        emailDeliveryService.markFailed(deliveryId, error)

        // Then
        verify(emailDeliveryRepository).findById(deliveryId)
        verify(emailDeliveryRepository).save(argThat {
            this.status == EmailDeliveryStatus.FAILED &&
            this.lastError == error &&
            this.attemptCount == 2
        })
    }

    @Test
    fun `markFailed should throw exception when delivery not found`() {
        // Given
        whenever(emailDeliveryRepository.findById(deliveryId)).thenReturn(Optional.empty())

        // When & Then
        assertThrows(NoSuchElementException::class.java) {
            emailDeliveryService.markFailed(deliveryId, "error")
        }

        verify(emailDeliveryRepository).findById(deliveryId)
        verify(emailDeliveryRepository, never()).save(any())
    }

    @Test
    fun `scheduleRetry should schedule retry with backoff when attempts below max`() {
        // Given
        val delivery = EmailDelivery(
            id = deliveryId,
            dedupeKey = "key",
            recipientEmail = recipient,
            template = template,
            locale = locale,
            paramsJson = "{}",
            status = EmailDeliveryStatus.FAILED,
            attemptCount = 1,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(emailDeliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery))
        whenever(emailDeliveryRepository.save(any())).thenAnswer { it.arguments[0] as EmailDelivery }

        // When
        emailDeliveryService.scheduleRetry(deliveryId)

        // Then
        verify(emailDeliveryRepository).findById(deliveryId)
        verify(emailDeliveryRepository).save(argThat {
            this.status == EmailDeliveryStatus.PENDING &&
            this.nextAttemptAt != null &&
            this.nextAttemptAt!!.isAfter(LocalDateTime.now())
        })
    }

    @Test
    fun `scheduleRetry should not schedule retry when max attempts reached`() {
        // Given
        val delivery = EmailDelivery(
            id = deliveryId,
            dedupeKey = "key",
            recipientEmail = recipient,
            template = template,
            locale = locale,
            paramsJson = "{}",
            status = EmailDeliveryStatus.FAILED,
            attemptCount = 3, // MAX_ATTEMPTS
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(emailDeliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery))

        // When
        emailDeliveryService.scheduleRetry(deliveryId)

        // Then
        verify(emailDeliveryRepository).findById(deliveryId)
        verify(emailDeliveryRepository, never()).save(any())
    }

    @Test
    fun `scheduleRetry should throw exception when delivery not found`() {
        // Given
        whenever(emailDeliveryRepository.findById(deliveryId)).thenReturn(Optional.empty())

        // When & Then
        assertThrows(NoSuchElementException::class.java) {
            emailDeliveryService.scheduleRetry(deliveryId)
        }

        verify(emailDeliveryRepository).findById(deliveryId)
        verify(emailDeliveryRepository, never()).save(any())
    }
}
