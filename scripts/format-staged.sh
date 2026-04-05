#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"

kotlin_changed=0
doc_files=()
shell_files=()

while IFS= read -r -d '' file; do
  case "${file}" in
    *.kt|*.kts)
      kotlin_changed=1
      ;;
    *.md|*.json|*.yaml|*.yml)
      doc_files+=("${file}")
      ;;
    *.sh|.githooks/pre-commit)
      shell_files+=("${file}")
      ;;
  esac
done < <(git diff --cached --name-only --diff-filter=ACMR -z)

if [ "${kotlin_changed}" -eq 1 ]; then
  ./gradlew --quiet spotlessApply
fi

if [ "${#doc_files[@]}" -gt 0 ]; then
  npx --no-install prettier --write "${doc_files[@]}"
fi

if [ "${#shell_files[@]}" -gt 0 ]; then
  if ! command -v shfmt >/dev/null 2>&1; then
    echo "shfmt is required to format shell files. Install it locally and retry." >&2
    exit 1
  fi

  shfmt -w "${shell_files[@]}"
fi

if [ "${kotlin_changed}" -eq 1 ] || [ "${#doc_files[@]}" -gt 0 ] || [ "${#shell_files[@]}" -gt 0 ]; then
  git add --update
fi
