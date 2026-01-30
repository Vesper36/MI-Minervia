package edu.minervia.platform.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.jwt")
class JwtProperties {
    var secret: String = ""
    var expiration: Long = 3600000 // 1 hour
    var requireSecret: Boolean = false
}
