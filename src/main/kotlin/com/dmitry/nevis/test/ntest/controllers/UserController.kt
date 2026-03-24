package com.dmitry.nevis.test.ntest.controllers

import com.dmitry.nevis.test.ntest.service.ClientIndexRequest
import com.dmitry.nevis.test.ntest.service.DocumentIndexRequest
import com.dmitry.nevis.test.ntest.service.IndexService
import java.time.Instant
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping(ApiPaths.API_V1)
class UserController(
	private val indexService: IndexService,
) {

	@PostMapping("/clients")
	fun createClient(@RequestBody request: CreateClientRequest): ResponseEntity<Client> {
		val response = indexService.indexClient(
			ClientIndexRequest(
				firstName = request.firstName,
				lastName = request.lastName,
				email = request.email,
				description = request.description,
				socialLinks = request.socialLinks,
			)
		)

		return ResponseEntity.status(HttpStatus.CREATED).body(
			Client(
				id = response.clientId.toString(),
				firstName = request.firstName,
				lastName = request.lastName,
				email = request.email,
				description = request.description,
				socialLinks = request.socialLinks,
			)
		)
	}

	@PostMapping("/clients/{id}/documents")
	fun createClientDocument(
		@PathVariable id: String,
		@RequestBody request: CreateDocumentRequest,
	): ResponseEntity<Document> {
		val clientId = parseClientId(id)
		val response = try {
			indexService.indexDocument(
				DocumentIndexRequest(
					clientId = clientId,
					title = request.title,
					content = request.content,
				)
			)
		} catch (exception: NoSuchElementException) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND, exception.message, exception)
		}

		return ResponseEntity.status(HttpStatus.CREATED).body(
			Document(
				id = response.documentId.toString(),
				clientId = response.clientId.toString(),
				title = request.title,
				content = request.content,
				createdAt = response.createdAt,
			)
		)
	}

	private fun parseClientId(rawClientId: String): UUID {
		return try {
			UUID.fromString(rawClientId)
		} catch (exception: IllegalArgumentException) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid client id: $rawClientId", exception)
		}
	}
}
