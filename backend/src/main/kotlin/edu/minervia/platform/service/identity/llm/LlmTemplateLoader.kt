package edu.minervia.platform.service.identity.llm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import edu.minervia.platform.domain.enums.IdentityType
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import java.io.File

data class LlmTemplate(
    val familyBackground: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val academicGoals: List<String> = emptyList()
)

@Service
class LlmTemplateLoader {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    private val templates = mutableMapOf<String, LlmTemplate>()
    private val resourceResolver = PathMatchingResourcePatternResolver()

    companion object {
        val REQUIRED_NATIONALITIES = setOf("PL", "CN", "US", "DE")
        val REQUIRED_MAJORS = setOf("CS", "BA", "ENG", "MED")
        val REQUIRED_IDENTITY_TYPES = IdentityType.entries.map { it.name }.toSet()
    }

    @PostConstruct
    fun loadTemplates() {
        loadFromFileSystem()
        loadFromClasspath()
        validateRequiredTemplates()
        logger.info("Loaded {} LLM fallback templates", templates.size)
    }

    private fun loadFromFileSystem() {
        val configDir = File("config/llm-templates")
        if (configDir.exists() && configDir.isDirectory) {
            configDir.walkTopDown()
                .filter { it.isFile && it.extension == "yaml" }
                .forEach { file ->
                    try {
                        val relativePath = file.relativeTo(configDir).path
                        val parts = relativePath.split(File.separator)
                        if (parts.size >= 3) {
                            val key = buildKey(parts[0], parts[1], parts[2].removeSuffix(".yaml"))
                            val template = yamlMapper.readValue(file, LlmTemplate::class.java)
                            templates[key] = template
                            logger.debug("Loaded template from file: {}", key)
                        }
                    } catch (ex: Exception) {
                        logger.warn("Failed to load template from {}: {}", file.path, ex.message)
                    }
                }
        }
    }

    private fun loadFromClasspath() {
        val resources = resourceResolver.getResources("classpath:llm-templates/**/*.yaml")
        for (resource in resources) {
            try {
                val path = resource.url.path
                val parts = path.substringAfter("llm-templates/").split("/")
                if (parts.size >= 3) {
                    val key = buildKey(parts[0], parts[1], parts[2].removeSuffix(".yaml"))
                    if (!templates.containsKey(key)) {
                        val template = yamlMapper.readValue(resource.inputStream, LlmTemplate::class.java)
                        templates[key] = template
                        logger.debug("Loaded template from classpath: {}", key)
                    }
                }
            } catch (ex: Exception) {
                logger.warn("Failed to load template from {}: {}", resource.filename, ex.message)
            }
        }
    }

    private fun validateRequiredTemplates() {
        val missingTemplates = mutableListOf<String>()
        for (nationality in REQUIRED_NATIONALITIES) {
            for (major in REQUIRED_MAJORS) {
                for (identityType in REQUIRED_IDENTITY_TYPES) {
                    val key = buildKey(nationality, major, identityType)
                    if (!templates.containsKey(key)) {
                        missingTemplates.add(key)
                    }
                }
            }
        }
        if (missingTemplates.isNotEmpty()) {
            logger.error(
                "Missing required LLM templates (PBT-23/24/25 violation): {}",
                missingTemplates.joinToString(", ")
            )
        }
    }

    private fun buildKey(nationality: String, major: String, identityType: String): String {
        return "${nationality.uppercase()}/${major.uppercase()}/${identityType.uppercase()}"
    }

    fun getTemplate(countryCode: String, majorCode: String, identityType: IdentityType): LlmTemplate? {
        val key = buildKey(countryCode, majorCode, identityType.name)
        return templates[key]
    }

    fun getFamilyBackground(countryCode: String, majorCode: String, identityType: IdentityType): String? {
        return getTemplate(countryCode, majorCode, identityType)
            ?.familyBackground
            ?.randomOrNull()
    }

    fun getInterests(countryCode: String, majorCode: String, identityType: IdentityType): String? {
        return getTemplate(countryCode, majorCode, identityType)
            ?.interests
            ?.randomOrNull()
    }

    fun getAcademicGoals(countryCode: String, majorCode: String, identityType: IdentityType): String? {
        return getTemplate(countryCode, majorCode, identityType)
            ?.academicGoals
            ?.randomOrNull()
    }

    fun hasTemplate(countryCode: String, majorCode: String, identityType: IdentityType): Boolean {
        val key = buildKey(countryCode, majorCode, identityType.name)
        return templates.containsKey(key)
    }

    fun getSupportedNationalities(): Set<String> {
        return templates.keys.map { it.split("/")[0] }.toSet()
    }

    fun getSupportedMajors(): Set<String> {
        return templates.keys.map { it.split("/")[1] }.toSet()
    }
}
