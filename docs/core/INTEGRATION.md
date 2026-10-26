# Core integration — alpha.1.7

Public names from PLAN §4.2–4.3 remain stable. Content belongs to the map/act packages.
Only the server thread may access `CoreRuntime.get`, session state, sequencer or server bus.
`javacheck.sh` type-checks locally; Gradle, JUnit execution and real game runs happen only in CI.

## Arming, consent and first join

- A normal world is unarmed. `SessionState.get(server)` caches a clean dormant object, including
  clearing the dirty flag that vanilla's `SavedDataStorage.set` sets. Ordinary opening, joining,
  status queries and rejected commands never create `unallocated_session.dat`.
- **CONTRACT belongs to the client before the menu.** The client launcher never reaches into
  `getSingleplayerServer()`/the player list to start a session. Its installed template already
  contains the armed marker. No C2S start packet or client-thread server state access is needed.
- **Command template pipeline:** successful `/unallocated build all` arms the overworld. Core
  decorates the map package's registered Brigadier command at `SERVER_STARTED` (and after successful
  datapack reload), after all command registrations. The original build, verification, permissions
  and suggestions remain intact. Failed builds and individual sections never arm. Core adds no
  fake map command when the map package is absent, and repeated installation is idempotent.
- **Direct Java template builders:** after successfully constructing/verifying the complete map,
  call `CoreRuntime.arm(server)` **on the server thread**, then save and stop before packaging the
  template. This writes `armed=true` into the same SessionState. Calling `SessionState.get(server).arm()`
  is equivalent; prefer the runtime hook for its `UNALLOCATED session armed` log.
- First `ServerPlayConnectionEvents.JOIN` in an armed, unstarted world calls `start(player)` on
  the server thread. It logs `UNALLOCATED act=CONTRACT complete t=0`, then enters LOBBY immediately
  (or skips it if globally disabled). There is no server CONTRACT tick or 60-second wait. A
  CONTRACT debug target is normalized to this client boundary instead of waiting in-world.
- Reconnect/reload restores the current checkpoint and remaining budget; it does not start anew.
  A finished session never restarts on join. Arming an already-open authoring world does not
  start it inside the build command; its next join is the normal startup point.
- `CoreRuntime.start(ServerPlayer)` is retained for existing server-side callers, but is guarded
  by `armed` and asserts the server thread. `get(server)` also rejects client-thread calls before
  touching its server map. No public start or advance operation implicitly arms a world.
- `/unallocated arm` is an explicit gamemaster-only authoring/debug operation for the core-only
  branch and old disposable test saves. It prepares the world but does not start it. `act` and
  `flag` reject unarmed worlds. `status`/`fear` remain read-only. Normal judges need none of these.

## Modules, budgets and save state

- Register one `ActModule` per act with `CoreRuntime.register(module)` during mod initialization.
  It replaces a content-free fallback. Duplicate content registrations fail clearly.
- Use `sequencer()`, `state()`, `events()` and `features()` from the per-server runtime. Content
  calls `advance(player)` or returns true from `complete`; `advance` on a dormant/finished session
  is a no-op. Narrative order after the client boundary is LOBBY → THEFT → LOOP → POCKET → CLIMAX
  → EPILOGUE. Automatic selection requires **both** `features.actEnabled(act)` and `module.enabled`;
  a module cannot bypass the global switch by returning true. Explicit admin jumps can test an
  individually disabled act in an armed world; that override lasts until the next server reload.
- `forceAdvance` implements the diegetic escape synchronously; core advances afterwards even if
  the callback throws. `enter` restores a checkpoint on reconnect/respawn/reload: make it idempotent
  and persist progress in SessionState, not module fields. Core never resolves MapLayout positions.
- `enabled`, `budgetTicks`, `checkpointId`, `enter`, `tick`, `complete` and `forceAdvance` exceptions
  are isolated. A broken enabled callback skips the act. Budget/checkpoint metadata is cached per
  entry/resume so status never invokes content. Invalid/throwing budgets and failed enter callbacks
  get a one-tick escape budget; a failed checkpoint getter reports `unavailable`.
