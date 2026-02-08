package edu.minervia.platform.web.controller

import edu.minervia.platform.service.email.EmailBounceService
import edu.minervia.platform.web.dto.ApiResponse
import edu.minervia.platform.web.dto.EmailSuppressionDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/email-suppressions")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
class EmailSuppressionAdminController(
    private val emailBounceService: EmailBounceService
) {

    @GetMapping
    fun getSuppressedEmails(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<EmailSuppressionDto>>> {
        val page = emailBounceService.getSuppressedEmails(pageable)
            .map { suppression ->
                EmailSuppressionDto(
                    id = suppression.id,
                    email = suppression.email,
                    reason = suppression.reason,
                    bounceCount = suppression.bounceCount,
                    firstBounceAt = suppression.firstBounceAt,
                    lastBounceAt = suppression.lastBounceAt,
                    suppressedAt = suppression.suppressedAt,
                    createdAt = suppression.createdAt
                )
            }
        return ResponseEntity.ok(ApiResponse.success(page))
    }

    @DeleteMapping("/{id}")
    fun unsuppressEmail(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        emailBounceService.unsuppressById(id)
        return ResponseEntity.ok(ApiResponse.success(Unit, "Email unsuppressed"))
    }
}
