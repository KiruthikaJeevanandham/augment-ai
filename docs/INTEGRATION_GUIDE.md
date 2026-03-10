# Integration Guide: Add Prod Requirement Verification to Your Project

This guide shows how to integrate the Prod Requirement Verification workflow into any project (e.g., `maine-workflow-app`).

## Prerequisites

1. **GitHub Secrets** configured in your target repo:
   - `GOOGLE_API_KEY` - Google API Key (get from https://aistudio.google.com/app/apikey)
   - `JIRA_BASE_URL` - Your Jira instance URL
   - `JIRA_USERNAME` - Jira username
   - `JIRA_API_TOKEN` - Jira API token
   - `PAT_TOKEN` - GitHub Personal Access Token with `repo` scope (if conductor repo is private)

2. **Branch naming convention**: Use Jira ticket IDs in branch names
   - `feature/WFPLUS-576-add-field`
   - `bugfix/PROJ-123-fix-crash`
   - `WFPLUS-576-description`

## Option 1: Copy Workflow File (Recommended)

### Step 1: Copy the workflow file

```bash
# In your target project (e.g., maine-workflow-app)
mkdir -p .github/workflows
cp /path/to/agentic-ai-conductor/.github/workflows/prod_req_verification.yml \
   .github/workflows/prod_req_verification.yml
```

### Step 2: Update the workflow for your repo

Edit `.github/workflows/prod_req_verification.yml`:

```yaml
on:
  push:
    branches:
      - 'feature/**'
      - 'bugfix/**'
      - 'chore/**'
      - 'hotfix/**'
    paths-ignore:
      - '**.md'
      - 'docs/**'
      - '.github/**'
```

### Step 3: Commit and push

```bash
git add .github/workflows/prod_req_verification.yml
git commit -m "Add prod requirement verification workflow"
git push origin main
```

### Step 4: Test it

Create a feature branch with a Jira ticket:

```bash
git checkout -b feature/WFPLUS-576-test-verification
# Make some changes
git add .
git commit -m "Test verification"
git push origin feature/WFPLUS-576-test-verification
```

The workflow will **automatically run** and verify your changes against the Jira ticket.

---

## Option 2: Reusable Workflow (Advanced)

If you want to centralize the workflow in the conductor repo and call it from other repos:

### In conductor repo: Make workflow reusable

Add to `.github/workflows/prod_req_verification.yml`:

```yaml
on:
  workflow_call:
    inputs:
      target_repo:
        required: true
        type: string
      jira_ticket_id:
        required: false
        type: string
      tech_stack:
        required: false
        type: string
        default: 'unspecified'
      target_env:
        required: false
        type: string
        default: 'prod'
      base_ref:
        required: false
        type: string
        default: 'origin/develop'
      head_ref:
        required: false
        type: string
        default: 'HEAD'
    secrets:
      GOOGLE_API_KEY:
        required: true
      JIRA_BASE_URL:
        required: true
      JIRA_USERNAME:
        required: true
      JIRA_API_TOKEN:
        required: true
      PAT_TOKEN:
        required: true
```

### In target repo (maine-workflow-app): Call the workflow

Create `.github/workflows/verify.yml`:

```yaml
name: Verify Requirements

on:
  push:
    branches:
      - 'feature/**'
      - 'bugfix/**'
      - 'chore/**'
      - 'hotfix/**'

jobs:
  verify:
    uses: PeopleNet/agentic-ai-conductor/.github/workflows/prod_req_verification.yml@main
    with:
      target_repo: ${{ github.repository }}
      tech_stack: 'android'  # Customize for your project
      target_env: 'prod'
    secrets:
      GOOGLE_API_KEY: ${{ secrets.GOOGLE_API_KEY }}
      JIRA_BASE_URL: ${{ secrets.JIRA_BASE_URL }}
      JIRA_USERNAME: ${{ secrets.JIRA_USERNAME }}
      JIRA_API_TOKEN: ${{ secrets.JIRA_API_TOKEN }}
      PAT_TOKEN: ${{ secrets.PAT_TOKEN }}
```

---

## How It Works

1. **Push to feature branch** → Workflow triggers automatically
2. **Extract Jira ticket** from branch name (e.g., `WFPLUS-576`)
3. **Fetch Jira requirements** from your Jira instance
4. **Analyze git diff** between `origin/develop` and your branch
5. **Verify requirements** using Gemini AI
6. **Pass/Fail** based on whether changes satisfy requirements
7. **Block merge** if verification fails (with branch protection)

---

## Customization

### Change base branch

Default is `origin/develop`. To use `main`:

```yaml
on:
  push:
    branches:
      - 'feature/**'

jobs:
  verify:
    # ... existing config ...
    env:
      BASE_REF: 'origin/main'  # Change here
```

### Add tech stack hint

For better AI analysis, specify your tech stack:

```yaml
env:
  TECH_STACK: 'android,kotlin,jetpack-compose'
```

### Customize trigger branches

```yaml
on:
  push:
    branches:
      - 'feature/**'
      - 'fix/**'
      - 'enhancement/**'
```

---

## Troubleshooting

### Workflow doesn't trigger

- Check branch name matches the pattern (`feature/**`, etc.)
- Verify workflow file is in `.github/workflows/`
- Check GitHub Actions is enabled in repo settings

### "Could not extract Jira ticket"

- Branch name must contain Jira ticket ID (e.g., `PROJ-123`)
- Format: `feature/PROJ-123-description` or `PROJ-123-description`

### "Jira ticket not found"

- Verify Jira secrets are correct
- Check ticket exists in your Jira instance
- Ensure `JIRA_USERNAME` has access to the ticket

### "GOOGLE_API_KEY not configured"

- Get your API key from https://aistudio.google.com/app/apikey
- Add it to GitHub repository secrets as `GOOGLE_API_KEY`
- Ensure the key starts with `AIza`

---

## Branch Protection (Optional)

To **block merges** when verification fails:

1. Go to repo **Settings** → **Branches**
2. Add rule for `develop` or `main`
3. Enable **Require status checks to pass**
4. Select **Prod Requirement Verification Gate**
5. Save

Now PRs cannot be merged until verification passes ✅

---

## Example: maine-workflow-app Integration

```bash
# 1. Navigate to maine-workflow-app
cd /path/to/maine-workflow-app

# 2. Copy workflow
mkdir -p .github/workflows
cp /path/to/agentic-ai-conductor/.github/workflows/prod_req_verification.yml \
   .github/workflows/prod_req_verification.yml

# 3. Commit
git add .github/workflows/
git commit -m "Add prod requirement verification"
git push origin main

# 4. Test with a feature branch
git checkout -b feature/WFPLUS-576-add-new-field
# Make changes...
git add .
git commit -m "Add new field to form"
git push origin feature/WFPLUS-576-add-new-field

# Workflow runs automatically and verifies changes!
```

---

## Next Steps

- Fill in `.conductor/` context files in the conductor repo
- Set up branch protection rules
- Train team on branch naming conventions
- Monitor workflow runs in GitHub Actions tab
