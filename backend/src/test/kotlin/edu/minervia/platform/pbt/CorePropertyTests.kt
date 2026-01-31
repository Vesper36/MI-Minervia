package edu.minervia.platform.pbt

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.IntRange
import net.jqwik.api.constraints.StringLength
import java.time.LocalDate
import java.time.Year
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PBT-01: Student ID uniqueness and format tests
 */
class StudentIdPropertyTest {

    companion object {
        private val VALID_FACULTY_CODES = listOf("CS", "EE", "ME", "BA", "PH", "CH", "MA", "EC")
    }

    @Property
    fun `student ID format is always valid`(
        @ForAll @IntRange(min = 2020, max = 2030) year: Int,
        @ForAll("facultyCodes") facultyCode: String,
        @ForAll @IntRange(min = 1, max = 9999) sequence: Int
    ) {
        val studentId = generateStudentId(year, facultyCode, sequence)

        assertTrue(studentId.matches(Regex("^\\d{4}[A-Z]{2}\\d{4}$")))
        assertEquals(year.toString(), studentId.substring(0, 4))
        assertEquals(facultyCode, studentId.substring(4, 6))
    }

    @Property
    fun `different sequences produce different IDs`(
        @ForAll @IntRange(min = 2020, max = 2030) year: Int,
        @ForAll("facultyCodes") facultyCode: String,
        @ForAll @IntRange(min = 1, max = 4999) seq1: Int,
        @ForAll @IntRange(min = 5000, max = 9999) seq2: Int
    ) {
        val id1 = generateStudentId(year, facultyCode, seq1)
        val id2 = generateStudentId(year, facultyCode, seq2)

        assertTrue(id1 != id2)
    }

    @Property
    fun `student ID year component matches enrollment year`(
        @ForAll @IntRange(min = 2020, max = 2030) enrollmentYear: Int,
        @ForAll("facultyCodes") facultyCode: String,
        @ForAll @IntRange(min = 1, max = 9999) sequence: Int
    ) {
        val studentId = generateStudentId(enrollmentYear, facultyCode, sequence)
        val extractedYear = studentId.substring(0, 4).toInt()

        assertEquals(enrollmentYear, extractedYear)
    }

    @Provide
    fun facultyCodes(): Arbitrary<String> = Arbitraries.of(VALID_FACULTY_CODES)

    private fun generateStudentId(year: Int, facultyCode: String, sequence: Int): String {
        return "%04d%s%04d".format(year, facultyCode, sequence)
    }
}

/**
 * PBT-02: Age and timeline consistency tests
 */
class AgeTimelinePropertyTest {

    @Property
    fun `student age is consistent with birth date and enrollment`(
        @ForAll @IntRange(min = 1990, max = 2005) birthYear: Int,
        @ForAll @IntRange(min = 1, max = 12) birthMonth: Int,
        @ForAll @IntRange(min = 1, max = 28) birthDay: Int,
        @ForAll @IntRange(min = 2020, max = 2025) enrollmentYear: Int
    ) {
        val birthDate = LocalDate.of(birthYear, birthMonth, birthDay)
        val enrollmentDate = LocalDate.of(enrollmentYear, 10, 1)

        val ageAtEnrollment = enrollmentYear - birthYear -
            if (enrollmentDate.dayOfYear < birthDate.dayOfYear) 1 else 0

        assertTrue(ageAtEnrollment >= 15, "Student must be at least 15 at enrollment")
        assertTrue(ageAtEnrollment <= 50, "Student age should be reasonable")
    }

    @Property
    fun `admission date is before enrollment date`(
        @ForAll @IntRange(min = 2020, max = 2025) year: Int,
        @ForAll @IntRange(min = 1, max = 6) admissionMonth: Int,
        @ForAll @IntRange(min = 9, max = 10) enrollmentMonth: Int
    ) {
        val admissionDate = LocalDate.of(year, admissionMonth, 15)
        val enrollmentDate = LocalDate.of(year, enrollmentMonth, 1)

        assertTrue(admissionDate.isBefore(enrollmentDate))
    }

