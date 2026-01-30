package edu.minervia.platform.web.controller

import edu.minervia.platform.service.StatisticsService
import edu.minervia.platform.web.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
class StatisticsController(
    private val statisticsService: StatisticsService
) {
    @GetMapping("/dashboard")
    fun getDashboardStats(): ResponseEntity<ApiResponse<DashboardStatsDto>> {
        val stats = statisticsService.getDashboardStats()
        return ResponseEntity.ok(ApiResponse.success(stats))
    }

    @GetMapping("/students")
    fun getStudentStats(): ResponseEntity<ApiResponse<StudentStatsDetailDto>> {
        val stats = statisticsService.getStudentStats()
        return ResponseEntity.ok(ApiResponse.success(stats))
    }

    @GetMapping("/registrations")
    fun getRegistrationStats(): ResponseEntity<ApiResponse<RegistrationStatsDto>> {
        val stats = statisticsService.getRegistrationStats()
        return ResponseEntity.ok(ApiResponse.success(stats))
    }
}
