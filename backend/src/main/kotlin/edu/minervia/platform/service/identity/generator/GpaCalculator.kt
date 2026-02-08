package edu.minervia.platform.service.identity.generator

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class GpaCalculator {

    fun calculateGpaFromSemesters(semesters: List<GeneratedSemester>): BigDecimal {
        return calculateGpa(semesters.flatMap { it.courses })
    }

    fun calculateGpa(courses: List<GeneratedCourse>): BigDecimal {
        if (courses.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        var totalWeighted = BigDecimal.ZERO
        var totalCredits = BigDecimal.ZERO

        for (course in courses) {
            val credits = BigDecimal.valueOf(course.credits.toLong())
            totalWeighted = totalWeighted.add(course.grade.multiply(credits))
            totalCredits = totalCredits.add(credits)
        }

        if (totalCredits.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        return totalWeighted.divide(totalCredits, 2, RoundingMode.HALF_UP)
    }
}
