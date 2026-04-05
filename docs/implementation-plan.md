# mobile-workshop v1 implementation plan

## Summary

`mobile-workshop` will be built as a universal-core monorepo with the source of truth in `src/core/` and generated host bundles in `dist/`. Version 1 focuses on orchestrating work inside existing mobile repositories through a shell-first orchestrator, strict agent contracts, host-native adapters, and a Kotlin/JVM helper for structured runtime operations.

## Scope and Non-Goals

### In Scope

- Multi-agent orchestration for existing mobile repositories
- Support for Kotlin Multiplatform, Android UI, iOS UI, backend integration, validation, and supporting roles
- Distribution bundles for Claude Code, Codex, Gemini CLI, and OpenCode
- Parallel execution for independent work lanes
- Durable execution state and self-heal validation loops

### Out of Scope for v1

- New-project or template bootstrapping
- Full Windows support
- Continuous background daemons or OS-level file watching
- External LLM API integrations managed directly from the shell runtime
- Queued concurrent runs in a single target repository

## Architecture

The architecture is split into four layers:

1. Core agent definitions in `src/core/agents/`
2. Shell orchestrator runtime in `src/core/orchestrator/`
3. Kotlin/JVM helper for structured operations in `src/helper/kotlin/`
4. Host adapters and generated distributions for each supported AI coding host

The orchestrator is shell-first because process control, command execution, and build validation are naturally handled in shell. Structured tasks such as JSON parsing, state aggregation, lock management, and log extraction are delegated to Kotlin because they are brittle in shell and should share the same language ecosystem familiar to mobile engineers. This is a V1 bias, not a permanent architectural constraint.

## Repository Structure

```text
mobile-workshop/
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── build/
│   └── templates/
├── docs/
│   ├── agent-contract.md
│   ├── config-reference.md
│   ├── host-capability-matrix.md
│   └── implementation-plan.md
├── examples/
│   └── mobile-workshop.config.json
└── src/
    ├── core/
    │   ├── agents/
    │   │   ├── pm.md
    │   │   ├── designer.md
    │   │   ├── architect.md
    │   │   ├── kmp_engineer.md
    │   │   ├── backend_engineer.md
    │   │   ├── android_ui.md
    │   │   ├── ios_ui.md
    │   │   ├── security_qa.md
    │   │   ├── sdet.md
    │   │   ├── devops.md
    │   │   └── ux_writer.md
    │   └── orchestrator/
    │       ├── workshop.sh
    │       └── lib/
    └── helper/
        └── kotlin/
```

The directory name is standardized as `src/core/orchestrator/`.

## Agent Contract

Each agent definition is a strict Markdown document with fixed front matter:

- `id`
- `role`
- `activation_rules`
- `inputs`
- `outputs`
- `handoffs`
- `self_heal_scope`
- `done_criteria`

Each agent body uses the same sections:

- `Mission`
- `When To Use`
- `Execution Rules`
- `Collaboration Rules`
- `Validation Rules`

The initial agent set is:

- `pm`
- `designer`
- `architect`
- `kmp_engineer`
- `backend_engineer`
- `android_ui`
- `ios_ui`
- `security_qa`
- `sdet`
- `devops`
- `ux_writer`

## Host Adapter Contract

Host-specific integration is owned by adapters, not by the shell runtime. Each host adapter must expose a stable interface for:

- host identity probes
- capability detection
- planner invocation
- agent invocation
- agent spawning when supported
- native agent messaging when supported
- usage metric collection when supported

Each adapter reports:

- host detection confidence and evidence
- native parallel agent support
- native agent messaging support
- structured planner invocation support
- interactive confirmation support
- usage metadata availability

Planning is always host-local. If a host supports planner or subagent invocation, the adapter uses that mechanism. If it does not, the adapter falls back to deterministic routing and requires a clearer user request or a precomputed execution plan.

## Host Resolution

