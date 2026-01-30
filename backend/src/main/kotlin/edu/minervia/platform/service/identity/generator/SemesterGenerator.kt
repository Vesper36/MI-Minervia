package edu.minervia.platform.service.identity.generator

import org.springframework.stereotype.Component

@Component
class SemesterGenerator(
    private val courseGenerator: CourseGenerator
) {
    fun generateSemesters(enrollmentYear: Int, majorCode: String, years: Int = 4): List<GeneratedSemester> {
        if (years <= 0) {
            return emptyList()
        }

        val semesters = mutableListOf<GeneratedSemester>()
        for (offset in 0 until years) {
            val fallYear = enrollmentYear + offset
            semesters.add(
                GeneratedSemester(
                    year = fallYear,
                    season = "Fall",
                    courses = courseGenerator.generateCourses(majorCode)
                )
            )

            val springYear = enrollmentYear + offset + 1
            semesters.add(
                GeneratedSemester(
                    year = springYear,
                    season = "Spring",
                    courses = courseGenerator.generateCourses(majorCode)
                )
            )
        }

        return semesters
    }
}
