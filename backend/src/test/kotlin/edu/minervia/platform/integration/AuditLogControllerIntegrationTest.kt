package edu.minervia.platform.integration

import edu.minervia.platform.domain.entity.AuditLog
import edu.minervia.platform.domain.enums.AuditAction
import edu.minervia.platform.domain.enums.AuditEntityType
import edu.minervia.platform.domain.repository.AuditLogRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

class AuditLogControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @BeforeEach
    fun setupAuditLogs() {
        auditLogRepository.deleteAll()
    }

    @Test
    fun `get audit logs returns paginated list`() {
        val accessToken = getAdminAccessToken()
        createTestAuditLogs(5)

        mockMvc.perform(
            get("/api/admin/audit-logs")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.totalElements").value(5))
    }

    @Test
    fun `get audit logs without token returns 401`() {
        mockMvc.perform(get("/api/admin/audit-logs"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `filter audit logs by action type`() {
        val accessToken = getAdminAccessToken()
        createTestAuditLogs(3)
        auditLogRepository.save(
            AuditLog(
                action = AuditAction.DELETE,
                entityType = AuditEntityType.STUDENT,
                entityId = "999",
                actorId = testAdmin.id,
                actorUsername = testAdmin.username,
                details = "Deleted student",
                ipAddress = "127.0.0.1"
            )
        )

        mockMvc.perform(
            get("/api/admin/audit-logs")
                .header("Authorization", "Bearer $accessToken")
                .param("action", "DELETE")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].action").value("DELETE"))
    }

    @Test
    fun `filter audit logs by entity type`() {
        val accessToken = getAdminAccessToken()
        createTestAuditLogs(2)
        auditLogRepository.save(
            AuditLog(
                action = AuditAction.CREATE,
                entityType = AuditEntityType.REGISTRATION_CODE,
                entityId = "CODE-123",
                actorId = testAdmin.id,
                actorUsername = testAdmin.username,
                details = "Created registration code",
                ipAddress = "127.0.0.1"
            )
        )

        mockMvc.perform(
            get("/api/admin/audit-logs")
                .header("Authorization", "Bearer $accessToken")
                .param("entityType", "REGISTRATION_CODE")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))
    }

    @Test
    fun `filter audit logs by actor username`() {
        val accessToken = getAdminAccessToken()
        createTestAuditLogs(2)
        auditLogRepository.save(
            AuditLog(
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.STUDENT,
                entityId = "1",
                actorId = testSuperAdmin.id,
                actorUsername = testSuperAdmin.username,
                details = "Updated by super admin",
                ipAddress = "127.0.0.1"
            )
        )

        mockMvc.perform(
            get("/api/admin/audit-logs")
                .header("Authorization", "Bearer $accessToken")
                .param("actorUsername", TEST_SUPER_ADMIN_USERNAME)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))
    }

    @Test
    fun `filter audit logs by date range`() {
        val accessToken = getAdminAccessToken()
        val now = Instant.now()

        auditLogRepository.save(
            AuditLog(
                action = AuditAction.CREATE,
                entityType = AuditEntityType.STUDENT,
                entityId = "1",
                actorId = testAdmin.id,
                actorUsername = testAdmin.username,
                details = "Recent log",
                ipAddress = "127.0.0.1"
            )
        )

        mockMvc.perform(
            get("/api/admin/audit-logs")
                .header("Authorization", "Bearer $accessToken")
                .param("startDate", now.minusSeconds(3600).toString())
                .param("endDate", now.plusSeconds(3600).toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))
    }

    @Test
    fun `export audit logs as JSON`() {
        val accessToken = getAdminAccessToken()
        createTestAuditLogs(3)

        mockMvc.perform(
            get("/api/admin/audit-logs/export")
                .header("Authorization", "Bearer $accessToken")
                .param("format", "json")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    private fun createTestAuditLogs(count: Int): List<AuditLog> {
        return (1..count).map { i ->
            auditLogRepository.save(
                AuditLog(
                    action = AuditAction.CREATE,
                    entityType = AuditEntityType.STUDENT,
                    entityId = i.toString(),
                    actorId = testAdmin.id,
                    actorUsername = testAdmin.username,
                    details = "Test audit log $i",
                    ipAddress = "127.0.0.1"
                )
            )
        }
    }
}
