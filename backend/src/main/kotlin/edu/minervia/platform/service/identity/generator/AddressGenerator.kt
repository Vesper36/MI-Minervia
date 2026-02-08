package edu.minervia.platform.service.identity.generator

import org.springframework.stereotype.Component
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

@Component
class AddressGenerator {
    private val random = SecureRandom().asKotlinRandom()

    private val polishCities = listOf("Warsaw", "Krakow", "Gdansk", "Wroclaw", "Poznan", "Lodz", "Katowice")
    private val polishStreets = listOf("Kwiatowa", "Sloneczna", "Polna", "Lipowa", "Mickiewicza", "Szkolna", "Kosciuszki")

    private val usCities = listOf("New York", "Chicago", "Seattle", "Austin", "Denver", "Boston", "San Diego")
    private val usStreets = listOf("Maple", "Oak", "Pine", "Cedar", "Washington", "Lake", "Hill")
    private val usStates = listOf("NY", "IL", "WA", "TX", "CO", "MA", "CA")

    private val ukCities = listOf("London", "Manchester", "Birmingham", "Leeds", "Bristol", "Liverpool")
    private val ukStreets = listOf("High Street", "Station Road", "Church Lane", "Victoria Road", "Park Avenue")
    private val ukPostcodes = listOf("SW1A 1AA", "EC1A 1BB", "M1 1AE", "B1 1AA", "LS1 1UR", "BS1 5AH")

    private val germanCities = listOf("Berlin", "Munich", "Hamburg", "Cologne", "Frankfurt", "Stuttgart")
    private val germanStreets = listOf("Hauptstrasse", "Bahnhofstrasse", "Schulstrasse", "Gartenweg", "Bergstrasse")

    private val chinaCities = listOf("Beijing", "Shanghai", "Shenzhen", "Guangzhou", "Chengdu", "Hangzhou")
    private val chinaDistricts = listOf("Haidian", "Pudong", "Nanshan", "Tianhe", "Wuhou")
    private val chinaStreets = listOf("Renmin Rd", "Zhongshan Rd", "Jiefang Rd", "Xinhua Rd", "Gongyuan Rd")

    private val canadaCities = listOf("Toronto", "Vancouver", "Montreal", "Calgary", "Ottawa")
    private val canadaStreets = listOf("King", "Queen", "Main", "Broadway", "Church", "Elm")
    private val canadaProvinces = listOf("ON", "BC", "QC", "AB", "ON")
    private val canadaPostcodes = listOf("M5V 2T6", "V6B 1H4", "H2Y 1C6", "T2P 1J9", "K1P 5G4")

    private val australiaCities = listOf("Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide")
    private val australiaStreets = listOf("George", "Elizabeth", "Collins", "King", "Queen")
    private val australiaStates = listOf("NSW", "VIC", "QLD", "WA", "SA")
    private val australiaPostcodes = listOf("2000", "3000", "4000", "6000", "5000")

    fun generateAddress(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "PL" -> polishAddress()
            "US" -> usAddress()
            "GB", "UK" -> ukAddress()
            "DE", "AT", "CH" -> germanAddress()
            "CN", "TW", "HK" -> chinaAddress()
            "CA" -> canadaAddress()
            "AU" -> australiaAddress()
            else -> usAddress()
        }
    }

    private fun polishAddress(): String {
        val city = polishCities.random(random)
        val street = polishStreets.random(random)
        val number = 1 + random.nextInt(200)
        val postal = "${10 + random.nextInt(90)}-${100 + random.nextInt(900)}"
        return "ul. $street $number, $postal $city, Poland"
    }

    private fun usAddress(): String {
        val city = usCities.random(random)
        val street = usStreets.random(random)
        val state = usStates.random(random)
        val number = 100 + random.nextInt(900)
        val zip = 10000 + random.nextInt(90000)
        return "$number $street St, $city, $state $zip, USA"
    }

    private fun ukAddress(): String {
        val city = ukCities.random(random)
        val street = ukStreets.random(random)
        val number = 1 + random.nextInt(200)
        val postcode = ukPostcodes.random(random)
        return "$number $street, $city $postcode, United Kingdom"
    }

    private fun germanAddress(): String {
        val city = germanCities.random(random)
        val street = germanStreets.random(random)
        val number = 1 + random.nextInt(200)
        val postal = 10000 + random.nextInt(90000)
        return "$street $number, $postal $city, Germany"
    }

    private fun chinaAddress(): String {
        val city = chinaCities.random(random)
        val district = chinaDistricts.random(random)
        val street = chinaStreets.random(random)
        val number = 1 + random.nextInt(200)
        return "No. $number, $street, $district, $city, China"
    }

    private fun canadaAddress(): String {
        val city = canadaCities.random(random)
        val street = canadaStreets.random(random)
        val province = canadaProvinces.random(random)
        val number = 10 + random.nextInt(500)
        val postcode = canadaPostcodes.random(random)
        return "$number $street St, $city, $province $postcode, Canada"
    }

    private fun australiaAddress(): String {
        val city = australiaCities.random(random)
        val street = australiaStreets.random(random)
        val state = australiaStates.random(random)
        val number = 10 + random.nextInt(500)
        val postcode = australiaPostcodes.random(random)
        return "$number $street St, $city, $state $postcode, Australia"
    }
}
