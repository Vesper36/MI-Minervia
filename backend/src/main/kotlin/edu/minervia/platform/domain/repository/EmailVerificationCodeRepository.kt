package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.EmailVerificationCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional

@Repository
interface EmailVerificationCodeRepository : JpaRepository<EmailVerificationCode, Long> {
    fun findByApplicationIdAndVerifiedAtIsNull(applicationId: Long): Optional<EmailVerificationCode>

    @Query("SELECT c FROM EmailVerificationCode c WHERE c.application.id = :appId AND c.verifiedAt IS NULL AND c.expiresAt > :now ORDER BY c.createdAt DESC")
    fun findValidCodeForApplication(appId: Long, now: Instant): Optional<EmailVerificationCode>

    @Query("SELECT COUNT(c) FROM EmailVerificationCode c WHERE c.application.externalEmail = :email AND c.createdAt > :after")
    fun countByEmailSince(email: String, after: Instant): Long
}
