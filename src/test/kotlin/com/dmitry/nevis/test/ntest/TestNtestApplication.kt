package com.dmitry.nevis.test.ntest

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<NtestApplication>().run(*args)
}
