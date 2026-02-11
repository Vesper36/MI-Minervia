package edu.minervia.platform.integration

import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.enums.AdminRole
import edu.minervia.platform.web.dto.LoginRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuditorPermissionsIntegrationTest : BaseIntegrationTest() {

    private lateinit var testAuditor: Admin
    private lateinit var auditorAccessToken: String

    companion object {
        const val TEST_AUDITOR_USERNAME = "testauditor"
        const val TEST_AUDITOR_PASSWORD = "AuditorPassword123!"
    }

    @BeforeEach
    fun setupAuditor() {
        testAuditor = adminRepository.save(
            Admin(
                username = TEST_AUDITOR_USERNAME,
                passwordHash = passwordEncoder.encode(TEST_AUDITOR_PASSWORD),
                email = "auditor@minervia.edu",
                role = AdminRole.AUDITOR
            )
        )

        val response = authService.login(
            LoginRequest(TEST_AUDITOR_USERNAME, TEST_AUDITOR_PASSWORD)
        )
        auditorAccessToken = response.accessToken
    }

    // Read-only endpoints - AUDITOR should have access

    @Test
    fun `auditor can access audit logs`() {
        mockMvc.perform(
            get("/api/admin/audit-logs")
                .header("Authorization", "Bearer $auditorAccessToken")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `auditor can access statistics`() {
        mockMvc.perform(
            get("/api/admin/statistics/students")
                .header("Authorization", "Bearer $auditorAccessToken")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `auditor can view students list`() {
        mockMvc.perform(
            get("/api/admin/students")
                .header("Authorization", "Bearer $auditorAccessToken")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `auditor can view registration codes`() {
        mockMvc.perform(
            get("/api/admin/codes")
                .header("Authorization", "Bearer $auditorAccessToken")
        )
            .andExpect(status().isOk)
    }

    // Write endpoints - AUDITOR should NOT have access

    @Test
    fun `auditor cannot create registration codes`() {
        val requestBody = """
            {
                "count": 10,
                "expiresInDays": 30,
                "maxUses": 1
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/admin/codes/generate")
                .header("Authorization", "Bearer $auditorAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `auditor cannot revoke registration codes`() {
        mockMvc.perform(
            post("/api/admin/codes/1/revoke")
                .header("Authorization", "Bearer $auditorAccessToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `auditor cannot ban students`() {
        mockMvc.perform(
            post("/api/admin/students/1/ban")
                .header("Authorization", "Bearer $auditorAccessToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `auditor cannot unban students`() {
        mockMvc.perform(
            post("/api/admin/students/1/unban")
                .header("Authorization", "Bearer $auditorAccessToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `auditor cannot approve applications`() {
        mockMvc.perform(
            post("/api/admin/applications/1/approve")
                .header("Authorization", "Bearer $auditorAccessToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `auditor cannot reject applications`() {
        val requestBody = """
            {
                "reason": "Incomplete information"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/admin/applications/1/reject")
                .header("Authorization", "Bearer $auditorAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `auditor cannot update system configs`() {
        val requestBody = """
            {
                "value": "24"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/admin/system-configs/jwt_expiration_hours")
                .header("Authorization", "Bearer $auditorAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `auditor cannot create admin accounts`() {
        val requestBody = """
            {
                "username": "newadmin",
                "email": "newadmin@minervia.edu",
                "password": "Password123!",
                "role": "ADMIN"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/super-admin/admins")
                .header("Authorization", "Bearer $auditorAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `auditor cannot update admin roles`() {
        val requestBody = """
            {
                "role": "SUPER_ADMIN"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/super-admin/admins/1/role")
                .header("Authorization", "Bearer $auditorAccessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
    }
}
