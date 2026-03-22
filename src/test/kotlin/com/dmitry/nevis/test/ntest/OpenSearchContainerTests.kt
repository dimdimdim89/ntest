package com.dmitry.nevis.test.ntest

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
class OpenSearchContainerTests {

	@Test
	fun opensearchContainerStarts() {
		assertTrue(opensearch.isRunning)
	}

	companion object {
		@Container
		@JvmStatic
		val opensearch = OpenSearchTestContainer()
	}
}
