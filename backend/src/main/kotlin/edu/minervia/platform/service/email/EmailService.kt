package edu.minervia.platform.service.email

interface EmailService {
    fun send(
        to: String,
        template: EmailTemplate,
        params: Map<String, Any>,
        locale: String = "en"
    ): EmailDeliveryResult

    fun sendVerificationCode(to: String, code: String, locale: String = "en"): EmailDeliveryResult

    fun sendWelcomeEmail(
        to: String,
        studentName: String,
        eduEmail: String,
        tempPassword: String,
        locale: String = "en"
    ): EmailDeliveryResult

    fun sendRejectionEmail(
        to: String,
        applicantName: String,
        reason: String,
        locale: String = "en"
    ): EmailDeliveryResult

    fun sendAlertEmail(
        to: List<String>,
        alertType: String,
        severity: String,
        message: String
    ): EmailDeliveryResult
}
