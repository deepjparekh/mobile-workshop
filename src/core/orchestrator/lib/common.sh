#!/usr/bin/env bash

workshop::log() {
  printf '[workshop] %s\n' "$*"
}

workshop::usage() {
  cat <<'EOF'
Usage:
  workshop.sh run [--host <claude|codex|gemini|opencode>] --repo <path> --plan <execution-graph.json>
  workshop.sh run [--host <claude|codex|gemini|opencode>] --repo <path> --goal "<request>"
  workshop.sh doctor --repo <path> [--repair]
  workshop.sh resume --repo <path>
EOF
}
