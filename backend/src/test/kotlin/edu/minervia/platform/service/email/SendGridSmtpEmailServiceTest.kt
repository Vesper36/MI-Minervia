package edu.minervia.platform.service.email

import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender

@ExtendWith(MockitoExtension::class)
class SendGridSmtpEmailServiceTest {

    @Mock
    private lateinit var mailSender: JavaMailSender

    @Mock
    private lateinit var bounceService: EmailBounceService

    @Mock
    private lateinit var templateRenderer: EmailTemplateRenderer

    @Mock
    private lateinit var mimeMessage: MimeMessage

    private lateinit var emailService: SendGridSmtpEmailService

    @BeforeEach
    fun setUp() {
        emailService = SendGridSmtpEmailService(mailSender, bounceService, templateRenderer)
    }

    @Test
    fun `send returns success when email is sent successfully`() {
        val to = "test@example.com"
        val template = EmailTemplate.VERIFICATION
        val params = mapOf("code" to "123456", "expiryMinutes" to 15)

        `when`(bounceService.isEmailSuppressed(to)).thenReturn(false)
        `when`(templateRenderer.renderSubject(template, params, "en")).thenReturn("Test Subject")
        `when`(templateRenderer.renderBody(template, params, "en")).thenReturn("<html>Test Body</html>")
        `when`(mailSender.createMimeMessage()).thenReturn(mimeMessage)

        val result = emailService.send(to, template, params, "en")

        assertTrue(result.success)
        assertNotNull(result.messageId)
        assertNull(result.errorMessage)
        verify(mailSender).send(mimeMessage)
    }

    @Test
    fun `send returns skipped when email is suppressed`() {
        val to = "suppressed@example.com"
        val template = EmailTemplate.VERIFICATION
        val params = mapOf("code" to "123456")

        `when`(bounceService.isEmailSuppressed(to)).thenReturn(true)

        val result = emailService.send(to, template, params, "en")

        assertTrue(result.success)
        assertEquals("Email address is suppressed", result.errorMessage)
        verify(mailSender, never()).send(any(MimeMessage::class.java))
    }

