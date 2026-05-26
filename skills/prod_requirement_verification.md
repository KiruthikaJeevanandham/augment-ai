# Product Requirement Verification

## Overview

Product Requirement Verification uses AI (Gemini 2.5 Pro) to verify code changes against Jira requirements. It categorizes findings into two categories:

- **Category 1: Straightforward Crucial Cases** - Clear violations that MUST be fixed before merging
- **Category 2: Edge Cases** - Identifying edge cases for developer verification

**Note**: This uses direct Gemini API calls for analysis - no additional plugins required.

## What It Detects

The skill analyzes code changes for:

1. **API Layer Violations** - Bypassing established API layers to hit databases/services directly
2. **Abstraction Breakdown** - Adding special-case conditionals instead of proper abstractions
3. **Pattern Inconsistency** - Using different patterns than the established baseline
4. **Data Access Drift** - Changing how data is accessed (e.g., direct SQL vs repository pattern)
5. **Tight Coupling** - Creating dependencies that violate established boundaries
6. **Utility Duplication** - Reinventing utilities that already exist
7. **Error Handling Drift** - Using different error handling patterns
8. **State Management Drift** - Changing how state is managed
9. **Dependency Injection Violations** - Direct instantiation instead of DI
10. **Architectural Boundary Crossings** - Crossing module/component boundaries incorrectly

## Quick Start

### Prerequisites

1. **Get Gemini API Key**:
   - Visit https://aistudio.google.com/app/apikey
   - Create an API key
   - Add to `.env` file: `GOOGLE_API_KEY=your-api-key`

2. **Install jq**:
   ```bash
   brew install jq  # macOS
   sudo apt-get install jq  # Linux
   ```

3. **Configure environment variables** in `.env`:
   ```bash
   GOOGLE_API_KEY=your-gemini-api-key
   JIRA_BASE_URL=https://your-jira-instance.atlassian.net
   JIRA_USERNAME=your-email@company.com
   JIRA_API_TOKEN=your-jira-api-token
   ```

### Local Execution

```bash
# Run on current branch (auto-detects Jira ticket)
./scripts/run_product_requirement_verification.sh /path/to/repo

# Run with specific Jira ticket
./scripts/run_product_requirement_verification.sh /path/to/repo PROJ-123

# Run with custom base/head refs
./scripts/run_product_requirement_verification.sh /path/to/repo PROJ-123 origin/develop HEAD
```

### GitHub Actions (Remote Execution)

The workflow runs automatically on push to feature branches (excluding develop, release, main).

Manual trigger:
1. Go to **Actions** → **Product Requirement Verification**
2. Click **Run workflow**
3. Provide:
   - Target repository
   - Jira ticket ID
   - Base ref (default: origin/develop)
   - Head ref (default: HEAD)

## Categorization Logic

### Category 1: Straightforward Crucial Cases (MUST PASS)

These are clear violations that MUST be fixed:
- Direct database access bypassing API layers
- Special-case conditionals instead of abstractions
- Tight coupling to implementation details
- Security-relevant pattern violations
- Breaking established architectural boundaries

**Result**: If any Category 1 violations are found, the skill fails. You MUST fix these before merging.

### Category 2: Edge Cases (Identifying Edge Cases for Developer Verification)

These are edge cases related to product requirements that developers can verify:
- Potential edge cases in business logic
- Scenarios that may not be covered by current implementation
- Boundary conditions in the requirements
- Integration considerations

**Result**: These are informational. Developers can review and decide whether to address them based on context.

## Report Format

The generated report includes:

```markdown
## Product Requirement Verification Report

### Summary
Brief summary (2-3 bullets)

### Category 1: Straightforward Crucial Cases (MUST PASS)
- Violation 1: Brief description (1 sentence)
- Overall Category 1 Status: PASS ✅ or FAIL ❌

### Category 2: Edge Cases (Identifying Edge Cases for Developer Verification)
- Violation 1: Detailed description (2-3 sentences to help developer understand context)
- Overall Category 2 Status: PASS ✅ or FAIL ❌

### New Features/Changes
- ✅ Feature 1: Brief description
- ✅ Feature 2: Brief description

### Requirements Coverage
- Requirement 1: PASS ✅
- Requirement 2: PASS ✅

### Risk Assessment
- LOW/MEDIUM/HIGH

### Recommendations
- Recommendation 1
- Recommendation 2

### JSON Block
```json
{
  "overall_status": "PASS|FAIL",
  "category1_status": "PASS|FAIL",
  "category1_violations": [...],
  "category2_status": "PASS|FAIL",
  "category2_violations": [...],
  "total_violations": 0,
  "critical_violations": 0,
  "risk_level": "LOW|MEDIUM|HIGH",
  "notes": "..."
}
```

## Integration with CI/CD

### As a Required Check

Add to your branch protection rules:

1. Go to repository **Settings** → **Branches**
2. Add branch protection rule for `develop`/`main`
3. Require status check: "Architecture Drift Detection"

### Combined with Other Skills

Run multiple skills in sequence:

```bash
# 1. Verify requirements
./scripts/run_skill.sh prod_req_verification PROJ-123

# 2. Check architecture drift
./scripts/run_architecture_drift_detection.sh /path/to/repo PROJ-123

