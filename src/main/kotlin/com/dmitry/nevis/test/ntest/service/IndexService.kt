package com.dmitry.nevis.test.ntest.service

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch.core.ExistsRequest
import org.opensearch.client.opensearch.core.IndexRequest
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Service
class IndexService(
    private val client: OpenSearchClient,
    private val properties: SearchIndexProperties,
) {
    fun indexClient(clientIndexRequest: ClientIndexRequest): ClientIndexResponse {
        val clientId = clientIdFor(clientIndexRequest)

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
        requireClientExists(documentIndexRequest.clientId)

        val documentId = UUID.randomUUID()
        val createdAt = Instant.now()
        val indexedDocument = IndexedDocument(
            documentId = documentId,
            clientId = documentIndexRequest.clientId,
            title = documentIndexRequest.title,
            content = documentIndexRequest.content,
            createdAt = createdAt.toString(),
        )

        client.index(
            IndexRequest.of<IndexedDocument> {
                it.index(properties.documentIndex)
                    .id(documentId.toString())
                    .refresh(Refresh.WaitFor)
                    .document(indexedDocument)
            }
        )

        return DocumentIndexResponse(
            documentId = documentId,
            clientId = documentIndexRequest.clientId,
            createdAt = createdAt,
        )
    }

    private fun clientIdFor(clientIndexRequest: ClientIndexRequest): UUID {
        val businessKey = listOf(
            clientIndexRequest.firstName,
            clientIndexRequest.lastName,
            clientIndexRequest.email,
        ).joinToString("|")

        return UUID.nameUUIDFromBytes(businessKey.toByteArray(StandardCharsets.UTF_8))
    }

    private fun requireClientExists(clientId: UUID) {
        val clientExists = client.exists(
            ExistsRequest.of {
                it.index(properties.clientIndex)
                    .id(clientId.toString())
            }
        )

        if (!clientExists.value()) {
            throw NoSuchElementException("Client $clientId was not found")
        }
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
    val createdAt: Instant
)

data class IndexedClient(
    val clientId: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val description: String? = null,
    val socialLinks: List<String> = emptyList(),
)

data class IndexedDocument(
    val documentId: UUID,
    val clientId: UUID,
    val title: String,
    val content: String,
    val createdAt: String,
)
