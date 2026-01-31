package edu.minervia.platform.service.email

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class EmailTemplateRenderer {

    fun renderSubject(template: EmailTemplate, params: Map<String, Any>, locale: String): String {
        return when (template) {
            EmailTemplate.VERIFICATION -> getLocalizedSubject("verification", locale)
            EmailTemplate.WELCOME -> getLocalizedSubject("welcome", locale)
            EmailTemplate.REJECTION -> getLocalizedSubject("rejection", locale)
            EmailTemplate.ALERT -> "[${params["severity"]}] Minervia Alert: ${params["alertType"]}"
        }
    }

    fun renderBody(template: EmailTemplate, params: Map<String, Any>, locale: String): String {
        val templateContent = loadTemplate(template.templateName, locale)
        return replaceParams(templateContent, params)
    }

    private fun getLocalizedSubject(templateName: String, locale: String): String {
        return when (templateName) {
            "verification" -> when (locale) {
                "pl" -> "Kod weryfikacyjny Minervia Institute"
                "zh", "zh-CN" -> "Minervia Institute 验证码"
                else -> "Minervia Institute Verification Code"
            }
            "welcome" -> when (locale) {
                "pl" -> "Witamy w Minervia Institute"
                "zh", "zh-CN" -> "欢迎加入 Minervia Institute"
                else -> "Welcome to Minervia Institute"
            }
            "rejection" -> when (locale) {
                "pl" -> "Aktualizacja statusu aplikacji Minervia Institute"
                "zh", "zh-CN" -> "Minervia Institute 申请状态更新"
                else -> "Minervia Institute Application Status Update"
            }
            else -> "Minervia Institute Notification"
        }
    }

    private fun loadTemplate(templateName: String, locale: String): String {
        val paths = listOf(
            "email-templates/$locale/$templateName.html",
            "email-templates/en/$templateName.html"
        )

        for (path in paths) {
            try {
                val resource = ClassPathResource(path)
                if (resource.exists()) {
                    return resource.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
                }
            } catch (_: Exception) {
                continue
            }
        }

        return getDefaultTemplate(templateName)
    }

    private fun getDefaultTemplate(templateName: String): String {
        return when (templateName) {
            "verification" -> """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>Minervia Institute</h2>
                    <p>Your verification code is:</p>
                    <h1 style="color: #2563eb; letter-spacing: 4px;">{{code}}</h1>
                    <p>This code will expire in {{expiryMinutes}} minutes.</p>
                    <hr>
                    <p style="color: #666; font-size: 12px;">This is a simulated educational platform for testing purposes only.</p>
                </body>
                </html>
            """.trimIndent()

            "welcome" -> """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>Welcome to Minervia Institute</h2>
                    <p>Dear {{studentName}},</p>
                    <p>Your student account has been created successfully.</p>
                    <p><strong>EDU Email:</strong> {{eduEmail}}</p>
                    <p><strong>Temporary Password:</strong> {{tempPassword}}</p>
                    <p>Please change your password after your first login.</p>
                    <hr>
                    <p style="color: #666; font-size: 12px;">This is a simulated educational platform for testing purposes only.</p>
                </body>
                </html>
            """.trimIndent()

            "rejection" -> """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>Minervia Institute - Application Update</h2>
                    <p>Dear {{applicantName}},</p>
                    <p>We regret to inform you that your application has not been approved.</p>
                    <p><strong>Reason:</strong> {{reason}}</p>
                    <p>If you have any questions, please contact our admissions office.</p>
                    <hr>
                    <p style="color: #666; font-size: 12px;">This is a simulated educational platform for testing purposes only.</p>
                </body>
                </html>
            """.trimIndent()

            "alert" -> """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #dc2626;">Minervia Institute - System Alert</h2>
                    <p><strong>Alert Type:</strong> {{alertType}}</p>
                    <p><strong>Severity:</strong> {{severity}}</p>
                    <p><strong>Message:</strong></p>
                    <p style="background: #f3f4f6; padding: 12px; border-radius: 4px;">{{message}}</p>
                    <hr>
                    <p style="color: #666; font-size: 12px;">This is an automated system alert.</p>
                </body>
                </html>
            """.trimIndent()

            else -> "<html><body><p>{{message}}</p></body></html>"
        }
    }

    private fun replaceParams(template: String, params: Map<String, Any>): String {
        var result = template
        for ((key, value) in params) {
            result = result.replace("{{$key}}", value.toString())
        }
        return result
    }
}
