package edu.minervia.platform.service.async

import edu.minervia.platform.domain.entity.RegistrationApplication
import edu.minervia.platform.domain.entity.Student
import edu.minervia.platform.domain.repository.StudentRepository
import edu.minervia.platform.service.MajorService
import edu.minervia.platform.service.identity.IdentityGenerationRequest
import edu.minervia.platform.service.identity.IdentityGenerationService
import edu.minervia.platform.service.identity.PlaceholderPhotoService
import edu.minervia.platform.service.identity.llm.LlmPolishService
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Component
class RegistrationStepExecutor(
    private val identityGenerationService: IdentityGenerationService,
    private val llmPolishService: LlmPolishService,
    private val placeholderPhotoService: PlaceholderPhotoService,
    private val studentRepository: StudentRepository,
    private val majorService: MajorService,
    private val passwordEncoder: PasswordEncoder
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val random = SecureRandom()

    @Transactional
    fun executeIdentityRulesStep(application: RegistrationApplication): Student {
        log.debug("Executing identity rules step for application: {}", application.id)

        val existingStudent = studentRepository.findByApplicationId(application.id)
        if (existingStudent != null) {
            log.info("Student already exists for application {}, skipping identity generation", application.id)
            return existingStudent
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

        return try {
            val savedStudent = studentRepository.save(student)
            log.info("Created student {} for application {}", savedStudent.studentNumber, application.id)
            savedStudent
        } catch (e: DataIntegrityViolationException) {
            log.warn("Concurrent student creation detected for application {}, fetching existing", application.id)
            studentRepository.findByApplicationId(application.id)
                ?: throw IllegalStateException("Student creation failed and no existing student found for application ${application.id}")
        }
    }

    @Transactional
    fun executeLlmPolishStep(application: RegistrationApplication): Student {
        log.debug("Executing LLM polish step for application: {}", application.id)

        val student = findStudentByApplication(application.id)
            ?: throw IllegalStateException("Student not found for application ${application.id}")

        if (student.familyBackground != null && student.interests != null && student.academicGoals != null) {
            log.info("Student {} already has LLM polish data, skipping", student.studentNumber)
            return student
        }

        val majorCode = getMajorCode(application.majorId)
        val result = llmPolishService.generateProfile(
            countryCode = student.countryCode,
            majorCode = majorCode,
            identityType = student.identityType
        )

        student.familyBackground = result.familyBackground
        student.interests = result.interests
        student.academicGoals = result.academicGoals

        val savedStudent = studentRepository.save(student)
        log.info("Applied LLM polish for student {}", savedStudent.studentNumber)
        return savedStudent
    }

    @Transactional
    fun executePhotoGenerationStep(application: RegistrationApplication): Student {
        log.debug("Executing photo generation step for application: {}", application.id)

        val student = findStudentByApplication(application.id)
            ?: throw IllegalStateException("Student not found for application ${application.id}")

        if (student.photoUrl != null) {
            log.info("Student {} already has photo URL, skipping", student.studentNumber)
            return student
        }

        val photoUrl = placeholderPhotoService.generatePhotoUrl(student.firstName, student.lastName)
        student.photoUrl = photoUrl

        val savedStudent = studentRepository.save(student)
        log.info("Generated photo URL for student {}", savedStudent.studentNumber)
        return savedStudent
    }

    private fun findStudentByApplication(applicationId: Long): Student? {
        return studentRepository.findByApplicationId(applicationId)
    }

    private fun getMajorCode(majorId: Long?): String {
        return majorId?.let { majorService.getCodeById(it) } ?: MajorService.DEFAULT_MAJOR_CODE
    }

    private fun generateTempPassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#"
        return (1..12).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}
