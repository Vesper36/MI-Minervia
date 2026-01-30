package edu.minervia.platform.service.identity

import edu.minervia.platform.service.identity.generator.DateGenerator
import edu.minervia.platform.service.identity.generator.NameGenerator
import edu.minervia.platform.service.identity.generator.StudentNumberGenerator
import edu.minervia.platform.service.identity.validator.IdentityValidator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IdentityGenerationService(
    private val nameGenerator: NameGenerator,
    private val dateGenerator: DateGenerator,
    private val studentNumberGenerator: StudentNumberGenerator,
    private val identityValidator: IdentityValidator
) {
    private val logger = LoggerFactory.getLogger(IdentityGenerationService::class.java)
    private val generationVersion = "1.0.0"
    private val maxRetries = 5

    fun generateIdentity(request: IdentityGenerationRequest): GeneratedIdentity {
        var attempts = 0
        var lastErrors: List<String> = emptyList()

        while (attempts < maxRetries) {
            attempts++
            val identity = createIdentity(request)
            val errors = identityValidator.validate(identity)

            if (errors.isEmpty()) {
                logger.info("Identity generated successfully on attempt $attempts")
                return identity
            }

            lastErrors = errors
            logger.warn("Identity validation failed on attempt $attempts: $errors")
        }

        throw IllegalStateException("Failed to generate valid identity after $maxRetries attempts. Errors: $lastErrors")
    }

    private fun createIdentity(request: IdentityGenerationRequest): GeneratedIdentity {
        val enrollmentYear = request.enrollmentYear ?: dateGenerator.getCurrentEnrollmentYear()
        val (firstName, lastName) = nameGenerator.generateName(request.countryCode)
        val birthDate = dateGenerator.generateBirthDate(enrollmentYear)
        val admissionDate = dateGenerator.generateAdmissionDate(enrollmentYear)
        val enrollmentDate = dateGenerator.generateEnrollmentDate(enrollmentYear)
        val studentNumber = studentNumberGenerator.generateStudentNumber(enrollmentYear, request.majorCode)
        val generationSeed = UUID.randomUUID().toString()

        return GeneratedIdentity(
            firstName = firstName,
            lastName = lastName,
            birthDate = birthDate,
            studentNumber = studentNumber,
            admissionDate = admissionDate,
            enrollmentDate = enrollmentDate,
            enrollmentYear = enrollmentYear,
            countryCode = request.countryCode,
            identityType = request.identityType,
            generationSeed = generationSeed,
            generationVersion = generationVersion
        )
    }
}
