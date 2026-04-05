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
  local _plan=""
  local _goal=""

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
      _plan="$2"
      shift 2
      ;;
    --goal)
      _goal="$2"
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

    # Check for existing config
    if [[ ! -f "${repo}/.mobile-workshop/config.json" ]]; then
      workshop::log "No confirmed workspace map found. Starting discovery..."
      local detection
      detection=$(workshop::call_helper discover-workspace --repo "${repo}")

      workshop::log "Proposed Workspace Map:"
      echo "${detection}"

      # In V1, we require manual confirmation or a flag.
      # For this workshop, let's assume if it's not interactive we fail closed as per plan.
      if [[ -t 0 ]]; then
        read -p "[workshop] Confirm this map? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
          workshop::call_helper confirm-workspace --repo "${repo}" --detection-json "${detection}"
        else
          workshop::err "Workspace map rejected. Please correct .mobile-workshop/config.json manually."
          exit 1
        fi
      else
        workshop::err "Non-interactive environment detected. Run with a confirmed .mobile-workshop/config.json"
        exit 1
      fi
    fi

    workshop::log "Workspace confirmed. Proceeding with run..."
    # Next: Execute plan (Phase 3)
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