`workshop.sh` resolves host identity before loading adapter capabilities. Resolution precedence is:

1. explicit `--host <claude|codex|gemini|opencode>`
2. `MOBILE_WORKSHOP_HOST` injected by the packaged host launcher or wrapper
3. adapter-owned detection probes over host-provided environment markers and runtime hints

Direct CLI use should prefer `--host`. Generated distributions should pass the host identity explicitly so normal plugin usage does not rely on probing.

Adapter probes must be conservative. They may inspect host-specific environment markers, invocation metadata, or launcher hints exposed by the active host, but they must not infer host identity from repository contents.

If exactly one adapter reports a high-confidence match, `workshop.sh` binds to that adapter and then loads capabilities. If zero adapters match, or more than one adapter matches, execution fails closed and instructs the user to rerun with `--host`.

## Execution Model

`workshop.sh` exposes these commands:

- `run [--host <claude|codex|gemini|opencode>] --repo <path> --plan <execution-graph.json>`
- `run [--host <...>] --repo <path> --goal "<request>"`
- `doctor --repo <path> [--repair]`
- `resume --repo <path>`

The canonical execution unit is an `execution-graph.json` file. `--goal` is a convenience input that must be translated into an execution graph before the main runtime executes work.

Execution flow:

1. Resolve host identity
2. Load host adapter capabilities
3. Load `.mobile-workshop/config.json` if present
4. Run read-only workspace discovery when config is absent or incomplete
5. Emit a workspace report with detected modules, capabilities, confidence, and evidence
6. Require user confirmation for any write-capable run that does not have a confirmed workspace map
7. Build or load an execution graph from config or the confirmed workspace map
8. Acquire the run lock
9. Initialize run state
10. Execute the selected agent lanes
11. Run validation gates
12. Offer targeted self-heal retries on failures
13. Aggregate metrics and archive the run summary

Deterministic routing is the default for simple requests. Ambiguous, cross-layer, cross-platform, or high-risk requests escalate to host-local planning with `pm` and `architect`.

Activation defaults:

- `designer` and `ux_writer` only for UX or copy changes
- `kmp_engineer` for shared or domain logic
- `backend_engineer` for API and integration work
- `android_ui` and `ios_ui` for platform UI work
- `security_qa` for risky or security-sensitive work
- `sdet` for behavior changes
- `devops` for CI, build, release, and tooling work

## Workspace Discovery and Confirmation

`.mobile-workshop/config.json` is the authoritative workspace map when present. Autodetection is a bootstrap aid only and must remain read-only.

The discovery step writes `.context/state/workspace-detection.json` with detected modules, ownership hints, and confidence signals. Each detected module record includes:

- `path`
- `kind`
- `confidence`
- `evidence`
- `build_tools`
- `owned_by`

The allowed `kind` values are:

- `kmp_shared`
- `android_app`
- `android_library`
- `ios_app`
- `backend_service`
- `unknown`
- `ambiguous`

Any module that shows both Android and Kotlin Multiplatform signals must be classified as `ambiguous` or `kmp_shared`; it must never be auto-routed as Android-only without confirmation.

If config is absent, the first write-capable `run` must show the proposed workspace map and ask the user to confirm or correct it before execution. A confirmed map may then be persisted into `.mobile-workshop/config.json` for subsequent runs.

If the active host cannot support interactive confirmation, `run` must stop after emitting the proposed workspace map and instructions for persisting corrections into config. V1 must not silently continue on unconfirmed detection.

Read-only commands such as `doctor` may use provisional discovery, but they must label the result as unconfirmed. Write-capable execution fails closed when discovery is low-confidence, materially incomplete, or awaiting confirmation.

If a previously confirmed repo changes materially, such as new modules, moved module roots, or build-graph changes, the confirmation is invalidated and must be re-established before the next write-capable run.

No execution graph may target module roots, rename modules, or edit build configuration outside the confirmed workspace map.

