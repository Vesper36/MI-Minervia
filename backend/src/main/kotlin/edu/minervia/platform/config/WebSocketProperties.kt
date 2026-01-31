package edu.minervia.platform.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.websocket")
class WebSocketProperties {
    var allowedOrigins: List<String> = listOf(
        "http://localhost:*",
        "https://localhost:*"
    )
    var endpoint: String = "/ws"
    var enableSockJs: Boolean = true
}
