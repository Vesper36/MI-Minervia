package edu.minervia.platform.service.identity.generator

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class FamilyInfoGenerator(
    private val occupationGenerator: OccupationGenerator,
    private val addressGenerator: AddressGenerator
) {
    private val random = SecureRandom()

    private val polishCulture = NameCulture(
        maleFirstNames = listOf("Jan", "Piotr", "Krzysztof", "Andrzej", "Tomasz", "Pawel", "Michal", "Marcin"),
        femaleFirstNames = listOf("Anna", "Maria", "Katarzyna", "Malgorzata", "Agnieszka", "Barbara", "Ewa", "Magdalena"),
        lastNames = listOf("Nowak", "Kowalski", "Wisniewski", "Wojcik", "Kaminski", "Lewandowski")
    )

    private val chineseCulture = NameCulture(
        maleFirstNames = listOf("Wei", "Ming", "Qiang", "Jun", "Lei", "Hao", "Bo", "Chao"),
        femaleFirstNames = listOf("Fang", "Jing", "Hui", "Xiao", "Yan", "Ying", "Juan", "Mei"),
        lastNames = listOf("Wang", "Li", "Zhang", "Liu", "Chen", "Yang", "Huang", "Zhao")
    )

    private val americanCulture = NameCulture(
        maleFirstNames = listOf("James", "John", "Robert", "Michael", "William", "David", "Daniel", "Matthew"),
        femaleFirstNames = listOf("Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Susan", "Jessica", "Sarah"),
        lastNames = listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Taylor")
    )

    private val germanCulture = NameCulture(
        maleFirstNames = listOf("Lukas", "Leon", "Finn", "Paul", "Jonas", "Felix", "Noah", "Elias"),
        femaleFirstNames = listOf("Emma", "Mia", "Hannah", "Sofia", "Anna", "Lena", "Lea", "Marie"),
        lastNames = listOf("Muller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Koch", "Wolf")
    )

    private val cultures = mapOf(
        "PL" to polishCulture,
        "CN" to chineseCulture,
        "TW" to chineseCulture,
        "HK" to chineseCulture,
        "US" to americanCulture,
        "GB" to americanCulture,
        "AU" to americanCulture,
        "CA" to americanCulture,
        "DE" to germanCulture,
        "AT" to germanCulture,
        "CH" to germanCulture
    )

    fun generateFamilyInfo(countryCode: String, studentLastName: String? = null): GeneratedFamilyInfo {
        val culture = cultures[countryCode.uppercase()] ?: americanCulture
        val lastName = studentLastName ?: culture.lastNames.random(random)

        val fatherName = "${culture.maleFirstNames.random(random)} $lastName"
        val motherName = "${culture.femaleFirstNames.random(random)} $lastName"

        return GeneratedFamilyInfo(
            fatherName = fatherName,
            fatherOccupation = occupationGenerator.generateOccupation(),
            motherName = motherName,
            motherOccupation = occupationGenerator.generateOccupation(),
            address = addressGenerator.generateAddress(countryCode)
        )
    }

    private data class NameCulture(
        val maleFirstNames: List<String>,
        val femaleFirstNames: List<String>,
        val lastNames: List<String>
    )
}
