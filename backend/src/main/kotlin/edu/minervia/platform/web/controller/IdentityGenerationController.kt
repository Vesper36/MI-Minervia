package edu.minervia.platform.web.controller

import edu.minervia.platform.service.identity.BatchIdentityGenerationService
import edu.minervia.platform.service.identity.IdentityExportService
import edu.minervia.platform.web.dto.BatchGenerateRequest
import edu.minervia.platform.web.dto.BatchGenerateResponse
import edu.minervia.platform.web.dto.ExportFormat
import edu.minervia.platform.web.dto.GeneratedIdentityDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/admin/identity")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Validated
class IdentityGenerationController(
    private val batchGenerationService: BatchIdentityGenerationService,
    private val exportService: IdentityExportService
) {
    @PostMapping("/generate")
    fun generateBatch(
        @Valid @RequestBody request: BatchGenerateRequest,
        @RequestParam(defaultValue = "true") includeTimeline: Boolean,
        @RequestParam(defaultValue = "true") includeFamilyInfo: Boolean,
        @RequestParam(defaultValue = "true") includeLlmPolish: Boolean
    ): ResponseEntity<BatchGenerateResponse> {
        val response = batchGenerationService.generateBatch(
            count = request.count,
            countryCode = request.countryCode,
            majorCode = request.majorCode,
            identityType = request.identityType,
            enrollmentYear = request.enrollmentYear,
            includeAcademicTimeline = includeTimeline,
            includeFamilyInfo = includeFamilyInfo,
            includeLlmPolish = includeLlmPolish
        )

        return if (response.generatedCount == 0 && response.requestedCount > 0) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        } else {
            ResponseEntity.ok(response)
        }
    }

    @PostMapping("/generate/export")
    fun generateAndExport(
        @Valid @RequestBody request: BatchGenerateRequest,
        @RequestParam(defaultValue = "JSON") format: ExportFormat,
        @RequestParam(defaultValue = "true") includeTimeline: Boolean,
        @RequestParam(defaultValue = "true") includeFamilyInfo: Boolean,
        @RequestParam(defaultValue = "true") includeLlmPolish: Boolean
    ): ResponseEntity<ByteArray> {
        val response = batchGenerationService.generateBatch(
            count = request.count,
            countryCode = request.countryCode,
            majorCode = request.majorCode,
            identityType = request.identityType,
            enrollmentYear = request.enrollmentYear,
            includeAcademicTimeline = includeTimeline,
            includeFamilyInfo = includeFamilyInfo,
            includeLlmPolish = includeLlmPolish
        )

        if (response.generatedCount == 0 && response.requestedCount > 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }

        val bytes = exportService.export(response.identities, format)
        val contentType = exportService.getContentType(format)
        val extension = exportService.getFileExtension(format)
        val filename = "identities_${Instant.now().epochSecond}.$extension"

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(bytes)
    }

    @PostMapping("/export")
    fun exportIdentities(
        @RequestBody @Size(max = 500, message = "Maximum 500 identities allowed per export")
        identities: List<@Valid GeneratedIdentityDto>,
        @RequestParam(defaultValue = "JSON") format: ExportFormat
    ): ResponseEntity<ByteArray> {
        val bytes = exportService.export(identities, format)
        val contentType = exportService.getContentType(format)
        val extension = exportService.getFileExtension(format)
        val filename = "identities_${Instant.now().epochSecond}.$extension"

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(bytes)
    }
}
