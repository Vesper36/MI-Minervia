package edu.minervia.platform

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MinerviaApplication

fun main(args: Array<String>) {
    runApplication<MinerviaApplication>(*args)
}
