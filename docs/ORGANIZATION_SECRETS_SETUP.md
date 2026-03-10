# Organization-Level Secrets Setup

This guide shows how to set up **organization-level secrets** in GitHub so you don't have to configure secrets in every repository.

## Why Organization-Level Secrets?

✅ **Set once, use everywhere** - Configure secrets at the organization level  
✅ **No duplication** - All repos in the organization can access the same secrets  
✅ **Centralized management** - Update secrets in one place  
✅ **Easy onboarding** - New repos automatically have access  

---

## Prerequisites

You need **Owner** or **Admin** permissions in the GitHub organization (`PeopleNet`).

---

## Setup Steps

### Step 1: Navigate to Organization Settings

1. Go to https://github.com/PeopleNet
2. Click **Settings** (top right)
3. In the left sidebar, click **Secrets and variables** → **Actions**

### Step 2: Add Organization Secrets

Click **New organization secret** and add each of these:

#### 1. GOOGLE_API_KEY

- **Name**: `GOOGLE_API_KEY`
- **Value**: Your Google API Key (get from https://aistudio.google.com/app/apikey)
- **Repository access**: Select **All repositories** or **Selected repositories**

#### 2. JIRA_BASE_URL

- **Name**: `JIRA_BASE_URL`
- **Value**: `https://your-domain.atlassian.net`
- **Repository access**: Select **All repositories**

#### 3. JIRA_USERNAME

- **Name**: `JIRA_USERNAME`
- **Value**: Your Jira email (e.g., `user@peoplenet.com`)
- **Repository access**: Select **All repositories**

#### 4. JIRA_API_TOKEN

- **Name**: `JIRA_API_TOKEN`
- **Value**: Your Jira API token
  - Get from: Jira → Profile → Security → API tokens → Create API token
- **Repository access**: Select **All repositories**

#### 5. PAT_TOKEN (Optional - only if conductor repo is private)

- **Name**: `PAT_TOKEN`
- **Value**: GitHub Personal Access Token with `repo` scope
  - Get from: GitHub → Settings → Developer settings → Personal access tokens → Generate new token (classic)
  - Select `repo` scope
- **Repository access**: Select **All repositories**

---

## Repository Access Options

When adding each secret, you'll see:

### Option 1: All repositories (Recommended)
- ✅ All current and future repos can access the secret
- ✅ No manual configuration needed for new repos
- ✅ Easiest to manage

### Option 2: Selected repositories
- Choose specific repos that can access the secret
- More granular control
- Need to manually add new repos

**Recommendation**: Use **All repositories** for simplicity.

---

## Verify Secrets Are Set

After adding all secrets, you should see:

```
Organization secrets (5)
├── GOOGLE_API_KEY        (All repositories)
├── JIRA_BASE_URL         (All repositories)
├── JIRA_USERNAME         (All repositories)
├── JIRA_API_TOKEN        (All repositories)
└── PAT_TOKEN             (All repositories)
```

---

## Using Organization Secrets in Workflows

Once organization secrets are set, **any repository** in the organization can use them:

```yaml
steps:
  - name: Use Organization Secret
    run: |
      echo "Using JIRA_BASE_URL: ${{ secrets.JIRA_BASE_URL }}"
    env:
      GOOGLE_API_KEY: ${{ secrets.GOOGLE_API_KEY }}
```

**No additional configuration needed!** The secrets are automatically available.

---

## Adding Verification to Any Project

Once organization secrets are set up, adding verification to any project is **super simple**:

### 1. Copy the workflow file

```bash
# In your project repo (e.g., maine-form-workflow-app)
mkdir -p .github/workflows
cp /path/to/agentic-ai-conductor/templates/workflows/verify-on-commit.yml \
   .github/workflows/verify-on-commit.yml
```

### 2. Commit and push

```bash
git add .github/workflows/verify-on-commit.yml
git commit -m "feat: add automated requirement verification"
git push
```

### 3. Done! ✅

That's it! The workflow will automatically:
- Trigger on every commit to feature/bugfix/chore/hotfix branches
- Extract Jira ticket from branch name
- Verify code against requirements
- Upload verification report

**No secrets to configure** - it uses the organization-level secrets automatically!

---

## Testing

### Test in any repo:

```bash
git checkout -b feature/WFPLUS-123-test-verification
git push -u origin feature/WFPLUS-123-test-verification
```

Go to Actions tab → See the verification running! 🚀

---

## Updating Secrets

To update a secret (e.g., rotate API key):

1. Go to Organization Settings → Secrets and variables → Actions
2. Click on the secret name
3. Click **Update secret**
4. Enter new value
5. Click **Update secret**

**All repos using that secret are automatically updated!** No need to update each repo individually.

---

## Security Best Practices

### ✅ Do:
- Use organization secrets for shared credentials
- Rotate API keys regularly
- Use least-privilege access (e.g., read-only Jira API token)
- Review secret access logs periodically

### ❌ Don't:
- Commit secrets to code
- Share secrets in chat/email
- Use personal API keys for organization workflows
- Give secrets broader access than needed

---

## Troubleshooting

### "Secret not found" error

**Cause**: Secret not accessible to the repository

**Fix**:
1. Go to Organization Settings → Secrets → Click the secret
2. Check **Repository access** is set to **All repositories** or includes your repo
3. Save changes

### "Permission denied" error

**Cause**: You don't have permission to add organization secrets

**Fix**: Ask an organization Owner or Admin to add the secrets

### Workflow can't access organization secret

**Cause**: Workflow is triggered from a fork

**Fix**: Organization secrets are not available to forked repos for security reasons. This is expected behavior.

---

## Summary

**One-time setup:**
1. Add 5 secrets at organization level (5 minutes)

**Per-project setup:**
1. Copy one workflow file (30 seconds)
2. Commit and push (30 seconds)

**Total time per new project: 1 minute!** 🎉

---

## Next Steps

1. ✅ Set up organization secrets (this guide)
2. ✅ Copy `verify-on-commit.yml` to your project
3. ✅ Push to a feature branch
4. ✅ Watch verification run automatically!
