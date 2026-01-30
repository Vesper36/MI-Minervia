package edu.minervia.platform.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.jwt")
class JwtProperties {
    var secret: String = ""
    var accessTokenExpiration: Long = 1800000 // 30 minutes (CONSTRAINT: JWT-DUAL-TOKEN)
    var refreshTokenExpiration: Long = 1209600000 // 14 days (CONSTRAINT: JWT-DUAL-TOKEN)
    var requireSecret: Boolean = false

    @Deprecated("Use accessTokenExpiration instead")
    var expiration: Long
        get() = accessTokenExpiration
        set(value) { accessTokenExpiration = value }
}
