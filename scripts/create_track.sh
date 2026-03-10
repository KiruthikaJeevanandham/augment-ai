#!/bin/bash

# This script implements the Track Creation Protocol.
# It takes a skill name and optionally a Jira ticket ID as arguments and sets up the track directory.
# If Jira ticket ID is not provided, it will be extracted from the current git branch name.

set -e

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <SKILL_NAME> [JIRA_TICKET_ID]"
  echo "Example: $0 pr_creator WFPLUS-576"
  echo "If JIRA_TICKET_ID is not provided, it will be extracted from the current branch name."
  exit 1
fi

# Determine the project's root directory (where this script is located)
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SKILL_NAME=$1
JIRA_TICKET_ID=$2

# Use GIT_REPO_PATH if set, otherwise use conductor repo
if [ -n "$GIT_REPO_PATH" ]; then
    TRACKS_BASE_DIR="$GIT_REPO_PATH/.conductor/tracks"
    echo "Creating track in target repo: $GIT_REPO_PATH"
else
    TRACKS_BASE_DIR="$PROJECT_DIR/conductor/tracks"
    echo "Creating track in conductor repo: $PROJECT_DIR"
fi

# If Jira ticket ID is not provided, extract from branch name
if [ -z "$JIRA_TICKET_ID" ]; then
  # Get current branch name from GIT_REPO_PATH or current directory
  if [ -n "$GIT_REPO_PATH" ]; then
    BRANCH_NAME=$(cd "$GIT_REPO_PATH" && git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
  else
    BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
  fi
  
  if [ -z "$BRANCH_NAME" ]; then
    echo "Error: Could not determine current git branch. Please provide JIRA_TICKET_ID explicitly." >&2
    exit 1
  fi
  
  # Extract Jira ticket from branch name (supports patterns like PROJ-123, feature/PROJ-123, etc.)
  JIRA_TICKET_ID=$(echo "$BRANCH_NAME" | grep -oE '[A-Z]+-[0-9]+' | head -n 1)
  
  if [ -z "$JIRA_TICKET_ID" ]; then
    echo "Error: Could not extract Jira ticket ID from branch name '$BRANCH_NAME'." >&2
    echo "Branch name should contain a Jira ticket ID (e.g., PROJ-123)." >&2
    echo "Alternatively, provide JIRA_TICKET_ID as the second argument." >&2
    exit 1
  fi
  
  echo "Extracted Jira ticket ID from branch: $JIRA_TICKET_ID"
fi
# Create a unique track ID with a timestamp
TRACK_ID=$(date +%Y%m%d%H%M%S)-$(echo "$JIRA_TICKET_ID" | tr '[:upper:]' '[:lower:]')
TRACK_PATH="$TRACKS_BASE_DIR/$TRACK_ID"

# 1. Initialize Track Directory
mkdir -p "$TRACK_PATH"

# 2. Create metadata.json
# In a full implementation, this would fetch the title and description from Jira.
cat > "$TRACK_PATH/metadata.json" << EOL
{
  "id": "$TRACK_ID",
  "title": "Implement feature for $JIRA_TICKET_ID",
  "type": "feature",
  "status": "pending",
  "jira_ticket": "$JIRA_TICKET_ID",
  "description": "This track will implement the feature described in ticket $JIRA_TICKET_ID."
}
EOL

# 3. Create spec.md
# This would also be populated with details fetched from Jira.
cat > "$TRACK_PATH/spec.md" << EOL
# Specification for Ticket $JIRA_TICKET_ID

**Goal**: Implement the feature described in Jira ticket $JIRA_TICKET_ID.

**Description**:
*This section would be automatically populated with the full description from the Jira ticket.*

**Acceptance Criteria**:
*This section would be populated with the acceptance criteria from the Jira ticket.*
EOL

# 4. Create plan.md from template
PLAN_TEMPLATE="$PROJECT_DIR/templates/plans/${SKILL_NAME}_plan.md"
if [ ! -f "$PLAN_TEMPLATE" ]; then
    echo "Error: Plan template not found at '$PLAN_TEMPLATE'" >&2
    exit 1
fi
sed "s/{{JIRA_TICKET_ID}}/$JIRA_TICKET_ID/g" "$PLAN_TEMPLATE" > "$TRACK_PATH/plan.md"
chmod +x "$TRACK_PATH/plan.md"

# Output only the track ID for scripting
echo $TRACK_ID
