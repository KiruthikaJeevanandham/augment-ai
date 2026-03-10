package com.agentic.conductor

import com.agentic.conductor.context.ContextLoader
import com.agentic.conductor.engine.PlanExecutor
import com.agentic.conductor.tools.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File

fun main(args: Array<String>) = Conductor().main(args)

class Conductor : CliktCommand(help = "Executes a plan for a given track.") {

    private val logger = LoggerFactory.getLogger(Conductor::class.java)

    private val trackId: String by option("--track-id", help = "The ID of the track to execute.").required()
    private val tracksPath: String by option("--tracks-path", help = "The base path for tracks.").default(".conductor/tracks")
    private val workingDir: String by option("--working-dir", help = "The root working directory.").default(System.getenv("GIT_REPO_PATH") ?: System.getProperty("user.dir"))

    override fun run() {
        logger.info("Initializing Conductor for track: $trackId")

        try {
            // 0. Read Jira Ticket ID from metadata.json
            val metadataPath = "$workingDir/$tracksPath/$trackId/metadata.json"
            val metadataFile = File(metadataPath)
            if (!metadataFile.exists()) {
                throw IllegalStateException("Metadata file not found at $metadataPath")
            }
            val objectMapper = ObjectMapper().registerModule(KotlinModule())
            val metadata = objectMapper.readTree(metadataFile)
            val ticketId = metadata.get("jira_ticket").asText()
            logger.info("Using TICKET_ID from metadata: $ticketId")

            // Load project context artifacts from .conductor/
            logger.info("Loading project context from .conductor/...")
            val projectContext = ContextLoader.loadProjectContext(workingDir)
            logger.info("Project context loaded: product=${projectContext.product != null}, techStack=${projectContext.techStack != null}, workflow=${projectContext.workflow != null}")

            // 1. Initialize Tool Configurations from Environment Variables
            val jiraConfig = JiraConfig(
                baseUrl = System.getenv("JIRA_BASE_URL") ?: throw IllegalStateException("JIRA_BASE_URL not set"),
                username = System.getenv("JIRA_USERNAME") ?: throw IllegalStateException("JIRA_USERNAME not set"),
                apiToken = System.getenv("JIRA_API_TOKEN") ?: throw IllegalStateException("JIRA_API_TOKEN not set")
            )
            val bootstrapModel = System.getenv("GEMINI_BOOTSTRAP_MODEL") ?: "gemini-pro"
            val googleApiKey = System.getenv("GOOGLE_API_KEY") 
                ?: throw IllegalStateException("GOOGLE_API_KEY environment variable must be set")
            
            logger.info("Using Google API Key authentication for Gemini")
            val geminiConfig = GeminiConfig(
                apiKey = googleApiKey,
                defaultModel = bootstrapModel
            )
            val gitRepoPath = System.getenv("GIT_REPO_PATH") ?: workingDir
            val gitConfig = GitConfig(
                workingDirectory = gitRepoPath
            )
            val fileSystemConfig = FileSystemConfig(
                basePath = gitRepoPath
            )

            // 2. Create and Register Tools
            val toolRegistry = mapOf(
                "jira" to JiraTool(jiraConfig),
                "gemini" to GeminiTool(geminiConfig),
                "git" to GitTool(gitConfig),
                "filesystem" to FileSystemTool(fileSystemConfig)
                // A 'McpTool' would be registered here as well
            )
            logger.info("Registered tools: ${toolRegistry.keys.joinToString()}")

            // 3. Initialize the Plan Executor with context
            val techStack = System.getenv("TECH_STACK") ?: "unspecified"
            val targetEnv = System.getenv("TARGET_ENV") ?: "prod"
            val baseRef = System.getenv("BASE_REF") ?: "origin/develop"
            val headRef = System.getenv("HEAD_REF") ?: "HEAD"
            
            // Build initial context with environment vars and project context
            val initialContext = mutableMapOf<String, Any>(
                "GEMINI_BOOTSTRAP_MODEL" to bootstrapModel,
                "TICKET_ID" to ticketId,
                "TECH_STACK" to techStack,
                "TARGET_ENV" to targetEnv,
                "BASE_REF" to baseRef,
                "HEAD_REF" to headRef
            )
            
            // Inject project context as structured data
            if (projectContext.product != null) {
                initialContext["PRODUCT_CONTEXT"] = objectMapper.writeValueAsString(projectContext.product)
            }
            if (projectContext.techStack != null) {
                initialContext["TECH_STACK_CONTEXT"] = objectMapper.writeValueAsString(projectContext.techStack)
            }
            if (projectContext.workflow != null) {
                initialContext["WORKFLOW_CONTEXT"] = objectMapper.writeValueAsString(projectContext.workflow)
            }
            
            val executor = PlanExecutor(toolRegistry, initialContext.toMap())

            // 4. Load and Execute the Plan
            val planPath = "$workingDir/$tracksPath/$trackId/plan.md"
            logger.info("Loading plan from: $planPath")
            val plan = PlanExecutor.loadPlanFromFile(planPath)

            // 5. Execute the Plan
            runBlocking {
                executor.execute(plan)
            }

        } catch (e: Exception) {
            logger.error("Conductor execution failed: ${e.message}", e)
            // Exit with a non-zero status code to indicate failure
            System.exit(1)
        }
    }
}
