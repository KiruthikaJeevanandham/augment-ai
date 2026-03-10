package com.agentic.conductor.tools

import com.agentic.conductor.models.ToolResult
import org.slf4j.LoggerFactory
import java.io.File

class FileSystemTool(private val config: FileSystemConfig) : Tool {

    override val name: String = "filesystem"
    private val logger = LoggerFactory.getLogger(FileSystemTool::class.java)

    override suspend fun execute(action: String, params: Map<String, Any>): ToolResult {
        return when (action) {
            "read_file" -> readFile(params)
            "write_file" -> writeFile(params)
            "apply_changes" -> applyChanges(params)
            "apply_patch" -> applyPatch(params)
            "edit_file" -> editFile(params)
            "create_branch_name" -> createBranchName(params)
            else -> ToolResult(success = false, error = "Action '$action' not supported by FileSystemTool.")
        }
    }

    private fun readFile(params: Map<String, Any>): ToolResult {
        val path = params["path"] as? String
            ?: return ToolResult(success = false, error = "Missing 'path' parameter.")

        logger.info("Reading file: $path")
        return try {
            val file = File(config.basePath, path)
            if (!file.exists()) {
                return ToolResult(success = false, error = "File not found: $path")
            }
            val content = file.readText()
            ToolResult(success = true, data = content)
        } catch (e: Exception) {
            logger.error("Failed to read file $path: ${e.message}", e)
            ToolResult(success = false, error = "Failed to read file: ${e.message}")
        }
    }

