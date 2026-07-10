#!/usr/bin/env bash
# Usage: ./setup-story-dir.sh <story_number>
# Ensures docs/<story_number>/ exists and reports any partial-run artifacts.
# Exits 0 if the directory is clean/new.
# Exits 2 if existing files were found — caller must prompt the user before continuing.

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <story_number>" >&2
  exit 1
fi

STORY_NUMBER="$1"
STORY_DIR="docs/${STORY_NUMBER}"

mkdir -p "$STORY_DIR"

existing=$(find "$STORY_DIR" -maxdepth 1 -name "*.md" | sort)

if [[ -n "$existing" ]]; then
  echo "PARTIAL_RUN"
  echo "$existing"
  exit 2
fi

echo "CLEAN"
exit 0
