package ru.pogosian.kazik

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class KazikApplication

fun main(args: Array<String>) {
    runApplication<KazikApplication>(*args)
}
