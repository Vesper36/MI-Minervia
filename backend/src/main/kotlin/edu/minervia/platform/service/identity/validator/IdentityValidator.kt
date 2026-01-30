package edu.minervia.platform.service.identity.validator

import edu.minervia.platform.service.identity.GeneratedIdentity
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Period

@Component
class IdentityValidator {

    fun validate(identity: GeneratedIdentity): List<String> {
        val errors = mutableListOf<String>()

        errors.addAll(validateAge(identity))
        errors.addAll(validateTimeline(identity))
        errors.addAll(validateStudentNumber(identity))

        return errors
    }

    private fun validateAge(identity: GeneratedIdentity): List<String> {
        val errors = mutableListOf<String>()
        val age = Period.between(identity.birthDate, identity.enrollmentDate).years

        if (age < 18) {
            errors.add("Age at enrollment ($age) is below minimum (18)")
        }
        if (age > 25) {
            errors.add("Age at enrollment ($age) exceeds maximum (25)")
        }

        return errors
    }

    private fun validateTimeline(identity: GeneratedIdentity): List<String> {
        val errors = mutableListOf<String>()

        if (!identity.admissionDate.isBefore(identity.enrollmentDate)) {
            errors.add("Admission date must be before enrollment date")
        }

        if (identity.enrollmentDate.isAfter(LocalDate.now())) {
            errors.add("Enrollment date cannot be in the future")
        }

        if (identity.birthDate.isAfter(identity.admissionDate)) {
            errors.add("Birth date must be before admission date")
        }

        return errors
    }

    private fun validateStudentNumber(identity: GeneratedIdentity): List<String> {
        val errors = mutableListOf<String>()
        val pattern = Regex("^\\d{4}[A-Z]{2}\\d{4}$")

        if (!pattern.matches(identity.studentNumber)) {
            errors.add("Student number format invalid: ${identity.studentNumber}")
        }

        val yearPrefix = identity.studentNumber.take(4).toIntOrNull()
        if (yearPrefix != identity.enrollmentYear) {
            errors.add("Student number year ($yearPrefix) doesn't match enrollment year (${identity.enrollmentYear})")
        }

        return errors
    }
}
