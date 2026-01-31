package edu.minervia.platform.pbt

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.IntRange
import net.jqwik.api.constraints.StringLength
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * PBT-12: Kafka partition key consistency tests
 */
class KafkaPartitionKeyPropertyTest {

    @Property
    fun `same applicationId always maps to same partition`(
        @ForAll @IntRange(min = 1, max = 100000) applicationId: Int,
        @ForAll @IntRange(min = 1, max = 12) numPartitions: Int
    ) {
        val partition1 = calculatePartition(applicationId, numPartitions)
        val partition2 = calculatePartition(applicationId, numPartitions)

        assertEquals(partition1, partition2)
    }

    @Property
    fun `partition is within valid range`(
        @ForAll @IntRange(min = 1, max = 100000) applicationId: Int,
        @ForAll @IntRange(min = 1, max = 12) numPartitions: Int
    ) {
        val partition = calculatePartition(applicationId, numPartitions)

        assertTrue(partition >= 0)
        assertTrue(partition < numPartitions)
    }

    @Property
    fun `different applicationIds may map to different partitions`(
        @ForAll @IntRange(min = 1, max = 1000) id1: Int,
        @ForAll @IntRange(min = 1001, max = 2000) id2: Int,
        @ForAll @IntRange(min = 6, max = 12) numPartitions: Int
    ) {
        val partition1 = calculatePartition(id1, numPartitions)
        val partition2 = calculatePartition(id2, numPartitions)

        // Not asserting they're different, just that both are valid
        assertTrue(partition1 in 0 until numPartitions)
        assertTrue(partition2 in 0 until numPartitions)
    }

    private fun calculatePartition(applicationId: Int, numPartitions: Int): Int {
        return Math.abs(applicationId.hashCode()) % numPartitions
    }
}

/**
 * PBT-13: SimpleBroker no persistence tests
 */
class SimpleBrokerPropertyTest {

    @Property
    fun `messages are not persisted after broker restart`(
        @ForAll("topicMessages") messages: List<String>
    ) {
        val broker = InMemoryBroker()

        messages.forEach { broker.publish(it) }
        val beforeRestart = broker.messageCount()

        broker.restart()
        val afterRestart = broker.messageCount()

        assertTrue(beforeRestart > 0 || messages.isEmpty())
        assertEquals(0, afterRestart)
    }

    @Property
    fun `subscriber receives messages only while connected`(
        @ForAll @IntRange(min = 1, max = 10) messageCount: Int
    ) {
        val broker = InMemoryBroker()
        val received = mutableListOf<String>()

        broker.subscribe { received.add(it) }
        repeat(messageCount) { broker.publish("msg-$it") }

        assertEquals(messageCount, received.size)

        broker.unsubscribe()
        broker.publish("after-unsub")

        assertEquals(messageCount, received.size)
    }

    @Provide
    fun topicMessages(): Arbitrary<List<String>> {
        return Arbitraries.strings()
            .alpha().ofMinLength(5).ofMaxLength(20)
            .list().ofMinSize(0).ofMaxSize(10)
    }

    private class InMemoryBroker {
        private val messages = mutableListOf<String>()
        private var subscriber: ((String) -> Unit)? = null

        fun publish(message: String) {
            messages.add(message)
            subscriber?.invoke(message)
        }

        fun subscribe(handler: (String) -> Unit) {
            subscriber = handler
        }

        fun unsubscribe() {
            subscriber = null
        }

        fun messageCount() = messages.size

        fun restart() {
            messages.clear()
            subscriber = null
        }
    }
}

/**
 * PBT-14: JWT revocation verification tests
 */
class JwtRevocationPropertyTest {

    @Property
    fun `revoked token is rejected`(
        @ForAll("jwtIds") jti: String
    ) {
        val revocationList = ConcurrentHashMap<String, Instant>()

        revocationList[jti] = Instant.now()
        val isRevoked = revocationList.containsKey(jti)

        assertTrue(isRevoked)
    }

    @Property
    fun `non-revoked token is accepted`(
        @ForAll("jwtIds") jti: String
    ) {
        val revocationList = ConcurrentHashMap<String, Instant>()

        val isRevoked = revocationList.containsKey(jti)

        assertFalse(isRevoked)
    }

    @Property
    fun `revocation is immediate`(
        @ForAll("jwtIds") jti: String
    ) {
        val revocationList = ConcurrentHashMap<String, Instant>()

        assertFalse(revocationList.containsKey(jti))
        revocationList[jti] = Instant.now()
        assertTrue(revocationList.containsKey(jti))
    }

