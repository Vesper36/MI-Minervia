package edu.minervia.platform.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Value("\${server.port:8080}")
    private var serverPort: Int = 8080

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(apiInfo())
            .servers(listOf(
                Server()
                    .url("http://localhost:$serverPort")
                    .description("Development Server"),
                Server()
                    .url("https://api.minervia.edu")
                    .description("Production Server")
            ))
            .components(
                Components()
                    .addSecuritySchemes("bearerAuth", securityScheme())
            )
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
            .tags(apiTags())
    }

    private fun apiInfo(): Info {
        return Info()
            .title("Minervia Institute Education Platform API")
            .description("""
                RESTful API for the Minervia Institute Education Platform.

                ## Authentication
                Most endpoints require JWT authentication. Use the `/api/auth/login` endpoint to obtain tokens.

                ## Rate Limiting
                - Email verification: 5 requests per hour
                - Registration: 3 requests per hour per IP
                - Email per address: 1 request per 24 hours

                ## Error Responses
                All error responses follow the standard ApiResponse format with `success: false`.
            """.trimIndent())
            .version("1.0.0")
            .contact(
                Contact()
                    .name("Minervia Institute")
                    .email("support@minervia.edu")
            )
            .license(
                License()
                    .name("Proprietary")
            )
    }

    private fun securityScheme(): SecurityScheme {
        return SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT Access Token. Obtain via POST /api/auth/login")
    }

    private fun apiTags(): List<Tag> {
        return listOf(
            Tag().name("Authentication").description("Login, logout, and token management"),
            Tag().name("Registration Codes").description("Registration code generation and verification"),
            Tag().name("Registration").description("Student registration flow"),
            Tag().name("Students").description("Student management operations"),
            Tag().name("Audit Logs").description("System audit log queries"),
            Tag().name("Statistics").description("Dashboard statistics"),
            Tag().name("System Config").description("System configuration management"),
            Tag().name("Email Suppression").description("Email bounce and suppression management"),
            Tag().name("Identity Generation").description("Student identity generation"),
            Tag().name("OAuth").description("OAuth integration (Linux.do)"),
            Tag().name("TOTP").description("Two-factor authentication management"),
            Tag().name("Webhooks").description("External webhook handlers")
        )
    }
}