## Messaging Model

The file journal in `.context/messages/` is always the durable baseline. Host-native messaging is an optional optimization layer.

Each message envelope uses the same canonical fields:

- `from`
- `to`
- `run_id`
- `phase`
- `subject`
- `summary`
- `artifacts`
- `requires_response`

When native host messaging is available, the adapter may forward the same message through the host’s native mechanism. The file journal remains the durable source for resume, debugging, and auditability.

During concurrent phases, the orchestrator uses cooperative polling at explicit intervals for progress collection and message pickup only. V1 does not use OS-level watchers or background daemons, and polling is never the authority for lock ownership.

## State and Locking Model

Runtime state lives inside the target repository:

- `.context/state/lock/lease.json`
- `.context/state/run.json`
- `.context/state/workspace-detection.json`
- `.context/state/agents/<agent>.json`
- `.context/messages/to_<agent>/YYYYMMDDTHHMMSSZ-from-<agent>.md`
- `.context/errors/<phase>-<agent>.raw.log`
- `.context/errors/<phase>-<agent>.excerpt.log`
- `.context/errors/<phase>-<agent>.summary.md`
- `.context/archive/<run-id>/summary.md`
- `.context/metrics/run-metrics.json`

V1 supports one active run per target repository. Parallel workers must never mutate shared state files directly. Each worker writes only to its own per-agent state file, and the main orchestrator process performs the final aggregation.

The repository lock is a fenced lease owned only by the main orchestrator process. The lease record contains `run_id`, `owner_id`, `owner_pid` when discoverable, `host`, `lease_epoch`, `acquired_at`, `heartbeat_at`, and `expires_at`.

Lock acquisition uses atomic directory creation for `.context/state/lock/` and atomic temp-file replacement for `lease.json`. Lease renewal is accepted only from the current owner and only when the observed `lease_epoch` still matches. Parallel workers do not participate in lock acquisition or renewal.

Stale-lock repair is conservative. A repair candidate must satisfy all of the following before takeover:

- `expires_at` is in the past
- the same expired lease is observed twice with jittered backoff
- the previous owner is confirmed dead when local PID inspection is available, or cannot refresh the same epoch during the recheck window

If ownership cannot be proven stale, V1 fails closed and asks the user to resolve the lock rather than risking split-brain execution.

Before enabling concurrent lanes, the Kotlin helper runs a filesystem safety probe against `.context/state/` using repeated create, rename, and stat round-trips. If latency or metadata visibility is outside the supported envelope, the run downgrades to serial execution for that repository. V1 prefers reduced parallelism over uncertain lock behavior on slow or heavily indexed filesystems.

All structured writes are handled through the Kotlin helper. Lease acquisition uses atomic directory creation; all other structured file updates use atomic temp-file writes followed by `mv`.

## Validation and Self-Heal

Validation defaults:

- Shared/core validation: `./gradlew build`
- Android validation: `./gradlew assembleDebug`
- iOS validation: configured `xcodebuild`

On failure:

1. Store the full raw log
2. Generate a bounded excerpt
3. Generate a structured summary
4. Ask the user whether the owning agent should attempt a self-heal
5. If approved, rerun only the owning agent

The excerpt algorithm is deterministic:

- find the earliest line matching `error|failed|exception|fatal`
- include 40 surrounding lines if found
- append the final 120 lines of the log
- if no match is found, include the first 80 and final 120 lines
- cap the result at 200 lines and 16 KB

Only the bounded excerpt and summary are sent back into the self-heal loop. Raw logs remain available for human inspection.

The default retry limit is 2 per failed lane unless overridden in config.

## Kotlin Helper Responsibilities

The Kotlin helper is packaged as a single JVM CLI artifact invoked by `workshop.sh`.

It is responsible for:

