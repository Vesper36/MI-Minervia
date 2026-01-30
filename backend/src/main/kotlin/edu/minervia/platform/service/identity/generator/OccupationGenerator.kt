package edu.minervia.platform.service.identity.generator

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class OccupationGenerator {
    private val random = SecureRandom()

    private val occupations = listOf(
        "Teacher",
        "Engineer",
        "Accountant",
        "Nurse",
        "Doctor",
        "Pharmacist",
        "Software Developer",
        "Electrician",
        "Mechanic",
        "Architect",
        "Sales Manager",
        "Marketing Specialist",
        "Civil Servant",
        "Police Officer",
        "Small Business Owner",
        "Logistics Coordinator",
        "Financial Analyst",
        "HR Specialist",
        "Researcher",
        "Chef",
        "Journalist",
        "Graphic Designer"
    )

    fun generateOccupation(): String = occupations[random.nextInt(occupations.size)]
}
