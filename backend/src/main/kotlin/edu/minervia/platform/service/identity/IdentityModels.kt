package edu.minervia.platform.service.identity

import edu.minervia.platform.domain.enums.IdentityType
import java.time.LocalDate

data class GeneratedIdentity(
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val studentNumber: String,
    val admissionDate: LocalDate,
    val enrollmentDate: LocalDate,
    val enrollmentYear: Int,
    val countryCode: String,
    val identityType: IdentityType,
    val generationSeed: String,
    val generationVersion: String
)

data class IdentityGenerationRequest(
    val identityType: IdentityType,
    val countryCode: String,
    val majorCode: String,
    val enrollmentYear: Int? = null
)
