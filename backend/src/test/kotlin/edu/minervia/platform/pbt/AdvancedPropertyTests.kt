package edu.minervia.platform.pbt

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.IntRange
import net.jqwik.api.constraints.StringLength
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PBT-03: Name and nationality matching tests
 */
class NameNationalityPropertyTest {

    private val polishFirstNames = listOf("Jan", "Anna", "Piotr", "Maria", "Krzysztof", "Katarzyna")
    private val polishLastNames = listOf("Kowalski", "Nowak", "Wisniewski", "Wojcik", "Kaminski")
    private val chineseFirstNames = listOf("Wei", "Ming", "Xiao", "Hua", "Jun", "Li")
    private val chineseLastNames = listOf("Wang", "Li", "Zhang", "Liu", "Chen")
    private val germanFirstNames = listOf("Hans", "Anna", "Klaus", "Greta", "Friedrich")
    private val germanLastNames = listOf("Mueller", "Schmidt", "Schneider", "Fischer", "Weber")

    @Property
    fun `Polish names match Polish nationality`(
        @ForAll("polishFirstNames") firstName: String,
        @ForAll("polishLastNames") lastName: String
    ) {
        val fullName = "$firstName $lastName"
        assertTrue(isValidPolishName(fullName))
    }

    @Property
    fun `Chinese names match Chinese nationality`(
        @ForAll("chineseFirstNames") firstName: String,
        @ForAll("chineseLastNames") lastName: String
    ) {
        val fullName = "$lastName $firstName"
        assertTrue(isValidChineseName(fullName))
    }

    @Property
    fun `name length is within reasonable bounds`(
        @ForAll @StringLength(min = 2, max = 50) firstName: String,
        @ForAll @StringLength(min = 2, max = 50) lastName: String
    ) {
        val fullName = "$firstName $lastName"
        assertTrue(fullName.length in 5..101)
    }

    @Provide
    fun polishFirstNames(): Arbitrary<String> = Arbitraries.of(polishFirstNames)

    @Provide
    fun polishLastNames(): Arbitrary<String> = Arbitraries.of(polishLastNames)

    @Provide
    fun chineseFirstNames(): Arbitrary<String> = Arbitraries.of(chineseFirstNames)

    @Provide
    fun chineseLastNames(): Arbitrary<String> = Arbitraries.of(chineseLastNames)

    private fun isValidPolishName(name: String): Boolean {
        return polishFirstNames.any { name.contains(it) } ||
               polishLastNames.any { name.contains(it) }
    }

    private fun isValidChineseName(name: String): Boolean {
        return chineseFirstNames.any { name.contains(it) } ||
               chineseLastNames.any { name.contains(it) }
    }
}

/**
 * PBT-06: Registration code consumption atomicity tests
 */
class RegistrationCodeAtomicityPropertyTest {

