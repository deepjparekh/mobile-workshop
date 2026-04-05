# Agent Contract

Each agent definition in `src/core/agents/` is a strict Markdown document with fixed front matter:

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

V1 agent documents are intentionally concise. They exist to lock the contract shape before runtime behavior is implemented.