- config validation
- execution plan validation
- workspace discovery, confidence scoring, and confirmation persistence
- run initialization
- per-agent state writes
- aggregate run-state updates
- lock acquisition, lease renewal, release, stale-lock repair, and filesystem safety probing
- error excerpt generation
- error summary generation
- metrics aggregation
- distribution assembly during the build process

This keeps the shell runtime focused on orchestration, process control, and command execution.

## Build and Distribution

`build.gradle.kts` is the single build and packaging entrypoint.

Gradle is responsible for:

- compiling the Kotlin helper artifact
- copying the core shell runtime and agent definitions
- rendering host-specific templates from `build/templates/`
- injecting version metadata
- generating self-contained bundles into `dist/`

Planned distribution targets:

- `dist/claude-plugin`
- `dist/codex-plugin`
- `dist/gemini-extension`
- `dist/opencode-hook`

Each distribution must include:

- bundled agent definitions
- bundled shell orchestrator
- bundled Kotlin helper artifact
- host adapter and manifest metadata

Distributions must not reference `src/core/` at runtime.

## Delivery Phases

### Phase 1

- Scaffold Gradle and repository structure
- Define the agent contract and config contract
- Create baseline agent files

### Phase 2

- Implement Kotlin helper commands for config, workspace discovery, confirmation persistence, state, fenced leases, filesystem safety probes, metrics, and error extraction
- Implement shell libraries for process orchestration, explicit-first host resolution, and conservative adapter probing

### Phase 3a

- Implement deterministic routing and file-journal baseline execution on confirmed workspace maps

### Phase 3b

- Implement host-local planner adapters that emit canonical execution graphs

### Phase 3c

- Implement native messaging and native parallelism upgrades where hosts support them

### Phase 4

- Implement validation gates and self-heal loops

### Phase 5

- Implement Gradle-based distribution generation for all supported hosts

### Phase 6

- Add smoke tests, docs polish, and release artifacts

## Future Improvements

- If targeted self-heals, retry policy, or parallel worker aggregation become too stateful or brittle for `workshop.sh`, move that orchestration logic into the Kotlin helper and reduce the shell entrypoint to process launch, environment setup, and host handoff.
- Treat Bash as the compatibility boundary, not the long-term home for complex execution policy. The preferred migration path is incremental extraction of graph execution, retry coordination, and aggregation semantics into the JVM helper while preserving the same CLI contract.

## Test Plan

- Verify Gradle builds all distribution targets from a clean checkout
- Verify each distribution is self-contained
- Verify packaged host launchers pass explicit host identity to `workshop.sh`
- Verify deterministic routing for simple requests
- Verify planner escalation for ambiguous requests on hosts that support it
- Verify ambiguous `--goal` fails cleanly when no planner-capable host adapter exists
- Verify explicit `--host` overrides injected launcher identity and probe results
- Verify ambiguous or missing host detection fails closed with rerun instructions
- Verify repository contents are never used to infer host identity
- Verify config overrides autodetection when both are present
- Verify mixed Android and Kotlin Multiplatform modules are never auto-classified as Android-only
- Verify the first write-capable run without config requires user confirmation and can persist the corrected workspace map
- Verify material repo-structure changes invalidate prior confirmation before execution resumes
- Verify native messaging and file-journal fallback map to the same contract
- Verify concurrent Android and iOS lanes do not corrupt state
- Verify lease takeover requires expiry plus recheck and never occurs on a live epoch
- Verify crash recovery clears only provably stale locks and preserves the last durable lease record
- Verify high-latency or aggressively indexed filesystems downgrade concurrent lanes to serial execution
- Verify raw-log capture, excerpt generation, and bounded self-heal retries
- Verify metrics include only real host usage data when the host exposes it

## Assumptions

- `src/core/orchestrator/` is the canonical shell runtime path
- macOS is the only fully supported runtime in v1
- `java 17+` is a hard prerequisite
- Native planning and native messaging are adapter-owned, not Bash-owned
- File journaling is always present for durability, resume, and debugging