    @Test
    fun `send returns failure when mail sender throws exception`() {
        val to = "test@example.com"
        val template = EmailTemplate.VERIFICATION
        val params = mapOf("code" to "123456", "expiryMinutes" to 15)

        `when`(bounceService.isEmailSuppressed(to)).thenReturn(false)
        `when`(templateRenderer.renderSubject(template, params, "en")).thenReturn("Test Subject")
        `when`(templateRenderer.renderBody(template, params, "en")).thenReturn("<html>Test Body</html>")
        `when`(mailSender.createMimeMessage()).thenReturn(mimeMessage)
        org.mockito.Mockito.doThrow(MailSendException("SMTP error")).`when`(mailSender).send(mimeMessage)

        val result = emailService.send(to, template, params, "en")

        assertFalse(result.success)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `sendVerificationCode delegates to send with correct params`() {
        val to = "test@example.com"
        val code = "654321"

        `when`(bounceService.isEmailSuppressed(to)).thenReturn(false)
        `when`(templateRenderer.renderSubject(any(), any(), any())).thenReturn("Subject")
        `when`(templateRenderer.renderBody(any(), any(), any())).thenReturn("<html>Body</html>")
        `when`(mailSender.createMimeMessage()).thenReturn(mimeMessage)

        val result = emailService.sendVerificationCode(to, code, "en")

        assertTrue(result.success)
        verify(templateRenderer).renderSubject(
            org.mockito.kotlin.eq(EmailTemplate.VERIFICATION),
            org.mockito.kotlin.argThat { params ->
                params["code"] == code && params["expiryMinutes"] == 15
            },
            org.mockito.kotlin.eq("en")
        )
    }

    @Test
    fun `sendWelcomeEmail delegates to send with correct params`() {
        val to = "test@example.com"
        val studentName = "John Doe"
        val eduEmail = "john.doe@minervia.edu"
        val tempPassword = "TempPass123!"

        `when`(bounceService.isEmailSuppressed(to)).thenReturn(false)
        `when`(templateRenderer.renderSubject(any(), any(), any())).thenReturn("Subject")
        `when`(templateRenderer.renderBody(any(), any(), any())).thenReturn("<html>Body</html>")
        `when`(mailSender.createMimeMessage()).thenReturn(mimeMessage)

        val result = emailService.sendWelcomeEmail(to, studentName, eduEmail, tempPassword, "en")

        assertTrue(result.success)
        verify(templateRenderer).renderBody(
            org.mockito.kotlin.eq(EmailTemplate.WELCOME),
            org.mockito.kotlin.argThat { params ->
                params["studentName"] == studentName &&
                    params["eduEmail"] == eduEmail &&
                    params["tempPassword"] == tempPassword
            },
            org.mockito.kotlin.eq("en")
        )
    }

    @Test
    fun `sendRejectionEmail sanitizes reason parameter`() {
        val to = "test@example.com"
        val applicantName = "Jane Doe"
        val reason = "<script>alert('xss')</script>Reason"

        `when`(bounceService.isEmailSuppressed(to)).thenReturn(false)
        `when`(templateRenderer.renderSubject(any(), any(), any())).thenReturn("Subject")
        `when`(templateRenderer.renderBody(any(), any(), any())).thenReturn("<html>Body</html>")
        `when`(mailSender.createMimeMessage()).thenReturn(mimeMessage)

        val result = emailService.sendRejectionEmail(to, applicantName, reason, "en")

        assertTrue(result.success)
        verify(templateRenderer).renderBody(
            org.mockito.kotlin.eq(EmailTemplate.REJECTION),
            org.mockito.kotlin.argThat { params ->
                val sanitizedReason = params["reason"] as? String ?: ""
                !sanitizedReason.contains("<") && !sanitizedReason.contains(">")
            },
            org.mockito.kotlin.eq("en")
        )
    }

    @Test
    fun `sendAlertEmail sends to multiple recipients`() {
        val recipients = listOf("admin1@example.com", "admin2@example.com")
        val alertType = "SECURITY"
        val severity = "HIGH"
        val message = "Suspicious activity detected"

        `when`(bounceService.isEmailSuppressed(any())).thenReturn(false)
        `when`(templateRenderer.renderSubject(any(), any(), any())).thenReturn("Subject")
        `when`(templateRenderer.renderBody(any(), any(), any())).thenReturn("<html>Body</html>")
        `when`(mailSender.createMimeMessage()).thenReturn(mimeMessage)

        val result = emailService.sendAlertEmail(recipients, alertType, severity, message)

        assertTrue(result.success)
        verify(mailSender, org.mockito.Mockito.times(2)).send(mimeMessage)
    }

    @Test
    fun `sendAlertEmail returns failure when no recipients provided`() {
        val result = emailService.sendAlertEmail(emptyList(), "SECURITY", "HIGH", "Message")

        assertFalse(result.success)
        assertEquals("No recipients", result.errorMessage)
    }

    @Test
    fun `send uses locale for template rendering`() {
        val to = "test@example.com"
        val template = EmailTemplate.VERIFICATION
        val params = mapOf("code" to "123456", "expiryMinutes" to 15)
        val locale = "pl"

        `when`(bounceService.isEmailSuppressed(to)).thenReturn(false)
        `when`(templateRenderer.renderSubject(template, params, locale)).thenReturn("Polish Subject")
        `when`(templateRenderer.renderBody(template, params, locale)).thenReturn("<html>Polish Body</html>")
        `when`(mailSender.createMimeMessage()).thenReturn(mimeMessage)

        emailService.send(to, template, params, locale)

        verify(templateRenderer).renderSubject(template, params, locale)
        verify(templateRenderer).renderBody(template, params, locale)
    }
}
