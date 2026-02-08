package edu.minervia.platform.service.identity.generator

import org.springframework.stereotype.Component
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

@Component
class NameGenerator {
    private val random = SecureRandom().asKotlinRandom()

    private val polishFirstNamesMale = listOf(
        "Jan", "Piotr", "Krzysztof", "Andrzej", "Tomasz", "Pawel", "Michal", "Marcin",
        "Jakub", "Adam", "Lukasz", "Mateusz", "Kamil", "Maciej", "Rafal", "Dawid"
    )
    private val polishFirstNamesFemale = listOf(
        "Anna", "Maria", "Katarzyna", "Malgorzata", "Agnieszka", "Barbara", "Ewa",
        "Magdalena", "Monika", "Joanna", "Dorota", "Aleksandra", "Natalia", "Karolina"
    )
    private val polishLastNames = listOf(
        "Nowak", "Kowalski", "Wisniewski", "Wojcik", "Kowalczyk", "Kaminski", "Lewandowski",
        "Zielinski", "Szymanski", "Wozniak", "Dabrowski", "Kozlowski", "Jankowski", "Mazur"
    )

    private val chineseLastNames = listOf(
        "Wang", "Li", "Zhang", "Liu", "Chen", "Yang", "Huang", "Zhao", "Wu", "Zhou",
        "Xu", "Sun", "Ma", "Zhu", "Hu", "Guo", "He", "Lin", "Luo", "Gao"
    )
    private val chineseFirstNames = listOf(
        "Wei", "Fang", "Ming", "Jing", "Hui", "Xiao", "Lei", "Yan", "Ying", "Juan",
        "Qiang", "Jun", "Jie", "Tao", "Ping", "Hua", "Chao", "Bo", "Hao", "Yi"
    )

    private val americanFirstNamesMale = listOf(
        "James", "John", "Robert", "Michael", "William", "David", "Joseph", "Charles",
        "Thomas", "Daniel", "Matthew", "Anthony", "Donald", "Steven", "Andrew", "Paul"
    )
    private val americanFirstNamesFemale = listOf(
        "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan",
        "Jessica", "Sarah", "Karen", "Nancy", "Lisa", "Margaret", "Betty", "Sandra"
    )
    private val americanLastNames = listOf(
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Garcia",
        "Rodriguez", "Wilson", "Martinez", "Anderson", "Taylor", "Thomas", "Moore", "Jackson"
    )

    private val germanFirstNamesMale = listOf(
        "Lukas", "Leon", "Finn", "Paul", "Jonas", "Felix", "Noah", "Elias",
        "Ben", "Maximilian", "Tim", "Julian", "Niklas", "Moritz", "Jan", "David"
    )
    private val germanFirstNamesFemale = listOf(
        "Emma", "Mia", "Hannah", "Sofia", "Anna", "Lena", "Lea", "Marie",
        "Laura", "Julia", "Sarah", "Lisa", "Lara", "Nele", "Jana", "Katharina"
    )
    private val germanLastNames = listOf(
        "Muller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker",
        "Schulz", "Hoffmann", "Schafer", "Koch", "Bauer", "Richter", "Klein", "Wolf"
    )

    fun generateName(countryCode: String): Pair<String, String> {
        val isMale = random.nextBoolean()
        return when (countryCode.uppercase()) {
            "PL" -> {
                val firstName = if (isMale) polishFirstNamesMale.random(random) else polishFirstNamesFemale.random(random)
                val lastName = polishLastNames.random(random)
                firstName to lastName
            }
            "CN", "TW", "HK" -> {
                val lastName = chineseLastNames.random(random)
                val firstName = chineseFirstNames.random(random)
                firstName to lastName
            }
            "US", "GB", "AU", "CA" -> {
                val firstName = if (isMale) americanFirstNamesMale.random(random) else americanFirstNamesFemale.random(random)
                val lastName = americanLastNames.random(random)
                firstName to lastName
            }
            "DE", "AT", "CH" -> {
                val firstName = if (isMale) germanFirstNamesMale.random(random) else germanFirstNamesFemale.random(random)
                val lastName = germanLastNames.random(random)
                firstName to lastName
            }
            else -> {
                val firstName = if (isMale) americanFirstNamesMale.random(random) else americanFirstNamesFemale.random(random)
                val lastName = americanLastNames.random(random)
                firstName to lastName
            }
        }
    }
}
