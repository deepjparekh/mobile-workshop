#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=src/core/orchestrator/lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

workshop::main() {
  local command="${1:-help}"

  case "${command}" in
    run|doctor|resume)
      workshop::log "mobile-workshop orchestrator scaffold"
      workshop::log "command: ${command}"
      workshop::log "implementation pending"
      ;;
    *)
      workshop::usage
      ;;
  esac
}

workshop::main "$@"
