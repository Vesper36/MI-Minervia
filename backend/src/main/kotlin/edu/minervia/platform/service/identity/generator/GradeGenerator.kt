package edu.minervia.platform.service.identity.generator

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.SecureRandom

@Component
class GradeGenerator {
    private val random = SecureRandom()

    private val gradeBands = listOf(
        GradeBand(2.0, 2),
        GradeBand(2.5, 5),
        GradeBand(3.0, 10),
        GradeBand(3.5, 22),
        GradeBand(4.0, 28),
        GradeBand(4.5, 22),
        GradeBand(5.0, 11)
    )
    private val totalWeight = gradeBands.sumOf { it.weight }

    fun generateGrade(): BigDecimal {
        val roll = random.nextInt(totalWeight)
        var cumulative = 0
        for (band in gradeBands) {
            cumulative += band.weight
            if (roll < cumulative) {
                return BigDecimal.valueOf(band.grade).setScale(1, RoundingMode.HALF_UP)
            }
        }
        val fallback = gradeBands.last().grade
        return BigDecimal.valueOf(fallback).setScale(1, RoundingMode.HALF_UP)
    }

    private data class GradeBand(val grade: Double, val weight: Int)
}
