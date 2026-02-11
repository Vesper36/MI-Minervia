package edu.minervia.platform.web.controller

import edu.minervia.platform.security.StudentPrincipal
import edu.minervia.platform.service.DocumentService
import edu.minervia.platform.web.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/student/documents")
class StudentDocumentController(
    private val documentService: DocumentService
) {

    @PostMapping
    fun initializeUpload(
        @AuthenticationPrincipal principal: StudentPrincipal,
        @RequestBody request: InitUploadRequest
    ): ResponseEntity<ApiResponse<InitUploadResponse>> {
        val response = documentService.initializeUpload(principal.studentId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/{id}/complete")
    fun completeUpload(
        @AuthenticationPrincipal principal: StudentPrincipal,
        @PathVariable id: Long,
        @RequestBody request: CompleteUploadRequest
    ): ResponseEntity<ApiResponse<DocumentDto>> {
        val response = documentService.completeUpload(principal.studentId, id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping
    fun listDocuments(
        @AuthenticationPrincipal principal: StudentPrincipal,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<DocumentDto>>> {
        val response = documentService.listDocuments(principal.studentId, pageable)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{id}/download-url")
    fun getDownloadUrl(
        @AuthenticationPrincipal principal: StudentPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<DownloadUrlResponse>> {
        val response = documentService.getDownloadUrl(principal.studentId, id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{id}")
    fun deleteDocument(
        @AuthenticationPrincipal principal: StudentPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        documentService.deleteDocument(principal.studentId, id)
        return ResponseEntity.ok(ApiResponse.success(Unit))
    }
}
