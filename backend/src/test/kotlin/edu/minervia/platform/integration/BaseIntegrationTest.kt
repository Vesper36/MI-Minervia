package edu.minervia.platform.integration

import com.fasterxml.jackson.databind.ObjectMapper
import edu.minervia.platform.config.EmbeddedRedisConfig
import edu.minervia.platform.config.TestR2Config
import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.enums.AdminRole
import edu.minervia.platform.domain.repository.AdminRepository
import edu.minervia.platform.service.AuthService
import edu.minervia.platform.web.dto.LoginRequest
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestR2Config::class, EmbeddedRedisConfig::class)
abstract class BaseIntegrationTest {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var adminRepository: AdminRepository

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    protected lateinit var authService: AuthService

    protected lateinit var testAdmin: Admin
    protected lateinit var testSuperAdmin: Admin

    companion object {
        const val TEST_ADMIN_USERNAME = "testadmin"
        const val TEST_ADMIN_PASSWORD = "TestPassword123!"
        const val TEST_SUPER_ADMIN_USERNAME = "testsuperadmin"
        const val TEST_SUPER_ADMIN_PASSWORD = "SuperPassword123!"
    }

    @BeforeEach
    fun setupTestData() {
        adminRepository.deleteAll()

        testAdmin = adminRepository.save(
            Admin(
                username = TEST_ADMIN_USERNAME,
                passwordHash = passwordEncoder.encode(TEST_ADMIN_PASSWORD),
                email = "testadmin@minervia.edu",
                role = AdminRole.ADMIN
            )
        )

        testSuperAdmin = adminRepository.save(
            Admin(
                username = TEST_SUPER_ADMIN_USERNAME,
                passwordHash = passwordEncoder.encode(TEST_SUPER_ADMIN_PASSWORD),
                email = "superadmin@minervia.edu",
                role = AdminRole.SUPER_ADMIN
            )
        )
    }

    protected fun getAdminAccessToken(): String {
        val response = authService.login(
            LoginRequest(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD)
        )
        return response.accessToken
    }

    protected fun getSuperAdminAccessToken(): String {
        val response = authService.login(
            LoginRequest(TEST_SUPER_ADMIN_USERNAME, TEST_SUPER_ADMIN_PASSWORD)
        )
        return response.accessToken
    }

    protected fun <T> toJson(obj: T): String = objectMapper.writeValueAsString(obj)
}
