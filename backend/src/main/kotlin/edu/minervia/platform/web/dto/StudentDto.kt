package edu.minervia.platform.web.dto

import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.StudentStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class StudentDto(
    val id: Long,
    val studentNumber: String,
    val eduEmail: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val identityType: IdentityType,
    val countryCode: String,
    val majorId: Long?,
    val classId: Long?,
    val enrollmentYear: Int,
    val enrollmentDate: LocalDate,
    val admissionDate: LocalDate,
    val gpa: BigDecimal?,
    val status: StudentStatus,
    val suspensionReason: String?,
    val dailyEmailLimit: Int,
    val photoUrl: String?,
    val familyBackground: String?,
    val interests: String?,
    val academicGoals: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class StudentListDto(
    val id: Long,
    val studentNumber: String,
    val eduEmail: String,
    val fullName: String,
    val identityType: IdentityType,
    val enrollmentYear: Int,
    val status: StudentStatus,
    val createdAt: Instant
)

data class SuspendStudentRequest(
    val reason: String
)

data class StudentStatsDto(
    val total: Long,
    val active: Long,
    val suspended: Long,
    val graduated: Long
)

data class CreateStudentRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val firstName: String,

    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val lastName: String,

    @field:NotBlank
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in yyyy-MM-dd format")
    val birthDate: String,

    val identityType: IdentityType = IdentityType.LOCAL,

    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{2,3}$", message = "Country code must be 2-3 uppercase letters")
    val countryCode: String,

    val majorId: Long? = null,

    val classId: Long? = null,

    @field:Min(2000)
    val enrollmentYear: Int? = null,

    @field:Min(1)
    val dailyEmailLimit: Int = 1
)

data class UpdateStudentRequest(
    @field:Size(min = 1, max = 100)
    val firstName: String? = null,

    @field:Size(min = 1, max = 100)
    val lastName: String? = null,

    val majorId: Long? = null,

    val classId: Long? = null,

    @field:Min(1)
    val dailyEmailLimit: Int? = null,

    @field:Size(max = 500)
    @field:Pattern(regexp = "^(https?://.*)?$", message = "Photo URL must be a valid URL")
    val photoUrl: String? = null,

    @field:Size(max = 2000)
    val familyBackground: String? = null,

    @field:Size(max = 1000)
    val interests: String? = null,

    @field:Size(max = 1000)
    val academicGoals: String? = null
)

data class StudentSearchCriteria(
    val query: String? = null,
    val status: StudentStatus? = null,
    val identityType: IdentityType? = null,
    val enrollmentYear: Int? = null,
    val countryCode: String? = null
)
