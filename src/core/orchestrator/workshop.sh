#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=src/core/orchestrator/lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

workshop::main() {
  local command="${1:-help}"
  shift || true

  local host=""
  local repo=""
  local plan=""
  local goal=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
    --host)
      host="$2"
      shift 2
      ;;
    --repo)
      repo="$2"
      shift 2
      ;;
    --plan)
      plan="$2"
      shift 2
      ;;
    --goal)
      goal="$2"
      shift 2
      ;;
    *)
      shift
      ;;
    esac
  done

  case "${command}" in
  run)
    workshop::resolve_host "${host}" || exit 1
    workshop::log "Using host: ${MOBILE_WORKSHOP_HOST}"

    if [[ -z "${repo}" ]]; then
      workshop::err "Missing --repo <path>"
      exit 1
    fi

    workshop::log "Discovering workspace for ${repo}..."
    # We use our new helper!
    workshop::call_helper discover-workspace --repo "${repo}"
    ;;
  doctor)
    workshop::log "Running doctor for ${repo:-.}"
    workshop::call_helper filesystem-safety-probe --repo "${repo:-.}"
    ;;
  *)
    workshop::usage
    ;;
  esac
}

workshop::main "$@"