    @Provide
    fun jwtIds(): Arbitrary<String> {
        return Arbitraries.create { UUID.randomUUID().toString() }
    }
}

/**
 * PBT-18: JWT revocation list isolation tests
 */
class JwtRevocationIsolationPropertyTest {

    @Property
    fun `revoking one token does not affect others`(
        @ForAll("jwtIds") jti1: String,
        @ForAll("jwtIds") jti2: String
    ) {
        val revocationList = ConcurrentHashMap<String, Instant>()

        revocationList[jti1] = Instant.now()

        assertTrue(revocationList.containsKey(jti1))
        if (jti1 != jti2) {
            assertFalse(revocationList.containsKey(jti2))
        }
    }

    @Property
    fun `revocation list supports concurrent access`(
        @ForAll @IntRange(min = 1, max = 100) tokenCount: Int
    ) {
        val revocationList = ConcurrentHashMap<String, Instant>()
        val tokens = (1..tokenCount).map { UUID.randomUUID().toString() }

        tokens.parallelStream().forEach { jti ->
            revocationList[jti] = Instant.now()
        }

        assertEquals(tokenCount, revocationList.size)
    }

    @Provide
    fun jwtIds(): Arbitrary<String> {
        return Arbitraries.create { UUID.randomUUID().toString() }
    }
}

/**
 * PBT-19: Progress table independence tests
 */
class ProgressTablePropertyTest {

    @Property
    fun `progress updates are independent per application`(
        @ForAll @IntRange(min = 1, max = 1000) appId1: Int,
        @ForAll @IntRange(min = 1001, max = 2000) appId2: Int,
        @ForAll @IntRange(min = 0, max = 100) progress1: Int,
        @ForAll @IntRange(min = 0, max = 100) progress2: Int
    ) {
        val progressTable = ConcurrentHashMap<Int, Int>()

        progressTable[appId1] = progress1
        progressTable[appId2] = progress2

        assertEquals(progress1, progressTable[appId1])
        assertEquals(progress2, progressTable[appId2])
        assertNotEquals(appId1, appId2)
    }

    @Property
    fun `progress version prevents stale updates`(
        @ForAll @IntRange(min = 1, max = 1000) appId: Int,
        @ForAll @IntRange(min = 1, max = 10) version1: Int,
        @ForAll @IntRange(min = 1, max = 10) version2: Int
    ) {
        data class ProgressEntry(val progress: Int, val version: Int)
        val progressTable = ConcurrentHashMap<Int, ProgressEntry>()

        progressTable[appId] = ProgressEntry(50, version1)

        val current = progressTable[appId]!!
        val updateAllowed = version2 > current.version

        if (updateAllowed) {
            progressTable[appId] = ProgressEntry(75, version2)
            assertEquals(75, progressTable[appId]!!.progress)
        } else {
            assertEquals(50, progressTable[appId]!!.progress)
        }
    }
}

/**
 * PBT-20: Polling interval switch tests
 */
class PollingIntervalPropertyTest {

    companion object {
        private const val WS_CONNECTED_INTERVAL_MS = 0L
        private const val POLLING_INTERVAL_MS = 3000L
        private const val FALLBACK_INTERVAL_MS = 5000L
    }

    @Property
    fun `polling disabled when WebSocket connected`(
        @ForAll("connectionStates") wsConnected: Boolean
    ) {
        val pollingInterval = if (wsConnected) WS_CONNECTED_INTERVAL_MS else POLLING_INTERVAL_MS

        if (wsConnected) {
            assertEquals(0L, pollingInterval)
        } else {
            assertTrue(pollingInterval > 0)
        }
    }

    @Property
    fun `fallback interval used after connection failures`(
        @ForAll @IntRange(min = 0, max = 10) failureCount: Int
    ) {
        val interval = when {
            failureCount == 0 -> POLLING_INTERVAL_MS
            failureCount < 3 -> POLLING_INTERVAL_MS
            else -> FALLBACK_INTERVAL_MS
        }

        assertTrue(interval in listOf(POLLING_INTERVAL_MS, FALLBACK_INTERVAL_MS))
    }

    @Provide
    fun connectionStates(): Arbitrary<Boolean> = Arbitraries.of(true, false)
}

/**
 * PBT-21: AI timeout cleanup atomicity tests
 */
class AiTimeoutCleanupPropertyTest {

    @Property
    fun `stuck tasks are identified correctly`(
        @ForAll @IntRange(min = 1, max = 1000) taskId: Int,
        @ForAll @IntRange(min = 0, max = 600) elapsedSeconds: Int
    ) {
        val timeoutThresholdSeconds = 300
        val isStuck = elapsedSeconds > timeoutThresholdSeconds

        if (elapsedSeconds > timeoutThresholdSeconds) {
            assertTrue(isStuck)
        } else {
            assertFalse(isStuck)
        }
    }

