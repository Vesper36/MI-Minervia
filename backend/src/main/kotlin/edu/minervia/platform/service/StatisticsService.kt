package edu.minervia.platform.service

import edu.minervia.platform.domain.enums.ApplicationStatus
import edu.minervia.platform.domain.enums.StudentStatus
import edu.minervia.platform.domain.repository.RegistrationApplicationRepository
import edu.minervia.platform.domain.repository.RegistrationCodeRepository
import edu.minervia.platform.domain.repository.StudentRepository
import edu.minervia.platform.web.dto.DashboardStatsDto
import edu.minervia.platform.web.dto.RegistrationStatsDto
import edu.minervia.platform.web.dto.StudentStatsDetailDto
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class StatisticsService(
    private val studentRepository: StudentRepository,
    private val registrationApplicationRepository: RegistrationApplicationRepository,
    private val registrationCodeRepository: RegistrationCodeRepository
) {
    fun getDashboardStats(): DashboardStatsDto {
        val totalStudents = studentRepository.count()
        val activeStudents = studentRepository.countByStatus(StudentStatus.ACTIVE)
        val pendingApplications = registrationApplicationRepository
            .findAllByStatus(ApplicationStatus.PENDING_APPROVAL, org.springframework.data.domain.Pageable.unpaged())
            .totalElements

        return DashboardStatsDto(
            totalStudents = totalStudents,
            activeStudents = activeStudents,
            pendingApplications = pendingApplications,
            totalRegistrationCodes = registrationCodeRepository.count()
        )
    }

    fun getStudentStats(): StudentStatsDetailDto {
        val total = studentRepository.count()
        val active = studentRepository.countByStatus(StudentStatus.ACTIVE)
        val suspended = studentRepository.countByStatus(StudentStatus.SUSPENDED)
        val graduated = studentRepository.countByStatus(StudentStatus.GRADUATED)

        val last7Days = studentRepository.findAll()
            .count { it.createdAt.isAfter(Instant.now().minus(7, ChronoUnit.DAYS)) }
            .toLong()

        val last30Days = studentRepository.findAll()
            .count { it.createdAt.isAfter(Instant.now().minus(30, ChronoUnit.DAYS)) }
            .toLong()

        return StudentStatsDetailDto(
            total = total,
            active = active,
            suspended = suspended,
            graduated = graduated,
            newLast7Days = last7Days,
            newLast30Days = last30Days
        )
    }

    fun getRegistrationStats(): RegistrationStatsDto {
        val total = registrationApplicationRepository.count()
        val pending = registrationApplicationRepository
            .findAllByStatus(ApplicationStatus.PENDING_APPROVAL, org.springframework.data.domain.Pageable.unpaged())
            .totalElements
        val approved = registrationApplicationRepository
            .findAllByStatus(ApplicationStatus.APPROVED, org.springframework.data.domain.Pageable.unpaged())
            .totalElements
        val rejected = registrationApplicationRepository
            .findAllByStatus(ApplicationStatus.REJECTED, org.springframework.data.domain.Pageable.unpaged())
            .totalElements
        val completed = registrationApplicationRepository
            .findAllByStatus(ApplicationStatus.COMPLETED, org.springframework.data.domain.Pageable.unpaged())
            .totalElements

        return RegistrationStatsDto(
            total = total,
            pending = pending,
            approved = approved,
            rejected = rejected,
            completed = completed
        )
    }
}
