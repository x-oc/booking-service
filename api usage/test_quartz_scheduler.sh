#!/bin/bash

WORKER_SERVICE="${WORKER_SERVICE:-app-worker}"
LOOKBACK_LINES="${LOOKBACK_LINES:-600}"

print_block() {
  echo ""
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

print_block "PRECHECK"
if ! docker compose ps --status running | grep -q "$WORKER_SERVICE"; then
  echo "Service $WORKER_SERVICE is not running"
  exit 1
fi

print_block "CLEAR GUIDANCE"
echo "Quartz cron intervals are configured to short periods in application.yml."
echo "This script validates that all three jobs are firing by inspecting worker logs."

echo "Waiting 40 seconds to let scheduler fire all jobs..."
sleep 40

LOGS=$(docker compose logs "$WORKER_SERVICE" --tail "$LOOKBACK_LINES")

print_block "VERIFY LOG MARKERS"
FAILURES=0

if echo "$LOGS" | grep -q "Quartz payment timeout job started"; then
  echo "PASS: payment timeout job fired"
else
  echo "FAIL: payment timeout job marker not found"
  FAILURES=$((FAILURES + 1))
fi

if echo "$LOGS" | grep -q "Quartz stay completion job started"; then
  echo "PASS: stay completion job fired"
else
  echo "FAIL: stay completion job marker not found"
  FAILURES=$((FAILURES + 1))
fi

if echo "$LOGS" | grep -q "Quartz booking activation job started"; then
  echo "PASS: booking activation job fired"
else
  echo "FAIL: booking activation job marker not found"
  FAILURES=$((FAILURES + 1))
fi

print_block "RESULT"
if [ "$FAILURES" -eq 0 ]; then
  echo "PASS: Quartz scheduler executes periodic jobs on worker node"
  exit 0
fi

echo "FAILED checks: ${FAILURES}"
exit 1
