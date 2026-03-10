package com.example.userservice.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for application health checks.
 * This provides a simple endpoint for monitoring systems to verify the service is running.
 */
@RestController
class HealthController {

    /**
     * A simple health check endpoint that conforms to the acceptance criteria.
     * It responds with a 200 OK status and a JSON body indicating the service is 'UP'.
     * This endpoint is public and requires no authentication.
     *
     * @return A ResponseEntity containing a map with the health status.
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        val response = mapOf("status" to "UP")
        return ResponseEntity.ok(response)
    }
}
