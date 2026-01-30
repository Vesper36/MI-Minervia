package edu.minervia.platform.service.identity

import com.fasterxml.jackson.databind.ObjectMapper
import edu.minervia.platform.web.dto.ExportFormat
import edu.minervia.platform.web.dto.GeneratedIdentityDto
import org.springframework.stereotype.Service

@Service
class IdentityExportService(
    private val objectMapper: ObjectMapper
) {
    fun export(identities: List<GeneratedIdentityDto>, format: ExportFormat): ByteArray {
        return when (format) {
            ExportFormat.JSON -> exportJson(identities)
            ExportFormat.CSV -> exportCsv(identities)
        }
    }

    fun getContentType(format: ExportFormat): String {
        return when (format) {
            ExportFormat.JSON -> "application/json"
            ExportFormat.CSV -> "text/csv"
        }
    }

    fun getFileExtension(format: ExportFormat): String {
        return when (format) {
            ExportFormat.JSON -> "json"
            ExportFormat.CSV -> "csv"
        }
    }

    private fun exportJson(identities: List<GeneratedIdentityDto>): ByteArray {
        return objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsBytes(identities)
    }

    private fun exportCsv(identities: List<GeneratedIdentityDto>): ByteArray {
        val sb = StringBuilder()
        sb.appendLine(CSV_HEADER)

        for (identity in identities) {
            sb.appendLine(toCsvRow(identity))
        }

        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun toCsvRow(identity: GeneratedIdentityDto): String {
        val totalCredits = identity.semesters?.flatMap { it.courses }?.sumOf { it.credits } ?: 0
        val courseCount = identity.semesters?.flatMap { it.courses }?.size ?: 0

        return listOf(
            escapeCsv(identity.studentNumber),
            escapeCsv(identity.firstName),
            escapeCsv(identity.lastName),
            escapeCsv(identity.birthDate),
            escapeCsv(identity.countryCode),
            escapeCsv(identity.identityType.name),
            escapeCsv(identity.enrollmentYear.toString()),
            escapeCsv(identity.admissionDate),
            escapeCsv(identity.enrollmentDate),
            escapeCsv(identity.gpa ?: ""),
            escapeCsv(totalCredits.toString()),
            escapeCsv(courseCount.toString()),
            escapeCsv(identity.familyInfo?.fatherName ?: ""),
            escapeCsv(identity.familyInfo?.fatherOccupation ?: ""),
            escapeCsv(identity.familyInfo?.motherName ?: ""),
            escapeCsv(identity.familyInfo?.motherOccupation ?: ""),
            escapeCsv(identity.familyInfo?.address ?: ""),
            escapeCsv(identity.familyBackground ?: ""),
            escapeCsv(identity.interests ?: ""),
            escapeCsv(identity.academicGoals ?: "")
        ).joinToString(",")
    }

    private fun escapeCsv(value: String): String {
        val sanitized = sanitizeFormulaInjection(value)
        val needsQuoting = sanitized.contains(',') || sanitized.contains('"') || sanitized.contains('\n')
        return if (needsQuoting) {
            "\"${sanitized.replace("\"", "\"\"")}\""
        } else {
            sanitized
        }
    }

    private fun sanitizeFormulaInjection(value: String): String {
        val formulaPrefixes = setOf('=', '+', '-', '@', '\t', '\r')
        return if (value.isNotEmpty() && value[0] in formulaPrefixes) {
            "'$value"
        } else {
            value
        }
    }

    companion object {
        private const val CSV_HEADER = "student_number,first_name,last_name,birth_date,country_code," +
                "identity_type,enrollment_year,admission_date,enrollment_date,gpa,total_credits," +
                "course_count,father_name,father_occupation,mother_name,mother_occupation,address," +
                "family_background,interests,academic_goals"
    }
}
