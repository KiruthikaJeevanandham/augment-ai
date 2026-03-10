package com.agentic.conductor.engine

import com.agentic.conductor.models.*
import com.agentic.conductor.tools.Tool
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import java.io.File

class PlanExecutor(
    private val toolRegistry: Map<String, Tool>,
    initialContext: Map<String, Any> = emptyMap()
) {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())

    private val logger = LoggerFactory.getLogger(PlanExecutor::class.java)
    private val executionContext = initialContext.toMutableMap()

    suspend fun execute(plan: Plan) {
        logger.info("Starting plan execution...")
        if (plan.isHierarchical()) {
            executeHierarchical(plan)
        } else {
            executeFlat(plan)
        }
        logger.info("Plan execution finished.")
    }

    private suspend fun executeHierarchical(plan: Plan) {
        for (phase in plan.phases!!.sortedBy { it.phase }) {
            logger.info("\n========== PHASE ${phase.phase}: ${phase.name} ==========")
            if (phase.description != null) {
                logger.info("Description: ${phase.description}")
            }
            
            for (task in phase.tasks.sortedBy { it.task }) {
                logger.info("\n--- Task ${phase.phase}.${task.task}: ${task.name} ---")
                if (task.description != null) {
                    logger.info("Description: ${task.description}")
                }
                
                if (task.isLeaf()) {
                    // Execute direct task action
                    executeAction(
                        tool = task.tool!!,
                        action = task.action!!,
                        params = task.params ?: emptyMap(),
                        outputKey = task.output,
                        label = "Task ${phase.phase}.${task.task}",
                        contextKey = "phases.${phase.phase}.tasks.${task.task}.output"
                    )
                } else {
                    // Execute subtasks
                    for (subtask in task.subtasks!!.sortedBy { it.subtask }) {
                        val subtaskLabel = subtask.name ?: "Subtask ${phase.phase}.${task.task}.${subtask.subtask}"
                        logger.info("  → ${subtaskLabel}")
                        executeAction(
                            tool = subtask.tool,
                            action = subtask.action,
                            params = subtask.params ?: emptyMap(),
                            outputKey = subtask.output,
                            label = subtaskLabel,
                            contextKey = "phases.${phase.phase}.tasks.${task.task}.subtasks.${subtask.subtask}.output"
                        )
                    }
                }
            }
        }
    }

    private suspend fun executeFlat(plan: Plan) {
        for (step in plan.steps!!.sortedBy { it.step }) {
            val stepLabel = step.name ?: "Step ${step.step}"
            logger.info("--- Executing ${stepLabel}: [${step.tool}.${step.action}] ---")
            executeAction(
                tool = step.tool,
                action = step.action,
                params = step.params ?: emptyMap(),
                outputKey = step.output,
                label = stepLabel,
                contextKey = "steps.${step.step}.output",
                continueOnError = step.continueOnError
            )
        }
    }

    private suspend fun executeAction(
        tool: String,
        action: String,
        params: Map<String, Any>,
        outputKey: String?,
        label: String,
        contextKey: String,
        continueOnError: Boolean = false
    ) {
        val toolInstance = toolRegistry[tool]
            ?: throw IllegalArgumentException("Tool '$tool' not found in registry.")

        val processedParams = processParams(params)
        logger.info("Executing [$tool.$action] with parameters: ${processedParams.keys.joinToString(", ") { "$it=..." }}")

        try {
            val result = toolInstance.execute(action, processedParams)
            if (result.success) {
                logger.info("$label executed successfully.")
                if (outputKey != null) {
                    executionContext[contextKey] = result.data!!
                    // Also store with legacy key format for backward compatibility
                    if (contextKey.startsWith("steps.")) {
                        executionContext[contextKey] = result.data
                    }
                    if (outputKey == "verification_report" && result.data is String) {
                        logger.info("Stored output for $label as '$outputKey'. Full report:\n${result.data}")
                    } else {
                        logger.info("Stored output for $label as '$outputKey'. Summary: ${summarizeOutput(result.data)}")
                    }
                }
            } else {
                if (continueOnError) {
                    logger.warn("⚠️  $label failed but continuing (continue_on_error=true): ${result.error}")
                    logger.info("ℹ️  This is expected for renovate branches - will use default dependency-update requirements instead of Jira ticket.")
                    // Store empty/error result so downstream steps can handle it
                    if (outputKey != null) {
                        executionContext[contextKey] = result.error ?: ""
                    }
                } else {
                    logger.error("❌ $label failed: ${result.error}")
                    System.exit(1)
                }
            }
        } catch (e: Exception) {
            if (continueOnError) {
                logger.warn("⚠️  An error occurred during $label but continuing (continue_on_error=true): ${e.message}")
                logger.info("ℹ️  This is expected for renovate branches - will use default dependency-update requirements instead of Jira ticket.")
                if (outputKey != null) {
                    executionContext[contextKey] = "Error: ${e.message}"
                }
            } else {
                logger.error("An unexpected error occurred during $label: ${e.message}", e)
                System.exit(1)
            }
        }
    }

    private fun summarizeOutput(data: Any?): String {
        return when (data) {
            null -> "<null>"
            is String -> {
                val normalized = data.replace("\n", " ").trim()
                if (normalized.length > 200) normalized.take(200) + "..." else normalized
            }
            is List<*> -> {
                val sample = data.take(3).joinToString(", ") { it?.toString() ?: "null" }
                "List(size=${data.size}${if (sample.isNotBlank()) ", sample=[$sample]" else ""})"
            }
            is Map<*, *> -> {
                val keys = data.keys.take(6).joinToString(", ") { it?.toString() ?: "null" }
                "Map(keys=[${keys}${if (data.size > 6) ", ..." else ""}])"
            }
            else -> data.toString()
        }
    }

    private fun processParams(params: Map<String, Any>): Map<String, Any> {
        val processed = mutableMapOf<String, Any>()
        for ((key, value) in params) {
            if (value is String) {
                // Check for single variable replacement (e.g. "{{steps.1.output}}")
                // We want to preserve the type in this case (e.g. return a Map or List directly)
                if (value.startsWith("{{") && value.endsWith("}}") && !value.substring(2, value.length - 2).contains("}}")) {
                    val contextKey = value.substring(2, value.length - 2).trim()
                    processed[key] = resolveContextValue(contextKey)
                } else if (value.contains("{{")) {
                    // String interpolation (e.g. "Analyze this: {{steps.1.output}}")
                    // In this case, we convert everything to string/JSON
                    var newValue = value
                    val regex = Regex("\\{\\{([^}]+)\\}\\}")
                    newValue = regex.replace(newValue) { matchResult ->
                        val contextKey = matchResult.groupValues[1].trim()
                        try {
                            val resolved = resolveContextValue(contextKey)
                            if (resolved is String) {
                                resolved
                            } else {
                                // Serialize complex objects (Lists, Maps) to JSON for better LLM comprehension
                                objectMapper.writeValueAsString(resolved)
                            }
                        } catch (e: Exception) {
                            logger.warn("Failed to resolve context key '$contextKey' in string interpolation: ${e.message}")
                            matchResult.value // Keep the original {{key}} if resolution fails
                        }
                    }
                    processed[key] = newValue
                } else {
                    processed[key] = value
                }
            } else {
                processed[key] = value
            }
        }
        return processed
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveContextValue(key: String): Any {
        // The key is expected to be in the format 'steps.<step_number>.output.<nested_field>...'
        val keyParts = key.split('.').toMutableList()

        if (keyParts.size < 3 || keyParts[0] != "steps" || keyParts[2] != "output") {
            // Handle simple context keys that are not from step outputs
            return executionContext[key] ?: throw IllegalStateException("Could not resolve simple key '$key'.")
        }

        // Reconstruct the main context key for the step's output
        val contextKey = "${keyParts.removeAt(0)}.${keyParts.removeAt(0)}.${keyParts.removeAt(0)}"
        var currentValue: Any? = executionContext[contextKey]
            ?: throw IllegalStateException("Could not resolve base output for key '$key'. Context key '$contextKey' not found.")

        // Now, traverse the nested fields within the step's output data
        for (part in keyParts) {
            if (currentValue == null) {
                throw IllegalStateException("Cannot resolve key '$key'. Intermediate value is null at '$part'.")
            }

            // If the current value is a JSON string, parse it into a Map first.
            if (currentValue is String) {
                var jsonString = currentValue.trim()
                
                // Robust Markdown code block stripping
                val markdownRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
                val match = markdownRegex.find(jsonString)
                if (match != null) {
                    jsonString = match.groupValues[1].trim()
                }

                if (jsonString.startsWith("{")) {
                    currentValue = try {
                        objectMapper.readValue(jsonString, Map::class.java)
                    } catch (e: Exception) {
                        logger.warn("Failed to parse JSON for key '$part': ${e.message}. Content: ${jsonString.take(100)}...")
                        currentValue // Keep as string if parsing fails
                    }
                }
            }

            val valueAsMap = when (currentValue) {
                is Map<*, *> -> currentValue as Map<String, Any>
                else -> {
                    try {
                        objectMapper.convertValue(currentValue, Map::class.java) as Map<String, Any>
                    } catch (e: Exception) {
                        throw IllegalStateException("Cannot convert value at '$part' to Map. Value type: ${currentValue?.javaClass?.name}. Error: ${e.message}")
                    }
                }
            }

            currentValue = valueAsMap[part]
        }

        return currentValue ?: throw IllegalStateException("Could not resolve final value for key '$key'.")
    }

    companion object {
        private val yamlMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

        fun loadPlanFromFile(filePath: String): Plan {
            val file = File(filePath)
            if (!file.exists()) {
                throw IllegalArgumentException("Plan file not found at: $filePath")
            }
            // A simple way to parse a markdown-like YAML structure
            // This assumes the plan is essentially a YAML document within the markdown file.
            val planContent = file.readText().lines().filterNot { it.trim().startsWith("#") }.joinToString("\n")
            return yamlMapper.readValue(planContent, Plan::class.java)
        }
    }
}
