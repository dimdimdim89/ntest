package com.dmitry.nevis.test.ntest

import com.dmitry.nevis.test.ntest.service.SearchIndexService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers
@SpringBootTest
class SearchIndexTest(
    @Autowired private val subject: SearchIndexService
) {

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
    fun `index test`() {
        //subject.test()
        println("good:${UUID.randomUUID()}")
        Thread.sleep(1000)
        //println(subject.search())
    }

}