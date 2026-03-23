package com.dmitry.nevis.test.ntest.controllers

import java.time.Instant

data class CreateClientRequest(
	val firstName: String,
	val lastName: String,
	val email: String,
	val description: String? = null,
	val socialLinks: List<String> = emptyList(),
)

data class CreateDocumentRequest(
	val title: String,
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

data class SearchResults(
	val query: String,
	val clients: List<Client>,
	val documents: List<Document>,
)
