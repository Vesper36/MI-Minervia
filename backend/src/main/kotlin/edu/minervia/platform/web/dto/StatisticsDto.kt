package edu.minervia.platform.web.dto

data class DashboardStatsDto(
    val totalStudents: Long,
    val activeStudents: Long,
    val pendingApplications: Long,
    val totalRegistrationCodes: Long
)

data class StudentStatsDetailDto(
    val total: Long,
    val active: Long,
    val suspended: Long,
    val graduated: Long,
    val newLast7Days: Long,
    val newLast30Days: Long
)

data class RegistrationStatsDto(
    val total: Long,
    val pending: Long,
    val approved: Long,
    val rejected: Long,
    val completed: Long
)
