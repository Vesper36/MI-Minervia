package edu.minervia.platform.service.identity

import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class LlmFallbackService {
    private val random = SecureRandom()

    private val countryNames = mapOf(
        "PL" to "Poland",
        "CN" to "China",
        "US" to "United States",
        "DE" to "Germany",
        "FR" to "France",
        "ES" to "Spain",
        "IT" to "Italy",
        "JP" to "Japan",
        "KR" to "South Korea",
        "IN" to "India",
        "BR" to "Brazil",
        "MX" to "Mexico",
        "CA" to "Canada",
        "GB" to "United Kingdom",
        "UA" to "Ukraine"
    )

    private val majorNames = mapOf(
        "CS" to "Computer Science",
        "EE" to "Electrical Engineering",
        "ME" to "Mechanical Engineering",
        "CE" to "Civil Engineering",
        "BA" to "Business Administration",
        "EC" to "Economics",
        "DS" to "Data Science",
        "IT" to "Information Technology",
        "GE" to "General Studies"
    )

    private val familyBackgroundTemplates = listOf(
        "Raised in a %s household that values education and community, the student grew up in a supportive, close-knit family. Their parents encouraged curiosity and steady academic effort from an early age.",
        "Coming from a %s cultural background, the student learned the importance of responsibility and perseverance at home. Family traditions and a focus on learning shaped their outlook through secondary school.",
        "The student was brought up in a %s family that emphasized both practical skills and academic growth. This environment fostered a balanced, hardworking approach to new challenges."
    )

    private val interestTemplates = listOf(
        "Their interest in %s began through hands-on projects and a curiosity about how systems work. They enjoy practical problem solving and keeping up with new developments in the field.",
        "They are especially drawn to %s topics that connect theory with real-world applications. Outside class, they explore tools and ideas that deepen their understanding.",
        "Curiosity about %s motivates them to learn beyond coursework, including reading articles and experimenting with small personal projects."
    )

    private val academicGoalTemplates = listOf(
        "They aim to build a strong foundation in %s and apply it to meaningful, real-world challenges. In the long term, they hope to contribute to impactful projects in their specialty.",
        "Their academic goal is to develop solid %s expertise and graduate with experience in collaborative problem solving. They plan to pursue internships that align with this focus.",
        "They plan to deepen their %s knowledge and use it to solve practical problems in industry or research settings."
    )

    fun familyBackground(countryCode: String): String {
        val countryName = resolveCountryName(countryCode)
        val template = pickTemplate(familyBackgroundTemplates)
        return template.format(countryName)
    }

    fun interests(majorCode: String): String {
        val majorName = resolveMajorName(majorCode)
        val template = pickTemplate(interestTemplates)
        return template.format(majorName)
    }

    fun academicGoals(majorCode: String): String {
        val majorName = resolveMajorName(majorCode)
        val template = pickTemplate(academicGoalTemplates)
        return template.format(majorName)
    }

    fun resolveCountryName(countryCode: String): String {
        return countryNames[countryCode.uppercase()] ?: "their home country"
    }

    fun resolveMajorName(majorCode: String): String {
        return majorNames[majorCode.uppercase()] ?: "their major"
    }

    private fun pickTemplate(templates: List<String>): String {
        return templates[random.nextInt(templates.size)]
    }
}
