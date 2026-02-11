package edu.minervia.platform.web.controller

import edu.minervia.platform.web.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/statistics")
@PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN', 'SUPER_ADMIN')")
class AdminStatisticsController(
    private val statisticsService: StatisticsService
) {

    @GetMapping("/students")
    fun getStudentStatistics(): ResponseEntity<ApiResponse<StudentStatistics>> {
        val stats = statisticsService.getStudentStatistics()
        return ResponseEntity.ok(ApiResponse.success(stats))
    }

    @GetMapping("/registrations")
    fun getRegistrationStatistics(): ResponseEntity<ApiResponse<RegistrationStatistics>> {
        val stats = statisticsService.getRegistrationStatistics()
        return ResponseEntity.ok(ApiResponse.success(stats))
    }

    @GetMapping("/emails")
    fun getEmailStatistics(): ResponseEntity<ApiResponse<EmailStatistics>> {
        val stats = statisticsService.getEmailStatistics()
        return ResponseEntity.ok(ApiResponse.success(stats))
    }
}

data class StudentStatistics(
    val total: Long,
    val active: Long,
    val suspended: Long,
    val graduated: Long
)

data class RegistrationStatistics(
    val pending: Long,
    val approved: Long,
    val rejected: Long
)

data class EmailStatistics(
    val sentToday: Long,
    val sentThisMonth: Long,
    val failedToday: Long
)
