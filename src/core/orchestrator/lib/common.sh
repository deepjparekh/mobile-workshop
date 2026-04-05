#!/usr/bin/env bash

workshop::log() {
  printf '[workshop] %s\n' "$*"
}

workshop::err() {
  printf '[workshop] [ERROR] %s\n' "$*" >&2
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

workshop::resolve_host() {
  local explicit_host="${1:-}"

  # 1. explicit --host
  if [[ -n "${explicit_host}" ]]; then
    MOBILE_WORKSHOP_HOST="${explicit_host}"
    return 0
  fi

  # 2. MOBILE_WORKSHOP_HOST injected
  if [[ -n "${MOBILE_WORKSHOP_HOST:-}" ]]; then
    return 0
  fi

  # 3. conservative adapter probing
  local detected_host=""
  local matches=0

  # Gemini Probe
  if [[ "${GEMINI_CLI:-}" == "1" ]]; then
    detected_host="gemini"
    ((matches++))
  fi

  # Claude Probe (hypothetical)
  if [[ -n "${CLAUDE_CODE_VERSION:-}" ]]; then
    detected_host="claude"
    ((matches++))
  fi

  if [[ "${matches}" -eq 1 ]]; then
    MOBILE_WORKSHOP_HOST="${detected_host}"
    return 0
  elif [[ "${matches}" -gt 1 ]]; then
    workshop::err "Ambiguous host detection. Multiple adapters matched."
    workshop::err "Rerun with --host <claude|codex|gemini|opencode>"
    return 1
  else
    workshop::err "No host detected. Host resolution failed."
    workshop::err "Rerun with --host <claude|codex|gemini|opencode>"
    return 1
  fi
}

workshop::call_helper() {
  local jar_path="${SCRIPT_DIR}/../../helper/kotlin/build/libs/mobile-workshop-helper-0.1.0-SNAPSHOT-all.jar"
  if [[ ! -f "${jar_path}" ]]; then
    workshop::err "Helper jar not found at ${jar_path}. Run ./gradlew :helper:kotlin:shadowJar first."
    return 1
  fi
  java -jar "${jar_path}" "$@"
}
