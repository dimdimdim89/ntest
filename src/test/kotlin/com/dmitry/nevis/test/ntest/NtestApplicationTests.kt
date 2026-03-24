package com.dmitry.nevis.test.ntest

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class NtestApplicationTests {

	companion object {
		@Container
		@JvmStatic
		val container = OpenSearchTestContainer()

		@JvmStatic
		@DynamicPropertySource
		fun registerProps(registry: DynamicPropertyRegistry) {
			registry.add("opensearch.url") {
				"http://${container.host}:${container.getMappedPort(9200)}"
			}
		}
	}

	@Test
	fun contextLoads() {
	}

}
