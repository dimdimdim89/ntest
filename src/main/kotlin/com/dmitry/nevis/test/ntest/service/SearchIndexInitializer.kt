package com.dmitry.nevis.test.ntest.service

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.Property
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class SearchIndexInitializer(
    private val client: OpenSearchClient,
    private val properties: SearchIndexProperties,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        createIndexes()
    }

    fun createIndexes() {
        createClientIndexIfMissing()
        createDocumentIndexIfMissing()
    }

    private fun createClientIndexIfMissing() {
        if (client.indices().exists { it.index(properties.clientIndex) }.value()) {
            return
        }

        client.indices().create { request ->
            request.index(properties.clientIndex)
                .mappings { mappings ->
                    mappings
                        .properties("clientId", Property.of { it.keyword { keyword -> keyword } })
                        .properties("firstName", Property.of { property ->
                            property.text { text ->
                                text.fields("keyword", Property.of { it.keyword { keyword -> keyword } })
                            }
                        })
                        .properties("lastName", Property.of { property ->
                            property.text { text ->
                                text.fields("keyword", Property.of { it.keyword { keyword -> keyword } })
                            }
                        })
                        .properties("email", Property.of { it.keyword { keyword -> keyword } })
                        .properties("description", Property.of { it.text { text -> text } })
                        .properties("socialLinks", Property.of { it.keyword { keyword -> keyword } })
                }
        }
    }

    private fun createDocumentIndexIfMissing() {
        if (client.indices().exists { it.index(properties.documentIndex) }.value()) {
            return
        }

        client.indices().create { request ->
            request.index(properties.documentIndex)
                .mappings { mappings ->
                    mappings
                        .properties("documentId", Property.of { it.keyword { keyword -> keyword } })
                        .properties("clientId", Property.of { it.keyword { keyword -> keyword } })
                        .properties("title", Property.of { it.text { text -> text } })
                        .properties("content", Property.of { it.text { text -> text } })
                        .properties("createdAt", Property.of { it.date { date -> date } })
                }
        }
    }
}
