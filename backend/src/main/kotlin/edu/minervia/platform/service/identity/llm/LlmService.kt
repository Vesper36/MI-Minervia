package edu.minervia.platform.service.identity.llm

interface LlmService {
    fun generateText(prompt: String): String
}
