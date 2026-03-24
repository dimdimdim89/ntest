package com.dmitry.nevis.test.ntest.config

import com.dmitry.nevis.test.ntest.service.SearchIndexProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(SearchIndexProperties::class)
class SearchIndexConfiguration
