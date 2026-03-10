# Conductor Plan: Prod Requirement Verification Gate
# Purpose: Validate code changes against Jira requirements (summary, description, acceptance criteria).

steps:
  # --- PHASE 1: CONTEXT & REQUIREMENTS ---
  - step: 1
    name: Fetch Jira requirements
    tool: jira
    action: get_ticket_details
    params:
      ticket_id: "{{TICKET_ID}}"
    output: ticket_details

  - step: 2
    name: Build requirement spec
    tool: gemini
    action: generate_text
    params:
      model: "gemini-2.5-pro"
      prompt: |
        You are a requirements analyst. Extract a clean, structured requirement set from the Jira ticket.

        Jira Ticket:
        {{steps.1.output}}

        Context:
        - Tech Stack: {{TECH_STACK}}
        - Target Environment: {{TARGET_ENV}}

        Requirements for output:
        - Summarize the product requirement in 2-3 sentences.
        - Extract acceptance criteria as bullet points. If none exist, infer from description.
        - Extract key functional requirements.

        Return ONLY a JSON object with this shape:
        {
          "summary": "...",
          "acceptance_criteria": ["..."],
          "requirements": ["..."],
          "assumptions": ["..."]
        }
    output: requirement_spec

  # --- PHASE 2: CODE DIFF EXTRACTION ---
  # NOTE: BASE_REF/HEAD_REF are injected from environment (default BASE_REF=origin/develop).
  - step: 3
    name: Verify base ref exists
    tool: git
    action: verify_ref
    params:
      ref: "{{BASE_REF}}"
    output: verified_base_ref

  - step: 4
    name: Collect changed files
    tool: git
    action: diff
    params:
      base_ref: "{{BASE_REF}}"
      head_ref: "{{HEAD_REF}}"
      mode: "name_only"
    output: changed_files

  - step: 5
    name: Fail if no changes
    tool: git
    action: assert_changes
    params:
      files: "{{steps.4.output}}"
      base_ref: "{{BASE_REF}}"
      head_ref: "{{HEAD_REF}}"

  - step: 6
    name: Collect full diff
    tool: git
    action: diff
    params:
      base_ref: "{{BASE_REF}}"
      head_ref: "{{HEAD_REF}}"
      mode: "full"
    output: code_diff

  # --- PHASE 3: VERIFICATION GATE ---
  - step: 7
    name: Evaluate changes vs requirements
    tool: gemini
    action: generate_text
    params:
      model: "gemini-2.5-pro"
      prompt: |
        You are a production requirement verification gate.
        Compare the Jira requirements against the code diff and decide if the changes satisfy the requirements.

        Context:
        - Tech Stack: {{TECH_STACK}}
        - Target Environment: {{TARGET_ENV}}

        Requirements (JSON):
        {{steps.2.output}}

        Changed Files:
        {{steps.4.output}}

        Code Diff (language-agnostic; analyze all file types in diff):
        {{steps.6.output}}

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

  - step: 8
    name: Write verification report
    tool: filesystem
    action: write_file
    params:
      path: "verification_report.md"
      content: "{{steps.7.output}}"

  - step: 9
    name: Fail if verification gate fails
    tool: filesystem
    action: assert_gate_result
    params:
      report: "{{steps.7.output}}"
      expected: "PASS"
