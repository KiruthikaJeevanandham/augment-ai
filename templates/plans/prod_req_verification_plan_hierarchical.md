# Conductor Plan: Prod Requirement Verification Gate
# Purpose: Validate code changes against Jira requirements (summary, description, acceptance criteria).

phases:
  - phase: 1
    name: Context & Requirements Gathering
    description: Fetch Jira ticket details and extract structured requirements
    tasks:
      - task: 1
        name: Fetch Jira requirements
        tool: jira
        action: get_ticket_details
        params:
          ticket_id: "{{TICKET_ID}}"
        output: ticket_details

      - task: 2
        name: Build requirement spec
        tool: gemini
        action: generate_text
        params:
          model: "gemini-2.5-pro"
          prompt: |
            You are a requirements analyst. Extract a clean, structured requirement set from the Jira ticket.

            Jira Ticket:
            {{phases.1.tasks.1.output}}

            Project Context:
            - Tech Stack: {{TECH_STACK}}
            - Target Environment: {{TARGET_ENV}}
            - Product Context: {{PRODUCT_CONTEXT}}
            - Tech Stack Details: {{TECH_STACK_CONTEXT}}
            - Workflow Practices: {{WORKFLOW_CONTEXT}}

            Requirements for output:
            - Summarize the product requirement in 2-3 sentences.
            - Extract acceptance criteria as bullet points. If none exist, infer from description.
            - Extract key functional requirements.
            - Consider the product goals and constraints from the project context.

            Return ONLY a JSON object with this shape:
            {
              "summary": "...",
              "acceptance_criteria": ["..."],
              "requirements": ["..."],
              "assumptions": ["..."]
            }
        output: requirement_spec

  - phase: 2
    name: Code Diff Extraction
    description: Collect git diff between base and head refs
    tasks:
      - task: 1
        name: Verify and collect changes
        description: Verify base ref exists, collect changed files, and fail fast if no changes
        subtasks:
          - subtask: 1
            name: Verify base ref exists
            tool: git
            action: verify_ref
            params:
              ref: "{{BASE_REF}}"
            output: verified_base_ref

          - subtask: 2
            name: Collect changed files
            tool: git
            action: diff
            params:
              base_ref: "{{BASE_REF}}"
              head_ref: "{{HEAD_REF}}"
              mode: "name_only"
            output: changed_files

          - subtask: 3
            name: Fail if no changes
            tool: git
            action: assert_changes
            params:
              files: "{{phases.2.tasks.1.subtasks.2.output}}"
              base_ref: "{{BASE_REF}}"
              head_ref: "{{HEAD_REF}}"

      - task: 2
        name: Collect full diff
        tool: git
        action: diff
        params:
          base_ref: "{{BASE_REF}}"
          head_ref: "{{HEAD_REF}}"
          mode: "full"
        output: code_diff

  - phase: 3
    name: Verification Gate
    description: Evaluate changes against requirements and generate verification report
    tasks:
      - task: 1
        name: Evaluate changes vs requirements
        tool: gemini
        action: generate_text
        params:
          model: "gemini-2.5-pro"
          prompt: |
            You are a production requirement verification gate.
            Compare the Jira requirements against the code diff and decide if the changes satisfy the requirements.

            Project Context:
            - Tech Stack: {{TECH_STACK}}
            - Target Environment: {{TARGET_ENV}}
            - Product Context: {{PRODUCT_CONTEXT}}
            - Tech Stack Details: {{TECH_STACK_CONTEXT}}
            - Workflow Practices: {{WORKFLOW_CONTEXT}}

            Requirements (JSON):
            {{phases.1.tasks.2.output}}

            Changed Files:
            {{phases.2.tasks.1.subtasks.2.output}}

            Code Diff (language-agnostic; analyze all file types in diff):
            {{phases.2.tasks.2.output}}

            Output a verification report in Markdown with this format:
            - Title: "Prod Requirement Verification Gate"
            - Status Badge: PASS ✅ or FAIL ❌
            - Summary (3-5 bullets)
            - Diff Scope: Base={{BASE_REF}}, Head={{HEAD_REF}}
            - Requirements Coverage Table with status markers:
              Columns: Requirement | Status (+/- and ✅/❌) | Evidence in Diff
            - Missing requirements (if any)
            - Unexpected changes (if any)
            - Risk assessment (LOW/MEDIUM/HIGH with rationale)
            - Recommended next steps

            If Changed Files is empty ("[]"), explicitly state: "No changes applied to branch" and set Gate Result: FAIL.

            At the end, include a JSON block with this shape:
            {
              "gate_result": "PASS|FAIL",
              "missing_requirements": ["..."],
              "unexpected_changes": ["..."],
              "risk_level": "LOW|MEDIUM|HIGH",
              "notes": "..."
            }
        output: verification_report

      - task: 2
        name: Write verification report
        tool: filesystem
        action: write_file
        params:
          path: "verification_report.md"
          content: "{{phases.3.tasks.1.output}}"

      - task: 3
        name: Fail if verification gate fails
        tool: filesystem
        action: assert_gate_result
        params:
          report: "{{phases.3.tasks.1.output}}"
          expected: "PASS"
