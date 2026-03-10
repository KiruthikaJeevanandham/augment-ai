# Quick Start: Add Verification to Any Project

This is the **fastest way** to add automated requirement verification to any project in the PeopleNet organization.

## Prerequisites

✅ Organization-level secrets already set up (one-time, done by admin)  
✅ Your project uses Jira tickets in branch names  

---

## 1-Minute Setup

### Step 1: Copy the workflow file (30 seconds)

```bash
# In your project repository
mkdir -p .github/workflows
curl -o .github/workflows/verify-on-commit.yml \
  https://raw.githubusercontent.com/PeopleNet/agentic-ai-conductor/main/templates/workflows/verify-on-commit.yml
```

Or manually copy from:
`agentic-ai-conductor/templates/workflows/verify-on-commit.yml`

### Step 2: Commit and push (30 seconds)

```bash
git add .github/workflows/verify-on-commit.yml
git commit -m "feat: add automated requirement verification"
git push
```

### Done! ✅

---

## How It Works

1. **Create a feature branch** with Jira ticket ID:
   ```bash
   git checkout -b feature/WFPLUS-576-add-validation
   ```

2. **Make changes and push**:
   ```bash
   git add .
   git commit -m "feat: add validation logic"
   git push
   ```

3. **Verification runs automatically**:
   - Extracts Jira ticket ID from branch name
   - Fetches requirements from Jira
   - Analyzes code changes
   - AI verifies code matches requirements
   - Uploads verification report

4. **Check results**:
   - Go to Actions tab in GitHub
   - See verification status (✅ PASS or ❌ FAIL)
   - Download verification report artifact

---

## Branch Naming Convention

Your branch name **must contain a Jira ticket ID** unless it is an automated dependency update:

✅ **Valid:**
- `feature/WFPLUS-576-add-field`
- `bugfix/PROJ-123-fix-crash`
- `WFPLUS-576-description`
- `chore/WFPLUS-999-update-deps`
- `renovate/all` *(special case: treated as dependency-updates even without ticket)*

❌ **Invalid:**
- `feature/add-field` (no ticket ID)
- `my-branch` (no ticket ID)

---

## Configuration

The workflow is pre-configured for:
- **Environment**: Staging
- **Tech Stack**: Android
- **Base Branch**: `origin/develop`
- **Triggers**: Push to `feature/**`, `bugfix/**`, `chore/**`, `hotfix/**`

To customize, edit `.github/workflows/verify-on-commit.yml`:

```yaml
export TECH_STACK='android'      # Change to: node, python, java, etc.
export TARGET_ENV='staging'      # Change to: prod, dev, etc.
export BASE_REF='origin/develop' # Change to: origin/main, etc.
```

---

## Viewing Results

### In GitHub Actions UI

1. Go to your repo → **Actions** tab
2. Click on the workflow run
3. Expand **Execute Verification Gate** step
4. See the verification report in logs

### Download Report Artifact

1. Scroll to bottom of workflow run
2. Click **verification-report-WFPLUS-XXX** under Artifacts
3. Download and open `verification_report.md`

---

## Optional: Block Merges on Failure

To **require verification to pass** before merging:

1. Go to repo **Settings** → **Branches**
2. Add branch protection rule for `develop` or `main`
3. Enable **Require status checks to pass before merging**
4. Search for and select **Verify Against Jira Requirements**
5. Save changes

Now PRs cannot be merged if verification fails! 🚀

---

## Troubleshooting

### "Could not extract Jira ticket ID"
- Branch name must contain Jira ticket ID (e.g., `WFPLUS-123`)

### "GOOGLE_API_KEY not configured"
- Organization secrets not set up
- Contact admin to set up organization-level secrets

### Workflow doesn't trigger
- Check branch name matches pattern (`feature/**`, `bugfix/**`, etc.)
- Ensure you're not only modifying `.md` files (they're ignored)

---

## Need Help?

- **Organization secrets not set up?** See: `docs/ORGANIZATION_SECRETS_SETUP.md`
- **Want to customize?** See: `docs/MAINE_WORKFLOW_APP_INTEGRATION.md`
- **Issues?** Check conductor repo: https://github.com/PeopleNet/agentic-ai-conductor

---

## Summary

**To add verification to any project:**

1. Copy one file: `verify-on-commit.yml`
2. Commit and push
3. Done!

**No secrets to configure** - uses organization-level secrets automatically! 🎉
