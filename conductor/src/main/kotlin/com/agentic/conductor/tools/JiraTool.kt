package com.agentic.conductor.tools

import com.agentic.conductor.models.ToolResult
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import java.util.Base64

class JiraTool(private val config: JiraConfig) : Tool {

    override val name: String = "jira"
    private val logger = LoggerFactory.getLogger(JiraTool::class.java)

    private val jiraApi: JiraApi

    init {
        val objectMapper = ObjectMapper().registerModule(KotlinModule())
        jiraApi = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build()
            .create(JiraApi::class.java)
    }

    override suspend fun execute(action: String, params: Map<String, Any>): ToolResult {
        return when (action) {
            "get_ticket_details" -> getTicketDetails(params)
            else -> ToolResult(success = false, error = "Action '$action' not supported by JiraTool.")
        }
    }

    private suspend fun getTicketDetails(params: Map<String, Any>): ToolResult {
        val ticketId = params["ticket_id"] as? String
            ?: return ToolResult(success = false, error = "Missing 'ticket_id' parameter.")

        logger.info("Fetching details for Jira ticket: $ticketId")
        return try {
            val authHeader = "Basic ${Base64.getEncoder().encodeToString("${config.username}:${config.apiToken}".toByteArray())}"
            val ticket = jiraApi.getTicket(authHeader, ticketId)
            logger.info("Successfully fetched details for ticket $ticketId.")
            ToolResult(success = true, data = ticket)
        } catch (e: Exception) {
            logger.error("Failed to fetch Jira ticket $ticketId: ${e.message}", e)
            ToolResult(success = false, error = "Failed to fetch Jira ticket: ${e.message}")
        }
    }
}

// --- Data Classes for Jira API ---

data class JiraConfig(
    val baseUrl: String,
    val username: String,
    val apiToken: String
)

interface JiraApi {
    @GET("rest/api/3/issue/{ticketId}")
    suspend fun getTicket(
        @Header("Authorization") auth: String,
        @Path("ticketId") ticketId: String
    ): JiraTicketResponse
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class JiraTicketResponse(
    val id: String,
    val key: String,
    val fields: JiraTicketFields
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JiraTicketFields(
    val summary: String,
    val description: Any?, // Can be complex object (Atlassian Document Format)
    val status: JiraStatus,
    val issuetype: JiraIssueType
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JiraStatus(val name: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JiraIssueType(val name: String)
