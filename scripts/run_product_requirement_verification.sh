#!/bin/bash
# Product Requirement Verification using Gemini
# Uses Gemini models directly to verify code changes against Jira requirements

set -e

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <TARGET_REPO_PATH> [JIRA_TICKET_ID] [BASE_REF] [HEAD_REF]"
  echo ""
  echo "Examples:"
  echo "  $0 /path/to/repo PROJ-123"
  echo "  $0 /path/to/repo PROJ-123 origin/develop HEAD"
  echo "  $0 /path/to/repo PROJ-123 origin/main feature/PROJ-123-add-feature"
  echo ""
  echo "If JIRA_TICKET_ID is not provided, it will be extracted from the current branch."
  echo "If BASE_REF is not provided, it defaults to 'origin/develop'."
  echo "If HEAD_REF is not provided, it defaults to 'HEAD'."
  echo ""
  echo "Prerequisites:"
  echo "  - jq installed (brew install jq)"
  echo "  - GOOGLE_API_KEY set in .env"
  exit 1
fi

TARGET_REPO_PATH="$1"
JIRA_TICKET_ID="$2"
BASE_REF="${3:-origin/develop}"
HEAD_REF="${4:-HEAD}"

# Determine the project's root directory
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Product Requirement Verification - Superpower Plugin with Gemini"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Target Repository: $TARGET_REPO_PATH"
echo "Jira Ticket ID: ${JIRA_TICKET_ID:-<auto-detect>}"
echo "Base Ref: $BASE_REF"
echo "Head Ref: $HEAD_REF"
echo ""

# Validate target repo
if [ ! -d "$TARGET_REPO_PATH/.git" ]; then
  echo "❌ Error: Target repository is not a git repository: $TARGET_REPO_PATH"
  exit 1
fi

# Check for jq
if ! command -v jq &> /dev/null; then
  echo "❌ Error: jq not found."
  echo "   Install with: brew install jq"
  exit 1
fi

# Auto-detect Jira ticket if not provided
if [ -z "$JIRA_TICKET_ID" ]; then
  echo "[Step 1/6] Auto-detecting Jira ticket from branch..."
  BRANCH_NAME=$(cd "$TARGET_REPO_PATH" && git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
  
  if [ -z "$BRANCH_NAME" ]; then
    echo "❌ Error: Could not determine current git branch."
    exit 1
  fi
  
  JIRA_TICKET_ID=$(echo "$BRANCH_NAME" | grep -oE '[A-Z]+-[0-9]+' | head -n 1)
  
  if [ -z "$JIRA_TICKET_ID" ]; then
    echo "❌ Error: Could not extract Jira ticket ID from branch name '$BRANCH_NAME'."
    echo "   Branch name should contain a Jira ticket ID (e.g., PROJ-123)."
    echo "   Alternatively, provide JIRA_TICKET_ID as the second argument."
    exit 1
  fi
  
  echo "✅ Extracted Jira ticket ID: $JIRA_TICKET_ID from branch: $BRANCH_NAME"
else
  echo "[Step 1/6] Using provided Jira ticket ID: $JIRA_TICKET_ID"
fi

# Load environment variables
echo "[Step 2/6] Loading environment variables..."
if [ -f "$PROJECT_DIR/.env" ]; then
    set -a
    source "$PROJECT_DIR/.env"
    set +a
    echo "✅ Environment variables loaded from $PROJECT_DIR/.env"
else
    echo "⚠️  Warning: .env file not found at $PROJECT_DIR/.env"
    echo "   Make sure JIRA credentials are set in your environment."
fi

# Fetch Jira ticket details
echo "[Step 3/6] Fetching Jira ticket details..."
cd "$TARGET_REPO_PATH"
JIRA_RESPONSE=$(curl -s -u "$JIRA_USERNAME:$JIRA_API_TOKEN" \
  "${JIRA_BASE_URL}/rest/api/3/issue/${JIRA_TICKET_ID}?fields=summary,description")

if [ -z "$JIRA_RESPONSE" ] || echo "$JIRA_RESPONSE" | grep -q "errorMessages"; then
  echo "❌ Error: Failed to fetch Jira ticket details."
  echo "   Check your JIRA credentials in .env file."
  exit 1
fi

JIRA_SUMMARY=$(echo "$JIRA_RESPONSE" | jq -r '.fields.summary // empty')
JIRA_DESCRIPTION=$(echo "$JIRA_RESPONSE" | jq -r '.fields.description // empty')

echo "✅ Fetched Jira ticket: $JIRA_SUMMARY"
echo ""

# Auto-detect tech stack
echo "[Step 4/6] Auto-detecting tech stack..."
if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ] || [ -f "settings.gradle" ]; then
  TECH_STACK="android/java"
elif [ -f "package.json" ]; then
  if grep -q "react-native" package.json; then
    TECH_STACK="react-native"
  elif grep -q "\"react\"" package.json; then
    TECH_STACK="react"
  else
    TECH_STACK="node"
  fi
elif [ -f "requirements.txt" ] || [ -f "pyproject.toml" ] || [ -f "setup.py" ]; then
  TECH_STACK="python"
elif [ -f "go.mod" ]; then
  TECH_STACK="go"
elif [ -f "Cargo.toml" ]; then
  TECH_STACK="rust"
elif [ -f "pom.xml" ]; then
  TECH_STACK="java"
elif [ -f "Gemfile" ]; then
  TECH_STACK="ruby"
elif [ -f "composer.json" ]; then
  TECH_STACK="php"
else
  TECH_STACK="unspecified"
fi

