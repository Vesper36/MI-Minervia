package edu.minervia.platform.web.dto

import edu.minervia.platform.domain.enums.IdentityType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class BatchGenerateRequest(
    @field:Min(1)
    @field:Max(100)
    val count: Int = 10,

    @field:NotBlank
    @field:Size(min = 2, max = 5)
    val countryCode: String,

    @field:NotBlank
    @field:Size(min = 2, max = 5)
    val majorCode: String,

    val identityType: IdentityType = IdentityType.LOCAL,

    @field:Min(2000)
    @field:Max(2100)
    val enrollmentYear: Int? = null
)

data class BatchGenerateResponse(
    val requestedCount: Int,
    val generatedCount: Int,
    val failedCount: Int,
    val identities: List<GeneratedIdentityDto>
)

data class GeneratedIdentityDto(
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val studentNumber: String,
    val admissionDate: String,
    val enrollmentDate: String,
    val enrollmentYear: Int,
    val countryCode: String,
    val identityType: IdentityType,
    val familyBackground: String?,
    val interests: String?,
    val academicGoals: String?,
    val semesters: List<SemesterDto>?,
    val gpa: String?,
    val familyInfo: FamilyInfoDto?
)

data class SemesterDto(
    val year: Int,
    val season: String,
    val courses: List<CourseDto>
)

data class CourseDto(
    val name: String,
    val code: String,
    val credits: Int,
    val grade: String
)

data class FamilyInfoDto(
    val fatherName: String,
    val fatherOccupation: String,
    val motherName: String,
    val motherOccupation: String,
    val address: String
)

enum class ExportFormat {
    JSON,
    CSV
}
