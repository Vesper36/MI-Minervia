package edu.minervia.platform.service.email

import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SendGridSmtpEmailService(
    private val mailSender: JavaMailSender,
    private val bounceService: EmailBounceService,
    private val templateRenderer: EmailTemplateRenderer
) : EmailService {

    private val log = LoggerFactory.getLogger(SendGridSmtpEmailService::class.java)

    override fun send(
        to: String,
        template: EmailTemplate,
        params: Map<String, Any>,
        locale: String
    ): EmailDeliveryResult {
        if (bounceService.isEmailSuppressed(to)) {
            log.info("Skipping email to suppressed address: {}", maskEmail(to))
            return EmailDeliveryResult.skipped("Email address is suppressed")
        }

        return try {
            val subject = templateRenderer.renderSubject(template, params, locale)
            val body = templateRenderer.renderBody(template, params, locale)
            doSend(to, subject, body)
        } catch (e: Exception) {
            log.error("Failed to send email to {}: {}", maskEmail(to), e.message)
            EmailDeliveryResult.failure(e.message ?: "Unknown error")
        }
    }

    override fun sendVerificationCode(to: String, code: String, locale: String): EmailDeliveryResult {
        val params = mapOf(
            "code" to code,
            "expiryMinutes" to 15
        )
        return send(to, EmailTemplate.VERIFICATION, params, locale)
    }

    override fun sendWelcomeEmail(
        to: String,
        studentName: String,
        eduEmail: String,
        tempPassword: String,
        locale: String
    ): EmailDeliveryResult {
        val params = mapOf(
            "studentName" to studentName,
            "eduEmail" to eduEmail,
            "tempPassword" to tempPassword
        )
        return send(to, EmailTemplate.WELCOME, params, locale)
    }

    override fun sendRejectionEmail(
        to: String,
        applicantName: String,
        reason: String,
        locale: String
    ): EmailDeliveryResult {
        val params = mapOf(
            "applicantName" to applicantName,
            "reason" to sanitizeReason(reason)
        )
        return send(to, EmailTemplate.REJECTION, params, locale)
    }

    override fun sendAlertEmail(
        to: List<String>,
        alertType: String,
        severity: String,
        message: String
    ): EmailDeliveryResult {
        val params = mapOf(
            "alertType" to alertType,
            "severity" to severity,
            "message" to message
        )

        var lastResult: EmailDeliveryResult = EmailDeliveryResult.failure("No recipients")
        for (recipient in to) {
            lastResult = send(recipient, EmailTemplate.ALERT, params, "en")
            if (!lastResult.success) {
                log.warn("Failed to send alert to {}", maskEmail(recipient))
            }
        }
        return lastResult
    }

    private fun doSend(to: String, subject: String, htmlBody: String): EmailDeliveryResult {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")

        helper.setTo(to)
        helper.setFrom("noreply@minervia.edu")
        helper.setSubject(subject)
        helper.setText(htmlBody, true)

        mailSender.send(message)

        val messageId = UUID.randomUUID().toString()
        log.info("Email sent successfully to {} with messageId {}", maskEmail(to), messageId)
        return EmailDeliveryResult.success(messageId)
    }

    private fun sanitizeReason(reason: String): String {
        return reason
            .replace(Regex("[<>\"']"), "")
            .take(500)
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"
        val local = parts[0]
        val domain = parts[1]
        val maskedLocal = if (local.length > 2) {
            "${local.take(2)}***"
        } else {
            "***"
        }
        return "$maskedLocal@$domain"
    }
}