# 3. Review both reports before merging
```

## Configuration

### Environment Variables

Required secrets (set in `.env` or GitHub Actions secrets):

| Variable | Description |
|----------|-------------|
| `GOOGLE_API_KEY` | Gemini API key for AI analysis |
| `JIRA_BASE_URL` | Your Jira instance URL |
| `JIRA_USERNAME` | Jira user email |
| `JIRA_API_TOKEN` | Jira API token |

### Getting Gemini API Key

1. Visit https://aistudio.google.com/app/apikey
2. Create a new API key
3. Add it to your `.env` file: `GOOGLE_API_KEY=your-api-key`
4. For GitHub Actions, add it as a repository secret

### Customizing Detection Logic

Edit the verification prompt in the script to add custom detection criteria:

**File**: `scripts/run_product_requirement_verification.sh`

Add to the VERIFICATION_PROMPT section:

```bash
### Category 1: Straightforward Crucial Cases (MUST PASS)
These are clear violations that MUST be fixed:
- Direct database access bypassing API layers
- Special-case conditionals instead of abstractions
- Tight coupling to implementation details
- Security-relevant pattern violations
- Breaking established architectural boundaries
- Missing core functionality from requirements
- **Your custom rule here**
```

## Troubleshooting

### "GOOGLE_API_KEY not set" Error

Add your Gemini API key to `.env`:
```bash
GOOGLE_API_KEY=your-api-key
```

Get an API key from: https://aistudio.google.com/app/apikey

### "jq not found" Error

Install jq:
```bash
brew install jq  # macOS
sudo apt-get install jq  # Linux
```

### "Category 1 failed" Error

This means you have straightforward crucial violations. Check the report for:
- File:line evidence
- Description of the violation
- Why it's a problem
- Suggested fix

Fix all Category 1 violations before retrying.

### "No changes applied to branch"

This occurs when the diff is empty. Ensure:
- You have uncommitted changes
- The HEAD_REF and BASE_REF are correct
- You're on the correct branch

### "Could not extract Jira ticket ID"

Ensure your branch name contains a Jira ticket ID:
- ✅ `feature/PROJ-123-add-feature`
- ✅ `bugfix/PROJ-456-fix-issue`
- ❌ `feature/add-new-button`

### "Failed to fetch Jira ticket details"

Check your Jira credentials in `.env`:
- JIRA_BASE_URL is correct
- JIRA_USERNAME is correct
- JIRA_API_TOKEN is valid

### "Failed to get response from Gemini API"

Check:
- GOOGLE_API_KEY is valid
- You have API quota available
- Network connectivity is working

## Best Practices

### For Developers

1. **Run locally before pushing** - Catch issues early
2. **Review Category 2 findings** - Even though they're not blocking, they may indicate technical debt
3. **Fix Category 1 violations immediately** - These are architectural violations
4. **Document architectural decisions** - If you intentionally deviate, add comments explaining why

### For Teams

1. **Set up CI/CD integration** - Make it a required check
2. **Review drift trends** - Track architectural violations over time
3. **Update skill plan** - Add team-specific detection rules
4. **Train on common violations** - Educate developers on architectural patterns

### For Organizations

1. **Standardize architectural patterns** - Define and document patterns
2. **Track drift metrics** - Monitor architectural health
3. **Refactor when needed** - Address systematic drift
4. **Use findings for retrospectives** - Improve architectural awareness

## Examples

### Example 1: API Layer Violation (Category 1)

```
Category 1: Straightforward Crucial Cases (MUST PASS)

1. API Layer Violation (Severity: CRITICAL)
   File: src/services/UserService.kt:45
   Line: 45-50
   Description: Direct database query bypassing repository layer
   Why it's a problem: Violates established API layer pattern, creates tight coupling
   Suggested fix: Move database access to UserRepository and use it from UserService
```

### Example 2: Pattern Inconsistency (Category 2)

```
Category 2: Edge Cases (Developer Discretion)

1. Pattern Inconsistency (Severity: LOW)
   File: src/utils/DateHelper.kt:23
   Line: 23-28
   Description: Using different date formatting pattern than established
   Why it's a problem: Minor inconsistency, but doesn't break functionality
   Suggested fix: Use the standard date formatter from DateUtils
```

## Technical Details

### Architecture

- **Local Script**: `scripts/run_product_requirement_verification.sh`
- **GitHub Actions**: `.github/workflows/product-requirement-verification.yml`
- **AI Engine**: Gemini 2.5 Pro via Google AI API
- **Model**: gemini-2.5-pro

### How It Works

1. **Extract Jira ticket** - Get ticket details from Jira API
2. **Get code diff** - Compare base ref to head ref
3. **Auto-detect tech stack** - Identify project type from files
4. **Run Gemini analysis** - Use Gemini API to verify changes against requirements
5. **Categorize findings** - Split into Category 1 and Category 2
6. **Generate report** - Create detailed markdown report
7. **Check Category 1** - Fail if any Category 1 violations

### Model Configuration

Uses `gemini-2.5-pro` via Google AI API for:
- Requirement verification
- Architecture drift detection
- Categorization of findings

## Next Steps

1. ✅ Run the skill locally on your current changes
2. ✅ Review the report and fix Category 1 violations
3. ✅ Consider Category 2 findings for technical debt
4. ✅ Integrate into your CI/CD pipeline
5. ✅ Customize detection rules for your team's patterns

## Resources

- **Local Script**: `scripts/run_product_requirement_verification.sh`
- **GitHub Actions**: `.github/workflows/product-requirement-verification.yml`
- **Documentation**: `docs/ARCHITECTURE_DRIFT_DETECTION.md`
- **Devil's Advocate Integration**: `docs/DEVILS_ADVOCATE_INTEGRATION.md`

---

**Last Updated**: 2026-05-26  
**Version**: 3.0 (Gemini Edition)  
**Maintained by**: DevOps Team
