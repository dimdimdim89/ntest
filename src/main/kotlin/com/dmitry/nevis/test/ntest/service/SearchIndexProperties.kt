package com.dmitry.nevis.test.ntest.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "search-index")
data class SearchIndexProperties(
    val clientIndex: String = "clients",
    val documentIndex: String = "documents",
)
