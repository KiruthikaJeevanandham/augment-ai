#!/bin/bash

# This script implements the Track Creation Protocol.
# It takes a skill name and a Jira ticket ID as arguments and sets up the track directory.

set -e

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <SKILL_NAME> <JIRA_TICKET_ID>"
  echo "Example: $0 pr_creator WFPLUS-576"
  exit 1
fi

# Determine the project's root directory (where this script is located)
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SKILL_NAME=$1
JIRA_TICKET_ID=$2
# Create a unique track ID with a timestamp
TRACK_ID=$(date +%Y%m%d%H%M%S)-$(echo "$JIRA_TICKET_ID" | tr '[:upper:]' '[:lower:]')
TRACK_PATH="$PROJECT_DIR/conductor/tracks/$TRACK_ID"

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
