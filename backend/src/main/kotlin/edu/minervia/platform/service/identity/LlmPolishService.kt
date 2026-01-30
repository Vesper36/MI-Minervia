package edu.minervia.platform.service.identity

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LlmPolishService(
    private val llmService: LlmService,
    private val fallbackService: LlmFallbackService
) {
    private val logger = LoggerFactory.getLogger(LlmPolishService::class.java)

    fun generateProfile(countryCode: String, majorCode: String): LlmPolishResult {
        val countryName = fallbackService.resolveCountryName(countryCode)
        val majorName = fallbackService.resolveMajorName(majorCode)

        val familyPrompt = buildFamilyPrompt(countryName)
        val interestsPrompt = buildInterestsPrompt(majorName)
        val goalsPrompt = buildAcademicGoalsPrompt(majorName)

        val familyBackground = generateWithFallback(
            field = "familyBackground",
            prompt = familyPrompt,
            fallback = { fallbackService.familyBackground(countryCode) }
        )

        val interests = generateWithFallback(
            field = "interests",
            prompt = interestsPrompt,
            fallback = { fallbackService.interests(majorCode) }
        )

        val academicGoals = generateWithFallback(
            field = "academicGoals",
            prompt = goalsPrompt,
            fallback = { fallbackService.academicGoals(majorCode) }
        )

        return LlmPolishResult(
            familyBackground = familyBackground,
            interests = interests,
            academicGoals = academicGoals
        )
    }

    private fun generateWithFallback(
        field: String,
        prompt: String,
        fallback: () -> String
    ): String {
        return try {
            val response = llmService.generateText(prompt).trim()
            if (response.isBlank()) {
                logger.warn("LLM returned blank text for $field, falling back to templates")
                fallback()
            } else {
                response
            }
        } catch (ex: Exception) {
            logger.warn("LLM failed for $field, falling back to templates: ${ex.message}")
            fallback()
        }
    }

    private fun buildFamilyPrompt(countryName: String): String {
        return "Write 2-3 sentences describing a university student's family background influenced by $countryName culture. " +
            "Keep it realistic and neutral. Do not include the student's name. Output only the text."
    }

    private fun buildInterestsPrompt(majorName: String): String {
        return "Write 1-2 sentences about the student's personal interests related to $majorName. " +
            "Keep it realistic. Do not use bullet points or titles. Output only the text."
    }

    private fun buildAcademicGoalsPrompt(majorName: String): String {
        return "Write 1-2 sentences about the student's academic goals related to $majorName. " +
            "Keep it realistic. Do not use bullet points or titles. Output only the text."
    }
}

data class LlmPolishResult(
    val familyBackground: String,
    val interests: String,
    val academicGoals: String
)