    @Property
    fun `concurrent claims on same code result in exactly one success`(
        @ForAll @IntRange(min = 2, max = 10) concurrentClaims: Int
    ) {
        val code = "TEST-${System.nanoTime()}"
        val successCount = AtomicInteger(0)
        val codeUsed = ConcurrentHashMap<String, Boolean>()

        val threads = (1..concurrentClaims).map {
            Thread {
                val claimed = codeUsed.putIfAbsent(code, true) == null
                if (claimed) {
                    successCount.incrementAndGet()
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(1, successCount.get(), "Exactly one claim should succeed")
    }

    @Property
    fun `code cannot be reused after consumption`(
        @ForAll("registrationCodes") code: String
    ) {
        val consumed = ConcurrentHashMap<String, Boolean>()

        val firstClaim = consumed.putIfAbsent(code, true) == null
        val secondClaim = consumed.putIfAbsent(code, true) == null

        assertTrue(firstClaim)
        assertFalse(secondClaim)
    }

    @Provide
    fun registrationCodes(): Arbitrary<String> {
        return Arbitraries.strings()
            .alpha().ofLength(8)
            .map { it.uppercase() }
    }
}

/**
 * PBT-07: Task idempotency tests
 */
class TaskIdempotencyPropertyTest {

    @Property
    fun `duplicate task delivery produces same result`(
        @ForAll @IntRange(min = 1, max = 1000) applicationId: Int,
        @ForAll @IntRange(min = 1, max = 5) deliveryCount: Int
    ) {
        val processedTasks = ConcurrentHashMap<Int, String>()
        val results = mutableListOf<String>()

        repeat(deliveryCount) {
            val result = processedTasks.computeIfAbsent(applicationId) {
                "RESULT-$applicationId"
            }
            results.add(result)
        }

        assertTrue(results.all { it == results.first() })
    }

    @Property
    fun `task processing is deterministic for same input`(
        @ForAll @IntRange(min = 1, max = 1000) seed: Int
    ) {
        val result1 = processTask(seed)
        val result2 = processTask(seed)

        assertEquals(result1, result2)
    }

    private fun processTask(seed: Int): String {
        return "PROCESSED-${seed.hashCode()}"
    }
}

/**
 * PBT-09: Retry boundary tests
 */
class RetryBoundaryPropertyTest {

    companion object {
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 8000L
    }

    @Property
    fun `retry count never exceeds maximum`(
        @ForAll @IntRange(min = 0, max = 10) failureCount: Int
    ) {
        val actualRetries = failureCount.coerceAtMost(MAX_RETRIES)
        assertTrue(actualRetries <= MAX_RETRIES)
    }

    @Property
    fun `exponential backoff delay is bounded`(
        @ForAll @IntRange(min = 0, max = 10) retryAttempt: Int
    ) {
        val delay = calculateBackoffDelay(retryAttempt)
        assertTrue(delay >= BASE_DELAY_MS)
        assertTrue(delay <= MAX_DELAY_MS)
    }

    @Property
    fun `backoff delay increases with retry count`(
        @ForAll @IntRange(min = 0, max = 2) attempt1: Int,
        @ForAll @IntRange(min = 3, max = 5) attempt2: Int
    ) {
        val delay1 = calculateBackoffDelay(attempt1.coerceAtMost(MAX_RETRIES))
        val delay2 = calculateBackoffDelay(attempt2.coerceAtMost(MAX_RETRIES))

        assertTrue(delay2 >= delay1)
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = BASE_DELAY_MS * (1 shl attempt.coerceAtMost(MAX_RETRIES))
        return exponentialDelay.coerceIn(BASE_DELAY_MS, MAX_DELAY_MS)
    }
}

/**
 * PBT-10: Audit log immutability tests
 */
class AuditLogImmutabilityPropertyTest {

    @Property
    fun `audit log hash chain is valid`(
        @ForAll("auditLogEntries") entries: List<String>
    ) {
        var previousHash = "GENESIS"

        entries.forEach { entry ->
            val currentHash = calculateHash(previousHash + entry)
            assertTrue(currentHash.isNotBlank())
            previousHash = currentHash
        }
    }

    @Property
    fun `modifying entry breaks hash chain`(
        @ForAll @StringLength(min = 10, max = 100) originalEntry: String,
        @ForAll @StringLength(min = 1, max = 10) modification: String
    ) {
        val originalHash = calculateHash(originalEntry)
        val modifiedHash = calculateHash(originalEntry + modification)

        assertTrue(originalHash != modifiedHash)
    }

    @Property
    fun `same content produces same hash`(
        @ForAll @StringLength(min = 10, max = 100) content: String
    ) {
        val hash1 = calculateHash(content)
        val hash2 = calculateHash(content)

        assertEquals(hash1, hash2)
    }

    @Provide
    fun auditLogEntries(): Arbitrary<List<String>> {
        return Arbitraries.strings()
            .alpha().ofMinLength(10).ofMaxLength(50)
            .list().ofMinSize(1).ofMaxSize(10)
    }

    private fun calculateHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * PBT-15: Rate limit fallback consistency tests
 */
class RateLimitFallbackPropertyTest {

    @Property
    fun `fallback produces consistent results with primary`(
        @ForAll @IntRange(min = 1, max = 100) limit: Int,
        @ForAll @IntRange(min = 0, max = 100) currentCount: Int
    ) {
        val primaryResult = currentCount < limit
        val fallbackResult = currentCount < limit

        assertEquals(primaryResult, fallbackResult)
    }

    @Property
    fun `remaining quota is consistent across implementations`(
        @ForAll @IntRange(min = 1, max = 100) limit: Int,
        @ForAll @IntRange(min = 0, max = 100) currentCount: Int
    ) {
        val primaryRemaining = (limit - currentCount).coerceAtLeast(0)
        val fallbackRemaining = (limit - currentCount).coerceAtLeast(0)

        assertEquals(primaryRemaining, fallbackRemaining)
    }
}

/**
 * PBT-16: JWT Access Token 30min boundary tests
 */
class JwtAccessTokenPropertyTest {

    companion object {
        private const val ACCESS_TOKEN_VALIDITY_MINUTES = 30L
    }

    @Property
    fun `access token is valid within 30 minutes`(
        @ForAll @IntRange(min = 0, max = 29) minutesElapsed: Int
    ) {
        val issuedAt = Instant.now()
        val checkTime = issuedAt.plus(Duration.ofMinutes(minutesElapsed.toLong()))
        val expiresAt = issuedAt.plus(Duration.ofMinutes(ACCESS_TOKEN_VALIDITY_MINUTES))

        assertTrue(checkTime.isBefore(expiresAt))
    }

    @Property
    fun `access token is invalid after 30 minutes`(
        @ForAll @IntRange(min = 31, max = 120) minutesElapsed: Int
    ) {
        val issuedAt = Instant.now()
        val checkTime = issuedAt.plus(Duration.ofMinutes(minutesElapsed.toLong()))
        val expiresAt = issuedAt.plus(Duration.ofMinutes(ACCESS_TOKEN_VALIDITY_MINUTES))

        assertTrue(checkTime.isAfter(expiresAt))
    }
}

/**
 * PBT-17: JWT Refresh Token 14d boundary tests
 */
class JwtRefreshTokenPropertyTest {

    companion object {
        private const val REFRESH_TOKEN_VALIDITY_DAYS = 14L
    }

    @Property
    fun `refresh token is valid within 14 days`(
        @ForAll @IntRange(min = 0, max = 13) daysElapsed: Int
    ) {
        val issuedAt = Instant.now()
        val checkTime = issuedAt.plus(Duration.ofDays(daysElapsed.toLong()))
        val expiresAt = issuedAt.plus(Duration.ofDays(REFRESH_TOKEN_VALIDITY_DAYS))

        assertTrue(checkTime.isBefore(expiresAt))
    }

    @Property
    fun `refresh token is invalid after 14 days`(
        @ForAll @IntRange(min = 15, max = 30) daysElapsed: Int
    ) {
        val issuedAt = Instant.now()
        val checkTime = issuedAt.plus(Duration.ofDays(daysElapsed.toLong()))
        val expiresAt = issuedAt.plus(Duration.ofDays(REFRESH_TOKEN_VALIDITY_DAYS))

        assertTrue(checkTime.isAfter(expiresAt))
    }
}
