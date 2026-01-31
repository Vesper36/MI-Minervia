package edu.minervia.platform.web.dto

import edu.minervia.platform.domain.enums.EmailSuppressionReason
import java.time.Instant

data class EmailSuppressionDto(
    val id: Long,
    val email: String,
    val reason: EmailSuppressionReason?,
    val bounceCount: Int,
    val firstBounceAt: Instant?,
    val lastBounceAt: Instant?,
    val suppressedAt: Instant?,
    val createdAt: Instant
)
