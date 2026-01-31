package edu.minervia.platform.service

import edu.minervia.platform.domain.entity.EmailSuppression
import edu.minervia.platform.domain.enums.EmailSuppressionReason
import edu.minervia.platform.domain.repository.EmailSuppressionRepository
import edu.minervia.platform.service.audit.AuditEvent
import edu.minervia.platform.service.audit.AuditLogService
import edu.minervia.platform.service.email.EmailBounceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class EmailBounceServiceTest {

    @Mock
    private lateinit var emailSuppressionRepository: EmailSuppressionRepository

    @Mock
    private lateinit var auditLogService: AuditLogService

    private lateinit var service: EmailBounceService

    @BeforeEach
    fun setUp() {
        service = EmailBounceService(
            emailSuppressionRepository = emailSuppressionRepository,
            auditLogService = auditLogService
        )
    }

    @Test
    fun handleHardBounce_createsSuppression_andLogsEvent() {
        `when`(emailSuppressionRepository.findByEmail("user@example.com")).thenReturn(Optional.empty())
        `when`(emailSuppressionRepository.save(any(EmailSuppression::class.java)))
            .thenAnswer { it.arguments[0] as EmailSuppression }

        service.handleHardBounce("User@Example.com ", "Mailbox not found")

        val captor = ArgumentCaptor.forClass(EmailSuppression::class.java)
        verify(emailSuppressionRepository).save(captor.capture())
        val saved = captor.value

        assertEquals("user@example.com", saved.email)
        assertEquals(EmailSuppressionReason.HARD_BOUNCE, saved.reason)
        assertEquals(1, saved.bounceCount)
        assertNotNull(saved.suppressedAt)
        verify(auditLogService).logAsync(any(), any())
    }

    @Test
    fun handleSoftBounce_reachesThreshold_suppressesEmail() {
        val suppression = EmailSuppression(
            email = "user@example.com",
            bounceCount = 4,
            firstBounceAt = Instant.now().minus(1, ChronoUnit.HOURS)
        )
        `when`(emailSuppressionRepository.findByEmail("user@example.com")).thenReturn(Optional.of(suppression))
        `when`(emailSuppressionRepository.save(any(EmailSuppression::class.java)))
            .thenAnswer { it.arguments[0] as EmailSuppression }

        service.handleSoftBounce("user@example.com", "Temporary failure")

        assertEquals(5, suppression.bounceCount)
        assertEquals(EmailSuppressionReason.SOFT_BOUNCE, suppression.reason)
        assertNotNull(suppression.suppressedAt)
        verify(auditLogService).logAsync(any(), any())
    }

    @Test
    fun handleSoftBounce_outsideWindow_resetsCount() {
        val suppression = EmailSuppression(
            email = "user@example.com",
            bounceCount = 3,
            firstBounceAt = Instant.now().minus(80, ChronoUnit.HOURS)
        )
        `when`(emailSuppressionRepository.findByEmail("user@example.com")).thenReturn(Optional.of(suppression))
        `when`(emailSuppressionRepository.save(any(EmailSuppression::class.java)))
            .thenAnswer { it.arguments[0] as EmailSuppression }

        service.handleSoftBounce("user@example.com", "Temporary failure")

        assertEquals(1, suppression.bounceCount)
        assertNotNull(suppression.firstBounceAt)
        assertNull(suppression.suppressedAt)
        verify(auditLogService).logAsync(any(), any())
    }

    @Test
    fun handleSpamComplaint_setsSuppression_andLogsEvent() {
        `when`(emailSuppressionRepository.findByEmail("user@example.com")).thenReturn(Optional.empty())
        `when`(emailSuppressionRepository.save(any(EmailSuppression::class.java)))
            .thenAnswer { it.arguments[0] as EmailSuppression }

        service.handleSpamComplaint("user@example.com")

        val captor = ArgumentCaptor.forClass(EmailSuppression::class.java)
        verify(emailSuppressionRepository).save(captor.capture())
        val saved = captor.value

        assertEquals(EmailSuppressionReason.SPAM_COMPLAINT, saved.reason)
        assertNotNull(saved.suppressedAt)
        verify(auditLogService).logAsync(any(), any())
    }

    @Test
    fun unsuppressEmail_clearsFields_andLogsUnsuppressedEvent() {
        val suppression = EmailSuppression(
            id = 10L,
            email = "user@example.com",
            reason = EmailSuppressionReason.HARD_BOUNCE,
            bounceCount = 2,
            firstBounceAt = Instant.now().minus(1, ChronoUnit.DAYS),
            lastBounceAt = Instant.now(),
            suppressedAt = Instant.now()
        )
        `when`(emailSuppressionRepository.findByEmail("user@example.com")).thenReturn(Optional.of(suppression))
        `when`(emailSuppressionRepository.save(any(EmailSuppression::class.java)))
            .thenAnswer { it.arguments[0] as EmailSuppression }

        service.unsuppressEmail("user@example.com")

        assertNull(suppression.suppressedAt)
        assertNull(suppression.reason)
        assertEquals(0, suppression.bounceCount)
        assertNull(suppression.firstBounceAt)
        assertNull(suppression.lastBounceAt)

        val eventCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
        verify(auditLogService).logAsync(any(), eventCaptor.capture())
        assertEquals(AuditLogService.EVENT_EMAIL_UNSUPPRESSED, eventCaptor.value.eventType)
    }

    @Test
    fun handleHardBounce_blankEmail_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            service.handleHardBounce("   ", "invalid")
        }
    }
}
