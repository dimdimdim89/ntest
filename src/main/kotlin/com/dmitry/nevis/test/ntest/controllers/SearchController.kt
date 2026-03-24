package com.dmitry.nevis.test.ntest.controllers

import com.dmitry.nevis.test.ntest.service.SearchRequest
import com.dmitry.nevis.test.ntest.service.SearchService
import com.dmitry.nevis.test.ntest.service.SearchType
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping(ApiPaths.API_V1)
class SearchController(
	private val searchService: SearchService,
) {

	@GetMapping("/search")
	fun search(
		@RequestParam q: String,
		@RequestParam(required = false) type: String?,
	): SearchResults {
		if (q.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "q: must not be blank")
		}

		val response = searchService.search(
			SearchRequest(
				query = q,
				type = parseType(type),
			)
		)

		return SearchResults(
			query = response.query,
			clients = response.clients.map { client ->
				Client(
					id = client.clientId,
					firstName = client.firstName,
					lastName = client.lastName,
					email = client.email,
					description = client.description,
					socialLinks = client.socialLinks,
				)
			},
			documents = response.documents.map { document ->
				Document(
					id = document.documentId,
					clientId = document.clientId,
					title = document.title,
					content = document.content,
					createdAt = document.createdAt,
				)
			},
		)
	}

	private fun parseType(type: String?): SearchType? {
		val normalizedType = type?.trim()?.lowercase() ?: return null

		return when (normalizedType) {
			"client" -> SearchType.CLIENT
			"document" -> SearchType.DOCUMENT
			else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid search type: $type")
		}
	}
}
