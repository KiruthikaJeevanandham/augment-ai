package com.agentic.conductor.tools

import com.agentic.conductor.models.ToolResult

/**
 * Defines the contract for a tool that can be executed by the PlanExecutor.
 * A tool exposes a set of actions that can be called with parameters.
 */
interface Tool {
    /**
     * The name of the tool, used to identify it in the plan.
     */
    val name: String

    /**
     * Executes a specific action provided by the tool.
     *
     * @param action The name of the action to execute.
     * @param params A map of parameters for the action.
     * @return A ToolResult containing the outcome of the execution.
     */
    suspend fun execute(action: String, params: Map<String, Any>): ToolResult
}
