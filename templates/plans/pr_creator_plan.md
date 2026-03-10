# Conductor Plan: Pull Request Creation
# Follows the Analyze -> Plan -> Implement workflow.

steps:
  # --- PHASE 1: CONTEXT & ANALYSIS ---
  - step: 1
    tool: jira
    action: get_ticket_details
    params:
      ticket_id: "{{TICKET_ID}}"
    output: ticket_details

  - step: 2
    tool: filesystem
    action: list_files
    params:
      path: "."
      recursive: false
      files_only: false
    output: project_roots

  - step: 3
    tool: filesystem
    action: list_files
    params:
      path: "."
      recursive: true
      files_only: true
      max_depth: 8
      limit: 300
      include_patterns:
        - "**/build.gradle"
        - "**/build.gradle.kts"
        - "**/settings.gradle"
        - "**/settings.gradle.kts"
        - "**/gradle.properties"
        - "**/gradle-wrapper.properties"
        - "**/buildkite-properties.yaml"
        - "**/renovate.json"
        - "**/README.md"
        - "**/.github/workflows/*.yml"
        - "**/.github/workflows/*.yaml"
        - "**/Dockerfile"
        - "**/pom.xml"
    output: build_config_index

  - step: 4
    tool: filesystem
    action: read_files
    params:
      paths: "{{steps.3.output}}"
    output: build_file_contents

  # --- PHASE 2: SPECIFICATION & PLANNING ---
  - step: 5
    tool: gemini
    action: generate_text
    params:
      model: "gemini-2.5-pro"
      prompt: |
        You are a Staff Software Engineer. Your goal is to create a detailed implementation plan for the Jira ticket.

        CONTEXT:
        - Jira Ticket: {{steps.1.output}}
        - Project Structure (top-level): {{steps.2.output}}
        - Build Configuration Files (read content): {{steps.4.output}}

        RULES:
        - Infer the build system ONLY from the provided build files.
        - If no pom.xml is present, DO NOT mention Maven or pom.xml.
        - If build.gradle/build.gradle.kts exists, treat this as a Gradle project.

        TASK:
        1. Analyze the requirements.
        2. Identify the specific files that need modification.
        3. Create a step-by-step implementation plan.

        OUTPUT:
        Generate a Markdown file content called `implementation_spec.md`.
        It must contain:
        - **Objective**: Summary of what needs to be done.
        - **Target Files**: List of files to modify.
        - **Proposed Changes**: High-level description of changes for each file.
    output: implementation_spec_content

  - step: 6
    tool: filesystem
    action: write_file
    params:
      path: "implementation_spec.md"
      content: "{{steps.5.output}}"

  # --- PHASE 3: IMPLEMENTATION PREPARATION ---
  - step: 7
    tool: filesystem
    action: list_files
    params:
      path: "."
      recursive: true
      files_only: true
      max_depth: 8
      limit: 500
      include_patterns:
        - "**/*.kt"
        - "**/*.java"
        - "**/*.kts"
        - "**/*.gradle"
        - "**/*.gradle.kts"
        - "**/*.xml"
        - "**/*.yml"
        - "**/*.yaml"
        - "**/*.json"
        - "**/*.properties"
    output: source_index

  - step: 8
    tool: gemini
    action: generate_text
    params:
      model: "gemini-2.5-pro"
      prompt: |
        Analyze the `implementation_spec.md` and extract the list of files that need to be read to perform the implementation.

        RULES:
        - You MUST ONLY return paths that exist in the Source Index below.
        - If a needed file is not present, omit it.
        - Always include build/config files if dependency versions are being updated.

        Spec:
        {{steps.5.output}}

        Source Index:
        {{steps.7.output}}

        Return ONLY a JSON object with the list of paths.
        Example: {"paths": ["build.gradle", "app/build.gradle"]}
    output: required_files

  - step: 9
    tool: filesystem
    action: filter_existing_paths
    params:
      paths: "{{steps.8.output.paths}}"
    output: validated_files

  - step: 10
    tool: filesystem
    action: read_files
    params:
      paths: "{{steps.9.output.paths}}"
    output: file_contents

  # --- PHASE 4: EXECUTION ---
  - step: 11
    tool: filesystem
    action: create_branch_name
    params:
      ticket_key: "{{steps.1.output.key}}"
      issue_type: "{{steps.1.output.fields.issuetype.name}}"
      summary: "{{steps.1.output.fields.summary}}"
    output: branch_name

  - step: 12
    tool: git
    action: checkout_branch
    params:
      branch_name: "{{steps.11.output}}"
      base_branch: "develop"

  - step: 13
    tool: gemini
    action: generate_text
    params:
      model: "gemini-2.5-pro"
      prompt: |
        You are an expert Coding Agent. Implement the changes described in the spec.

        CONTEXT:
        - Spec: {{steps.5.output}}
        - Build Config Contents: {{steps.4.output}}
        - File Contents: {{steps.10.output}}
        - Valid Paths: {{steps.9.output.paths}}

        TASK:
        Generate the surgical regex edits to apply the changes.
        - STRICTLY follow the spec.
        - Only modify files provided in "Valid Paths".
        - Use precise regex.
        - You MAY use glob patterns (e.g. "src/**/*.java") in the 'path' field if they match entries in Valid Paths.

        OUTPUT:
        Return a valid JSON object.
        The root element MUST be an object with a single key "edits".
        "edits" MUST be an array of edit objects.

        Example:
        {
          "edits": [
            {
              "path": "build.gradle",
              "search_pattern": "(com\\.example:library:)[0-9\\.]+",
              "replacement": "$12.0.0"
            }
          ]
        }
    output: surgical_edits

  - step: 14
    tool: filesystem
    action: apply_surgical_edits
    params:
      edits: "{{steps.13.output.edits}}"

  # --- PHASE 5: DELIVERY ---
  - step: 15
    tool: git
    action: commit
    params:
      message: "feat({{steps.1.output.key}}): Implement {{steps.1.output.fields.summary}}"

  - step: 16
    tool: git
    action: push

  - step: 17
    tool: git
    action: create_pr
    params:
      title: "{{steps.1.output.fields.summary}}"
      body: "Implements {{steps.1.output.key}}\n\nSee `implementation_spec.md` for details."
