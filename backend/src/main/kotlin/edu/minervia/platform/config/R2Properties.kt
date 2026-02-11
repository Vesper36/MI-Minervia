package edu.minervia.platform.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.r2")
data class R2Properties(
    var enabled: Boolean = true,
    var endpoint: String = "",
    var bucket: String = "",
    var region: String = "auto",
    var accessKey: String = "",
    var secretKey: String = "",
    var uploadUrlTtl: Int = 900,  // 15 minutes
    var downloadUrlTtl: Int = 3600,  // 1 hour
    var maxFileSize: Long = 10485760,  // 10MB
    var allowedContentTypes: List<String> = listOf(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )
)
