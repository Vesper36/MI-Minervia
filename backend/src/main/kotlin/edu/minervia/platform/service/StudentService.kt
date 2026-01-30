package edu.minervia.platform.service

import edu.minervia.platform.domain.entity.Student
import edu.minervia.platform.domain.entity.StudentFamilyInfo
import edu.minervia.platform.domain.enums.ApplicationStatus
import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.StudentStatus
import edu.minervia.platform.domain.repository.RegistrationApplicationRepository
import edu.minervia.platform.domain.repository.StudentFamilyInfoRepository
import edu.minervia.platform.domain.repository.StudentRepository
import edu.minervia.platform.service.identity.IdentityGenerationRequest
import edu.minervia.platform.service.identity.IdentityGenerationService
import edu.minervia.platform.web.dto.StudentDto
import edu.minervia.platform.web.dto.StudentListDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class StudentService(
    private val studentRepository: StudentRepository,
    private val studentFamilyInfoRepository: StudentFamilyInfoRepository,
    private val registrationApplicationRepository: RegistrationApplicationRepository,
    private val identityGenerationService: IdentityGenerationService,
    private val passwordEncoder: PasswordEncoder
) {
    private val random = SecureRandom()

    @Transactional
    fun createStudentFromApplication(applicationId: Long): StudentDto {
        val application = registrationApplicationRepository.findById(applicationId)
            .orElseThrow { NoSuchElementException("Application not found") }

        if (application.status != ApplicationStatus.GENERATING) {
            throw IllegalStateException("Application is not in GENERATING status")
        }

        val majorCode = getMajorCode(application.majorId)

        val request = IdentityGenerationRequest(
            identityType = application.identityType,
            countryCode = application.countryCode ?: "PL",
            majorCode = majorCode
        )

        val identity = identityGenerationService.generateIdentity(request)
        val eduEmail = "${identity.studentNumber.lowercase()}@minervia.edu"
        val tempPassword = generateTempPassword()

        val student = Student(
            studentNumber = identity.studentNumber,
            eduEmail = eduEmail,
            passwordHash = passwordEncoder.encode(tempPassword),
            application = application,
            firstName = identity.firstName,
            lastName = identity.lastName,
            birthDate = identity.birthDate,
            identityType = identity.identityType,
            countryCode = identity.countryCode,
            majorId = application.majorId,
            classId = application.classId,
            enrollmentYear = identity.enrollmentYear,
            enrollmentDate = identity.enrollmentDate,
            admissionDate = identity.admissionDate,
            generationSeed = identity.generationSeed,
            generationVersion = identity.generationVersion
        )

        val savedStudent = studentRepository.save(student)

        application.status = ApplicationStatus.COMPLETED
        registrationApplicationRepository.save(application)

        // TODO: Send welcome email with temp password

        return savedStudent.toDto()
    }

    fun getStudentById(id: Long): StudentDto {
        return studentRepository.findById(id)
            .orElseThrow { NoSuchElementException("Student not found") }
            .toDto()
    }

    fun getStudentByNumber(studentNumber: String): StudentDto {
        return studentRepository.findByStudentNumber(studentNumber)
            .orElseThrow { NoSuchElementException("Student not found") }
            .toDto()
    }

    fun getAllStudents(pageable: Pageable): Page<StudentListDto> {
        return studentRepository.findAll(pageable).map { it.toListDto() }
    }

    fun getStudentsByStatus(status: StudentStatus, pageable: Pageable): Page<StudentListDto> {
        return studentRepository.findAllByStatus(status, pageable).map { it.toListDto() }
    }

    @Transactional
    fun suspendStudent(id: Long, reason: String): StudentDto {
        val student = studentRepository.findById(id)
            .orElseThrow { NoSuchElementException("Student not found") }

        student.status = StudentStatus.SUSPENDED
        student.suspensionReason = reason

        return studentRepository.save(student).toDto()
    }

    @Transactional
    fun reactivateStudent(id: Long): StudentDto {
        val student = studentRepository.findById(id)
            .orElseThrow { NoSuchElementException("Student not found") }

        student.status = StudentStatus.ACTIVE
        student.suspensionReason = null

        return studentRepository.save(student).toDto()
    }

    fun getStudentStats(): Map<String, Long> {
        return mapOf(
            "total" to studentRepository.count(),
            "active" to studentRepository.countByStatus(StudentStatus.ACTIVE),
            "suspended" to studentRepository.countByStatus(StudentStatus.SUSPENDED),
            "graduated" to studentRepository.countByStatus(StudentStatus.GRADUATED)
        )
    }

    private fun getMajorCode(majorId: Long?): String {
        // TODO: Lookup from major table
        return "CS"
    }

    private fun generateTempPassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#"
        return (1..12).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    private fun Student.toDto() = StudentDto(
        id = this.id,
        studentNumber = this.studentNumber,
        eduEmail = this.eduEmail,
        firstName = this.firstName,
        lastName = this.lastName,
        birthDate = this.birthDate,
        identityType = this.identityType,
        countryCode = this.countryCode,
        majorId = this.majorId,
        classId = this.classId,
        enrollmentYear = this.enrollmentYear,
        enrollmentDate = this.enrollmentDate,
        admissionDate = this.admissionDate,
        gpa = this.gpa,
        status = this.status,
        suspensionReason = this.suspensionReason,
        dailyEmailLimit = this.dailyEmailLimit,
        photoUrl = this.photoUrl,
        familyBackground = this.familyBackground,
        interests = this.interests,
        academicGoals = this.academicGoals,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun Student.toListDto() = StudentListDto(
        id = this.id,
        studentNumber = this.studentNumber,
        eduEmail = this.eduEmail,
        fullName = this.fullName,
        identityType = this.identityType,
        enrollmentYear = this.enrollmentYear,
        status = this.status,
        createdAt = this.createdAt
    )
}
