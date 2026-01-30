package edu.minervia.platform.web.dto

import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.StudentStatus
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
