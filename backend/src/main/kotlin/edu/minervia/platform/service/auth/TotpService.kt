package edu.minervia.platform.service.auth

import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.repository.AdminRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

/**
 * Service for TOTP (Time-based One-Time Password) two-factor authentication.
 * Implements RFC 6238 TOTP algorithm.
 */
@Service
class TotpService(
    private val adminRepository: AdminRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SECRET_LENGTH = 20
        private const val CODE_DIGITS = 6
        private const val TIME_STEP_SECONDS = 30L
        private const val ALLOWED_TIME_DRIFT = 1
        private const val ISSUER = "Minervia"
        private val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray()
    }

    /**
     * Generate a new TOTP secret for an admin.
     * Returns the secret and QR code URI.
     */
    fun generateSecret(adminId: Long): TotpSetupResult {
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("Admin not found") }

        if (admin.totpEnabled) {
            throw IllegalStateException("TOTP is already enabled for this admin")
        }

        val secretBytes = ByteArray(SECRET_LENGTH)
        SecureRandom().nextBytes(secretBytes)
        val secret = encodeBase32(secretBytes)

        admin.totpSecret = secret
        admin.updatedAt = Instant.now()
        adminRepository.save(admin)

        val qrCodeUri = generateOtpAuthUri(admin.username, secret)

        log.info("Generated TOTP secret for admin: {}", admin.username)

        return TotpSetupResult(
            secret = secret,
            qrCodeUri = qrCodeUri,
            issuer = ISSUER,
            accountName = admin.username
        )
    }

    /**
     * Verify TOTP code and enable 2FA if valid.
     */
    fun verifyAndEnable(adminId: Long, code: String): Boolean {
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("Admin not found") }

        if (admin.totpEnabled) {
            throw IllegalStateException("TOTP is already enabled")
        }

        val secret = admin.totpSecret
            ?: throw IllegalStateException("TOTP secret not generated")

        if (!verifyCode(secret, code)) {
            log.warn("Invalid TOTP code during setup for admin: {}", admin.username)
            return false
        }

        admin.totpEnabled = true
        admin.updatedAt = Instant.now()
        adminRepository.save(admin)

        log.info("TOTP enabled for admin: {}", admin.username)
        return true
    }

    /**
     * Verify TOTP code for login.
     */
    fun verifyCode(adminId: Long, code: String): Boolean {
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("Admin not found") }

        if (!admin.totpEnabled) {
            return true
        }

        val secret = admin.totpSecret ?: return false
        return verifyCode(secret, code)
    }

    /**
     * Disable TOTP for an admin.
     */
    fun disable(adminId: Long, code: String): Boolean {
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("Admin not found") }

        if (!admin.totpEnabled) {
            return true
        }

        val secret = admin.totpSecret ?: return false

        if (!verifyCode(secret, code)) {
            log.warn("Invalid TOTP code during disable for admin: {}", admin.username)
            return false
        }

        admin.totpEnabled = false
        admin.totpSecret = null
        admin.updatedAt = Instant.now()
        adminRepository.save(admin)

        log.info("TOTP disabled for admin: {}", admin.username)
        return true
    }

    /**
     * Check if TOTP is enabled for an admin.
     */
    fun isTotpEnabled(adminId: Long): Boolean {
        return adminRepository.findById(adminId)
            .map { it.totpEnabled }
            .orElse(false)
    }

    private fun verifyCode(secret: String, code: String): Boolean {
        val currentTime = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS

        for (i in -ALLOWED_TIME_DRIFT..ALLOWED_TIME_DRIFT) {
            val expectedCode = generateCode(secret, currentTime + i)
            if (expectedCode == code) {
                return true
            }
        }
        return false
    }

    private fun generateCode(secret: String, timeCounter: Long): String {
        val secretBytes = decodeBase32(secret)
        val timeBytes = ByteArray(8)
        var time = timeCounter
        for (i in 7 downTo 0) {
            timeBytes[i] = (time and 0xFF).toByte()
            time = time shr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA1"))
        val hash = mac.doFinal(timeBytes)

        val offset = (hash[hash.size - 1] and 0x0F).toInt()
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % 1_000_000
        return otp.toString().padStart(CODE_DIGITS, '0')
    }

    private fun generateOtpAuthUri(accountName: String, secret: String): String {
        return "otpauth://totp/$ISSUER:$accountName?secret=$secret&issuer=$ISSUER&algorithm=SHA1&digits=$CODE_DIGITS&period=$TIME_STEP_SECONDS"
    }

    private fun encodeBase32(data: ByteArray): String {
        val result = StringBuilder()
        var buffer = 0
        var bitsLeft = 0

        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                result.append(BASE32_CHARS[index])
                bitsLeft -= 5
            }
        }

        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            result.append(BASE32_CHARS[index])
        }

        return result.toString()
    }

    private fun decodeBase32(encoded: String): ByteArray {
        val result = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0

        for (char in encoded.uppercase()) {
            val value = BASE32_CHARS.indexOf(char)
            if (value < 0) continue

            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                result.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }

        return result.toByteArray()
    }
}

data class TotpSetupResult(
    val secret: String,
    val qrCodeUri: String,
    val issuer: String,
    val accountName: String
)
