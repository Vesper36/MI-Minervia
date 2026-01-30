package edu.minervia.platform.service.identity.llm

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
    private val logger = LoggerFactory.getLogger(javaClass)
    private val restTemplate = restTemplateBuilder
        .setConnectTimeout(Duration.ofSeconds(60))
        .setReadTimeout(Duration.ofSeconds(60))
        .build()

    override fun generateText(prompt: String): String {
        if (properties.apiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key is not configured")
        }

        val endpoint = "${properties.baseUrl.trimEnd('/')}/v1/chat/completions"
        val request = ChatCompletionRequest(
            model = properties.model,
            messages = listOf(
                ChatMessage("system", "You are a helpful assistant for student profile generation."),
                ChatMessage("user", prompt)
            ),
            temperature = properties.temperature,
            maxTokens = properties.maxTokens
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(properties.apiKey)
        }

        return try {
            val response = restTemplate.postForEntity(
                endpoint,
                HttpEntity(request, headers),
                ChatCompletionResponse::class.java
            )
            response.body?.choices?.firstOrNull()?.message?.content?.trim()
                ?: throw IllegalStateException("Empty OpenAI response")
        } catch (ex: RestClientException) {
            logger.warn("OpenAI request failed: {}", ex.message)
            throw ex
        }
    }

    private data class ChatCompletionRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double,
        @JsonProperty("max_tokens") val maxTokens: Int
    )

    private data class ChatCompletionResponse(
        val choices: List<ChatChoice> = emptyList()
    )

    private data class ChatChoice(val message: ChatMessage)
    private data class ChatMessage(val role: String, val content: String)
}
