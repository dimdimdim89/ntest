package com.dmitry.nevis.test.ntest.config

import org.apache.hc.core5.http.HttpHost
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenSearchConfig(
    @Value("\${opensearch.url:http://localhost:9200}")
    private val openSearchUrl: String,
) {

    @Bean(destroyMethod = "close")
    fun openSearchTransport(): OpenSearchTransport {
        return ApacheHttpClient5TransportBuilder
            .builder(HttpHost.create(openSearchUrl))
            .build()
    }

    @Bean
    fun openSearchClient(transport: OpenSearchTransport): OpenSearchClient = OpenSearchClient(transport)
}
