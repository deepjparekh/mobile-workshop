# mobile-workshop

`mobile-workshop` is an open-source orchestrator for multi-agent mobile engineering. It is designed to coordinate specialized agents across Kotlin Multiplatform, Jetpack Compose, SwiftUI, backend integration, validation, and release workflows through a tool-agnostic core that can be packaged for multiple AI coding hosts.

## What It Is

The repository provides a universal mobile engineering control plane:

- a shared core of agent definitions
- a shell-based orchestrator for execution and coordination
- host adapters for Claude Code, Codex, Gemini CLI, and OpenCode
- generated distribution bundles for each supported host

The goal is to let a user install a native plugin or extension for their preferred tool and use natural language to drive app development, features, refactors, migrations, and validation workflows with a coordinated mobile-focused agent squad.

## Goals

- Keep the core architecture tool-agnostic
- Support both shared and platform-specific mobile work
- Optimize agent selection and token usage by activating only the needed roles
- Run independent work in parallel where the host and task graph allow it
- Preserve durable execution state, validation results, and self-heal flows
- Package the same core for multiple AI host environments

## Core Principles

- Tool-agnostic core: the core behavior and agent contracts should not depend on a single vendor or host
- Host-native adapters: planning, messaging, and agent execution should use host-native capabilities when available
- Efficient agent selection: simple requests should avoid unnecessary agent activation
- Parallel execution: Android, iOS, KMP, and backend work should run concurrently when dependencies allow
- Self-healing validation loops: failed builds should be summarized, routed back to the responsible agent, and retried intentionally
- Durable state: execution history, messages, locks, and validation artifacts should remain inspectable inside the target repo

## Proposed Architecture

`mobile-workshop` uses a universal-core monorepo pattern.

- `src/core/agents/` contains strict Markdown agent definitions
- `src/core/orchestrator/` contains the shell orchestrator and shared shell libraries
- `src/helper/kotlin/` contains the Kotlin/JVM helper for structured data, state, locking, error extraction, and packaging
- `build/templates/` contains host-specific adapter and manifest templates
- `dist/` contains generated host bundles

The runtime is shell-first, but not shell-only. The orchestrator remains a Bash entrypoint, while Kotlin handles the structured logic that would otherwise be brittle in shell.

Host resolution is explicit-first: `workshop.sh` prefers `--host`, then a `MOBILE_WORKSHOP_HOST` value injected by the packaged host launcher, and only then adapter-specific detection probes. Ambiguous detection fails closed instead of guessing.

## Supported v1 Hosts

- Claude Code
- Codex
- Gemini CLI
- OpenCode

## v1 Scope

- Existing mobile repositories only
- Full support on macOS first
- Explicit command entrypoints such as `/workshop` or `workshop`
- Durable file-journal state in the target repository
- Native planning and messaging only as host-provided optimizations

## Quality Tooling

- `./scripts/run-quality.sh` runs the full local quality suite that mirrors the PR check.
- `./gradlew quality` runs Gradle-backed checks for Kotlin and Gradle files.
- `npm run format:text:check` and `npm run lint:markdown` cover docs, JSON, and YAML files.
- `./scripts/install-hooks.sh` configures the repo-managed pre-commit hook that auto-formats staged files before commit.

## Gemini PR Review

- `.github/workflows/gemini-review.yml` runs Gemini code review on pull requests when they are opened, reopened, marked ready for review, or updated with new commits.
- The workflow fails closed if Gemini authentication is missing or ambiguous, or if `GEMINI_CLI_VERSION` is not pinned as a repository variable.
- Fork pull requests are skipped instead of running the review with elevated credentials.
- The workflow intentionally avoids third-party Gemini review extensions and relies on the base Gemini CLI action plus a constrained GitHub MCP tool set.

Required repository configuration:

- Set exactly one Gemini auth method:
  - `GEMINI_API_KEY` secret, or
  - `GOOGLE_API_KEY` secret with `GOOGLE_GENAI_USE_VERTEXAI=true`, or
  - `GCP_WIF_PROVIDER`, `GOOGLE_CLOUD_PROJECT`, `GOOGLE_CLOUD_LOCATION`, and `SERVICE_ACCOUNT_EMAIL` variables with exactly one of `GOOGLE_GENAI_USE_VERTEXAI=true` or `GOOGLE_GENAI_USE_GCA=true`
- Set `GEMINI_CLI_VERSION` as a repository variable to a specific Gemini CLI release instead of relying on `latest`

Optional repository configuration:

- Set `GEMINI_MODEL` to override the default review model
- Set `APP_ID` and `APP_PRIVATE_KEY` together if you want review comments posted through a GitHub App identity instead of the default workflow token

Local prerequisites:

- Java 17+
- Node.js 20+ with `npm ci --ignore-scripts`
- `shellcheck`
- `shfmt`

Node tooling safeguards:

- The repo pins exact formatter and linter versions in `package.json`.
- `package-lock.json` is committed and CI installs with `npm ci --ignore-scripts`.
- `.npmrc` disables package lifecycle scripts, audit prompts, and funding prompts for this repo.
- The staged-file formatter uses `npx --no-install` so hooks never download ad hoc packages.

## Planned Repository Layout

```text
mobile-workshop/
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── build/
│   └── templates/
├── config/
│   └── detekt/
├── docs/
│   └── implementation-plan.md
├── gradle/
│   └── wrapper/
├── examples/
│   └── mobile-workshop.config.json
├── scripts/
└── src/
    ├── core/
    │   ├── agents/
    │   └── orchestrator/
    └── helper/
        └── kotlin/
```

## Roadmap

1. Establish the architectural contract and implementation plan
2. Scaffold the Gradle build, Kotlin helper, and orchestrator shell runtime
3. Define strict agent contracts and host adapter contracts
4. Implement routing, messaging, validation, and self-heal flows
5. Generate host-specific distributions
6. Add smoke tests and release artifacts

## Status

The repository is currently in the planning and scaffolding stage. The first milestone is to establish the docs, contracts, and repository skeleton before runtime implementation begins.

## Plan

The authoritative implementation plan lives in [docs/implementation-plan.md](docs/implementation-plan.md).
