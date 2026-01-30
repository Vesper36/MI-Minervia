package edu.minervia.platform.service.identity.generator

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class CourseGenerator(
    private val gradeGenerator: GradeGenerator
) {
    private val random = SecureRandom()

    private val generalCourses = listOf(
        CourseTemplate("Academic Writing", "GE101", 2),
        CourseTemplate("Critical Thinking", "GE110", 3),
        CourseTemplate("Ethics and Society", "GE120", 2),
        CourseTemplate("Communication Skills", "GE130", 2),
        CourseTemplate("Calculus I", "MA101", 4),
        CourseTemplate("Statistics I", "MA120", 3),
        CourseTemplate("Physics I", "PH101", 4),
        CourseTemplate("Introduction to Programming", "CS100", 4),
        CourseTemplate("Economics for Non-Majors", "EC100", 3),
        CourseTemplate("Business Fundamentals", "BA100", 3),
        CourseTemplate("Research Methods", "GE210", 3),
        CourseTemplate("Project Management Basics", "GE220", 3)
    )

    private val majorCourses = mapOf(
        "CS" to listOf(
            CourseTemplate("Data Structures", "CS201", 4),
            CourseTemplate("Algorithms", "CS202", 4),
            CourseTemplate("Computer Architecture", "CS210", 3),
            CourseTemplate("Operating Systems", "CS301", 4),
            CourseTemplate("Databases", "CS220", 3),
            CourseTemplate("Software Engineering", "CS320", 3),
            CourseTemplate("Computer Networks", "CS310", 3),
            CourseTemplate("AI Fundamentals", "CS330", 3),
            CourseTemplate("Web Development", "CS240", 3),
            CourseTemplate("Discrete Mathematics", "MA201", 4)
        ),
        "IT" to listOf(
            CourseTemplate("Systems Administration", "IT210", 3),
            CourseTemplate("Database Systems", "IT220", 3),
            CourseTemplate("Network Security", "IT310", 3),
            CourseTemplate("Cloud Computing", "IT320", 3),
            CourseTemplate("IT Service Management", "IT330", 3),
            CourseTemplate("Web Applications", "IT240", 3),
            CourseTemplate("Scripting for IT", "IT201", 3),
            CourseTemplate("IT Infrastructure", "IT230", 3),
            CourseTemplate("Human-Computer Interaction", "IT260", 3)
        ),
        "EE" to listOf(
            CourseTemplate("Circuit Analysis", "EE201", 4),
            CourseTemplate("Digital Logic", "EE210", 4),
            CourseTemplate("Signals and Systems", "EE220", 4),
            CourseTemplate("Electromagnetics", "EE230", 3),
            CourseTemplate("Microcontrollers", "EE310", 3),
            CourseTemplate("Power Systems", "EE320", 3),
            CourseTemplate("Control Systems", "EE330", 3),
            CourseTemplate("Electronics", "EE240", 4),
            CourseTemplate("Embedded Systems", "EE340", 3)
        ),
        "ME" to listOf(
            CourseTemplate("Statics", "ME201", 4),
            CourseTemplate("Dynamics", "ME210", 4),
            CourseTemplate("Thermodynamics", "ME220", 4),
            CourseTemplate("Fluid Mechanics", "ME230", 4),
            CourseTemplate("Materials Science", "ME240", 3),
            CourseTemplate("Machine Design", "ME310", 3),
            CourseTemplate("Heat Transfer", "ME320", 3),
            CourseTemplate("Manufacturing Processes", "ME330", 3),
            CourseTemplate("Mechanical Systems", "ME340", 3)
        ),
        "BA" to listOf(
            CourseTemplate("Principles of Management", "BA201", 3),
            CourseTemplate("Marketing Fundamentals", "BA210", 3),
            CourseTemplate("Financial Accounting", "BA220", 3),
            CourseTemplate("Managerial Accounting", "BA230", 3),
            CourseTemplate("Operations Management", "BA310", 3),
            CourseTemplate("Business Analytics", "BA320", 3),
            CourseTemplate("Organizational Behavior", "BA330", 3),
            CourseTemplate("Business Law", "BA240", 3),
            CourseTemplate("Strategic Management", "BA410", 3)
        ),
        "EC" to listOf(
            CourseTemplate("Microeconomics", "EC201", 3),
            CourseTemplate("Macroeconomics", "EC202", 3),
            CourseTemplate("Econometrics", "EC310", 4),
            CourseTemplate("International Economics", "EC320", 3),
            CourseTemplate("Public Economics", "EC330", 3),
            CourseTemplate("Economic Development", "EC340", 3),
            CourseTemplate("Monetary Economics", "EC350", 3),
            CourseTemplate("Game Theory", "EC360", 3)
        ),
        "DS" to listOf(
            CourseTemplate("Data Mining", "DS301", 3),
            CourseTemplate("Machine Learning", "DS310", 4),
            CourseTemplate("Data Visualization", "DS320", 3),
            CourseTemplate("Statistical Modeling", "DS330", 4),
            CourseTemplate("Big Data Systems", "DS340", 3),
            CourseTemplate("Data Ethics", "DS350", 2),
            CourseTemplate("Data Engineering", "DS360", 3)
        ),
        "CE" to listOf(
            CourseTemplate("Structural Analysis", "CE201", 4),
            CourseTemplate("Construction Materials", "CE210", 3),
            CourseTemplate("Soil Mechanics", "CE220", 4),
            CourseTemplate("Hydraulics", "CE230", 3),
            CourseTemplate("Transportation Engineering", "CE310", 3),
            CourseTemplate("Environmental Engineering", "CE320", 3),
            CourseTemplate("Surveying", "CE240", 3)
        )
    )

    fun generateCourses(majorCode: String, minCourses: Int = 5, maxCourses: Int = 7): List<GeneratedCourse> {
        val normalizedMin = minOf(minCourses, maxCourses)
        val normalizedMax = maxOf(minCourses, maxCourses)
        val count = normalizedMin + random.nextInt(normalizedMax - normalizedMin + 1)
        val pool = (majorCourses[majorCode.uppercase()] ?: emptyList()) + generalCourses
        val selected = pickDistinct(pool.ifEmpty { generalCourses }, count)

        return selected.map { template ->
            GeneratedCourse(
                name = template.name,
                code = template.code,
                credits = template.credits,
                grade = gradeGenerator.generateGrade()
            )
        }
    }

    private fun <T> pickDistinct(items: List<T>, count: Int): List<T> {
        if (items.isEmpty()) {
            return emptyList()
        }
        val mutable = items.toMutableList()
        val selected = mutableListOf<T>()
        val picks = minOf(count, mutable.size)
        repeat(picks) {
            val idx = random.nextInt(mutable.size)
            selected.add(mutable.removeAt(idx))
        }
        return selected
    }

    private data class CourseTemplate(val name: String, val code: String, val credits: Int)
}
