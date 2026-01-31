package edu.minervia.platform.service.email

import java.time.Instant

data class EmailDeliveryResult(
    val success: Boolean,
    val messageId: String? = null,
    val errorMessage: String? = null,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun success(messageId: String) = EmailDeliveryResult(
            success = true,
            messageId = messageId
        )

        fun failure(errorMessage: String) = EmailDeliveryResult(
            success = false,
            errorMessage = errorMessage
        )

        fun skipped(reason: String) = EmailDeliveryResult(
            success = true,
            errorMessage = reason
        )
    }
}
