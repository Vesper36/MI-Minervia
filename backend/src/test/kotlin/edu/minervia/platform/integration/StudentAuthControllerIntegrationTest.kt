package edu.minervia.platform.integration

import edu.minervia.platform.domain.entity.Student
import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.StudentStatus
import edu.minervia.platform.domain.repository.StudentRepository
import edu.minervia.platform.web.dto.StudentLoginRequest
import edu.minervia.platform.web.dto.StudentRefreshTokenRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class StudentAuthControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var studentRepository: StudentRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testStudent: Student

    companion object {
        const val TEST_STUDENT_EMAIL = "2025cs0001@minervia.edu"
        const val TEST_STUDENT_NUMBER = "2025CS0001"
        const val TEST_STUDENT_PASSWORD = "StudentPass123!"
    }

    @BeforeEach
    fun setupStudentData() {
        studentRepository.deleteAll()

        testStudent = studentRepository.save(
            Student(
                studentNumber = TEST_STUDENT_NUMBER,
                eduEmail = TEST_STUDENT_EMAIL,
                passwordHash = passwordEncoder.encode(TEST_STUDENT_PASSWORD),
                firstName = "Test",
                lastName = "Student",
                birthDate = LocalDate.of(2000, 1, 15),
                identityType = IdentityType.LOCAL,
                countryCode = "PL",
                enrollmentYear = 2025,
                enrollmentDate = LocalDate.of(2025, 10, 1),
                admissionDate = LocalDate.of(2025, 6, 15),
                status = StudentStatus.ACTIVE
            )
        )
    }

    @Test
    fun `login with valid email returns tokens`() {
        val request = StudentLoginRequest(
            email = TEST_STUDENT_EMAIL,
            password = TEST_STUDENT_PASSWORD
        )

        mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.studentNumber").value(TEST_STUDENT_NUMBER))
            .andExpect(jsonPath("$.fullName").value("Test Student"))
            .andExpect(jsonPath("$.eduEmail").value(TEST_STUDENT_EMAIL))
    }

    @Test
    fun `login with valid student number returns tokens`() {
        val request = StudentLoginRequest(
            email = TEST_STUDENT_NUMBER,
            password = TEST_STUDENT_PASSWORD
        )

        mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.studentNumber").value(TEST_STUDENT_NUMBER))
    }

    @Test
    fun `login with invalid password returns 401`() {
        val request = StudentLoginRequest(
            email = TEST_STUDENT_EMAIL,
            password = "WrongPassword123!"
        )

        mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login with non-existent email returns 401`() {
        val request = StudentLoginRequest(
            email = "nonexistent@minervia.edu",
            password = TEST_STUDENT_PASSWORD
        )

        mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login with inactive student returns 401`() {
        testStudent.status = StudentStatus.SUSPENDED
        studentRepository.save(testStudent)

        val request = StudentLoginRequest(
            email = TEST_STUDENT_EMAIL,
            password = TEST_STUDENT_PASSWORD
        )

        mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh with valid token returns new tokens`() {
        val loginRequest = StudentLoginRequest(
            email = TEST_STUDENT_EMAIL,
            password = TEST_STUDENT_PASSWORD
        )

        val loginResult = mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val loginResponse = objectMapper.readTree(loginResult.response.contentAsString)
        val refreshToken = loginResponse.get("refreshToken").asText()

        val refreshRequest = StudentRefreshTokenRequest(refreshToken = refreshToken)

        mockMvc.perform(
            post("/api/student/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(refreshRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
    }

    @Test
    fun `refresh with invalid token returns 401`() {
        val refreshRequest = StudentRefreshTokenRequest(refreshToken = "invalid.token.here")

        mockMvc.perform(
            post("/api/student/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(refreshRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh with revoked token returns 401`() {
        val loginRequest = StudentLoginRequest(
            email = TEST_STUDENT_EMAIL,
            password = TEST_STUDENT_PASSWORD
        )

        val loginResult = mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val loginResponse = objectMapper.readTree(loginResult.response.contentAsString)
        val refreshToken = loginResponse.get("refreshToken").asText()

        val refreshRequest = StudentRefreshTokenRequest(refreshToken = refreshToken)
        mockMvc.perform(
            post("/api/student/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(refreshRequest))
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/api/student/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(refreshRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `logout with valid token returns 204`() {
        val loginRequest = StudentLoginRequest(
            email = TEST_STUDENT_EMAIL,
            password = TEST_STUDENT_PASSWORD
        )

        val loginResult = mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val loginResponse = objectMapper.readTree(loginResult.response.contentAsString)
        val accessToken = loginResponse.get("accessToken").asText()
        val refreshToken = loginResponse.get("refreshToken").asText()

        mockMvc.perform(
            post("/api/student/auth/logout")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "$refreshToken"}""")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `logout without refresh token still succeeds`() {
        val loginRequest = StudentLoginRequest(
            email = TEST_STUDENT_EMAIL,
            password = TEST_STUDENT_PASSWORD
        )

        val loginResult = mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val loginResponse = objectMapper.readTree(loginResult.response.contentAsString)
        val accessToken = loginResponse.get("accessToken").asText()

        mockMvc.perform(
            post("/api/student/auth/logout")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `login with blank email returns 400`() {
        val request = mapOf(
            "email" to "",
            "password" to TEST_STUDENT_PASSWORD
        )

        mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login with blank password returns 400`() {
        val request = mapOf(
            "email" to TEST_STUDENT_EMAIL,
            "password" to ""
        )

        mockMvc.perform(
            post("/api/student/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)
    }
}
