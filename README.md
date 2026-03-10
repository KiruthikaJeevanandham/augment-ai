# Agentic AI Conductor

This repository contains an instruction-driven agentic AI system powered by a plan-based execution engine. It is designed to be adaptable to any project by defining tasks in machine-readable `plan.md` files.

## Core Architecture

- **Plan Executor**: The heart of the system. It reads a `plan.md` file from a track and executes the defined steps in sequence.
- **Tools**: A collection of single-purpose modules that the executor can call (e.g., `JiraTool`, `GeminiTool`, `GitTool`, `FileSystemTool`).
- **Tracks**: Self-contained directories representing a single unit of work. Each track has a `spec.md` (the goal), a `plan.md` (the execution steps), and `metadata.json`.

## Local Development Setup

### Prerequisites

- Java 17+
- Gradle 8+
- Google Cloud SDK (for Gemini/Vertex AI)
- GitHub CLI (`gh`) (for creating pull requests)
- A GCP project with Vertex AI enabled
- A Jira project
- A GitHub repository

### 1. Set Up Authentication & Environment

**For GCP:**

Authenticate with Google Cloud. This allows the `GeminiTool` to access Vertex AI.
```bash
gcloud auth application-default login
gcloud config set project YOUR_GCP_PROJECT_ID
```

**For GitHub:**

Authenticate the GitHub CLI. This allows the `GitTool` to create pull requests.
```bash
gh auth login
```

**For Environment Variables:**

Create a `.env` file in the root of the project and add your secrets. This file provides credentials to the tools.

```sh
# .env file

# GCP Project ID
GCP_PROJECT_ID="your-gcp-project-id"

# (Optional) The initial model used to select other models. Use a model you know is available.
GEMINI_BOOTSTRAP_MODEL="gemini-pro"

# Jira Credentials
JIRA_BASE_URL="https://your-domain.atlassian.net"
JIRA_USERNAME="your-jira-email"
JIRA_API_TOKEN="your-jira-api-token"

# GitHub - The GitTool uses the GitHub CLI ('gh') for PRs, which uses its own auth.
# A token is not needed here unless you add a tool that uses it directly.
```

### 2. Run a Skill

To run a skill, use the `run_skill.sh` script. This script automates the entire process of track creation and execution.

```bash
# Usage: ./scripts/run_skill.sh <SKILL_NAME> <JIRA_TICKET_ID>
./scripts/run_skill.sh pr_creator WFPLUS-576
```

This single command will:
1.  Create a new track with a unique ID.
2.  Generate the `spec.md` and `plan.md` for the specified skill.
3.  Load your credentials from the `.env` file.
4.  Execute the plan, running all the steps from start to finish.
