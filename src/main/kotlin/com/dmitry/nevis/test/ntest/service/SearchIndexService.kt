package com.dmitry.nevis.test.ntest.service

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.stereotype.Service

@Service
class SearchIndexService(
    private val client: OpenSearchClient
) {
    fun test() {
        val indexName = "test-index"
        client.indices().create { it.index(indexName) }
        client.index(
            IndexRequest.Builder<Test>()
                .index(indexName)
                .id("1")
                .document(Test()).build()
        )
    }

    fun search(): Test {
        val response = client.search(
            SearchRequest.Builder().index("test-index")
                .query(
                    Query.Builder()
                        .matchAll { it }
                        .build()
                ).build(),
            Test::class.java
        )
        return response.hits().hits().first().source()!!
    }
}

data class Test(
    val oneF: String = "123",
    val twoF: String = "Hop"
)