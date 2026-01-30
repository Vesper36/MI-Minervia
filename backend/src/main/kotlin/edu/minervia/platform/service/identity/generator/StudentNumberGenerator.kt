package edu.minervia.platform.service.identity.generator

import edu.minervia.platform.domain.repository.StudentRepository
import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class StudentNumberGenerator(
    private val studentRepository: StudentRepository
) {
    private val random = SecureRandom()

    private val majorCodes = mapOf(
        "CS" to "CS",
        "IT" to "IT",
        "BA" to "BA",
        "EC" to "EC",
        "ME" to "ME",
        "EE" to "EE",
        "MA" to "MA",
        "PH" to "PH"
    )

    fun generateStudentNumber(enrollmentYear: Int, majorCode: String): String {
        val code = majorCodes[majorCode.uppercase()] ?: "GE"
        val prefix = "${enrollmentYear}${code}"

        val maxSeq = studentRepository.findMaxSequenceByPrefix(prefix) ?: 0
        val nextSeq = maxSeq + 1

        return "$prefix${nextSeq.toString().padStart(4, '0')}"
    }
}
