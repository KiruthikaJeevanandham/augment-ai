# Product Requirement Verification Skill

A standalone skill that uses AI (Gemini 2.5 Pro) to verify code changes against Jira requirements. It can be integrated into any application's CI/CD pipeline or used locally by developers.

## Overview

This skill analyzes code changes against Jira requirements and categorizes findings into:

- **Category 1: Straightforward Crucial Cases** - Clear violations that MUST be fixed before merging
- **Category 2: Edge Cases** - Identifying edge cases for developer verification

## Quick Start

### Prerequisites

- jq
- Gemini API Key (get from https://aistudio.google.com/app/apikey)
- Jira credentials (if using Jira integration)

### Local Execution

```bash
# Run on current branch (auto-detects Jira ticket)
./scripts/run_product_requirement_verification.sh /path/to/repo

# Run with specific Jira ticket
./scripts/run_product_requirement_verification.sh /path/to/repo PROJ-123
```

### GitHub Actions Integration

Copy the workflow file to your repository:

```bash
cp .github/workflows/product-requirement-verification.yml <your-repo>/.github/workflows/
```

Add the required secrets to your repository:
- `GOOGLE_API_KEY`
- `JIRA_BASE_URL`
- `JIRA_USERNAME`
- `JIRA_API_TOKEN`

## Documentation

- **Skill Documentation**: `skills/prod_requirement_verification.md`
- **Setup Instructions**: `docs/setup.md`
- **Execution Script**: `scripts/run_product_requirement_verification.sh`
- **GitHub Actions**: `.github/workflows/product-requirement-verification.yml`

## Features

- Direct Gemini API integration (no additional dependencies)
- Auto-detects Jira ticket from branch name
- Auto-detects tech stack from project files
- Categorizes violations into crucial and edge cases
- Generates detailed markdown reports with JSON summary
- Works both locally and in CI/CD

## License

MIT
