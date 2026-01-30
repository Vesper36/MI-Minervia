package edu.minervia.platform.service.identity.llm

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class LlmPolishResult(
    val familyBackground: String,
    val interests: String,
    val academicGoals: String
)

@Service
class LlmPolishService(
    private val llmService: LlmService,
    private val fallbackService: LlmFallbackService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun generateProfile(countryCode: String, majorCode: String): LlmPolishResult {
        val countryName = fallbackService.resolveCountryName(countryCode)
        val majorName = fallbackService.resolveMajorName(majorCode)

        return LlmPolishResult(
            familyBackground = generate(
                "familyBackground",
                "Write 2-3 sentences describing a university student's family background influenced by $countryName culture. Keep it realistic and neutral. Output only the text."
            ) { fallbackService.familyBackground(countryCode) },
            interests = generate(
                "interests",
                "Write 1-2 sentences about the student's personal interests related to $majorName. Keep it realistic. Output only the text."
            ) { fallbackService.interests(majorCode) },
            academicGoals = generate(
                "academicGoals",
                "Write 1-2 sentences about the student's academic goals related to $majorName. Keep it realistic. Output only the text."
            ) { fallbackService.academicGoals(majorCode) }
        )
    }

    private fun generate(field: String, prompt: String, fallback: () -> String): String {
        return try {
            val result = llmService.generateText(prompt).trim()
            if (result.isBlank()) {
                logger.warn("LLM returned blank for {}, using fallback", field)
                fallback()
            } else result
        } catch (ex: Exception) {
            logger.warn("LLM failed for {}: {}, using fallback", field, ex.message)
            fallback()
        }
    }
}
