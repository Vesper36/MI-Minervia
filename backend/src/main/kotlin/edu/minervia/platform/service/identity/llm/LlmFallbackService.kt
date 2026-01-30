package edu.minervia.platform.service.identity.llm

import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class LlmFallbackService {

    private val countryNames = mapOf(
        "PL" to "Polish", "CN" to "Chinese", "US" to "American",
        "DE" to "German", "FR" to "French", "ES" to "Spanish",
        "IT" to "Italian", "JP" to "Japanese", "KR" to "Korean",
        "IN" to "Indian", "BR" to "Brazilian", "GB" to "British",
        "UA" to "Ukrainian", "CA" to "Canadian", "MX" to "Mexican"
    )

    private val majorNames = mapOf(
        "CS" to "Computer Science", "EE" to "Electrical Engineering",
        "ME" to "Mechanical Engineering", "CE" to "Civil Engineering",
        "BA" to "Business Administration", "EC" to "Economics",
        "DS" to "Data Science", "IT" to "Information Technology"
    )

    private val familyTemplates = listOf(
        "Raised in a %s household that values education and community. Parents encouraged curiosity and academic effort from an early age.",
        "Coming from a %s cultural background, learned the importance of responsibility and perseverance at home.",
        "Brought up in a %s family that emphasized both practical skills and academic growth."
    )

    private val interestTemplates = listOf(
        "Interest in %s began through hands-on projects and curiosity about how systems work.",
        "Drawn to %s topics that connect theory with real-world applications.",
        "Curiosity about %s motivates learning beyond coursework."
    )

    private val goalTemplates = listOf(
        "Aims to build a strong foundation in %s and apply it to meaningful challenges.",
        "Goal is to develop solid %s expertise and graduate with collaborative problem-solving experience.",
        "Plans to deepen %s knowledge and apply it to practical problems in industry or research."
    )

    fun familyBackground(countryCode: String): String =
        familyTemplates.random().format(resolveCountryName(countryCode))

    fun interests(majorCode: String): String =
        interestTemplates.random().format(resolveMajorName(majorCode))

    fun academicGoals(majorCode: String): String =
        goalTemplates.random().format(resolveMajorName(majorCode))

    fun resolveCountryName(countryCode: String): String =
        countryNames[countryCode.uppercase()] ?: "their home country's"

    fun resolveMajorName(majorCode: String): String =
        majorNames[majorCode.uppercase()] ?: "their field of study"
}
