package com.dmitry.nevis.test.ntest

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	fun opensearchContainer(): OpenSearchTestContainer = OpenSearchTestContainer()
}
