package com.agentic.conductor.context

import com.agentic.conductor.models.*
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Loads project context artifacts from .conductor/ directory.
 */
object ContextLoader {
    private val logger = LoggerFactory.getLogger(ContextLoader::class.java)

    fun loadProjectContext(workingDir: String): ProjectContext {
        val contextDir = File(workingDir, ".conductor")
        if (!contextDir.exists() || !contextDir.isDirectory) {
            logger.warn("No .conductor/ directory found at $workingDir. Context artifacts will be empty.")
            return ProjectContext()
        }

        return ProjectContext(
            product = loadProductContext(contextDir),
            techStack = loadTechStackContext(contextDir),
            workflow = loadWorkflowContext(contextDir)
        )
    }

    private fun loadProductContext(contextDir: File): ProductContext? {
        val productFile = File(contextDir, "product.md")
        if (!productFile.exists()) {
            logger.debug("product.md not found in .conductor/")
            return null
        }

        return try {
            val content = productFile.readText()
            parseProductContext(content)
        } catch (e: Exception) {
            logger.error("Failed to load product.md: ${e.message}", e)
            null
        }
    }

    private fun loadTechStackContext(contextDir: File): TechStackContext? {
        val techStackFile = File(contextDir, "tech_stack.md")
        if (!techStackFile.exists()) {
            logger.debug("tech_stack.md not found in .conductor/")
            return null
        }

        return try {
            val content = techStackFile.readText()
            parseTechStackContext(content)
        } catch (e: Exception) {
            logger.error("Failed to load tech_stack.md: ${e.message}", e)
            null
        }
    }

    private fun loadWorkflowContext(contextDir: File): WorkflowContext? {
        val workflowFile = File(contextDir, "workflow.md")
        if (!workflowFile.exists()) {
            logger.debug("workflow.md not found in .conductor/")
            return null
        }

        return try {
            val content = workflowFile.readText()
            parseWorkflowContext(content)
        } catch (e: Exception) {
            logger.error("Failed to load workflow.md: ${e.message}", e)
            null
        }
    }

    private fun parseProductContext(content: String): ProductContext {
        val sections = extractSections(content)
        return ProductContext(
            overview = sections["Overview"]?.firstOrNull(),
            targetUsers = extractListItems(sections["Target Users"]),
            productGoals = extractListItems(sections["Product Goals"]),
            keyFeatures = extractListItems(sections["Key Features"]),
            successMetrics = extractListItems(sections["Success Metrics"]),
            constraints = extractListItems(sections["Constraints & Assumptions"])
        )
    }

    private fun parseTechStackContext(content: String): TechStackContext {
        val sections = extractSections(content)
        return TechStackContext(
            languages = extractListItems(sections["Languages"]),
            frameworks = extractListItems(sections["Frameworks & Libraries"]),
            database = sections["Database"]?.firstOrNull(),
            infrastructure = extractListItems(sections["Infrastructure"]),
            developmentTools = extractListItems(sections["Development Tools"]),
            architecturePatterns = extractListItems(sections["Architecture Patterns"]),
            thirdPartyServices = extractListItems(sections["Third-Party Services"]),
            versionRequirements = extractKeyValuePairs(sections["Version Requirements"])
        )
    }

    private fun parseWorkflowContext(content: String): WorkflowContext {
        val sections = extractSections(content)
        val testingLines = sections["Testing Strategy"] ?: emptyList()
        val testingStrategy = if (testingLines.isNotEmpty()) {
            TestingStrategy(
                unitTests = extractKeyValue(testingLines, "Unit tests"),
                integrationTests = extractKeyValue(testingLines, "Integration tests"),
                e2eTests = extractKeyValue(testingLines, "E2E tests"),
                coverageRequirements = extractKeyValue(testingLines, "Test coverage requirements")
            )
        } else null

        return WorkflowContext(
            developmentProcess = sections["Development Process"]?.firstOrNull(),
            codeStandards = extractListItems(sections["Code Standards"]),
            testingStrategy = testingStrategy,
            codeReviewProcess = sections["Code Review Process"]?.firstOrNull(),
            branchingStrategy = sections["Branching Strategy"]?.firstOrNull(),
            documentationRequirements = extractListItems(sections["Documentation Requirements"]),
            deploymentProcess = sections["Deployment Process"]?.firstOrNull(),
            qualityGates = extractListItems(sections["Quality Gates"])
        )
    }

    private fun extractSections(content: String): Map<String, List<String>> {
        val sections = mutableMapOf<String, MutableList<String>>()
        var currentSection: String? = null

        content.lines().forEach { line ->
            when {
                line.startsWith("## ") -> {
                    currentSection = line.removePrefix("## ").trim()
                    sections[currentSection!!] = mutableListOf()
                }
                currentSection != null && line.isNotBlank() && !line.startsWith("<!--") && !line.endsWith("-->") -> {
                    sections[currentSection]?.add(line.trim())
                }
            }
        }

        return sections
    }

    private fun extractListItems(lines: List<String>?): List<String> {
        return lines?.filter { it.startsWith("-") }
            ?.map { it.removePrefix("-").trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    private fun extractKeyValuePairs(lines: List<String>?): Map<String, String> {
        return lines?.filter { it.contains(":") }
            ?.associate {
                val parts = it.split(":", limit = 2)
                parts[0].trim() to parts.getOrNull(1)?.trim().orEmpty()
            }
            ?: emptyMap()
    }

    private fun extractKeyValue(lines: List<String>, key: String): String? {
        return lines.find { it.startsWith("- $key:") }
            ?.removePrefix("- $key:")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
