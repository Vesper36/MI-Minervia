package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.RegistrationCode
import edu.minervia.platform.domain.enums.RegistrationCodeStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional

@Repository
interface RegistrationCodeRepository : JpaRepository<RegistrationCode, Long> {
    fun findByCode(code: String): Optional<RegistrationCode>
    fun existsByCode(code: String): Boolean
    fun findAllByStatus(status: RegistrationCodeStatus, pageable: Pageable): Page<RegistrationCode>
    fun findAllByCreatedById(adminId: Long, pageable: Pageable): Page<RegistrationCode>

    @Query("SELECT c FROM RegistrationCode c WHERE c.status = :status AND c.expiresAt < :now")
    fun findExpiredCodes(status: RegistrationCodeStatus, now: Instant): List<RegistrationCode>

    @Modifying
    @Query("UPDATE RegistrationCode c SET c.status = 'EXPIRED' WHERE c.status = 'UNUSED' AND c.expiresAt < :now")
    fun markExpiredCodes(now: Instant): Int

    @Modifying
    @Query("UPDATE RegistrationCode c SET c.status = 'USED', c.usedAt = :usedAt WHERE c.id = :id AND c.status = 'UNUSED'")
    fun claimCode(id: Long, usedAt: Instant): Int
}
