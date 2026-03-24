package com.dmitry.nevis.test.ntest

import com.dmitry.nevis.test.ntest.controllers.Client
import com.dmitry.nevis.test.ntest.controllers.Document
import com.dmitry.nevis.test.ntest.controllers.ErrorResponse
import com.dmitry.nevis.test.ntest.controllers.SearchResults
import com.dmitry.nevis.test.ntest.service.SearchIndexInitializer
import com.dmitry.nevis.test.ntest.service.SearchIndexProperties
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import tools.jackson.databind.ObjectMapper

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class SearchingTest(
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
    fun `search finds client by email fragment`() {
        // init
        val client = createClient(
            """
            {
              "firstName": "John",
              "lastName": "Doe",
              "email": "john.doe@neviswealth.com",
              "description": "wealth manager"
            }
            """.trimIndent()
        )

        // when
        val response = search(query = "NevisWealth")

        // then
        assertEquals("NevisWealth", response.query)
        assertEquals(1, response.clients.size)
        assertEquals(client.id, response.clients.single().id)
        assertTrue(response.documents.isEmpty())
    }

    @Test
    fun `search finds client by description`() {
        // init
        val client = createClient(
            """
            {
              "firstName": "Anna",
              "lastName": "Smith",
              "email": "anna@example.com",
              "description": "passport verification specialist"
            }
            """.trimIndent()
        )

        // when
        val response = search(query = "passport")

        // then
        assertEquals(1, response.clients.size)
        assertEquals(client.id, response.clients.single().id)
    }

    @Test
    fun `search with document type returns only documents`() {
        // init
        val client = createClient(
            """
            {
              "firstName": "Jane",
              "lastName": "Roe",
              "email": "jane@example.com"
            }
            """.trimIndent()
        )

        val document = createDocument(
            client.id,
            """
            {
              "title": "Proof",
              "content": "utility bill for address confirmation"
            }
            """.trimIndent()
        )

        // when
        val response = search(query = "utility", type = "document")

        // then
        assertTrue(response.clients.isEmpty())
        assertEquals(1, response.documents.size)
        assertEquals(document.id, response.documents.single().id)
    }

    @Test
    fun `search finds document by configured synonym`() {
        // init
        val firstClient = createClient(
            """
            {
              "firstName": "Laura",
              "lastName": "Bell",
              "email": "laura@example.com"
            }
            """.trimIndent()
        )

        val synonymMatchedDocument = createDocument(
            firstClient.id,
            """
            {
              "title": "Residence evidence",
              "content": "Please attach a recent utility bill."
            }
            """.trimIndent()
        )

        val secondClient = createClient(
            """
            {
              "firstName": "Mark",
              "lastName": "Stone",
              "email": "mark@example.com"
            }
            """.trimIndent()
        )

        val exactMatchedDocument = createDocument(
            secondClient.id,
            """
            {
              "title": "Verification checklist",
              "content": "Accepted address proof includes government-issued residence confirmation."
            }
            """.trimIndent()
        )

        createDocument(
            secondClient.id,
            """
            {
              "title": "Identity file",
              "content": "Passport copy and selfie verification."
            }
            """.trimIndent()
        )

        val thirdClient = createClient(
            """
            {
              "firstName": "Nina",
              "lastName": "Cole",
              "email": "nina@example.com"
            }
            """.trimIndent()
        )

        createDocument(
            thirdClient.id,
            """
            {
              "title": "Income evidence",
              "content": "Latest bank statement and salary confirmation."
            }
            """.trimIndent()
        )

        // when
        val response = search(query = "address proof", type = "document")

        // then
        assertTrue(response.clients.isEmpty())
        assertEquals(2, response.documents.size)
        assertEquals(
            setOf(synonymMatchedDocument.id, exactMatchedDocument.id),
            response.documents.map { it.id }.toSet()
        )
    }

    @Test
    fun `search with client type returns only clients`() {
        // init
        val client = createClient(
            """
            {
              "firstName": "Peter",
              "lastName": "Miles",
              "email": "peter@example.com",
              "description": "passport owner"
            }
            """.trimIndent()
        )

        createDocument(
            client.id,
            """
            {
              "title": "Passport copy",
              "content": "passport number and issue date"
            }
            """.trimIndent()
        )

        // when
        val response = search(query = "passport", type = "client")

        // then
        assertEquals(1, response.clients.size)
        assertEquals(client.id, response.clients.single().id)
        assertTrue(response.documents.isEmpty())
    }

    @Test
    fun `search without type returns both clients and documents`() {
        // init
        val client = createClient(
            """
            {
              "firstName": "Olga",
              "lastName": "Stone",
              "email": "olga@example.com",
              "description": "passport check"
            }
            """.trimIndent()
        )

        val document = createDocument(
            client.id,
            """
            {
              "title": "Passport notes",
              "content": "passport scan attached"
            }
            """.trimIndent()
        )

        // when
        val response = search(query = "passport")

        // then
        assertEquals(1, response.clients.size)
        assertEquals(client.id, response.clients.single().id)
        assertEquals(1, response.documents.size)
        assertEquals(document.id, response.documents.single().id)
    }

    @Test
    fun `search returns empty results when nothing matches`() {
        // init
        createClient(
            """
            {
              "firstName": "Max",
              "lastName": "Payne",
              "email": "max@example.com"
            }
            """.trimIndent()
        )

        // when
        val response = search(query = "definitely-not-found")

        // then
        assertTrue(response.clients.isEmpty())
        assertTrue(response.documents.isEmpty())
    }

    @Test
    fun `search returns 400 for blank query`() {
        // init
        val request = get("/api/v1/search")
            .param("q", "   ")

        // when
        val mvcResult = mockMvc.perform(
            request
        )
            .andExpect(status().isBadRequest)
            .andReturn()

        // then
        val response = objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)
        assertTrue(response.message.contains("q"))
    }

    @Test
    fun `search returns 400 for invalid type`() {
        // init
        val request = get("/api/v1/search")
            .param("q", "passport")
            .param("type", "wrong")

        // when
        val mvcResult = mockMvc.perform(
            request
        )
            .andExpect(status().isBadRequest)
            .andReturn()

        // then
        val response = objectMapper.readValue(mvcResult.response.contentAsString, ErrorResponse::class.java)
        assertTrue(response.message.contains("Invalid search type"))
    }

    private fun search(query: String, type: String? = null): SearchResults {
        val requestBuilder = get("/api/v1/search")
            .param("q", query)

        if (type != null) {
            requestBuilder.param("type", type)
        }

        val mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readValue(mvcResult.response.contentAsString, SearchResults::class.java)
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
}
