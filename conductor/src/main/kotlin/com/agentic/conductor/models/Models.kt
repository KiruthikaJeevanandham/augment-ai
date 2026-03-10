package com.agentic.conductor.models

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a single track of work, containing its specification and execution plan.
 */
data class Track(
    val id: String,
    val spec: Spec,
    val plan: Plan,
    val metadata: TrackMetadata
)

/**
 * The specification for a track, outlining the goal and requirements.
 */
data class Spec(
    val goal: String,
    val description: String,
    @JsonProperty("acceptance_criteria") val acceptanceCriteria: List<String>
)

/**
 * The execution plan for a track, consisting of a series of steps.
 */
data class Plan(
    val steps: List<PlanStep>
)

/**
 * A single step within an execution plan.
 */
data class PlanStep(
    val step: Int,
    val tool: String,
    val action: String,
    val params: Map<String, Any>? = null,
    val output: String? = null
)

/**
 * Metadata associated with a track.
 */
data class TrackMetadata(
    val id: String,
    val title: String,
    val type: String,
    val status: String,
    @JsonProperty("jira_ticket") val jiraTicket: String? = null,
    val description: String
)

/**
 * A generic representation of the output from a tool's action.
 */
data class ToolResult(
    val success: Boolean,
    val data: Any? = null,
    val error: String? = null
)
