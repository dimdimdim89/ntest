package com.dmitry.nevis.test.ntest.controllers

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class CreateClientRequest(
	@field:NotBlank
	val firstName: String,

	@field:NotBlank
	val lastName: String,

	@field:NotBlank
	@field:Email
	val email: String,

	val description: String? = null,
	val socialLinks: List<String> = emptyList(),
)

data class CreateDocumentRequest(
	@field:NotBlank
	val title: String,

	@field:NotBlank
	val content: String,
)

data class Client(
	val id: String,
	val firstName: String,
	val lastName: String,
	val email: String,
	val description: String? = null,
	val socialLinks: List<String> = emptyList(),
)

data class Document(
	val id: String,
	val clientId: String,
	val title: String,
	val content: String,
	val createdAt: Instant,
)

data class SearchDocument(
	val id: String,
	val clientId: String,
	val title: String,
	val summary: String,
	val createdAt: Instant,
)

data class SearchResults(
	val query: String,
	val clients: List<Client>,
	val documents: List<SearchDocument>,
)

data class ErrorResponse(
	val message: String,
)
