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
 * The execution plan for a track, consisting of phases (hierarchical) or steps (flat).
 * Supports both formats for backward compatibility.
 */
data class Plan(
    val phases: List<Phase>? = null,
    val steps: List<PlanStep>? = null
) {
    init {
        require(phases != null || steps != null) {
            "Plan must have either 'phases' or 'steps' defined"
        }
    }

    fun isHierarchical(): Boolean = phases != null
}

/**
 * A phase in a hierarchical plan, containing tasks.
 */
data class Phase(
    val phase: Int,
    val name: String,
    val description: String? = null,
    val tasks: List<Task>
)

/**
 * A task within a phase, containing subtasks or direct tool actions.
 */
data class Task(
    val task: Int,
    val name: String,
    val description: String? = null,
    val subtasks: List<Subtask>? = null,
    // Direct action (if no subtasks)
    val tool: String? = null,
    val action: String? = null,
    val params: Map<String, Any>? = null,
    val output: String? = null
) {
    init {
        require((subtasks != null) || (tool != null && action != null)) {
            "Task must have either 'subtasks' or a direct 'tool' and 'action'"
        }
    }

    fun isLeaf(): Boolean = subtasks == null
}

/**
 * A subtask within a task, representing an atomic tool action.
 */
data class Subtask(
    val subtask: Int,
    val name: String? = null,
    val tool: String,
    val action: String,
    val params: Map<String, Any>? = null,
    val output: String? = null
)

/**
 * A single step within a flat execution plan (legacy format).
 */
data class PlanStep(
    val step: Int,
    val name: String? = null,
    val tool: String,
    val action: String,
    val params: Map<String, Any>? = null,
    val output: String? = null,
    @JsonProperty("continue_on_error") val continueOnError: Boolean = false
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

/**
 * Project context artifacts for better AI understanding and team alignment.
 */
data class ProjectContext(
    val product: ProductContext? = null,
    val techStack: TechStackContext? = null,
    val workflow: WorkflowContext? = null
)

data class ProductContext(
    val overview: String? = null,
    val targetUsers: List<String> = emptyList(),
    val productGoals: List<String> = emptyList(),
    val keyFeatures: List<String> = emptyList(),
    val successMetrics: List<String> = emptyList(),
    val constraints: List<String> = emptyList()
)

data class TechStackContext(
    val languages: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val database: String? = null,
    val infrastructure: List<String> = emptyList(),
    val developmentTools: List<String> = emptyList(),
    val architecturePatterns: List<String> = emptyList(),
    val thirdPartyServices: List<String> = emptyList(),
    val versionRequirements: Map<String, String> = emptyMap()
)

data class WorkflowContext(
    val developmentProcess: String? = null,
    val codeStandards: List<String> = emptyList(),
    val testingStrategy: TestingStrategy? = null,
    val codeReviewProcess: String? = null,
    val branchingStrategy: String? = null,
    val documentationRequirements: List<String> = emptyList(),
    val deploymentProcess: String? = null,
    val qualityGates: List<String> = emptyList()
)

data class TestingStrategy(
    val unitTests: String? = null,
    val integrationTests: String? = null,
    val e2eTests: String? = null,
    val coverageRequirements: String? = null
)
