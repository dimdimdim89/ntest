package com.dmitry.nevis.test.ntest.controllers

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = exception.bindingResult.allErrors
            .firstOrNull()
            ?.let { error ->
                val fieldError = error as? FieldError
                if (fieldError != null) "${fieldError.field}: ${error.defaultMessage}" else error.defaultMessage
            }
            ?: "Validation failed"

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = message))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(exception: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = "Invalid request payload"))
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleMethodValidationException(exception: HandlerMethodValidationException): ResponseEntity<ErrorResponse> {
        val validationResult = exception.getParameterValidationResults().firstOrNull()
        val parameterName = validationResult?.getMethodParameter()?.parameterName ?: "request"
        val validationMessage = validationResult?.getResolvableErrors()
            ?.firstOrNull()
            ?.defaultMessage
            ?: "Validation failed"

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = "$parameterName: $validationMessage"))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(exception: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val message = exception.constraintViolations.firstOrNull()?.let { violation ->
            val propertyPath = violation.propertyPath.toString().substringAfterLast('.')
            "$propertyPath: ${violation.message}"
        } ?: "Validation failed"

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = message))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(exception: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.valueOf(exception.statusCode.value())
        return ResponseEntity.status(status)
            .body(ErrorResponse(message = exception.reason ?: status.reasonPhrase))
    }
}
