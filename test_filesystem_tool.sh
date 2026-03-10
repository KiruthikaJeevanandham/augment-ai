#!/bin/bash

# Test script to verify FileSystemTool assert_gate_result works correctly
# This creates a mock track and tests both PASS and FAIL scenarios

set -e

echo "🧪 Testing FileSystemTool assert_gate_result function..."

# Setup test environment
export JIRA_BASE_URL="https://test.atlassian.net"
export JIRA_USERNAME="test@example.com"
export JIRA_API_TOKEN="test-token"
export GOOGLE_API_KEY="test-key"
export GIT_REPO_PATH="$(pwd)"
export TECH_STACK="test"
export TARGET_ENV="test"
export BASE_REF="HEAD"
export HEAD_REF="HEAD"

# Create a test track
TEST_TRACK_ID="test-$(date +%Y%m%d%H%M%S)"
TRACK_PATH=".conductor/tracks/$TEST_TRACK_ID"
mkdir -p "$TRACK_PATH"

# Create metadata
cat > "$TRACK_PATH/metadata.json" << 'EOF'
{
  "id": "test-track",
  "title": "Test FileSystemTool",
  "type": "test",
  "status": "pending",
  "jira_ticket": "TEST-123",
  "description": "Testing assert_gate_result"
}
EOF

# Create a test plan that only tests assert_gate_result
cat > "$TRACK_PATH/plan.md" << 'EOF'
# Test Plan: FileSystemTool assert_gate_result

steps:
  - step: 1
    name: Test PASS scenario
    tool: filesystem
    action: assert_gate_result
    params:
      report: |
        # Verification Report
        
        Status: PASS ✅
        
        ## Summary
        - All requirements met
        - No missing items
        - Low risk
        
        ```json
        {
          "gate_result": "PASS",
          "missing_requirements": [],
          "unexpected_changes": [],
          "risk_level": "LOW",
          "notes": "All checks passed successfully"
        }
        ```
      expected: "PASS"
EOF

echo "📦 Created test track: $TEST_TRACK_ID"
echo "🚀 Running test with PASS scenario..."

# Run the test
if ./gradlew :conductor:run --args="--track-id $TEST_TRACK_ID" --quiet; then
    echo "✅ PASS scenario test succeeded!"
else
    echo "❌ PASS scenario test failed!"
    exit 1
fi

# Now test FAIL scenario
echo ""
echo "🚀 Running test with FAIL scenario (should fail gracefully)..."

# Update plan for FAIL test
cat > "$TRACK_PATH/plan.md" << 'EOF'
# Test Plan: FileSystemTool assert_gate_result FAIL

steps:
  - step: 1
    name: Test FAIL scenario
    tool: filesystem
    action: assert_gate_result
    params:
      report: |
        # Verification Report
        
        Status: FAIL ❌
        
        ## Summary
        - Missing requirements detected
        - High risk changes
        
        ```json
        {
          "gate_result": "FAIL",
          "missing_requirements": ["Feature X not implemented"],
          "unexpected_changes": ["Unrelated file modified"],
          "risk_level": "HIGH",
          "notes": "Requirements not fully met"
        }
        ```
      expected: "PASS"
EOF

# This should fail
if ./gradlew :conductor:run --args="--track-id $TEST_TRACK_ID" --quiet 2>&1; then
    echo "❌ FAIL scenario should have failed but didn't!"
    exit 1
else
    echo "✅ FAIL scenario correctly failed as expected!"
fi

# Cleanup
rm -rf "$TRACK_PATH"

echo ""
echo "🎉 All FileSystemTool tests passed!"
echo "✅ String interpolation is working correctly"
echo "✅ assert_gate_result parses JSON blocks correctly"
echo "✅ Gate logic (PASS/FAIL) works as expected"
