package edu.minervia.platform.service.identity.generator

import java.math.BigDecimal

data class GeneratedSemester(
    val year: Int,
    val season: String,
    val courses: List<GeneratedCourse>
)

data class GeneratedCourse(
    val name: String,
    val code: String,
    val credits: Int,
    val grade: BigDecimal
)

data class GeneratedFamilyInfo(
    val fatherName: String,
    val fatherOccupation: String,
    val motherName: String,
    val motherOccupation: String,
    val address: String
)
