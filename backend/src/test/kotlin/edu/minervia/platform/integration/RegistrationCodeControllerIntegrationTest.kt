package edu.minervia.platform.integration

import edu.minervia.platform.domain.entity.RegistrationCode
import edu.minervia.platform.domain.enums.RegistrationCodeStatus
import edu.minervia.platform.domain.repository.RegistrationCodeRepository
import edu.minervia.platform.web.dto.BatchGenerateCodeRequest
import edu.minervia.platform.web.dto.GenerateCodeRequest
import edu.minervia.platform.web.dto.VerifyCodeRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit

class RegistrationCodeControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var registrationCodeRepository: RegistrationCodeRepository

    @BeforeEach
    fun setupCodes() {
        registrationCodeRepository.deleteAll()
    }

    @Test
    fun `generate code with admin token succeeds`() {
        val accessToken = getAdminAccessToken()
        val request = GenerateCodeRequest(expirationDays = 30)

        mockMvc.perform(
            post("/api/admin/registration-codes/generate")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.code").isNotEmpty)
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
    }

    @Test
    fun `generate code without token returns 401`() {
        val request = GenerateCodeRequest(expirationDays = 30)

        mockMvc.perform(
            post("/api/admin/registration-codes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `batch generate codes succeeds`() {
        val accessToken = getAdminAccessToken()
        val request = BatchGenerateCodeRequest(count = 5, expirationDays = 30)

        mockMvc.perform(
            post("/api/admin/registration-codes/generate-batch")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(5))
    }

    @Test
    fun `batch generate with count exceeding 100 returns 400`() {
        val accessToken = getAdminAccessToken()
        val request = BatchGenerateCodeRequest(count = 101, expirationDays = 30)

        mockMvc.perform(
            post("/api/admin/registration-codes/generate-batch")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `get all codes returns paginated list`() {
        val accessToken = getAdminAccessToken()
        createTestCodes(3)

        mockMvc.perform(
            get("/api/admin/registration-codes")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.totalElements").value(3))
    }

    @Test
    fun `get codes by status filters correctly`() {
        val accessToken = getAdminAccessToken()
        createTestCodes(3)
        val usedCode = registrationCodeRepository.save(
            RegistrationCode(
                code = "USED-CODE-123",
                status = RegistrationCodeStatus.USED,
                createdBy = testAdmin,
                expiresAt = Instant.now().plus(30, ChronoUnit.DAYS)
            )
        )

        mockMvc.perform(
            get("/api/admin/registration-codes")
                .header("Authorization", "Bearer $accessToken")
                .param("status", "USED")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].status").value("USED"))
    }

    @Test
    fun `get code by id returns code details`() {
        val accessToken = getAdminAccessToken()
        val code = createTestCodes(1).first()

        mockMvc.perform(
            get("/api/admin/registration-codes/${code.id}")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(code.id))
            .andExpect(jsonPath("$.data.code").value(code.code))
    }

    @Test
    fun `revoke code changes status to revoked`() {
        val accessToken = getAdminAccessToken()
        val code = createTestCodes(1).first()

        mockMvc.perform(
            post("/api/admin/registration-codes/${code.id}/revoke")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("REVOKED"))
    }

    @Test
    fun `verify valid code returns true`() {
        val code = createTestCodes(1).first()
        val request = VerifyCodeRequest(code = code.code)

        mockMvc.perform(
            post("/api/public/registration-codes/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.valid").value(true))
    }

    @Test
    fun `verify invalid code returns false`() {
        val request = VerifyCodeRequest(code = "INVALID-CODE")

        mockMvc.perform(
            post("/api/public/registration-codes/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.valid").value(false))
    }

    @Test
    fun `verify expired code returns false`() {
        val expiredCode = registrationCodeRepository.save(
            RegistrationCode(
                code = "EXPIRED-CODE-123",
                status = RegistrationCodeStatus.UNUSED,
                createdBy = testAdmin,
                expiresAt = Instant.now().minus(1, ChronoUnit.DAYS)
            )
        )
        val request = VerifyCodeRequest(code = expiredCode.code)

        mockMvc.perform(
            post("/api/public/registration-codes/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.valid").value(false))
    }

    @Test
    fun `verify revoked code returns false`() {
        val revokedCode = registrationCodeRepository.save(
            RegistrationCode(
                code = "REVOKED-CODE-123",
                status = RegistrationCodeStatus.REVOKED,
                createdBy = testAdmin,
                expiresAt = Instant.now().plus(30, ChronoUnit.DAYS)
            )
        )
        val request = VerifyCodeRequest(code = revokedCode.code)

        mockMvc.perform(
            post("/api/public/registration-codes/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.valid").value(false))
    }

    private fun createTestCodes(count: Int): List<RegistrationCode> {
        return (1..count).map { i ->
            registrationCodeRepository.save(
                RegistrationCode(
                    code = "TEST-CODE-$i-${System.currentTimeMillis()}",
                    status = RegistrationCodeStatus.UNUSED,
                    createdBy = testAdmin,
                    expiresAt = Instant.now().plus(30, ChronoUnit.DAYS)
                )
            )
        }
    }
}
