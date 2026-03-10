package com.agentic.conductor.tools

import com.agentic.conductor.models.ToolResult
import com.google.cloud.aiplatform.v1.ModelServiceClient
import com.google.cloud.aiplatform.v1.ModelServiceSettings
import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.generativeai.GenerativeModel
import com.google.cloud.vertexai.generativeai.ResponseHandler
import org.slf4j.LoggerFactory

class GeminiTool(private val config: GeminiConfig) : Tool {

    override val name: String = "gemini"
    private val logger = LoggerFactory.getLogger(GeminiTool::class.java)

    override suspend fun execute(action: String, params: Map<String, Any>): ToolResult {
        return when (action) {
            "generate_text" -> generateText(params)
            "list_models" -> listModels()
            else -> ToolResult(success = false, error = "Action '$action' not supported by GeminiTool.")
        }
    }

    private suspend fun generateText(params: Map<String, Any>): ToolResult {
        val prompt = params["prompt"] as? String
            ?: return ToolResult(success = false, error = "Missing 'prompt' parameter.")
        val modelName = params["model"] as? String ?: config.defaultModel

        logger.info("Generating text with Gemini model: $modelName")
        return try {
            val vertexAI = VertexAI(config.projectId, config.location)
            val model = GenerativeModel(modelName, vertexAI)
                        val response = model.generateContent(prompt)
            val responseText = ResponseHandler.getText(response)
            logger.info("Successfully generated text with Gemini.")
            ToolResult(success = true, data = responseText)
        } catch (e: Exception) {
            logger.error("Failed to generate text with Gemini: ${e.message}", e)
            ToolResult(success = false, error = "Failed to generate text: ${e.message}")
        }
    }

    private fun listModels(): ToolResult {
        logger.info("Listing available models from Vertex AI...")
        return try {
            val settings = ModelServiceSettings.newBuilder()
                .setEndpoint("${config.location}-aiplatform.googleapis.com:443")
                .build()

            ModelServiceClient.create(settings).use { client ->
                val parent = "projects/${config.projectId}/locations/${config.location}"
                val models = client.listModels(parent).iterateAll()
                    .map { it.name }
                    .filter { it.contains("gemini") } // Filter for Gemini models
                    .toList()
                logger.info("Found ${models.size} Gemini models.")
                ToolResult(success = true, data = models)
            }
        } catch (e: Exception) {
            logger.error("Failed to list models: ${e.message}", e)
            ToolResult(success = false, error = "Failed to list models: ${e.message}")
        }
    }
}

data class GeminiConfig(
    val projectId: String,
    val location: String,
    val defaultModel: String = "gemini-pro"
)
