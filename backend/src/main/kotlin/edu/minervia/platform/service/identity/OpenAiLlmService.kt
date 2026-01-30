package edu.minervia.platform.service.identity

import com.fasterxml.jackson.annotation.JsonProperty
import edu.minervia.platform.config.OpenAiProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import java.time.Duration

@Service
class OpenAiLlmService(
    restTemplateBuilder: RestTemplateBuilder,
    private val properties: OpenAiProperties
) : LlmService {
    private val logger = LoggerFactory.getLogger(OpenAiLlmService::class.java)

    private val restTemplate = restTemplateBuilder
        .setConnectTimeout(Duration.ofSeconds(60))
        .setReadTimeout(Duration.ofSeconds(60))
        .build()

    override fun generateText(prompt: String): String {
        if (properties.apiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key is not configured")
        }

        val endpoint = properties.baseUrl.trimEnd('/') + "/v1/chat/completions"
        val request = OpenAiChatCompletionRequest(
            model = properties.model,
            messages = listOf(
                OpenAiChatMessage(role = "system", content = "You are a helpful assistant for student profile polishing."),
                OpenAiChatMessage(role = "user", content = prompt)
            ),
            temperature = properties.temperature,
            maxTokens = properties.maxTokens
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(properties.apiKey)
        }

        val entity = HttpEntity(request, headers)

        try {
            val response = restTemplate.postForEntity(endpoint, entity, OpenAiChatCompletionResponse::class.java)
            val content = response.body
                ?.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?.trim()

            if (content.isNullOrBlank()) {
                throw IllegalStateException("OpenAI response is empty")
            }

            return content
        } catch (ex: RestClientException) {
            logger.warn("OpenAI request failed: ${ex.message}")
            throw ex
        }
    }

    data class OpenAiChatCompletionRequest(
        val model: String,
        val messages: List<OpenAiChatMessage>,
        val temperature: Double,
        @JsonProperty("max_tokens")
        val maxTokens: Int
    )

    data class OpenAiChatCompletionResponse(
        val choices: List<OpenAiChatChoice> = emptyList()
    )

    data class OpenAiChatChoice(
        val message: OpenAiChatMessage
    )

    data class OpenAiChatMessage(
        val role: String,
        val content: String
    )
}
