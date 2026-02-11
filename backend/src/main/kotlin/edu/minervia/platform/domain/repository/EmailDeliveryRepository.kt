package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.EmailDelivery
import edu.minervia.platform.domain.entity.EmailDeliveryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EmailDeliveryRepository : JpaRepository<EmailDelivery, Long> {

    fun findByDedupeKey(dedupeKey: String): EmailDelivery?

    fun findByStatusAndNextAttemptAtBefore(
        status: EmailDeliveryStatus,
        time: LocalDateTime
    ): List<EmailDelivery>
}
