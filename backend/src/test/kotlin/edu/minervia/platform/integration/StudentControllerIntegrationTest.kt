package edu.minervia.platform.integration

import edu.minervia.platform.domain.entity.Student
import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.StudentStatus
import edu.minervia.platform.domain.repository.StudentRepository
import edu.minervia.platform.web.dto.CreateStudentRequest
import edu.minervia.platform.web.dto.SuspendStudentRequest
import edu.minervia.platform.web.dto.UpdateStudentRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class StudentControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var studentRepository: StudentRepository

    @BeforeEach
    fun setupStudents() {
        studentRepository.deleteAll()
    }

    @Test
    fun `get all students returns paginated list`() {
        val accessToken = getAdminAccessToken()
        createTestStudents(3)

        mockMvc.perform(
            get("/api/admin/students")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.totalElements").value(3))
    }

    @Test
    fun `get students without token returns 401`() {
        mockMvc.perform(get("/api/admin/students"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `get student by id returns student details`() {
        val accessToken = getAdminAccessToken()
        val student = createTestStudents(1).first()

        mockMvc.perform(
            get("/api/admin/students/${student.id}")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(student.id))
            .andExpect(jsonPath("$.data.studentNumber").value(student.studentNumber))
    }

    @Test
    fun `get student by student number returns student`() {
        val accessToken = getAdminAccessToken()
        val student = createTestStudents(1).first()

        mockMvc.perform(
            get("/api/admin/students/by-number/${student.studentNumber}")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.studentNumber").value(student.studentNumber))
    }

    @Test
    fun `create student with valid data succeeds`() {
        val accessToken = getAdminAccessToken()
        val request = CreateStudentRequest(
            firstName = "John",
            lastName = "Doe",
            birthDate = "2000-01-15",
            identityType = IdentityType.LOCAL,
            countryCode = "PL",
            enrollmentYear = 2024
        )

        mockMvc.perform(
            post("/api/admin/students")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.firstName").value("John"))
            .andExpect(jsonPath("$.data.lastName").value("Doe"))
    }

    @Test
    fun `create student with invalid birth date format returns 400`() {
        val accessToken = getAdminAccessToken()
        val request = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "birthDate" to "01-15-2000",
            "countryCode" to "PL"
        )

        mockMvc.perform(
            post("/api/admin/students")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update student succeeds`() {
        val accessToken = getAdminAccessToken()
        val student = createTestStudents(1).first()
        val request = UpdateStudentRequest(
            firstName = "UpdatedName",
            dailyEmailLimit = 5
        )

        mockMvc.perform(
            patch("/api/admin/students/${student.id}")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.firstName").value("UpdatedName"))
            .andExpect(jsonPath("$.data.dailyEmailLimit").value(5))
    }

    @Test
    fun `suspend student changes status`() {
        val accessToken = getAdminAccessToken()
        val student = createTestStudents(1).first()
        val request = SuspendStudentRequest(reason = "Academic misconduct")

        mockMvc.perform(
            post("/api/admin/students/${student.id}/suspend")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("SUSPENDED"))
            .andExpect(jsonPath("$.data.suspensionReason").value("Academic misconduct"))
    }

    @Test
    fun `reactivate student changes status back to active`() {
        val accessToken = getAdminAccessToken()
        val student = createTestStudents(1).first()
        student.status = StudentStatus.SUSPENDED
        student.suspensionReason = "Test suspension"
        studentRepository.save(student)

        mockMvc.perform(
            post("/api/admin/students/${student.id}/reactivate")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
    }

    @Test
    fun `search students by status filters correctly`() {
        val accessToken = getAdminAccessToken()
        val students = createTestStudents(3)
        students[0].status = StudentStatus.SUSPENDED
        studentRepository.save(students[0])

        mockMvc.perform(
            get("/api/admin/students")
                .header("Authorization", "Bearer $accessToken")
                .param("status", "SUSPENDED")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))
    }

    @Test
    fun `search students by identity type filters correctly`() {
        val accessToken = getAdminAccessToken()
        createTestStudents(2)
        val intlStudent = studentRepository.save(
            Student(
                studentNumber = "2024INT001",
                eduEmail = "intl@minervia.edu",
                passwordHash = passwordEncoder.encode("password"),
                firstName = "International",
                lastName = "Student",
                birthDate = LocalDate.of(2000, 1, 1),
                identityType = IdentityType.INTERNATIONAL,
                countryCode = "US",
                enrollmentYear = 2024,
                enrollmentDate = LocalDate.now(),
                admissionDate = LocalDate.now().minusMonths(1)
            )
        )

        mockMvc.perform(
            get("/api/admin/students")
                .header("Authorization", "Bearer $accessToken")
                .param("identityType", "INTERNATIONAL")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].identityType").value("INTERNATIONAL"))
    }

    @Test
    fun `get student stats returns statistics`() {
        val accessToken = getAdminAccessToken()
        val students = createTestStudents(3)
        students[0].status = StudentStatus.SUSPENDED
        studentRepository.save(students[0])

        mockMvc.perform(
            get("/api/admin/students/stats")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.total").value(3))
    }

    private fun createTestStudents(count: Int): List<Student> {
        return (1..count).map { i ->
            studentRepository.save(
                Student(
                    studentNumber = "2024CS${String.format("%03d", i)}",
                    eduEmail = "student$i@minervia.edu",
                    passwordHash = passwordEncoder.encode("password"),
                    firstName = "Test$i",
                    lastName = "Student",
                    birthDate = LocalDate.of(2000, 1, i.coerceIn(1, 28)),
                    identityType = IdentityType.LOCAL,
                    countryCode = "PL",
                    enrollmentYear = 2024,
                    enrollmentDate = LocalDate.now(),
                    admissionDate = LocalDate.now().minusMonths(1)
                )
            )
        }
    }
}
