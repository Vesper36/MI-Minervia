package edu.minervia.platform.web.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import edu.minervia.platform.config.EmailWebhookProperties
import edu.minervia.platform.service.email.EmailBounceService
import edu.minervia.platform.web.dto.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestController
@RequestMapping("/api/webhooks/email")
class BounceWebhookController(
    private val emailBounceService: EmailBounceService,
    private val emailWebhookProperties: EmailWebhookProperties,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(BounceWebhookController::class.java)

    @PostMapping("/bounce")
    fun handleBounce(
        request: HttpServletRequest,
        @RequestBody(required = false) body: String?,
        @RequestParam params: Map<String, String>
    ): ResponseEntity<ApiResponse<String>> {
        val jsonNode = parseJson(body)
        if (!verifySignature(request, body, params, jsonNode)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid webhook signature"))
        }

        val events = parseBounceEvents(jsonNode, params)
        if (events.isEmpty()) {
            logger.warn("No bounce events parsed from webhook")
            return ResponseEntity.ok(ApiResponse.success("No bounce events"))
        }

        events.forEach { event ->
            when (event.type) {
                BounceType.HARD -> emailBounceService.handleHardBounce(event.email, event.reason ?: "Hard bounce")
                BounceType.SOFT -> emailBounceService.handleSoftBounce(event.email, event.reason ?: "Soft bounce")
                BounceType.SPAM -> emailBounceService.handleSpamComplaint(event.email)
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Processed ${events.size} bounce events"))
    }

    private fun parseJson(body: String?): JsonNode? {
        if (body.isNullOrBlank()) return null
        return try {
            objectMapper.readTree(body)
        } catch (ex: Exception) {
            logger.warn("Failed to parse webhook JSON payload", ex)
            null
        }
    }

    private fun parseBounceEvents(jsonNode: JsonNode?, params: Map<String, String>): List<BounceEvent> {
        val events = mutableListOf<BounceEvent>()

        if (jsonNode != null) {
            if (jsonNode.isArray) {
                jsonNode.forEach { eventNode ->
                    parseSendGridEvent(eventNode)?.let { events.add(it) }
                }
            } else {
                parseMailgunEvent(jsonNode)?.let { events.add(it) }
            }
        }

        if (events.isEmpty() && params.isNotEmpty()) {
            parseMailgunParams(params)?.let { events.add(it) }
        }

        return events
    }

    private fun parseSendGridEvent(eventNode: JsonNode): BounceEvent? {
        val email = eventNode.path("email").asText(null) ?: return null
        val eventType = eventNode.path("event").asText("").lowercase()
        val bounceType = when (eventType) {
            "bounce", "blocked", "dropped", "invalid_email" -> BounceType.HARD
            "deferred", "delayed", "soft_bounce" -> BounceType.SOFT
            "spamreport" -> BounceType.SPAM
            else -> null
        } ?: return null

        val reason = eventNode.path("reason").asText(null)
            ?: eventNode.path("status").asText(null)
            ?: eventNode.path("error").asText(null)

        return BounceEvent(email = email, type = bounceType, reason = reason)
    }

    private fun parseMailgunEvent(eventNode: JsonNode): BounceEvent? {
        val dataNode = if (eventNode.has("event-data")) eventNode.path("event-data") else eventNode
        val email = dataNode.path("recipient").asText(null)
            ?: dataNode.path("email").asText(null)
            ?: return null
        val eventType = dataNode.path("event").asText("").lowercase()
        val severity = dataNode.path("severity").asText("").lowercase()

        val bounceType = when {
            eventType == "complained" -> BounceType.SPAM
            eventType == "failed" && severity == "temporary" -> BounceType.SOFT
            eventType == "failed" && severity == "permanent" -> BounceType.HARD
            eventType == "failed" -> BounceType.HARD
            eventType == "rejected" -> BounceType.HARD
            eventType == "bounced" -> BounceType.HARD
            else -> null
        } ?: return null

        val reason = dataNode.path("reason").asText(null)
            ?: dataNode.path("description").asText(null)
            ?: dataNode.path("delivery-status").path("message").asText(null)

        return BounceEvent(email = email, type = bounceType, reason = reason)
    }

    private fun parseMailgunParams(params: Map<String, String>): BounceEvent? {
        val email = params["recipient"] ?: params["email"] ?: return null
        val eventType = params["event"]?.lowercase() ?: return null
        val severity = params["severity"]?.lowercase()

        val bounceType = when {
            eventType == "complained" -> BounceType.SPAM
            eventType == "failed" && severity == "temporary" -> BounceType.SOFT
            eventType == "failed" -> BounceType.HARD
            eventType == "bounced" -> BounceType.HARD
            eventType == "bounce" -> BounceType.HARD
            else -> null
        } ?: return null

        val reason = params["reason"] ?: params["description"] ?: params["message"]
        return BounceEvent(email = email, type = bounceType, reason = reason)
    }

    private fun verifySignature(
        request: HttpServletRequest,
        body: String?,
        params: Map<String, String>,
        jsonNode: JsonNode?
    ): Boolean {
        val sendGridVerified = verifySendGridSignature(request, body)
        if (sendGridVerified) return true

        val mailgunVerified = verifyMailgunSignature(request, params, jsonNode)
        if (mailgunVerified) return true

        val genericHeader = request.getHeader("X-Webhook-Signature")
            ?: request.getHeader("X-Signature")
        if (genericHeader.isNullOrBlank()) {
            return false
        }

        val signingKey = emailWebhookProperties.signingKey
        if (signingKey.isBlank() || body.isNullOrBlank()) {
            return false
        }

        val expected = hmacSha256Hex(signingKey, body)
        return constantTimeEquals(expected, genericHeader)
    }

    private fun verifySendGridSignature(request: HttpServletRequest, body: String?): Boolean {
        val signature = request.getHeader("X-Twilio-Email-Event-Webhook-Signature")
        val timestamp = request.getHeader("X-Twilio-Email-Event-Webhook-Timestamp")
        if (signature.isNullOrBlank() || timestamp.isNullOrBlank()) {
            return false
        }

        val publicKeyPem = emailWebhookProperties.sendgridPublicKey
        if (publicKeyPem.isBlank() || body.isNullOrBlank()) {
            return false
        }

        if (!isTimestampFresh(timestamp)) {
            return false
        }

        return try {
            val publicKey = parsePublicKey(publicKeyPem)
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update((timestamp + body).toByteArray(StandardCharsets.UTF_8))
            val signatureBytes = Base64.getDecoder().decode(signature)
            verifier.verify(signatureBytes)
        } catch (ex: Exception) {
            logger.warn("Failed to verify SendGrid signature", ex)
            false
        }
    }

    private fun verifyMailgunSignature(
        request: HttpServletRequest,
        params: Map<String, String>,
        jsonNode: JsonNode?
    ): Boolean {
        val signatureNode = jsonNode?.path("signature")
        val signature = request.getHeader("X-Mailgun-Signature")
            ?: params["signature"]
            ?: signatureNode?.path("signature")?.asText(null)
            ?: jsonNode?.path("signature")?.asText(null)
        val timestamp = request.getHeader("X-Mailgun-Timestamp")
            ?: params["timestamp"]
            ?: signatureNode?.path("timestamp")?.asText(null)
            ?: jsonNode?.path("timestamp")?.asText(null)
        val token = request.getHeader("X-Mailgun-Token")
            ?: params["token"]
            ?: signatureNode?.path("token")?.asText(null)
            ?: jsonNode?.path("token")?.asText(null)

        if (signature.isNullOrBlank() || timestamp.isNullOrBlank() || token.isNullOrBlank()) {
            return false
        }

        val signingKey = emailWebhookProperties.mailgunSigningKey
        if (signingKey.isBlank()) {
            return false
        }

        if (!isTimestampFresh(timestamp)) {
            return false
        }

        val expected = hmacSha256Hex(signingKey, timestamp + token)
        return constantTimeEquals(expected, signature)
    }

    private fun isTimestampFresh(timestamp: String): Boolean {
        val toleranceSeconds = emailWebhookProperties.signatureToleranceSeconds
        val epoch = timestamp.toLongOrNull() ?: return false
        val nowSeconds = System.currentTimeMillis() / 1000
        return kotlin.math.abs(nowSeconds - epoch) <= toleranceSeconds
    }

    private fun hmacSha256Hex(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val digest = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun parsePublicKey(publicKeyPem: String) =
        KeyFactory.getInstance("EC").generatePublic(
            X509EncodedKeySpec(
                Base64.getDecoder().decode(
                    publicKeyPem
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replace("\\s+".toRegex(), "")
                )
            )
        )

    private fun constantTimeEquals(expected: String, actual: String): Boolean {
        val expectedBytes = expected.lowercase().toByteArray(StandardCharsets.UTF_8)
        val actualBytes = actual.lowercase().toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.isEqual(expectedBytes, actualBytes)
    }

    private data class BounceEvent(
        val email: String,
        val type: BounceType,
        val reason: String?
    )

    private enum class BounceType {
        HARD,
        SOFT,
        SPAM
    }
}