echo "✅ Detected tech stack: $TECH_STACK"
echo ""

# Get code diff
echo "[Step 5/6] Getting code diff..."
git fetch "$BASE_REF" 2>/dev/null || true
CODE_DIFF=$(git diff "$BASE_REF" "$HEAD_REF" 2>/dev/null || git diff)

if [ -z "$CODE_DIFF" ]; then
  echo "⚠️  Warning: No code diff found between $BASE_REF and $HEAD_REF"
  echo "   Proceeding anyway..."
fi

echo "✅ Code diff collected"
echo ""

# Run verification with Gemini
echo "[Step 6/6] Running product requirement verification with Gemini..."
echo "🤖 Using Gemini 2.5 Pro"
echo ""

# Check for GOOGLE_API_KEY
if [ -z "$GOOGLE_API_KEY" ]; then
  echo "❌ Error: GOOGLE_API_KEY not set in environment."
  echo "   Add it to your .env file or export it."
  exit 1
fi

# Create verification prompt
VERIFICATION_PROMPT="You are a product requirement verification expert. Analyze the code changes against the Jira requirements.

## Context
- Tech Stack: $TECH_STACK
- Base Ref: $BASE_REF
- Head Ref: $HEAD_REF
- Jira Ticket: $JIRA_TICKET_ID

## Jira Ticket Details
**Summary**: $JIRA_SUMMARY

**Description**:
$JIRA_DESCRIPTION

## Code Changes
\`\`\`diff
$CODE_DIFF
\`\`\`

## Your Task
Verify that the code changes satisfy the Jira requirements. Categorize your findings into:

### Category 1: Straightforward Crucial Cases (MUST PASS)
These are clear violations that MUST be fixed:
- Direct database access bypassing API layers
- Special-case conditionals instead of abstractions
- Tight coupling to implementation details
- Security-relevant pattern violations
- Breaking established architectural boundaries
- Missing core functionality from requirements

### Category 2: Edge Cases (Identifying Edge Cases for Developer Verification)
These are edge cases related to product requirements that developers can verify:
- Potential edge cases in business logic
- Scenarios that may not be covered by current implementation
- Boundary conditions in the requirements
- Integration considerations

## Output Format
Generate a markdown report with:

1. **Summary** (2-3 bullets, very brief)
2. **Category 1: Straightforward Crucial Cases (MUST PASS)**
   - List all Category 1 violations with file:line evidence
   - Each: severity, brief description (1 sentence), suggested fix
   - Overall Category 1 Status: PASS ✅ or FAIL ❌
3. **Category 2: Edge Cases (Identifying Edge Cases for Developer Verification)**
   - List all Category 2 violations with file:line evidence
   - Each: severity, detailed description (2-3 sentences to help developer understand context), why it's a problem, suggested fix
   - Overall Category 2 Status: PASS ✅ or FAIL ❌
4. **New Features/Changes** (Concise list with tick marks)
   - ✅ Feature 1: Brief description
   - ✅ Feature 2: Brief description
5. **Requirements Coverage Table** (Simple format)
   - Requirement | Status
6. **Risk Assessment** (LOW/MEDIUM/HIGH)
7. **Recommendations** (2-3 bullets max)

End with a JSON block:
\`\`\`json
{
  \"overall_status\": \"PASS|FAIL\",
  \"category1_status\": \"PASS|FAIL\",
  \"category1_violations\": [...],
  \"category2_status\": \"PASS|FAIL\",
  \"category2_violations\": [...],
  \"total_violations\": 0,
  \"risk_level\": \"LOW|MEDIUM|HIGH\",
  \"notes\": \"...\"
}
\`\`\`

IMPORTANT: Category 1 MUST have zero violations for overall_status to be PASS."

# Call Gemini API
echo "$VERIFICATION_PROMPT" > /tmp/verification_prompt.txt
REPORT=$(jq -Rs '{ contents: [{ parts: [{ text: . }] }] }' /tmp/verification_prompt.txt > /tmp/prompt.json && curl -s -X POST \
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=${GOOGLE_API_KEY}" \
  -H "Content-Type: application/json" \
  -d @/tmp/prompt.json | jq -r '.candidates[0].content.parts[0].text // empty')
rm /tmp/verification_prompt.txt /tmp/prompt.json

if [ -z "$REPORT" ]; then
  echo "❌ Error: Failed to get response from Gemini API"
  exit 1
fi

# Save report
REPORT_DIR="$TARGET_REPO_PATH/.conductor/tracks"
mkdir -p "$REPORT_DIR"
TIMESTAMP=$(date +%Y%m%d%H%M%S)
REPORT_FILE="$REPORT_DIR/product_requirement_verification_${JIRA_TICKET_ID}_${TIMESTAMP}.md"
echo "$REPORT" > "$REPORT_FILE"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Product Requirement Verification Complete"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📄 Report: $REPORT_FILE"
echo ""

# Display summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 Report Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
head -50 "$REPORT_FILE"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔗 View full report: cat $REPORT_FILE"

# Extract and display Category 1 status
CATEGORY1_STATUS=$(grep -i "Category 1 Status" "$REPORT_FILE" | head -1 || echo "Not found")
if [ -n "$CATEGORY1_STATUS" ]; then
  echo ""
  echo "📊 $CATEGORY1_STATUS"
fi

# Check if Category 1 failed
if echo "$REPORT" | grep -q "Category 1 Status.*FAIL"; then
  echo ""
  echo "❌ Category 1 (Straightforward Crucial Cases) FAILED"
  echo "   These violations MUST be fixed before merging."
  exit 1
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
