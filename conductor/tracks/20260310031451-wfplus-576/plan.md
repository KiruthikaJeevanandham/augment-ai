# Default Plan for Pull Request Creation

# Description: "Automates the process of creating a pull request for a given Jira ticket."

steps:
  - step: 1
    tool: jira
    action: get_ticket_details
    params:
      ticket_id: "{{TICKET_ID}}"
    output: ticket_details

  - step: 2
    tool: filesystem
    action: create_branch_name
    params:
      ticket_key: "{{steps.1.output.key}}"
      issue_type: "{{steps.1.output.fields.issuetype.name}}"
      summary: "{{steps.1.output.fields.summary}}"
    output: branch_name

  - step: 3
    tool: git
    action: checkout_branch
    params:
      branch_name: "{{steps.2.output}}"
      base_branch: "develop"

  - step: 4
    tool: filesystem
    action: read_file
    params:
      path: "build.gradle"
    output: build_gradle_content

  - step: 5
    tool: gemini
    action: generate_text
    params:
      model: "gemini-2.5-pro"
      prompt: |
        You are an expert Android/Gradle developer. Analyze the Jira ticket and the provided build.gradle file.
        Your task is to create a precise, surgical plan. Do not generate code. Output only a JSON plan.

        Jira Ticket: {{steps.1.output}}
        Build File Content: {{steps.4.output}}

        Output a JSON plan with exact changes:
        {
          "analysis": "This is an Android project using Gradle. The goal is to update dependency versions.",
          "changes": [
            {
              "dependency": "com.google.firebase:firebase-crashlytics-gradle",
              "current_version": "3.0.3",
              "new_version": "3.1.0"
            },
            {
              "dependency": "androidx.appcompat:appcompat",
              "current_version": "1.7.0",
              "new_version": "1.8.0"
            }
          ]
        }
    output: dependency_update_plan

  - step: 6
    tool: filesystem
    action: edit_file
    params:
      path: "build.gradle"
      changes: "{{steps.5.output.changes}}"

  - step: 7
    tool: git
    action: commit
    params:
      message: "feat({{steps.1.output.key}}): Implement {{steps.1.output.fields.summary}}"

  - step: 8
    tool: git
    action: push

  - step: 9
    tool: git
    action: create_pr
    params:
      title: "{{steps.1.output.fields.summary}}"
      body: "Implements changes for {{steps.1.output.key}}"
