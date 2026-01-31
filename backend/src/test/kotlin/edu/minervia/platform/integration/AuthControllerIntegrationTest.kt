package edu.minervia.platform.integration

import edu.minervia.platform.web.dto.LoginRequest
import edu.minervia.platform.web.dto.RefreshTokenRequest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthControllerIntegrationTest : BaseIntegrationTest() {

    @Test
    fun `login with valid credentials returns tokens`() {
        val request = LoginRequest(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD)

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.data.username").value(TEST_ADMIN_USERNAME))
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
    }

    @Test
    fun `login with invalid password returns 401`() {
        val request = LoginRequest(TEST_ADMIN_USERNAME, "wrongpassword")

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `login with non-existent user returns 401`() {
        val request = LoginRequest("nonexistent", "password")

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `refresh token with valid token returns new tokens`() {
        val loginResponse = authService.login(
            LoginRequest(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD)
        )

        val refreshRequest = RefreshTokenRequest(loginResponse.refreshToken)

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(refreshRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty)
    }

    @Test
    fun `refresh token with invalid token returns 401`() {
        val refreshRequest = RefreshTokenRequest("invalid-refresh-token")

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(refreshRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `logout with valid token succeeds`() {
        val accessToken = getAdminAccessToken()

        mockMvc.perform(
            post("/api/auth/logout")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    fun `super admin login returns correct role`() {
        val request = LoginRequest(TEST_SUPER_ADMIN_USERNAME, TEST_SUPER_ADMIN_PASSWORD)

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.role").value("SUPER_ADMIN"))
    }

    @Test
    fun `login with blank username returns 400`() {
        val request = mapOf("username" to "", "password" to TEST_ADMIN_PASSWORD)

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login with blank password returns 400`() {
        val request = mapOf("username" to TEST_ADMIN_USERNAME, "password" to "")

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isBadRequest)
    }
}
