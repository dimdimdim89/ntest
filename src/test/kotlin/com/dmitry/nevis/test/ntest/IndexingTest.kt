package com.dmitry.nevis.test.ntest

import com.dmitry.nevis.test.ntest.controllers.Client
import com.dmitry.nevis.test.ntest.controllers.Document
import com.dmitry.nevis.test.ntest.service.SearchIndexInitializer
import com.dmitry.nevis.test.ntest.service.SearchIndexProperties
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.GetRequest
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.json.JsonData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class IndexingTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val openSearchClient: OpenSearchClient,
    @Autowired private val indexInitializer: SearchIndexInitializer,
    @Autowired private val properties: SearchIndexProperties,
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

    @BeforeEach
    fun setUp() {
        openSearchClient.indices().delete {
            it.index(properties.clientIndex, properties.documentIndex)
                .ignoreUnavailable(true)
                .allowNoIndices(true)
        }
        indexInitializer.createIndexes()
    }

    @Test
    fun `create client indexes document and upserts by business key`() {
        val firstResponse = createClient(
            """
            {
              "firstName": "John",
              "lastName": "Doe",
              "email": "john@example.com",
              "description": "first version",
              "socialLinks": ["https://x.example/john"]
            }
            """.trimIndent()
        )

        val secondResponse = createClient(
            """
            {
              "firstName": "John",
              "lastName": "Doe",
              "email": "john@example.com",
              "description": "updated version",
              "socialLinks": ["https://linkedin.example/john"]
            }
            """.trimIndent()
        )

        assertEquals(firstResponse.id, secondResponse.id)

        val indexedClient = getIndexedClient(firstResponse.id)
        assertNotNull(indexedClient)
        assertEquals(firstResponse.id, indexedClient["clientId"].stringValue())
        assertEquals("John", indexedClient["firstName"].stringValue())
        assertEquals("Doe", indexedClient["lastName"].stringValue())
        assertEquals("john@example.com", indexedClient["email"].stringValue())
        assertEquals("updated version", indexedClient["description"].stringValue())
        assertEquals("https://linkedin.example/john", indexedClient["socialLinks"][0].stringValue())

        val indexedClients = findAllIndexedClients()
        assertEquals(1, indexedClients.size)
    }

    @Test
    fun `create document indexes document for existing client`() {
        val clientResponse = createClient(
            """
            {
              "firstName": "Anna",
              "lastName": "Smith",
              "email": "anna@example.com"
            }
            """.trimIndent()
        )

        val documentResponse = createDocument(
            clientResponse.id,
            """
            {
              "title": "CV",
              "content": "Senior Kotlin developer"
            }
            """.trimIndent()
        )

        val indexedDocument = getIndexedDocument(documentResponse.id)
        assertNotNull(indexedDocument)
        assertEquals(documentResponse.id, indexedDocument["documentId"].stringValue())
        assertEquals(clientResponse.id, indexedDocument["clientId"].stringValue())
        assertEquals("CV", indexedDocument["title"].stringValue())
        assertEquals("Senior Kotlin developer", indexedDocument["content"].stringValue())
        assertEquals(documentResponse.createdAt.toString(), indexedDocument["createdAt"].stringValue())
    }

    @Test
    fun `create document returns 404 when client does not exist`() {
        val missingClientId = UUID.randomUUID().toString()

        mockMvc.perform(
            post("/api/v1/clients/{id}/documents", missingClientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "CV",
                      "content": "Missing parent"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isNotFound)
    }

    private fun createClient(body: String): Client {
        val mvcResult = mockMvc.perform(
            post("/api/v1/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readValue(mvcResult.response.contentAsString, Client::class.java)
    }

    private fun createDocument(clientId: String, body: String): Document {
        val mvcResult = mockMvc.perform(
            post("/api/v1/clients/{id}/documents", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readValue(mvcResult.response.contentAsString, Document::class.java)
    }

    private fun getIndexedClient(clientId: String): JsonNode? {
        return getIndexedSource(properties.clientIndex, clientId)
    }

    private fun getIndexedDocument(documentId: String): JsonNode? {
        return getIndexedSource(properties.documentIndex, documentId)
    }

    private fun getIndexedSource(index: String, id: String): JsonNode? {
        val response = openSearchClient.get(
            GetRequest.of {
                it.index(index)
                    .id(id)
            },
            JsonData::class.java
        )

        return response.source()?.let { objectMapper.readTree(it.toJson().toString()) }
    }

    private fun findAllIndexedClients(): List<JsonNode> {
        val response = openSearchClient.search(
            SearchRequest.Builder()
                .index(properties.clientIndex)
                .query(
                    Query.Builder()
                        .matchAll { it }
                        .build()
                )
                .build(),
            JsonData::class.java
        )

        return response.hits().hits()
            .mapNotNull { it.source() }
            .map { objectMapper.readTree(it.toJson().toString()) }
    }
}
