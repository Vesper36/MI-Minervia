package edu.minervia.platform.service.identity.generator

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.LocalDate
import java.time.Month

@Component
class DateGenerator {
    private val random = SecureRandom()

    fun generateBirthDate(enrollmentYear: Int, minAge: Int = 18, maxAge: Int = 25): LocalDate {
        val age = minAge + random.nextInt(maxAge - minAge + 1)
        val birthYear = enrollmentYear - age
        val month = Month.of(1 + random.nextInt(12))
        val maxDay = month.length(LocalDate.of(birthYear, 1, 1).isLeapYear)
        val day = 1 + random.nextInt(maxDay)
        return LocalDate.of(birthYear, month, day)
    }

    fun generateAdmissionDate(enrollmentYear: Int): LocalDate {
        val month = Month.of(5 + random.nextInt(3))
        val day = 1 + random.nextInt(28)
        return LocalDate.of(enrollmentYear, month, day)
    }

    fun generateEnrollmentDate(enrollmentYear: Int): LocalDate {
        return LocalDate.of(enrollmentYear, Month.SEPTEMBER, 1)
    }

    fun getCurrentEnrollmentYear(): Int {
        val now = LocalDate.now()
        return if (now.monthValue >= 9) now.year else now.year - 1
    }
}