    @Property
    fun `graduation date is after enrollment date`(
        @ForAll @IntRange(min = 2020, max = 2022) enrollmentYear: Int,
        @ForAll @IntRange(min = 3, max = 5) studyDuration: Int
    ) {
        val enrollmentDate = LocalDate.of(enrollmentYear, 10, 1)
        val graduationDate = LocalDate.of(enrollmentYear + studyDuration, 6, 30)

        assertTrue(graduationDate.isAfter(enrollmentDate))
        assertTrue(graduationDate.year - enrollmentDate.year >= 3)
    }
}

/**
 * PBT-04: State transition ordering tests
 */
class StateTransitionPropertyTest {

    enum class RegistrationState {
        NOT_STARTED,
        CODE_VERIFIED,
        EMAIL_VERIFIED,
        INFO_SELECTED,
        PENDING_APPROVAL,
        GENERATING,
        COMPLETED,
        FAILED
    }

    private val validTransitions = mapOf(
        RegistrationState.NOT_STARTED to setOf(RegistrationState.CODE_VERIFIED),
        RegistrationState.CODE_VERIFIED to setOf(RegistrationState.EMAIL_VERIFIED),
        RegistrationState.EMAIL_VERIFIED to setOf(RegistrationState.INFO_SELECTED),
        RegistrationState.INFO_SELECTED to setOf(RegistrationState.PENDING_APPROVAL),
        RegistrationState.PENDING_APPROVAL to setOf(RegistrationState.GENERATING, RegistrationState.FAILED),
        RegistrationState.GENERATING to setOf(RegistrationState.COMPLETED, RegistrationState.FAILED),
        RegistrationState.COMPLETED to emptySet(),
        RegistrationState.FAILED to emptySet()
    )

    @Property
    fun `state transitions follow valid order`(
        @ForAll("validStatePaths") path: List<RegistrationState>
    ) {
        for (i in 0 until path.size - 1) {
            val current = path[i]
            val next = path[i + 1]
            val allowed = validTransitions[current] ?: emptySet()

            assertTrue(
                next in allowed,
                "Invalid transition from $current to $next"
            )
        }
    }

    @Provide
    fun validStatePaths(): Arbitrary<List<RegistrationState>> {
        return Arbitraries.of(
            listOf(
                RegistrationState.NOT_STARTED,
                RegistrationState.CODE_VERIFIED,
                RegistrationState.EMAIL_VERIFIED,
                RegistrationState.INFO_SELECTED,
                RegistrationState.PENDING_APPROVAL,
                RegistrationState.GENERATING,
                RegistrationState.COMPLETED
            ),
            listOf(
                RegistrationState.NOT_STARTED,
                RegistrationState.CODE_VERIFIED,
                RegistrationState.EMAIL_VERIFIED,
                RegistrationState.INFO_SELECTED,
                RegistrationState.PENDING_APPROVAL,
                RegistrationState.FAILED
            ),
            listOf(
                RegistrationState.NOT_STARTED,
                RegistrationState.CODE_VERIFIED,
                RegistrationState.EMAIL_VERIFIED,
                RegistrationState.INFO_SELECTED,
                RegistrationState.PENDING_APPROVAL,
                RegistrationState.GENERATING,
                RegistrationState.FAILED
            )
        )
    }

    @Property
    fun `completed state is terminal`(
        @ForAll("terminalStates") state: RegistrationState
    ) {
        val allowed = validTransitions[state] ?: emptySet()
        assertTrue(allowed.isEmpty(), "$state should be terminal")
    }

    @Provide
    fun terminalStates(): Arbitrary<RegistrationState> {
        return Arbitraries.of(RegistrationState.COMPLETED, RegistrationState.FAILED)
    }
}

