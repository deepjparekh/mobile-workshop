#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"

if ! command -v shfmt >/dev/null 2>&1; then
  echo "shfmt is required to verify shell formatting. Install it locally and retry." >&2
  exit 1
fi

shell_files=()

while IFS= read -r -d '' file; do
  shell_files+=("${file}")
done < <(find src/core/orchestrator scripts .githooks -type f \( -name '*.sh' -o -name 'pre-commit' \) -print0)

if [ "${#shell_files[@]}" -eq 0 ]; then
  exit 0
fi

shfmt -d "${shell_files[@]}"