- SavedData ID is `unallocated:unallocated_session` in the overworld (`SessionState.ID` retains the
  plan's bare ID). Schema 1 contains every plan field plus `armed`, `ticks`, `started`, `finished`.
  Missing `armed` defaults to **false**, including legacy started saves: the old rescue bug could
  have started an ordinary world, so never infer consent/arming from `started=true`.
- Timestamps use persisted active ticks at 20 ticks/second, not process/wall/daytime ticks. No time
  elapses in an empty/stopped server or paused integrated server. Rejoining never renews the budget.
  `t=<s>` in markers is whole elapsed session seconds. Completion on the deadline wins over expiry.
- One session per world; one participant (first server-list player) drives callbacks. Multiplayer
  arbitration/per-player sessions remain future work. `profile` is `Map<String,Double>` until the
  telemetry package defines a richer schema. Collection getters are read-only; use state mutators
  so SavedData is marked dirty. Flags and counters survive act jumps.
- EPILOGUE completion/expiry sets `finished` once without wrapping. Ending content owns disconnect
  and return to title. Admin jumps can reactivate a finished session in an armed world.
- Features come from `assets/unallocated/features.json`. Missing switches are off; malformed values
  fail startup. Server fallback budgets (seconds): LOBBY 300, THEFT 90, LOOP 120, POCKET 180,
  CLIMAX 120, EPILOGUE 30. CONTRACT is always a client boundary irrespective of its feature switch.

## Commands, logs, events and network

- `/unallocated siguiente` is the no-cheats rescue, requires a real player and `state.active()`
  (`armed && started && !finished`), and waits **five active seconds in each new act** before it
  can be used. Repeated up-arrow/Enter cannot immediately skip another act. It runs the act's
  diegetic escape and records `skipped`, never `complete`. Its delay survives save/reload through
  the act timestamp. Completion callbacks and budget expiry remain immediate at their boundaries.
- `status`, `act <id>`, `flag <id>`, `fear`, `arm` require gamemaster permissions; console works.
  `scare` and `build` are merged into the same root by their owning packages. The template package
  must deliberately choose the judge's cheats setting; no-cheats rescue works either way.
- Log semantics: `enter` = entered server act; `complete` = real completion (plus the client CONTRACT
  boundary); `forced` = budget expiry/content callback failure with diegetic escape; `skipped` =
  player rescue, explicit admin jump or disabled-act selection/resume. Status also logs
  `UNALLOCATED status act=... ... armed=...`. Disabled modules do not receive player callbacks.
- `ServerEvents` retains all plan events as nested records. Subscribe to `runtime.events()`; failed
  listeners are logged without hiding the event from other listeners. **Reentrant advance/skip
  calls inside ActChanged listeners are ignored** while the transition is committed; schedule
  progression in a module tick/complete callback. **No ActChanged event fires when the final act
  finishes**, because there is no next act. Ending/audio packages should react to EPILOGUE entry
  or their explicit `EndingReached(id)` event, which the ending package owns, rather than waiting
  for an event with a null/sentinel act. Core does not invent an ending ID.
- CorePayloads owns the four plan payload IDs/fields/codecs. Act/knowledge synchronize on armed
  join/change. Client mirrors clear on disconnect. C2S signals send only when changed and never
  control progression. `CoreNetworking.signals(player)` exposes them for telemetry.
- Wire effects by `ClientSession.onEffect(payload -> ClientEffects.dispatch(...))`. Core creates
  no effects; options/screamer feature gates still apply. `CoreNetworking.send` checks canSend.
  Later packages must not register duplicate codecs/receivers for these IDs.

## Review notes and validation

POCKET still ships **off** until its content exists; CLIMAX remains an enabled empty fallback.
**When the world/pocket package is integrated, enable `acts.pocket` alongside its real module**;
otherwise the required act is deliberately skipped by the global gate. The core does not turn
unfinished content on to satisfy an integration milestone.

The namespaced SavedData type uses `SAVED_DATA_COMMAND_STORAGE` as its vanilla DFU category
(no command-storage content fields are emitted); core owns its schema. Unknown schema/act or
invalid numeric data is rejected by the codec. Recovery behavior of vanilla's loader on corrupt
saved data is not asserted by our tests: do not silently arm or start a fallback world on decode
failure. Per-player multiplayer, actual respawn checkpoint movement, single-player pause timing,
client-signals sampling, and merged-map/ending pacing still require integration/human validation.

JUnit covers dormant/unarmed rescue and advance, client-only CONTRACT, global act switches,
throwing callbacks/metadata, rescue delay/double-entry prevention, truthful skip markers, budgets,
SavedData arming/legacy migration/codec/reload and dirty tracking, and post-success build-all arming
with permissions/suggestions preserved. Existing feature/bus/network codec tests remain.

Dedicated smoke explicitly arms a disposable test world before its debug act/status/save/restart
checks. The real client helper now opens an **unarmed** world with a player, issues no-cheats
siguiente and verifies no session file after saving; arms before reopening, checks immediate
LOBBY on JOIN and client synchronization; checks delayed rescue and repeated-command rejection;
then reopens the single-player save and verifies CLIMAX progress and finished-session rejection.
It checks the exact production player JAR first. These new game assertions must pass CI; a local
type-check alone does not prove game behavior.

Alpha.1.6's six-module manifest, identity-based JAR verifier, notices and clean one-JAR harness
remain authoritative. Core needs no additional Fabric module. The optional full-API coexistence
smoke retains separate diagnostics and runs its command checks without another reload.
