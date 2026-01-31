package edu.minervia.platform.service.audit

interface AlertNotificationService {
    fun sendAlert(
        alertType: AlertType,
        severity: AlertSeverity,
        message: String,
        actorId: Long? = null,
        actorUsername: String? = null
    ): AlertNotificationResult
}

data class AlertNotificationResult(
    val success: Boolean,
    val notificationId: Long? = null,
    val recipientCount: Int = 0,
    val errorMessage: String? = null,
    val deduplicated: Boolean = false
)
