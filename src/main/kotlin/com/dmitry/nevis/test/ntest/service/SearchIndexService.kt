package com.dmitry.nevis.test.ntest.service

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.query_dsl.TermQuery
import org.opensearch.client.opensearch.core.DeleteRequest
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class SearchIndexService(
    private val client: OpenSearchClient,
    private val properties: SearchIndexProperties,
) {
    fun indexClient(clientIndexRequest: ClientIndexRequest): ClientIndexResponse {
        val duplicateIds = findDuplicateClientIds(clientIndexRequest)
        val clientId = duplicateIds.firstOrNull()?.let(UUID::fromString) ?: UUID.randomUUID()

        deleteClientsByIds(duplicateIds)

        val indexedClient = IndexedClient(
            clientId = clientId,
            firstName = clientIndexRequest.firstName,
            lastName = clientIndexRequest.lastName,
            email = clientIndexRequest.email,
            description = clientIndexRequest.description,
            socialLinks = clientIndexRequest.socialLinks,
        )

        client.index(
            IndexRequest.of<IndexedClient> {
                it.index(properties.clientIndex)
                    .id(clientId.toString())
                    .refresh(Refresh.WaitFor)
                    .document(indexedClient)
            }
        )

        return ClientIndexResponse(clientId)
    }

    fun indexDocument(documentIndexRequest: DocumentIndexRequest): DocumentIndexResponse {
        TODO("Not implemented yet")
    }

    private fun findDuplicateClientIds(clientIndexRequest: ClientIndexRequest): List<String> {
        val duplicateSearchResponse = client.search(
            SearchRequest.Builder()
                .index(properties.clientIndex)
                .size(properties.maxDuplicateScanSize)
                .ignoreUnavailable(true)
                .query(exactDuplicateClientQuery(clientIndexRequest))
                .build(),
            IndexedClient::class.java
        )

        return duplicateSearchResponse.hits().hits()
            .mapNotNull { it.id() }
    }

    private fun deleteClientsByIds(clientIds: List<String>) {
        clientIds.forEach { existingDocumentId ->
            client.delete(
                DeleteRequest.of {
                    it.index(properties.clientIndex)
                        .id(existingDocumentId)
                        .refresh(Refresh.WaitFor)
                }
            )
        }
    }

    private fun exactDuplicateClientQuery(clientIndexRequest: ClientIndexRequest): Query {
        return BoolQuery.of { bool ->
            bool.filter(exactTermQuery("firstName.keyword", clientIndexRequest.firstName))
                .filter(exactTermQuery("lastName.keyword", clientIndexRequest.lastName))
                .filter(exactTermQuery("email.keyword", clientIndexRequest.email))
        }.toQuery()
    }

    private fun exactTermQuery(field: String, value: String): Query {
        return TermQuery.of { term ->
            term.field(field)
                .value(FieldValue.of(value))
        }.toQuery()
    }
}

data class ClientIndexRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val description: String? = null,
    val socialLinks: List<String> = emptyList(),
)

data class ClientIndexResponse(
    val clientId: UUID
)

data class DocumentIndexRequest(
    val clientId: UUID,
    val title: String,
    val content: String
)

data class DocumentIndexResponse(
    val documentId: UUID,
    val clientId: UUID,
    val createdAt: LocalDateTime
)

data class IndexedClient(
    val clientId: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val description: String? = null,
    val socialLinks: List<String> = emptyList(),
)
