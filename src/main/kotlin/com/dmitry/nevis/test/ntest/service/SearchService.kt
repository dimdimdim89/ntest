package com.dmitry.nevis.test.ntest.service

import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.query_dsl.WildcardQuery
import org.opensearch.client.opensearch.core.SearchRequest as OpenSearchSearchRequest
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Service
class SearchService(
    private val client: OpenSearchClient,
    private val properties: SearchIndexProperties,
    private val objectMapper: ObjectMapper,
) {
    fun search(request: SearchRequest): SearchResponse {
        val normalizedQuery = request.query.trim()
        if (normalizedQuery.isEmpty()) {
            return SearchResponse(
                query = normalizedQuery,
                clients = emptyList(),
                documents = emptyList(),
            )
        }

        val clients = if (request.type == SearchType.DOCUMENT) {
            emptyList()
        } else {
            searchClients(normalizedQuery)
        }

        val documents = if (request.type == SearchType.CLIENT) {
            emptyList()
        } else {
            searchDocuments(normalizedQuery)
        }

        return SearchResponse(
            query = normalizedQuery,
            clients = clients,
            documents = documents,
        )
    }

    private fun searchClients(query: String): List<SearchClientResult> {
        val response = client.search(
            OpenSearchSearchRequest.Builder()
                .index(properties.clientIndex)
                .size(properties.searchResultSize)
                .query(clientSearchQuery(query))
                .build(),
            JsonData::class.java
        )

        return response.hits().hits()
            .mapNotNull { it.source() }
            .map { source -> objectMapper.readValue(source.toJson().toString(), SearchClientResult::class.java) }
    }

    private fun searchDocuments(query: String): List<SearchDocumentResult> {
        val response = client.search(
            OpenSearchSearchRequest.Builder()
                .index(properties.documentIndex)
                .size(properties.searchResultSize)
                .query(documentSearchQuery(query))
                .build(),
            JsonData::class.java
        )

        return response.hits().hits()
            .mapNotNull { it.source() }
            .map { source -> objectMapper.readValue(source.toJson().toString(), SearchDocumentResult::class.java) }
    }

    private fun clientSearchQuery(query: String): Query {
        return BoolQuery.of { bool ->
            bool.should(
                MultiMatchQuery.of { multiMatch ->
                    multiMatch.query(query)
                        .fields("firstName", "lastName", "description")
                }.toQuery()
            )
                .should(
                    WildcardQuery.of { wildcard ->
                        wildcard.field("email")
                            .wildcard("*${query.lowercase()}*")
                            .caseInsensitive(true)
                    }.toQuery()
                )
                .minimumShouldMatch("1")
        }.toQuery()
    }

    private fun documentSearchQuery(query: String): Query {
        return MatchQuery.of { match ->
            match.field("content")
                .query(FieldValue.of(query))
        }.toQuery()
    }
}

data class SearchRequest(
    val query: String,
    val type: SearchType? = null,
)

data class SearchResponse(
    val query: String,
    val clients: List<SearchClientResult>,
    val documents: List<SearchDocumentResult>,
)

enum class SearchType {
    CLIENT,
    DOCUMENT,
}

data class SearchClientResult(
    val clientId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val description: String? = null,
    val socialLinks: List<String> = emptyList(),
)

data class SearchDocumentResult(
    val documentId: String,
    val clientId: String,
    val title: String,
    val content: String,
    val createdAt: Instant,
)