    private fun writeFile(params: Map<String, Any>): ToolResult {
        val path = params["path"] as? String
            ?: return ToolResult(success = false, error = "Missing 'path' parameter.")
        val content = params["content"] as? String
            ?: return ToolResult(success = false, error = "Missing 'content' parameter.")

        logger.info("Writing to file: $path")
        return try {
            val file = File(config.basePath, path)
            file.parentFile.mkdirs() // Ensure parent directories exist
            file.writeText(content)
            ToolResult(success = true)
        } catch (e: Exception) {
            logger.error("Failed to write to file $path: ${e.message}", e)
            ToolResult(success = false, error = "Failed to write to file: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyChanges(params: Map<String, Any>): ToolResult {
        val rawChanges = params["changes"] as? String
            ?: return ToolResult(success = false, error = "Missing or invalid 'changes' parameter. Expected a JSON string.")

        // Find the first JSON block within the AI-generated text
        var jsonString = rawChanges.trim()
        val jsonBlockRegex = Regex("```json\\s*([\\s\\S]*?)```")
        val matchResult = jsonBlockRegex.find(rawChanges)
        if (matchResult != null) {
            jsonString = matchResult.groupValues[1].trim()
        } else {
            // Fallback for code blocks without the 'json' identifier
            val codeBlockRegex = Regex("```([\\s\\S]*?)```")
            val codeMatch = codeBlockRegex.find(rawChanges)
            if (codeMatch != null) {
                jsonString = codeMatch.groupValues[1].trim()
            }
        }

        val changes = try {
            com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonString, List::class.java) as List<Map<String, String>>
        } catch (e: Exception) {
            return ToolResult(success = false, error = "Failed to parse 'changes' parameter as JSON: ${e.message}")
        }

        logger.info("Applying ${changes.size} file change(s)...")
        try {
            for (change in changes) {
                val path = change["path"] ?: continue
                val content = change["content"] ?: ""
                val result = writeFile(mapOf("path" to path, "content" to content))
                if (!result.success) {
                    // If any file fails, we stop and report the error.
                    return ToolResult(success = false, error = "Failed to apply change to '$path': ${result.error}")
                }
            }
        } catch (e: Exception) {
            logger.error("An error occurred while applying changes: ${e.message}", e)
            return ToolResult(success = false, error = "An unexpected error occurred during file application: ${e.message}")
        }
        logger.info("All file changes applied successfully.")
        return ToolResult(success = true)
    }

    private fun createBranchName(params: Map<String, Any>): ToolResult {
        val ticketKey = params["ticket_key"] as? String
            ?: return ToolResult(success = false, error = "Missing 'ticket_key' parameter.")
        val issueType = params["issue_type"] as? String
            ?: return ToolResult(success = false, error = "Missing 'issue_type' parameter.")
        val summary = params["summary"] as? String
            ?: return ToolResult(success = false, error = "Missing 'summary' parameter.")

        logger.info("Creating branch name for ticket $ticketKey...")

        val prefix = when (issueType.lowercase()) {
            "bug" -> "bugfix/"
            else -> "feature/"
        }

        val slug = summary.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-')

        val branchName = "$prefix$ticketKey-$slug"
        logger.info("Generated branch name: $branchName")
        return ToolResult(success = true, data = branchName)
    }

    @Suppress("UNCHECKED_CAST")
    private fun editFile(params: Map<String, Any>): ToolResult {
        val path = params["path"] as? String
            ?: return ToolResult(success = false, error = "Missing 'path' parameter.")
        val changes = params["changes"] as? List<Map<String, String>>
            ?: return ToolResult(success = false, error = "Missing or invalid 'changes' parameter. Expected a list of change objects.")

        logger.info("Applying ${changes.size} surgical edit(s) to file: $path")

        val file = File(config.basePath, path)
        if (!file.exists()) {
            return ToolResult(success = false, error = "File not found: $path")
        }

        var content = file.readText()
        var appliedChanges = 0

        for (change in changes) {
            val dependency = change["dependency"]
            val oldVersion = change["current_version"]
            val newVersion = change["new_version"]

            if (dependency != null && oldVersion != null && newVersion != null) {
                // Escape special regex characters in the dependency name
                val escapedDependency = dependency.replace(Regex("[.*+?^\\$\\{\\}\\(\\)|\\[\\]]"), "\\\\$0")
                // Create a regex to find the dependency and its version
                // This is a simplified regex. A real agent would use a proper parser.
                // This regex looks for: dependency ... : 'version' or dependency ... : version
                val regex = Regex("($escapedDependency.*?:\\s*')?([^']+)'?")
                val replacement = "$1$newVersion'"
                
                if (content.contains(regex)) {
                    content = content.replace(regex, replacement)
                    appliedChanges++
                    logger.info("  - Updated $dependency from $oldVersion to $newVersion")
                } else {
                    logger.warn("  - Could not find dependency '$dependency' with version '$oldVersion' in file.")
                }
            }
        }

        file.writeText(content)
        logger.info("Successfully applied $appliedChanges out of ${changes.size} changes to $path.")
        return ToolResult(success = true)
    }

    private fun applyPatch(params: Map<String, Any>): ToolResult {
        val patchContent = params["patch"] as? String
            ?: return ToolResult(success = false, error = "Missing 'patch' parameter.")

        logger.info("Applying patch...")
        return try {
            // Create a temporary patch file
            val patchFile = File.createTempFile("patch", ".diff")
            patchFile.writeText(patchContent)

            // Apply the patch using git apply
            val result = runCommand("git", "apply", patchFile.absolutePath)
            patchFile.delete()

            if (result.exitCode == 0) {
                logger.info("Successfully applied patch.")
                ToolResult(success = true)
            } else {
                logger.error("Failed to apply patch: ${result.output}")
                ToolResult(success = false, error = "Failed to apply patch: ${result.output}")
            }
        } catch (e: Exception) {
            logger.error("An error occurred while applying patch: ${e.message}", e)
            ToolResult(success = false, error = "An unexpected error occurred during patch application: ${e.message}")
        }
    }

    private fun runCommand(vararg command: String): CommandResult {
        try {
            val process = ProcessBuilder(*command)
                .directory(File(config.basePath))
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            return CommandResult(exitCode, output + error)
        } catch (e: Exception) {
            throw RuntimeException("Failed to run command: ${e.message}", e)
        }
    }
}

data class CommandResult(val exitCode: Int, val output: String)

data class FileSystemConfig(
    val basePath: String
)
