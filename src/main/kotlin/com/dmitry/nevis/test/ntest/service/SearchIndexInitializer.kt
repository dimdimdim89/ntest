package com.dmitry.nevis.test.ntest.service

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.analysis.Analyzer
import org.opensearch.client.opensearch._types.analysis.CustomAnalyzer
import org.opensearch.client.opensearch._types.mapping.Property
import org.opensearch.client.opensearch.indices.IndexSettings
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class SearchIndexInitializer(
    private val client: OpenSearchClient,
    private val properties: SearchIndexProperties,
    private val documentSynonymsLoader: DocumentSynonymsLoader,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(SearchIndexInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        createIndexes()
    }

    fun createIndexes() {
        createClientIndexIfMissing()
        createDocumentIndexIfMissing()
    }

    private fun createClientIndexIfMissing() {
        if (client.indices().exists { it.index(properties.clientIndex) }.value()) {
            log.info("Skipping client index creation, index '{}' already exists", properties.clientIndex)
            return
        }

        log.info("Creating client index '{}'", properties.clientIndex)
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
            log.info("Skipping document index creation, index '{}' already exists", properties.documentIndex)
            return
        }

        val documentSynonyms = documentSynonymsLoader.load()

        log.info(
            "Creating document index '{}' with {} configured synonym rules",
            properties.documentIndex,
            documentSynonyms.size
        )
        client.indices().create { request ->
            request.index(properties.documentIndex)
                .settings(documentIndexSettings(documentSynonyms))
                .mappings { mappings ->
                    mappings
                        .properties("documentId", Property.of { it.keyword { keyword -> keyword } })
                        .properties("clientId", Property.of { it.keyword { keyword -> keyword } })
                        .properties("title", Property.of { it.text { text -> text } })
                        .properties("content", Property.of { property ->
                            property.text { text ->
                                text.searchAnalyzer(DOCUMENT_SEARCH_ANALYZER)
                            }
                        })
                        .properties("createdAt", Property.of { it.date { date -> date } })
                }
        }
    }

    private fun documentIndexSettings(documentSynonyms: List<String>): IndexSettings {
        return IndexSettings.of { settings ->
            settings.analysis(
                IndexSettingsAnalysis.of { analysis ->
                    analysis
                        .filter(
                            DOCUMENT_SYNONYM_FILTER,
                            org.opensearch.client.opensearch._types.analysis.TokenFilter.of { filter ->
                                filter.definition { definition ->
                                    definition.synonymGraph { synonymGraph ->
                                        synonymGraph
                                            .synonyms(documentSynonyms)
                                            .lenient(false)
                                    }
                                }
                            }
                        )
                        .analyzer(
                            DOCUMENT_SEARCH_ANALYZER,
                            Analyzer.of { analyzer ->
                                analyzer.custom(
                                    CustomAnalyzer.of { custom ->
                                        custom
                                            .tokenizer("standard")
                                            .filter("lowercase", DOCUMENT_SYNONYM_FILTER)
                                    }
                                )
                            }
                        )
                }
            )
        }
    }

    private companion object {
        const val DOCUMENT_SEARCH_ANALYZER = "document_search_analyzer"
        const val DOCUMENT_SYNONYM_FILTER = "document_synonym_filter"
    }
}
