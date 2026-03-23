package com.dmitry.nevis.test.ntest.controllers

import java.time.Instant
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping(ApiPaths.API_V1)
class UserController {

	@PostMapping("/clients")
	fun createClient(@RequestBody request: CreateClientRequest): ResponseEntity<Client> =
		ResponseEntity.status(HttpStatus.CREATED).body(
			Client(
				id = UUID.randomUUID().toString(),
				firstName = request.firstName,
				lastName = request.lastName,
				email = request.email,
				description = request.description,
				socialLinks = request.socialLinks,
			)
		)

	@PostMapping("/clients/{id}/documents")
	fun createClientDocument(
		@PathVariable id: String,
		@RequestBody request: CreateDocumentRequest,
	): ResponseEntity<Document> =
		ResponseEntity.status(HttpStatus.CREATED).body(
			Document(
				id = UUID.randomUUID().toString(),
				clientId = id,
				title = request.title,
				content = request.content,
				createdAt = Instant.now(),
			)
		)
}
