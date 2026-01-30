package edu.minervia.platform.service.identity

interface LlmService {
    fun generateText(prompt: String): String
}
