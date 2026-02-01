package edu.minervia.platform.integration

import edu.minervia.platform.domain.entity.Major
import edu.minervia.platform.domain.entity.RegistrationApplication
import edu.minervia.platform.domain.entity.RegistrationCode
import edu.minervia.platform.domain.enums.ApplicationStatus
import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.RegistrationCodeStatus
import edu.minervia.platform.domain.repository.MajorRepository
import edu.minervia.platform.domain.repository.RegistrationApplicationRepository
import edu.minervia.platform.domain.repository.RegistrationCodeRepository
import edu.minervia.platform.domain.repository.StudentRepository
import edu.minervia.platform.service.async.RegistrationStepExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit

class RegistrationStepExecutorIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var registrationStepExecutor: RegistrationStepExecutor

    @Autowired
    private lateinit var studentRepository: StudentRepository

    @Autowired
    private lateinit var registrationApplicationRepository: RegistrationApplicationRepository

    @Autowired
    private lateinit var registrationCodeRepository: RegistrationCodeRepository

    @Autowired
    private lateinit var majorRepository: MajorRepository

    private lateinit var testApplication: RegistrationApplication
    private lateinit var testMajor: Major

    @BeforeEach
    fun setupApplicationData() {
        studentRepository.deleteAll()
        registrationApplicationRepository.deleteAll()
        registrationCodeRepository.deleteAll()

        testMajor = majorRepository.findByCode("CS").orElseGet {
            majorRepository.save(Major(code = "CS", name = "Computer Science"))
        }

        val registrationCode = registrationCodeRepository.save(
            RegistrationCode(
                code = "TEST-CODE-${System.currentTimeMillis()}",
                createdBy = testAdmin,
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS),
                status = RegistrationCodeStatus.USED
            )
        )

        testApplication = registrationApplicationRepository.save(
            RegistrationApplication(
                registrationCode = registrationCode,
                externalEmail = "applicant@example.com",
                emailVerified = true,
                identityType = IdentityType.LOCAL,
                countryCode = "PL",
                majorId = testMajor.id,
                status = ApplicationStatus.PENDING_APPROVAL
            )
        )
    }

    @Test
    fun `executeIdentityRulesStep creates student with valid identity`() {
        val student = registrationStepExecutor.executeIdentityRulesStep(testApplication)

        assertNotNull(student.id)
        assertNotNull(student.studentNumber)
        assertNotNull(student.eduEmail)
        assertNotNull(student.passwordHash)
        assertNotNull(student.firstName)
        assertNotNull(student.lastName)
        assertNotNull(student.birthDate)
        assertEquals(testApplication.identityType, student.identityType)
        assertEquals(testApplication.countryCode, student.countryCode)
        assertEquals(testApplication.majorId, student.majorId)

        val savedStudent = studentRepository.findByApplicationId(testApplication.id)
        assertNotNull(savedStudent)
        assertEquals(student.studentNumber, savedStudent?.studentNumber)
    }

    @Test
    fun `executeIdentityRulesStep is idempotent`() {
        val student1 = registrationStepExecutor.executeIdentityRulesStep(testApplication)
        val student2 = registrationStepExecutor.executeIdentityRulesStep(testApplication)

        assertEquals(student1.id, student2.id)
        assertEquals(student1.studentNumber, student2.studentNumber)

        val count = studentRepository.findAll().count { it.application?.id == testApplication.id }
        assertEquals(1, count)
    }

    @Test
    fun `executeLlmPolishStep adds profile data to student`() {
        val student = registrationStepExecutor.executeIdentityRulesStep(testApplication)
        assertNull(student.familyBackground)
        assertNull(student.interests)
        assertNull(student.academicGoals)

        val polishedStudent = registrationStepExecutor.executeLlmPolishStep(testApplication)

        assertNotNull(polishedStudent.familyBackground)
        assertNotNull(polishedStudent.interests)
        assertNotNull(polishedStudent.academicGoals)
    }

    @Test
    fun `executeLlmPolishStep is idempotent`() {
        registrationStepExecutor.executeIdentityRulesStep(testApplication)

        val polished1 = registrationStepExecutor.executeLlmPolishStep(testApplication)
        val originalBackground = polished1.familyBackground

        val polished2 = registrationStepExecutor.executeLlmPolishStep(testApplication)

        assertEquals(originalBackground, polished2.familyBackground)
    }

    @Test
    fun `executeLlmPolishStep throws when student not found`() {
        val orphanCode = registrationCodeRepository.save(
            RegistrationCode(
                code = "ORPHAN-CODE-${System.currentTimeMillis()}",
                createdBy = testAdmin,
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS),
                status = RegistrationCodeStatus.USED
            )
        )

        val orphanApplication = registrationApplicationRepository.save(
            RegistrationApplication(
                registrationCode = orphanCode,
                externalEmail = "orphan@example.com",
                emailVerified = true,
                identityType = IdentityType.LOCAL,
                countryCode = "PL",
                status = ApplicationStatus.PENDING_APPROVAL
            )
        )

        assertThrows(IllegalStateException::class.java) {
            registrationStepExecutor.executeLlmPolishStep(orphanApplication)
        }
    }

    @Test
    fun `executePhotoGenerationStep adds photo URL to student`() {
        val student = registrationStepExecutor.executeIdentityRulesStep(testApplication)
        assertNull(student.photoUrl)

        val studentWithPhoto = registrationStepExecutor.executePhotoGenerationStep(testApplication)

        assertNotNull(studentWithPhoto.photoUrl)
    }

    @Test
    fun `executePhotoGenerationStep is idempotent`() {
        registrationStepExecutor.executeIdentityRulesStep(testApplication)

        val withPhoto1 = registrationStepExecutor.executePhotoGenerationStep(testApplication)
        val originalUrl = withPhoto1.photoUrl

        val withPhoto2 = registrationStepExecutor.executePhotoGenerationStep(testApplication)

        assertEquals(originalUrl, withPhoto2.photoUrl)
    }

    @Test
    fun `executePhotoGenerationStep throws when student not found`() {
        val orphanCode = registrationCodeRepository.save(
            RegistrationCode(
                code = "ORPHAN-PHOTO-${System.currentTimeMillis()}",
                createdBy = testAdmin,
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS),
                status = RegistrationCodeStatus.USED
            )
        )

        val orphanApplication = registrationApplicationRepository.save(
            RegistrationApplication(
                registrationCode = orphanCode,
                externalEmail = "orphan-photo@example.com",
                emailVerified = true,
                identityType = IdentityType.LOCAL,
                countryCode = "PL",
                status = ApplicationStatus.PENDING_APPROVAL
            )
        )

        assertThrows(IllegalStateException::class.java) {
            registrationStepExecutor.executePhotoGenerationStep(orphanApplication)
        }
    }

    @Test
    fun `full step sequence executes in correct order`() {
        val student = registrationStepExecutor.executeIdentityRulesStep(testApplication)
        assertNotNull(student.studentNumber)
        assertNull(student.familyBackground)
        assertNull(student.photoUrl)

        val polishedStudent = registrationStepExecutor.executeLlmPolishStep(testApplication)
        assertNotNull(polishedStudent.familyBackground)
        assertNull(polishedStudent.photoUrl)

        val finalStudent = registrationStepExecutor.executePhotoGenerationStep(testApplication)
        assertNotNull(finalStudent.familyBackground)
        assertNotNull(finalStudent.photoUrl)

        val savedStudent = studentRepository.findByApplicationId(testApplication.id)
        assertNotNull(savedStudent)
        assertNotNull(savedStudent?.familyBackground)
        assertNotNull(savedStudent?.photoUrl)
    }

    @Test
    fun `executeIdentityRulesStep generates valid student number format`() {
        val student = registrationStepExecutor.executeIdentityRulesStep(testApplication)

        val studentNumber = student.studentNumber
        val regex = Regex("^\\d{4}[A-Z]{2}\\d{4}$")
        assert(studentNumber.matches(regex)) {
            "Student number $studentNumber does not match expected format YYYYXX0000"
        }
    }

    @Test
    fun `executeIdentityRulesStep generates valid edu email`() {
        val student = registrationStepExecutor.executeIdentityRulesStep(testApplication)

        val eduEmail = student.eduEmail
        assert(eduEmail.endsWith("@minervia.edu")) {
            "EDU email $eduEmail does not end with @minervia.edu"
        }
        assert(eduEmail.contains(student.studentNumber.lowercase())) {
            "EDU email $eduEmail does not contain student number"
        }
    }
}
