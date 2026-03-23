package com.dmitry.nevis.test.ntest.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.API_V1)
class SearchController {

	@GetMapping("/search")
	fun search(
		@RequestParam q: String,
		@RequestParam(required = false) type: String?,
	): SearchResults {
		val normalizedType = type?.trim()?.lowercase()

		return when (normalizedType) {
			"client" -> SearchResults(query = q, clients = emptyList(), documents = emptyList())
			"document" -> SearchResults(query = q, clients = emptyList(), documents = emptyList())
			else -> SearchResults(query = q, clients = emptyList(), documents = emptyList())
		}
	}
}
