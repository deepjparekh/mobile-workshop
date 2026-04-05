#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"

./gradlew --no-daemon quality
npm run format:text:check
npm run lint:markdown
"${SCRIPT_DIR}/check-shell-format.sh"
"${SCRIPT_DIR}/lint-shell.sh"
