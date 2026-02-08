package edu.minervia.platform.service.identity

import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.service.identity.generator.FamilyInfoGenerator
import edu.minervia.platform.service.identity.generator.GpaCalculator
import edu.minervia.platform.service.identity.generator.SemesterGenerator
import edu.minervia.platform.service.identity.llm.LlmPolishService
import edu.minervia.platform.web.dto.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class FullGeneratedIdentity(
    val identity: GeneratedIdentity,
    val familyBackground: String?,
    val interests: String?,
    val academicGoals: String?,
    val semesters: List<edu.minervia.platform.service.identity.generator.GeneratedSemester>?,
    val gpa: java.math.BigDecimal?,
    val familyInfo: edu.minervia.platform.service.identity.generator.GeneratedFamilyInfo?
)

@Service
class BatchIdentityGenerationService(
    private val identityGenerationService: IdentityGenerationService,
    private val llmPolishService: LlmPolishService,
    private val semesterGenerator: SemesterGenerator,
    private val gpaCalculator: GpaCalculator,
    private val familyInfoGenerator: FamilyInfoGenerator
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun generateBatch(
        count: Int,
        countryCode: String,
        majorCode: String,
        identityType: IdentityType,
        enrollmentYear: Int?,
        includeAcademicTimeline: Boolean = true,
        includeFamilyInfo: Boolean = true,
        includeLlmPolish: Boolean = true
    ): BatchGenerateResponse {
        val identities = mutableListOf<GeneratedIdentityDto>()
        var failedCount = 0

        repeat(count) { index ->
            try {
                val full = generateFullIdentity(
                    countryCode = countryCode,
                    majorCode = majorCode,
                    identityType = identityType,
                    enrollmentYear = enrollmentYear,
                    includeAcademicTimeline = includeAcademicTimeline,
                    includeFamilyInfo = includeFamilyInfo,
                    includeLlmPolish = includeLlmPolish
                )
                identities.add(toDto(full))
            } catch (ex: Exception) {
                failedCount++
                logger.warn("Failed to generate identity {}/{}: {}", index + 1, count, ex.message)
            }
        }

        return BatchGenerateResponse(
            requestedCount = count,
            generatedCount = identities.size,
            failedCount = failedCount,
            identities = identities
        )
    }

    fun generateFullIdentity(
        countryCode: String,
        majorCode: String,
        identityType: IdentityType,
        enrollmentYear: Int?,
        includeAcademicTimeline: Boolean = true,
        includeFamilyInfo: Boolean = true,
        includeLlmPolish: Boolean = true
    ): FullGeneratedIdentity {
        val request = IdentityGenerationRequest(
            identityType = identityType,
            countryCode = countryCode,
            majorCode = majorCode,
            enrollmentYear = enrollmentYear
        )
        val identity = identityGenerationService.generateIdentity(request)

        val polishResult = if (includeLlmPolish) {
            try {
                llmPolishService.generateProfile(countryCode, majorCode, identityType)
            } catch (ex: Exception) {
                logger.warn("LLM polish failed: {}", ex.message)
                null
            }
        } else null

        val semesters = if (includeAcademicTimeline) {
            semesterGenerator.generateSemesters(identity.enrollmentYear, majorCode)
        } else null

        val gpa = semesters?.let { gpaCalculator.calculateGpaFromSemesters(it) }

        val familyInfo = if (includeFamilyInfo) {
            familyInfoGenerator.generateFamilyInfo(countryCode, identity.lastName)
        } else null

        return FullGeneratedIdentity(
            identity = identity,
            familyBackground = polishResult?.familyBackground,
            interests = polishResult?.interests,
            academicGoals = polishResult?.academicGoals,
            semesters = semesters,
            gpa = gpa,
            familyInfo = familyInfo
        )
    }

    private fun toDto(full: FullGeneratedIdentity): GeneratedIdentityDto {
        return GeneratedIdentityDto(
            firstName = full.identity.firstName,
            lastName = full.identity.lastName,
            birthDate = full.identity.birthDate.toString(),
            studentNumber = full.identity.studentNumber,
            admissionDate = full.identity.admissionDate.toString(),
            enrollmentDate = full.identity.enrollmentDate.toString(),
            enrollmentYear = full.identity.enrollmentYear,
            countryCode = full.identity.countryCode,
            identityType = full.identity.identityType,
            familyBackground = full.familyBackground,
            interests = full.interests,
            academicGoals = full.academicGoals,
            semesters = full.semesters?.map { sem ->
                SemesterDto(
                    year = sem.year,
                    season = sem.season,
                    courses = sem.courses.map { c ->
                        CourseDto(
                            name = c.name,
                            code = c.code,
                            credits = c.credits,
                            grade = c.grade.toPlainString()
                        )
                    }
                )
            },
            gpa = full.gpa?.toPlainString(),
            familyInfo = full.familyInfo?.let {
                FamilyInfoDto(
                    fatherName = it.fatherName,
                    fatherOccupation = it.fatherOccupation,
                    motherName = it.motherName,
                    motherOccupation = it.motherOccupation,
                    address = it.address
                )
            }
        )
    }
}
