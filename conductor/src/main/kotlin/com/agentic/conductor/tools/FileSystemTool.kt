package com.agentic.conductor.tools

import com.agentic.conductor.models.ToolResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths

class FileSystemTool(private val config: FileSystemConfig) : Tool {

    override val name: String = "filesystem"
    private val logger = LoggerFactory.getLogger(FileSystemTool::class.java)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule())

    override suspend fun execute(action: String, params: Map<String, Any>): ToolResult {
        return when (action) {
            "read_file" -> readFile(params)
            "read_files" -> readFiles(params)
            "write_file" -> writeFile(params)
            "list_files" -> listFiles(params)
            "apply_changes" -> applyChanges(params)
            "apply_patch" -> applyPatch(params)
            "edit_file" -> editFile(params)
            "apply_surgical_edits" -> applySurgicalEdits(params)
            "filter_existing_paths" -> filterExistingPaths(params)
            "create_branch_name" -> createBranchName(params)
            "assert_gate_result" -> assertGateResult(params)
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

    private fun readFiles(params: Map<String, Any>): ToolResult {
        val paths = params["paths"] as? List<String>
            ?: return ToolResult(success = false, error = "Missing 'paths' parameter. Expected a list of file paths.")

        logger.info("Reading ${paths.size} files...")
        val fileContents = mutableMapOf<String, String>()
        val errors = mutableListOf<String>()

        for (path in paths) {
            try {
                val file = File(config.basePath, path)
                if (file.exists() && file.isFile) {
                    fileContents[path] = file.readText()
                } else {
                    errors.add("File not found or not a file: $path")
                }
            } catch (e: Exception) {
                errors.add("Failed to read $path: ${e.message}")
            }
        }

        return if (errors.isEmpty()) {
            ToolResult(success = true, data = fileContents)
        } else {
            ToolResult(success = true, data = mapOf("files" to fileContents, "errors" to errors))
        }
    }

    private fun listFiles(params: Map<String, Any>): ToolResult {
        val path = params["path"] as? String ?: "."
        val recursive = params["recursive"] as? Boolean ?: false
        val includePatterns = (params["include_patterns"] as? List<*>)?.filterIsInstance<String>().orEmpty()
        val excludePatterns = (params["exclude_patterns"] as? List<*>)?.filterIsInstance<String>().orEmpty()
        val maxDepth = (params["max_depth"] as? Number)?.toInt() ?: 10
        val limit = (params["limit"] as? Number)?.toInt()
        val filesOnly = params["files_only"] as? Boolean ?: false

        logger.info("Listing files in: $path (recursive=$recursive)")
        return try {
            val rootDir = File(config.basePath, path)
            if (!rootDir.exists() || !rootDir.isDirectory) {
                return ToolResult(success = false, error = "Directory not found: $path")
            }

            val includeMatchers = includePatterns.takeIf { it.isNotEmpty() }?.map { buildGlobMatcher(it) }
            val excludeMatchers = excludePatterns.takeIf { it.isNotEmpty() }?.map { buildGlobMatcher(it) }

            val files = if (recursive) {
                rootDir.walkTopDown()
                    .onEnter { !it.name.startsWith(".") && it.name != "build" && it.name != "node_modules" && it.name != "target" }
                    .maxDepth(maxDepth)
                    .filter { !it.name.startsWith(".") }
                    .filter { file -> !filesOnly || file.isFile }
                    .map { it.relativeTo(rootDir).path }
                    .filter { relPath ->
                        val pathObj = Paths.get(relPath)
                        val matchesInclude = includeMatchers?.any { matcher -> matcher.matches(pathObj) } ?: true
                        val matchesExclude = excludeMatchers?.any { matcher -> matcher.matches(pathObj) } ?: false
                        matchesInclude && !matchesExclude
                    }
                    .toList()
            } else {
                rootDir.listFiles()
                    ?.filter { !it.name.startsWith(".") }
                    ?.filter { file -> !filesOnly || file.isFile }
                    ?.map { it.relativeTo(File(config.basePath)).path }
                    ?: emptyList()
            }
            val limitedFiles = limit?.let { files.take(it) } ?: files
            ToolResult(success = true, data = limitedFiles)
        } catch (e: Exception) {
            logger.error("Failed to list files in $path: ${e.message}", e)
            ToolResult(success = false, error = "Failed to list files: ${e.message}")
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

    private fun assertGateResult(params: Map<String, Any>): ToolResult {
        val report = params["report"] as? String
            ?: return ToolResult(success = false, error = "Missing 'report' parameter.")
        val expected = (params["expected"] as? String ?: "PASS").uppercase()

        val jsonBlock = extractJsonBlock(report)
            ?: return ToolResult(success = false, error = "Verification report missing JSON block with gate_result.")

        val parsed = try {
            objectMapper.readValue(jsonBlock, Map::class.java) as Map<*, *>
        } catch (e: Exception) {
            return ToolResult(success = false, error = "Failed to parse verification JSON block: ${e.message}")
        }

        val gateResult = parsed["gate_result"]?.toString()?.uppercase()
            ?: return ToolResult(success = false, error = "Verification JSON missing 'gate_result'.")

        return if (gateResult == expected) {
            ToolResult(success = true, data = mapOf("gate_result" to gateResult))
        } else {
            ToolResult(
                success = false,
                error = "Verification gate failed: gate_result='$gateResult' (expected '$expected')."
            )
        }
    }

    private fun extractJsonBlock(raw: String): String? {
        val markdownRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
        val match = markdownRegex.find(raw)
        return match?.groupValues?.get(1)?.trim()
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

    @Suppress("UNCHECKED_CAST")
    private fun applySurgicalEdits(params: Map<String, Any>): ToolResult {
        val edits = params["edits"] as? List<Map<String, String>>
            ?: return ToolResult(success = false, error = "Missing or invalid 'edits' parameter. Expected a list of objects with 'path', 'search_pattern', and 'replacement'.")

        logger.info("Applying ${edits.size} surgical edits across files...")
        var successfulEdits = 0
        val errors = mutableListOf<String>()

        // Expand globs in file paths
        val expandedEdits = mutableListOf<Map<String, String>>()
        for (edit in edits) {
            val pathPattern = edit["path"]
            if (pathPattern == null) {
                errors.add("Skipping edit with missing path")
                continue
            }

            if (pathPattern.contains("*") || pathPattern.contains("?")) {
                // It's a glob pattern, expand it
                val rootDir = File(config.basePath)
                val matches = try {
                    expandGlob(pathPattern, rootDir, 10)
                } catch (e: Exception) {
                    errors.add("Failed to expand glob '$pathPattern': ${e.message}")
                    emptyList()
                }

                if (matches.isEmpty()) {
                    logger.warn("  - No files matched glob pattern: $pathPattern")
                    // We don't error here, just skip
                } else {
                    logger.info("  - Expanded glob '$pathPattern' to ${matches.size} files")
                    for (match in matches) {
                        expandedEdits.add(edit + ("path" to match))
                    }
                }
            } else {
                expandedEdits.add(edit)
            }
        }

        // Group edits by file path to minimize file I/O
        val editsByFile = expandedEdits.groupBy { it["path"] }

        for ((path, fileEdits) in editsByFile) {
            if (path == null) continue // Should be handled above

            val file = File(config.basePath, path)
            if (!file.exists()) {
                errors.add("File not found: $path")
                continue
            }

            try {
                var content = file.readText()
                var fileModified = false

                for (edit in fileEdits) {
                    val searchPattern = edit["search_pattern"]
                    val replacement = edit["replacement"]

                    if (searchPattern != null && replacement != null) {
                        try {
                            val regex = Regex(searchPattern)
                            if (regex.containsMatchIn(content)) {
                                content = regex.replace(content, replacement)
                                fileModified = true
                                successfulEdits++
                                logger.info("  - Applied edit in $path for pattern: $searchPattern")
                            } else {
                                // Silent failure is better for batch regexes that might not apply to every file in a glob
                                // logger.warn("  - Pattern not found in $path: $searchPattern") 
                            }
                        } catch (e: Exception) {
                            errors.add("Invalid regex in $path: $searchPattern")
                        }
                    }
                }

                if (fileModified) {
                    file.writeText(content)
                }
            } catch (e: Exception) {
                errors.add("Failed to process file $path: ${e.message}")
            }
        }

        if (successfulEdits > 0) {
             val message = if (errors.isEmpty()) {
                 "Successfully applied $successfulEdits edits."
             } else {
                 "Successfully applied $successfulEdits edits. Warnings: ${errors.joinToString("; ")}"
             }
             return ToolResult(success = true, data = message)
        } else {
             if (errors.isEmpty()) {
                 return ToolResult(success = false, error = "No edits were applied (patterns matched nothing).")
             } else {
                 return ToolResult(success = false, error = "Failed to apply any edits. Errors: ${errors.joinToString("; ")}")
             }
        }
    }

    private fun filterExistingPaths(params: Map<String, Any>): ToolResult {
        val paths = params["paths"] as? List<String>
            ?: return ToolResult(success = false, error = "Missing 'paths' parameter. Expected a list of file paths.")

        val existing = mutableListOf<String>()
        val missing = mutableListOf<String>()
        for (path in paths) {
            val file = File(config.basePath, path)
            if (file.exists()) {
                existing.add(path)
            } else {
                missing.add(path)
            }
        }
        return ToolResult(success = true, data = mapOf("paths" to existing, "missing" to missing))
    }

    private fun buildGlobMatcher(pattern: String): PathMatcher {
        return FileSystems.getDefault().getPathMatcher("glob:$pattern")
    }

    private fun expandGlob(pattern: String, rootDir: File, maxDepth: Int): List<String> {
        val matcher = buildGlobMatcher(pattern)
        return rootDir.walkTopDown()
            .onEnter { !it.name.startsWith(".") && it.name != "build" && it.name != "node_modules" && it.name != "target" }
            .maxDepth(maxDepth)
            .filter { file -> matcher.matches(Paths.get(file.relativeTo(rootDir).path)) }
            .map { it.relativeTo(rootDir).path }
            .toList()
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
        
        val edits = params["edits"] as? List<Map<String, String>>
            ?: return ToolResult(success = false, error = "Missing or invalid 'edits' parameter. Expected a list of objects with 'search_pattern' and 'replacement'.")

        logger.info("Applying ${edits.size} surgical edit(s) to file: $path")

        val file = File(config.basePath, path)
        if (!file.exists()) {
            return ToolResult(success = false, error = "File not found: $path")
        }

        var content = file.readText()
        var appliedChanges = 0

        for (edit in edits) {
            val searchPattern = edit["search_pattern"]
            val replacement = edit["replacement"]

            if (searchPattern != null && replacement != null) {
                try {
                    val regex = Regex(searchPattern)
                    if (regex.containsMatchIn(content)) {
                        content = regex.replace(content, replacement)
                        appliedChanges++
                        logger.info("  - Applied edit for pattern: $searchPattern")
                    } else {
                        logger.warn("  - Pattern not found: $searchPattern")
                    }
                } catch (e: Exception) {
                    logger.error("  - Invalid regex pattern: $searchPattern", e)
                    return ToolResult(success = false, error = "Invalid regex pattern: $searchPattern. Error: ${e.message}")
                }
            } else {
                 logger.warn("  - Skipping invalid edit entry: $edit")
            }
        }

        if (appliedChanges > 0) {
            file.writeText(content)
            logger.info("Successfully applied $appliedChanges out of ${edits.size} edits to $path.")
            return ToolResult(success = true)
        } else {
            return ToolResult(success = false, error = "No edits were applied. Patterns matched nothing in the file.")
        }
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
