package com.dmitry.nevis.test.ntest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NtestApplication

fun main(args: Array<String>) {
	runApplication<NtestApplication>(*args)
}