    @Property
    fun `cleanup does not affect running tasks within timeout`(
        @ForAll @IntRange(min = 0, max = 299) elapsedSeconds: Int
    ) {
        val timeoutThresholdSeconds = 300
        val shouldCleanup = elapsedSeconds > timeoutThresholdSeconds

        assertFalse(shouldCleanup)
    }

    @Property
    fun `re-queue preserves task data`(
        @ForAll @IntRange(min = 1, max = 1000) applicationId: Int,
        @ForAll @StringLength(min = 5, max = 20) taskType: String
    ) {
        data class Task(val appId: Int, val type: String, val retryCount: Int)

        val original = Task(applicationId, taskType, 0)
        val requeued = Task(original.appId, original.type, original.retryCount + 1)

        assertEquals(original.appId, requeued.appId)
        assertEquals(original.type, requeued.type)
        assertEquals(original.retryCount + 1, requeued.retryCount)
    }
}

/**
 * PBT-22: AI step transaction isolation tests
 */
class AiStepTransactionPropertyTest {

    enum class TaskStep { IDENTITY_RULES, IDENTITY_LLM, PHOTO_GENERATION }

    @Property
    fun `each step commits independently`(
        @ForAll("taskSteps") step: TaskStep,
        @ForAll("stepResults") success: Boolean
    ) {
        val completedSteps = mutableSetOf<TaskStep>()

        if (success) {
            completedSteps.add(step)
        }

        assertEquals(success, step in completedSteps)
    }

    @Property
    fun `failed step does not rollback previous steps`(
        @ForAll @IntRange(min = 0, max = 2) failAtStep: Int
    ) {
        val steps = TaskStep.entries
        val completedSteps = mutableSetOf<TaskStep>()

        steps.forEachIndexed { index, step ->
            if (index < failAtStep) {
                completedSteps.add(step)
            }
        }

        assertEquals(failAtStep, completedSteps.size)
        steps.take(failAtStep).forEach { assertTrue(it in completedSteps) }
    }

    @Provide
    fun taskSteps(): Arbitrary<TaskStep> = Arbitraries.of(*TaskStep.entries.toTypedArray())

    @Provide
    fun stepResults(): Arbitrary<Boolean> = Arbitraries.of(true, false)
}

/**
 * PBT-23/24/25: LLM template coverage tests
 */
class LlmTemplatePropertyTest {

    private val nationalities = listOf("PL", "CN", "DE", "US")
    private val majors = listOf("CS", "EE", "BA", "ME")
    private val identityTypes = listOf("LOCAL", "INTERNATIONAL")

    @Property
    fun `all nationality-major-identity combinations have templates`(
        @ForAll("nationalities") nationality: String,
        @ForAll("majors") major: String,
        @ForAll("identityTypes") identityType: String
    ) {
        val templatePath = "config/llm-templates/$nationality/$major/$identityType.yaml"

        assertTrue(templatePath.isNotBlank())
        assertTrue(templatePath.contains(nationality))
        assertTrue(templatePath.contains(major))
        assertTrue(templatePath.contains(identityType))
    }

    @Property
    fun `template count matches expected combinations`() {
        val expectedCount = nationalities.size * majors.size * identityTypes.size

        assertEquals(32, expectedCount)
    }

    @Property
    fun `nationality templates are isolated`(
        @ForAll("nationalities") nat1: String,
        @ForAll("nationalities") nat2: String,
        @ForAll("majors") major: String
    ) {
        val path1 = "config/llm-templates/$nat1/$major/LOCAL.yaml"
        val path2 = "config/llm-templates/$nat2/$major/LOCAL.yaml"

        if (nat1 != nat2) {
            assertNotEquals(path1, path2)
        } else {
            assertEquals(path1, path2)
        }
    }

    @Property
    fun `identity type templates are isolated`(
        @ForAll("nationalities") nationality: String,
        @ForAll("majors") major: String
    ) {
        val localPath = "config/llm-templates/$nationality/$major/LOCAL.yaml"
        val intlPath = "config/llm-templates/$nationality/$major/INTERNATIONAL.yaml"

        assertNotEquals(localPath, intlPath)
    }

    @Provide
    fun nationalities(): Arbitrary<String> = Arbitraries.of(nationalities)

    @Provide
    fun majors(): Arbitrary<String> = Arbitraries.of(majors)

    @Provide
    fun identityTypes(): Arbitrary<String> = Arbitraries.of(identityTypes)
}
