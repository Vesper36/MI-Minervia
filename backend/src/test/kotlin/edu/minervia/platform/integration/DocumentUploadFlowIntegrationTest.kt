package edu.minervia.platform.integration

import edu.minervia.platform.domain.entity.Student
import edu.minervia.platform.domain.entity.StudentDocument
import edu.minervia.platform.domain.enums.StudentStatus
import edu.minervia.platform.domain.repository.StudentDocumentRepository
import edu.minervia.platform.domain.repository.StudentRepository
import edu.minervia.platform.service.StudentAuthService
import edu.minervia.platform.web.dto.StudentLoginRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class DocumentUploadFlowIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var studentRepository: StudentRepository

    @Autowired
    private lateinit var studentDocumentRepository: StudentDocumentRepository

    @Autowired
    private lateinit var studentAuthService: StudentAuthService

    private lateinit var testStudent: Student
    private lateinit var studentAccessToken: String

    companion object {
        const val TEST_STUDENT_EMAIL = "student@test.com"
        const val TEST_STUDENT_PASSWORD = "StudentPassword123!"
    }

    @BeforeEach
    fun setupStudent() {
        studentRepository.deleteAll()
        studentDocumentRepository.deleteAll()

        testStudent = studentRepository.save(
            Student(
                email = TEST_STUDENT_EMAIL,
                passwordHash = passwordEncoder.encode(TEST_STUDENT_PASSWORD),
                firstName = "Test",
                lastName = "Student",
                status = StudentStatus.ACTIVE
            )
        )

        val response = studentAuthService.login(
            StudentLoginRequest(TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD)
        )
        studentAccessToken = response.accessToken
    }

    @Test
    fun `initialize upload should return presigned URL`() {
        val requestBody = """
            {
                "fileName": "test-document.pdf",
                "contentType": "application/pdf",
                "sizeBytes": 1024
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/student/documents")
                .header("Authorization", "Bearer $studentAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.documentId").exists())
            .andExpect(jsonPath("$.data.uploadUrl").exists())
            .andExpect(jsonPath("$.data.expiresAt").exists())
    }

    @Test
    fun `initialize upload should reject file exceeding size limit`() {
        val requestBody = """
            {
                "fileName": "large-file.pdf",
                "contentType": "application/pdf",
                "sizeBytes": 20971520
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/student/documents")
                .header("Authorization", "Bearer $studentAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `initialize upload should reject disallowed content type`() {
        val requestBody = """
            {
                "fileName": "malware.exe",
                "contentType": "application/x-msdownload",
                "sizeBytes": 1024
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/student/documents")
                .header("Authorization", "Bearer $studentAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `list documents should return paginated list`() {
        mockMvc.perform(
            get("/api/student/documents")
                .header("Authorization", "Bearer $studentAccessToken")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    fun `get download url should return presigned URL for active document`() {
        // Create a test document
        val document = studentDocumentRepository.save(
            StudentDocument(
                studentId = testStudent.id!!,
                objectKey = "test-key",
                bucket = "test-bucket",
                originalFileName = "test.pdf",
                contentType = "application/pdf",
                sizeBytes = 1024,
                status = edu.minervia.platform.domain.entity.DocumentStatus.ACTIVE
            )
        )

        mockMvc.perform(
            get("/api/student/documents/${document.id}/download-url")
                .header("Authorization", "Bearer $studentAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.downloadUrl").exists())
            .andExpect(jsonPath("$.data.expiresAt").exists())
    }

    @Test
    fun `delete document should soft delete document`() {
        // Create a test document
        val document = studentDocumentRepository.save(
            StudentDocument(
                studentId = testStudent.id!!,
                objectKey = "test-key",
                bucket = "test-bucket",
                originalFileName = "test.pdf",
                contentType = "application/pdf",
                sizeBytes = 1024,
                status = edu.minervia.platform.domain.entity.DocumentStatus.ACTIVE
            )
        )

        mockMvc.perform(
            delete("/api/student/documents/${document.id}")
                .header("Authorization", "Bearer $studentAccessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `student cannot access another student's documents`() {
        // Create another student
        val anotherStudent = studentRepository.save(
            Student(
                email = "another@test.com",
                passwordHash = passwordEncoder.encode("Password123!"),
                firstName = "Another",
                lastName = "Student",
                status = StudentStatus.ACTIVE
            )
        )

        // Create document for another student
        val document = studentDocumentRepository.save(
            StudentDocument(
                studentId = anotherStudent.id!!,
                objectKey = "test-key",
                bucket = "test-bucket",
                originalFileName = "test.pdf",
                contentType = "application/pdf",
                sizeBytes = 1024,
                status = edu.minervia.platform.domain.entity.DocumentStatus.ACTIVE
            )
        )

        // Try to access with testStudent's token
        mockMvc.perform(
            get("/api/student/documents/${document.id}/download-url")
                .header("Authorization", "Bearer $studentAccessToken")
        )
            .andExpect(status().isNotFound)
    }
}
