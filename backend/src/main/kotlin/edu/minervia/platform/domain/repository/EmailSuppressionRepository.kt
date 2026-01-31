package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.EmailSuppression
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface EmailSuppressionRepository : JpaRepository<EmailSuppression, Long> {
    fun findByEmail(email: String): Optional<EmailSuppression>
    fun findAllBySuppressedAtIsNotNull(pageable: Pageable): Page<EmailSuppression>
}
