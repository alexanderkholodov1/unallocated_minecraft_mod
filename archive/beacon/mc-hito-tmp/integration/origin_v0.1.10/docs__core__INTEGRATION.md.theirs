# Core integration — v0.1.7

This implements PLAN §4.2–4.3's names. Content is deliberately absent. All APIs run on the server
thread unless marked client-side. No Java or Gradle was run on the worker host; CI builds and runs
the JUnit tests and real server/client smoke checks.

- Register one `ActModule` per act with `CoreRuntime.register(module)` during mod initialization.
  The module replaces a content-free fallback. Duplicate content registrations fail clearly.
- The launcher calls `CoreRuntime.get(server).start(player)` after consent/world setup. Ordinary
  worlds remain dormant. Installing the core does not change gamerules, inventory, options or maps.
- Get `sequencer()`, `state()`, `events()` and `features()` from that per-server runtime. Use
  `advance(player)` on completion, or return true from `ActModule.complete`. Normal order is
  CONTRACT → LOBBY → THEFT → LOOP → POCKET → CLIMAX → EPILOGUE. Disabled acts are skipped.
- `forceAdvance(player, state)` implements the act's diegetic escape, synchronously. The sequencer
  itself advances afterwards, even if that callback throws. All budgets must be positive.
- `enter` restores the checkpoint on reconnect, respawn and reload. Make it idempotent; persist
  progression in SessionState, not module fields. Core never resolves/teleports to MapLayout
  coordinates itself. `checkpointId()` is reported in status for the map package to implement.
- SavedData is `unallocated:unallocated_session`, stored in the overworld. The plan's bare ID is
  retained as `SessionState.ID`; 26.3's SavedDataType requires a namespaced Identifier. CODEC is
  the actual storage codec. Schema 1 stores every plan field plus ticks/started/finished.
- `actStartTick` and `sessionStartTick` refer to a persisted active-tick clock. It advances once
  per server tick with a participant, stops for empty servers and game pause, and survives reload.
  Rejoining does not renew the budget. `t=<s>` is whole elapsed session seconds at 20 ticks/sec.
  Wall time, day/night time changes and process restarts cannot expire or reset an act's budget.
- One session per world, one participant drives callbacks (first player in server list). This is
  intentionally single-player-first; per-player sessions/multiplayer arbitration are future work.
- The plan leaves `profile` unspecified: schema 1 uses `Map<String,Double>` for telemetry features.
  Collection getters are read-only; use `addFlag`, `removeFlag`, `setCounter`, `setProfile` and
  `setTemperature` so SavedData is marked dirty. Flags/counters survive act jumps.
- At EPILOGUE completion/expiry the session becomes finished, stays saved, and never wraps. Debug
  jumps reactivate it. Ending content owns disconnecting the player and returning to title.
- `Features.load()` reads `assets/unallocated/features.json` from the JAR. Missing switches are off;
  malformed values fail startup. Optional THEFT/LOOP/POCKET and option/screamer effects ship off.
  Fallback budgets (seconds): CONTRACT 60, LOBBY 300, THEFT 90, LOOP 120, POCKET 180, CLIMAX 120,
  EPILOGUE 30. Content modules own their real budgets and diegetic behavior.
- `ServerEvents` contains every plan event as a nested record. Subscribe to `runtime.events()`;
  its lifetime is one server. Failed listeners are logged without blocking other subscribers.
- `CorePayloads` defines all four wire records with the exact plan IDs and fields. S2C act and
  knowledge synchronize on join/change. C2S signals send only on change; read them with
  `CoreNetworking.signals(player)`. They never control authoritative timing or progression.
- The effects package wires `ClientSession.onEffect(payload -> ClientEffects.dispatch(...))`.
  Core supplies no effects; client mirrors clear on disconnect. Effect switches gate options and
  screamers. Send effects using `CoreNetworking.send(player, payload)`; unsupported clients are
  skipped via `canSend`. Do not register duplicate payload codecs/receivers in later packages.
- `/unallocated siguiente` needs no cheats, but requires a real player. `status`, `act <id>`,
  `flag <id>` and `fear` require gamemaster permissions (console works). `act` accepts any enum,
  including disabled acts; its module callback is deferred until a player exists. Status is also
  logged as `UNALLOCATED status act=...`. `scare`/`build` belong to their content packages: merge
  children into the same Brigadier root through Fabric's command registration callback.

## Choices where the plan was ambiguous

Debug jumps override feature switches for the explicitly selected act; automatic continuation
uses the switches. A reload of an act now disabled skips it. Explicit debug jumps are rescue
operations and log the outgoing act as forced; budget expiry calls its diegetic forceAdvance,
while jumps go directly to the selected checkpoint. Completion at the exact budget tick wins
over expiry. The debug override lasts for the current server instance; reloading reevaluates the
feature switch. A console jump commits state/logs immediately and defers player callbacks. An
empty/unstarted world does not advance. The final act finishes instead of looping.

The namespaced SavedData type uses `SAVED_DATA_COMMAND_STORAGE` as its vanilla DFU category
(no command-storage content fields are emitted); the core owns its explicit schema. This avoids
requiring Fabric's object-builder mixin solely to permit a null datafix type.

## CI and merge notes

JUnit exercises budget boundaries, diegetic fallback, broken fallback resilience, completion,
disabled skips, debug jumps, dormant/terminal state, actual SavedData codec round trips and reload
with a partially spent budget, dirty tracking, immutable views, feature parsing, event isolation and all four real network codecs.
The real dedicated smoke uses console `unallocated act LOBBY`, then `unallocated status`, checks
enter/status markers, stops cleanly, launches a second JVM against the same save, checks status
again and rejects an unsolicited LOBBY enter marker. No player means elapsedTicks must stay zero.
The watchdog requires each command's evidence to appear after that command, so stale logs cannot
acknowledge it. Real client startup exercises common and client payload registration.

This branch merges v0.1.6's six-module manifest, identity-based JAR verifier and clean
one-JAR smoke harness. The core uses only modules already in that allowlist and does not
maintain a separate include list or nested-archive hash policy. The optional full-API coexistence
check retains its own diagnostics and exercises the core commands without repeating reload.

Human validation after map/ending integration: reconnect/respawn at each checkpoint; verify each
diegetic fallback actually guides or moves the player; run a 10–15 minute session including pause,
save/reload, emergency siguiente without cheats, and the real ending. Core alone has no narrative
or map and cannot establish whether those behaviors feel clear or frightening.