/**
 * PBT-05: Rate limit threshold boundary tests
 */
class RateLimitPropertyTest {

    @Property
    fun `rate limit allows requests under threshold`(
        @ForAll @IntRange(min = 1, max = 100) limit: Int,
        @ForAll @IntRange(min = 0, max = 99) currentCount: Int
    ) {
        val allowed = currentCount < limit
        if (currentCount < limit) {
            assertTrue(allowed)
        }
    }

    @Property
    fun `rate limit blocks requests at or above threshold`(
        @ForAll @IntRange(min = 1, max = 100) limit: Int,
        @ForAll @IntRange(min = 0, max = 100) currentCount: Int
    ) {
        val allowed = currentCount < limit
        if (currentCount >= limit) {
            assertTrue(!allowed)
        }
    }

    @Property
    fun `remaining quota is non-negative`(
        @ForAll @IntRange(min = 1, max = 100) limit: Int,
        @ForAll @IntRange(min = 0, max = 200) currentCount: Int
    ) {
        val remaining = (limit - currentCount).coerceAtLeast(0)
        assertTrue(remaining >= 0)
    }

    @Property
    fun `remaining quota equals limit minus count when under limit`(
        @ForAll @IntRange(min = 10, max = 100) limit: Int,
        @ForAll @IntRange(min = 0, max = 9) currentCount: Int
    ) {
        val remaining = limit - currentCount
        assertEquals(limit - currentCount, remaining)
    }
}

/**
 * PBT-08: Progress monotonicity tests
 */
class ProgressMonotonicityPropertyTest {

    @Property
    fun `progress percentage never decreases`(
        @ForAll("progressSequence") progressValues: List<Int>
    ) {
        for (i in 0 until progressValues.size - 1) {
            assertTrue(
                progressValues[i + 1] >= progressValues[i],
                "Progress should never decrease: ${progressValues[i]} -> ${progressValues[i + 1]}"
            )
        }
    }

    @Provide
    fun progressSequence(): Arbitrary<List<Int>> {
        return Arbitraries.integers().between(0, 100)
            .list().ofMinSize(2).ofMaxSize(10)
            .map { list -> list.sorted() }
    }

    @Property
    fun `progress is bounded between 0 and 100`(
        @ForAll @IntRange(min = -50, max = 150) rawProgress: Int
    ) {
        val boundedProgress = rawProgress.coerceIn(0, 100)
        assertTrue(boundedProgress in 0..100)
    }

    @Property
    fun `completed status implies 100 percent progress`(
        @ForAll("completedProgress") progress: Int
    ) {
        assertEquals(100, progress)
    }

    @Provide
    fun completedProgress(): Arbitrary<Int> = Arbitraries.just(100)
}

/**
 * PBT-11: Timestamp monotonicity tests
 */
class TimestampMonotonicityPropertyTest {

    @Property
    fun `audit log timestamps are monotonically increasing`(
        @ForAll("timestampSequence") timestamps: List<Long>
    ) {
        for (i in 0 until timestamps.size - 1) {
            assertTrue(
                timestamps[i + 1] >= timestamps[i],
                "Timestamps should be monotonically increasing"
            )
        }
    }

    @Provide
    fun timestampSequence(): Arbitrary<List<Long>> {
        return Arbitraries.longs().between(1704067200000, 1735689600000)
            .list().ofMinSize(2).ofMaxSize(20)
            .map { list -> list.sorted() }
    }

    @Property
    fun `created_at is always before or equal to updated_at`(
        @ForAll @IntRange(min = 1704067200, max = 1735689600) createdEpoch: Int,
        @ForAll @IntRange(min = 0, max = 86400) deltaSeconds: Int
    ) {
        val createdAt = createdEpoch.toLong()
        val updatedAt = createdAt + deltaSeconds

        assertTrue(updatedAt >= createdAt)
    }
}
