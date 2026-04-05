# Config Reference

`mobile-workshop` uses `.mobile-workshop/config.json` as the authoritative workspace map when present.

The initial config surface is expected to cover:

- workspace modules and their kinds
- ownership hints for agent routing
- validation commands
- retry limits
- host-specific overrides when required

The example in [examples/mobile-workshop.config.json](../examples/mobile-workshop.config.json) shows the intended shape. The full schema will be finalized during Phase 1.
