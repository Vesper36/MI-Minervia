package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.RegistrationApplication
import edu.minervia.platform.domain.enums.ApplicationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional

@Repository
interface RegistrationApplicationRepository : JpaRepository<RegistrationApplication, Long> {
    fun findByExternalEmail(email: String): Optional<RegistrationApplication>
    fun findAllByStatus(status: ApplicationStatus, pageable: Pageable): Page<RegistrationApplication>
    fun existsByExternalEmailAndCreatedAtAfter(email: String, after: Instant): Boolean

    @Query("SELECT a FROM RegistrationApplication a WHERE a.status = 'PENDING_APPROVAL' ORDER BY a.createdAt ASC")
    fun findPendingApplications(pageable: Pageable): Page<RegistrationApplication>

    @Query("SELECT COUNT(a) FROM RegistrationApplication a WHERE a.ipAddress = :ip AND a.createdAt > :after")
    fun countByIpAddressSince(ip: String, after: Instant): Long

    @Query("SELECT a FROM RegistrationApplication a WHERE a.status = :status AND a.updatedAt < :cutoff")
    fun findByStatusAndUpdatedAtBefore(status: ApplicationStatus, cutoff: Instant): List<RegistrationApplication>
}
