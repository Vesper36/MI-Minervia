package edu.minervia.platform.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.email.webhook")
class EmailWebhookProperties {
    var signingKey: String = ""
    var mailgunSigningKey: String = ""
    var signatureToleranceSeconds: Long = 600
}
