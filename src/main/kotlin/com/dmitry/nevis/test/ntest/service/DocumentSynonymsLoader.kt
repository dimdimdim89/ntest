package com.dmitry.nevis.test.ntest.service

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class DocumentSynonymsLoader {

    fun load(): List<String> {
        val resource = ClassPathResource(SYNONYMS_RESOURCE_PATH)

        return resource.inputStream.bufferedReader().useLines { lines ->
            lines.map(String::trim)
                .filter { it.isNotEmpty() }
                .filterNot { it.startsWith("#") }
                .toList()
        }
    }

    private companion object {
        const val SYNONYMS_RESOURCE_PATH = "search/document-synonyms.txt"
    }
}
