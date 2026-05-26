# Google API Key Setup

This guide shows how to set up **Google API Key** authentication for the Agentic AI Conductor.

## Why Google API Key?

✅ **Simplest setup** - Just one API key, no GCP project needed  
✅ **No DevOps approval** - Developers can create their own keys  
✅ **Works immediately** - No infrastructure setup required  
✅ **Same pricing** - Identical cost to Vertex AI  
⚠️ **Rate limits** - 60 requests/minute on free tier (sufficient for most use cases)  

---

## Step 1: Get a Google API Key

### Option A: Google AI Studio (Recommended)

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Click **"Get API Key"**
3. Click **"Create API key in new project"** (or select existing project)
4. Copy the API key (starts with `AIza...`)

### Option B: Google Cloud Console

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or select existing)
3. Enable **Generative Language API**
4. Go to **APIs & Services** → **Credentials**
5. Click **"Create Credentials"** → **"API Key"**
6. Copy the API key

---

## Step 2: Add API Key to GitHub Secrets

### For GitHub Actions (CI/CD)

1. Go to your repository → **Settings** → **Secrets and variables** → **Actions**
2. Click **"New repository secret"**
3. Name: `GOOGLE_API_KEY`
4. Value: Your API key (e.g., `AIzaSyD...`)
5. Click **"Add secret"**

### For Local Development

Add to your `.env` file:

```bash
# Google API Key (instead of GCP service account)
GOOGLE_API_KEY="AIzaSyD..."

# Other required secrets
JIRA_BASE_URL="https://your-domain.atlassian.net"
JIRA_USERNAME="your-jira-email"
JIRA_API_TOKEN="your-jira-api-token"
```

---

## Step 3: Verify Setup

### Test Locally

```bash
# Set the API key
export GOOGLE_API_KEY="AIzaSyD..."

# Run verification
./scripts/run_skill.sh prod_req_verification WFPLUS-576
```

You should see:
```
Using Google API Key authentication for Gemini
```

### Test in GitHub Actions

Push to a feature branch and check the workflow logs:
```
Using Google API Key authentication (no GCP setup required)
```

---

---

## Rate Limits & Quotas

### Free Tier (API Key)
- **60 requests per minute**
- **1,500 requests per day**
- Sufficient for ~20 PRs/day

### Paid Tier
- **1,000 requests per minute**
- **Unlimited daily requests**
- Pay-as-you-go pricing

### If You Hit Limits
Upgrade to Vertex AI with service account for higher quotas.

---

## Security Best Practices

### ✅ Do
- Store API key in GitHub Secrets (never commit to code)
- Rotate API key every 90 days
- Use separate keys for dev/prod
- Monitor usage in Google Cloud Console

### ❌ Don't
- Commit API key to git
- Share API key in Slack/email
- Use same key across multiple projects
- Expose key in logs or error messages

---

## Troubleshooting

### Error: "API key not valid"

**Cause**: Invalid or expired API key

**Fix**:
1. Verify key is correct (starts with `AIza`)
2. Check key hasn't been deleted in Google Cloud Console
3. Ensure Generative Language API is enabled

### Error: "Quota exceeded"

**Cause**: Hit rate limits (60 requests/minute on free tier)

**Fix**:
1. Wait 1 minute and retry
2. Upgrade to paid tier
3. Switch to Vertex AI with service account

### Error: "Model not found"

**Cause**: Using Vertex AI model name with API Key

**Fix**: Use Gemini API model names:
- ✅ `gemini-pro`
- ✅ `gemini-1.5-pro`
- ✅ `gemini-1.5-flash`
- ❌ `gemini-2.5-pro` (Vertex AI only)

---

---

## Cost Estimate

### API Key Pricing (Gemini 1.5 Pro)
- **Input**: $0.00125 per 1K tokens
- **Output**: $0.005 per 1K tokens

### Example: 20 PRs/week
- 80 verifications/month
- ~5K input + 2K output tokens per verification
- **Cost**: ~$40/month

**Same cost as Vertex AI** - pricing is identical.

---

## Quick Start Summary

1. **Get API key** from [Google AI Studio](https://aistudio.google.com/app/apikey)
2. **Add to GitHub secrets**: `GOOGLE_API_KEY`
3. **Add to local `.env`**: `GOOGLE_API_KEY="AIzaSyD..."`
4. **Push to feature branch** - workflow runs automatically
5. **Done!** No GCP setup, no DevOps approval needed

---

## Support

**API Key Issues**: Check [Google AI Studio](https://aistudio.google.com/)  
**Rate Limits**: Monitor in [Google Cloud Console](https://console.cloud.google.com/)  
**Pricing**: See [Gemini API Pricing](https://ai.google.dev/pricing)

---

## Next Steps

1. Get your API key from Google AI Studio
2. Add to GitHub secrets and local `.env`
3. Test with a feature branch
4. Monitor usage and costs in Google Cloud Console
5. Upgrade to paid tier if you hit rate limits
