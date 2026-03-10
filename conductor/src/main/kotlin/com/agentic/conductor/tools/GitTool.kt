package com.agentic.conductor.tools

import com.agentic.conductor.models.ToolResult
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

class GitTool(private val config: GitConfig) : Tool {

    override val name: String = "git"
    private val logger = LoggerFactory.getLogger(GitTool::class.java)

    override suspend fun execute(action: String, params: Map<String, Any>): ToolResult {
        return when (action) {
            "create_branch" -> createBranch(params)
            "checkout_branch" -> checkoutBranch(params)
            "commit" -> commit(params)
            "push" -> push(params)
            "create_pr" -> createPr(params)
            else -> ToolResult(success = false, error = "Action '$action' not supported by GitTool.")
        }
    }

    private fun clone(params: Map<String, Any>): ToolResult {
        val repoUrl = params["repo_url"] as? String
            ?: return ToolResult(success = false, error = "Missing 'repo_url' parameter.")
        val targetDir = params["target_dir"] as? String
            ?: return ToolResult(success = false, error = "Missing 'target_dir' parameter.")

        logger.info("Cloning repository '$repoUrl' into '$targetDir'...")
        val result = runCommand("git", "clone", repoUrl, targetDir)
        return if (result.exitCode == 0) {
            ToolResult(success = true, data = mapOf("cloned_path" to targetDir))
        } else {
            ToolResult(success = false, error = result.output)
        }
    }

    private fun createBranch(params: Map<String, Any>): ToolResult {
        val branchName = params["branch_name"] as? String
            ?: return ToolResult(success = false, error = "Missing 'branch_name' parameter.")
        val baseBranch = params["base_branch"] as? String ?: "main"

        logger.info("Creating branch '$branchName' from '$baseBranch'...")
        return try {
            runCommand("git", "checkout", "-b", branchName, baseBranch)
            logger.info("Successfully created and switched to branch '$branchName'.")
            ToolResult(success = true)
        } catch (e: Exception) {
            logger.error("Failed to create branch '$branchName': ${e.message}", e)
            ToolResult(success = false, error = "Failed to create branch: ${e.message}")
        }
    }

    private fun checkoutBranch(params: Map<String, Any>): ToolResult {
        val branchName = params["branch_name"] as? String
            ?: return ToolResult(success = false, error = "Missing 'branch_name' parameter.")
        val baseBranch = params["base_branch"] as? String ?: "main"

        logger.info("Ensuring branch '$branchName' exists and is checked out...")

        // First, try to checkout the branch directly
        try {
            runCommand("git", "checkout", branchName)
            logger.info("Successfully checked out existing branch '$branchName'.")
            return ToolResult(success = true)
        } catch (e: Exception) {
            logger.info("Branch '$branchName' does not exist locally. Attempting to create it.")
        }

        // If checkout failed, try to create it from the base branch
        return try {
            runCommand("git", "checkout", "-b", branchName, baseBranch)
            logger.info("Successfully created and switched to new branch '$branchName' from '$baseBranch'.")
            ToolResult(success = true)
        } catch (e: Exception) {
            logger.error("Failed to create or checkout branch '$branchName': ${e.message}", e)
            ToolResult(success = false, error = "Failed to create or checkout branch: ${e.message}")
        }
    }

    private fun commit(params: Map<String, Any>): ToolResult {
        val message = params["message"] as? String
            ?: return ToolResult(success = false, error = "Missing 'message' parameter.")

        logger.info("Committing changes with message: '$message'")
        runCommand("git", "add", ".")
        val result = runCommand("git", "commit", "-m", message)
        return if (result.exitCode == 0) {
            logger.info("Successfully committed changes.")
            ToolResult(success = true)
        } else {
            logger.error("Failed to commit changes: ${result.output}")
            ToolResult(success = false, error = "Failed to commit changes: ${result.output}")
        }
    }

    private fun push(params: Map<String, Any>): ToolResult {
        logger.info("Pushing current branch to remote...")
        return try {
            runCommand("git", "push", "-u", "origin", "HEAD")
            logger.info("Successfully pushed branch to remote.")
            ToolResult(success = true)
        } catch (e: Exception) {
            logger.error("Failed to push branch to remote: ${e.message}", e)
            ToolResult(success = false, error = "Failed to push branch: ${e.message}")
        }
    }

    private fun createPr(params: Map<String, Any>): ToolResult {
        val title = params["title"] as? String
            ?: return ToolResult(success = false, error = "Missing 'title' parameter.")
        val body = params["body"] as? String
            ?: return ToolResult(success = false, error = "Missing 'body' parameter.")

        logger.info("Creating pull request with title: '$title'")
        return try {
            // Get the current branch name to use as the head
            val branchName = runCommand("git", "branch", "--show-current").output.trim()
            runCommand("gh", "pr", "create", "--title", title, "--body", body, "--head", branchName)
            logger.info("Successfully created pull request.")
            ToolResult(success = true)
        } catch (e: Exception) {
            logger.error("Failed to create pull request: ${e.message}", e)
            ToolResult(success = false, error = "Failed to create pull request: ${e.message}")
        }
    }

    private fun runCommand(vararg command: String): CommandResult {
        try {
            val process = ProcessBuilder(*command)
                .directory(File(config.workingDirectory))
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroy()
                return CommandResult(1, "Command timed out.")
            }

            val output = process.inputStream.bufferedReader().readText().trim()
            val error = process.errorStream.bufferedReader().readText().trim()

            return if (process.exitValue() == 0) {
                CommandResult(0, output)
            } else {
                CommandResult(process.exitValue(), error.ifEmpty { output })
            }
        } catch (e: Exception) {
            logger.error("Failed to run command '${command.joinToString(" ")}': ${e.message}", e)
            return CommandResult(1, "Failed to execute command: ${e.message}")
        }
    }

    private data class CommandResult(val exitCode: Int, val output: String)
}

data class GitConfig(
    val workingDirectory: String
)
