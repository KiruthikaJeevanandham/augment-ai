# Conductor Context Artifacts

This directory contains project context artifacts that help AI agents understand your project better. These files are automatically loaded by the Conductor execution engine and injected into plan execution.

## Files

### `product.md`
Defines your product's purpose, users, goals, and features. This helps AI agents understand:
- What problem you're solving
- Who your users are
- What success looks like
- Key constraints and assumptions

### `tech_stack.md`
Describes your technical architecture and tooling. This helps AI agents:
- Generate code in the right languages/frameworks
- Respect version constraints
- Understand architectural patterns
- Integrate with existing services

### `workflow.md`
Documents your team's development practices. This helps AI agents:
- Follow your coding standards
- Meet testing requirements
- Adhere to your branching strategy
- Respect quality gates

## Setup

Run the setup script to create template files:

```bash
./scripts/setup_context.sh
```

Then edit each file to fill in your project details.

## Usage

Context artifacts are automatically loaded when you run any Conductor skill. The context is available in plan templates via these variables:

- `{{PRODUCT_CONTEXT}}` - JSON string of product context
- `{{TECH_STACK_CONTEXT}}` - JSON string of tech stack context
- `{{WORKFLOW_CONTEXT}}` - JSON string of workflow context

### Example Plan Usage

```yaml
- task: 1
  name: Generate code
  tool: gemini
  action: generate_text
  params:
    prompt: |
      Generate a new feature based on these requirements.
      
      Product Context: {{PRODUCT_CONTEXT}}
      Tech Stack: {{TECH_STACK_CONTEXT}}
      Workflow: {{WORKFLOW_CONTEXT}}
      
      Requirements: ...
```

## Benefits

✅ **Team Alignment**: Everyone works with the same context  
✅ **Better AI Output**: AI understands your project deeply  
✅ **Onboarding**: New team members see documented practices  
✅ **Consistency**: All features follow the same patterns  
✅ **Version Control**: Context evolves with your project  

## Best Practices

1. **Keep it updated**: Review and update context files as your project evolves
2. **Be specific**: Concrete details help more than vague descriptions
3. **Commit to repo**: Share context with your team via version control
4. **Start simple**: Fill in what you know now, expand over time
5. **Reference in plans**: Use context variables in your plan templates

## Migration

If you have existing plans that don't use context, they will continue to work. Context variables are optional and will be empty strings if context files don't exist.
