#!/bin/bash

# This script provides a single entry point to create and execute a skill track.

set -e

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <SKILL_NAME> <JIRA_TICKET_ID>"
  echo "Example: $0 pr_creator WFPLUS-576"
  exit 1
fi

SKILL_NAME=$1
JIRA_TICKET_ID=$2

echo "--- Starting Conductor ---"

# Determine the project's root directory (where this script is located)
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# 1. Create the track and capture its unique ID
echo "[Step 1/3] Creating track for skill '$SKILL_NAME' and ticket '$JIRA_TICKET_ID'..."
TRACK_ID=$("$PROJECT_DIR/scripts/create_track.sh" "$SKILL_NAME" "$JIRA_TICKET_ID")
echo "Track created with ID: $TRACK_ID"

# 2. Load environment variables from .env file
echo "[Step 2/3] Loading environment variables from .env file..."
if [ -f "$PROJECT_DIR/.env" ]; then
    # Use 'set -a' to automatically export all variables defined in the sourced file.
    set -a
    source "$PROJECT_DIR/.env"
    set +a
    echo "Environment variables loaded from $PROJECT_DIR/.env."
else
    echo "Warning: .env file not found at $PROJECT_DIR/.env. Make sure credentials are set in your environment."
fi

# 3. Run the conductor engine on the created track
echo "[Step 3/3] Executing plan for track '$TRACK_ID'..."
cd "$PROJECT_DIR"
env ./gradlew :conductor:run --args="--track-id $TRACK_ID --working-dir $(pwd)"

echo "--- Conductor Finished ---"
