package edu.minervia.platform.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.ai.openai")
class OpenAiProperties {
    var apiKey: String = ""
    var baseUrl: String = "https://api.openai.com"
    var model: String = "gpt-4o-mini"
    var temperature: Double = 0.7
    var maxTokens: Int = 220
}
