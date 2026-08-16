package com.cricory.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CricoryBackendApplication

fun main(args: Array<String>) {
	runApplication<CricoryBackendApplication>(*args)
}
