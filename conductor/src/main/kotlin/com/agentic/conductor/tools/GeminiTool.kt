package com.agentic.conductor.tools

import com.agentic.conductor.models.ToolResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class GeminiTool(private val config: GeminiConfig) : Tool {

    override val name: String = "gemini"
    private val logger = LoggerFactory.getLogger(GeminiTool::class.java)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

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

        logger.info("Generating text with Gemini model: $modelName using Google API Key")
        
        return generateTextWithApiKey(prompt, modelName)
    }
    
    private fun generateTextWithApiKey(prompt: String, modelName: String): ToolResult {
        return try {
            val apiKey = config.apiKey!!
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    logger.error("API Key request failed: $errorBody")
                    return ToolResult(success = false, error = "API request failed: ${response.code} - $errorBody")
                }
                
                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                val text = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                
                logger.info("Successfully generated text with API Key.")
                ToolResult(success = true, data = text)
            }
        } catch (e: Exception) {
            logger.error("Failed to generate text with API Key: ${e.message}", e)
            ToolResult(success = false, error = "Failed to generate text with API Key: ${e.message}")
        }
    }

    private fun listModels(): ToolResult {
        logger.info("Listing available Gemini models")
        return ToolResult(
            success = true, 
            data = listOf(
                "gemini-pro",
                "gemini-1.5-pro",
                "gemini-1.5-flash",
                "gemini-2.0-flash-exp"
            )
        )
    }
}

data class GeminiConfig(
    val apiKey: String,
    val defaultModel: String = "gemini-pro"
)
