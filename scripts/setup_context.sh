#!/bin/bash

# Setup script to initialize .conductor/ context artifacts
# This creates template files for product, tech stack, and workflow context

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CONDUCTOR_DIR="$PROJECT_ROOT/.conductor"

echo "🚀 Setting up Conductor context artifacts..."

# Create .conductor directory if it doesn't exist
if [ ! -d "$CONDUCTOR_DIR" ]; then
    mkdir -p "$CONDUCTOR_DIR"
    echo "✅ Created .conductor/ directory"
else
    echo "ℹ️  .conductor/ directory already exists"
fi

# Check if files already exist
PRODUCT_EXISTS=false
TECH_STACK_EXISTS=false
WORKFLOW_EXISTS=false

if [ -f "$CONDUCTOR_DIR/product.md" ]; then
    PRODUCT_EXISTS=true
fi

if [ -f "$CONDUCTOR_DIR/tech_stack.md" ]; then
    TECH_STACK_EXISTS=true
fi

if [ -f "$CONDUCTOR_DIR/workflow.md" ]; then
    WORKFLOW_EXISTS=true
fi

# Interactive setup
echo ""
echo "This script will help you set up context artifacts for your project."
echo "These files help AI agents understand your project better."
echo ""

# Product context
if [ "$PRODUCT_EXISTS" = true ]; then
    echo "⚠️  product.md already exists. Skipping..."
else
    echo "📝 Creating product.md..."
    cat > "$CONDUCTOR_DIR/product.md" << 'EOF'
# Product Context

## Overview
<!-- High-level description of what this project/product does -->

## Target Users
<!-- Who are the primary users of this product? -->
- 

## Product Goals
<!-- What are the main objectives and value propositions? -->
- 

## Key Features
<!-- List the core features and capabilities -->
- 

## Success Metrics
<!-- How do you measure success? -->
- 

## Constraints & Assumptions
<!-- Any important constraints or assumptions to be aware of -->
- 
EOF
    echo "✅ Created product.md"
fi

# Tech stack context
if [ "$TECH_STACK_EXISTS" = true ]; then
    echo "⚠️  tech_stack.md already exists. Skipping..."
else
    echo "📝 Creating tech_stack.md..."
    cat > "$CONDUCTOR_DIR/tech_stack.md" << 'EOF'
# Tech Stack

## Languages
<!-- Primary programming languages used -->
- 

## Frameworks & Libraries
<!-- Key frameworks and libraries -->
- 

## Database
<!-- Database technology and schema approach -->
- 

## Infrastructure
<!-- Deployment, hosting, CI/CD -->
- 

## Development Tools
<!-- Build tools, package managers, testing frameworks -->
- 

## Architecture Patterns
<!-- Design patterns, architectural styles (e.g., microservices, monolith, event-driven) -->
- 

## Third-Party Services
<!-- External APIs, services, integrations -->
- 

## Version Requirements
<!-- Specific version constraints or compatibility requirements -->
- 
EOF
    echo "✅ Created tech_stack.md"
fi

# Workflow context
if [ "$WORKFLOW_EXISTS" = true ]; then
    echo "⚠️  workflow.md already exists. Skipping..."
else
    echo "📝 Creating workflow.md..."
    cat > "$CONDUCTOR_DIR/workflow.md" << 'EOF'
# Team Workflow

## Development Process
<!-- How does the team work? Agile, Scrum, Kanban, etc. -->
- 

## Code Standards
<!-- Coding conventions, style guides, linting rules -->
- 

## Testing Strategy
<!-- Testing approach and requirements -->
- Unit tests: 
- Integration tests: 
- E2E tests: 
- Test coverage requirements: 

## Code Review Process
<!-- PR/MR review guidelines -->
- 

## Branching Strategy
<!-- Git workflow (e.g., GitFlow, trunk-based) -->
- 

## Documentation Requirements
<!-- What documentation is expected? -->
- 

## Deployment Process
<!-- How are changes deployed? -->
- 

## Quality Gates
<!-- What must pass before merging/deploying? -->
- 

## Communication Channels
<!-- Where does the team communicate? -->
- 
EOF
    echo "✅ Created workflow.md"
fi

echo ""
echo "✨ Setup complete!"
echo ""
echo "Next steps:"
echo "1. Edit the files in .conductor/ to fill in your project details"
echo "2. Commit these files to your repository for team-wide context"
echo "3. Run your Conductor skills - they will automatically use this context"
echo ""
echo "Files created:"
echo "  - $CONDUCTOR_DIR/product.md"
echo "  - $CONDUCTOR_DIR/tech_stack.md"
echo "  - $CONDUCTOR_DIR/workflow.md"
