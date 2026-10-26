# Interface toolkit — 1.0.0-alpha.1.4

This package implements the brainstorm's “el idioma de Minecraft como arma” and plan 3.2, 3.5, 4.5,
5.2 and 5.7. The owner's October 26 update makes the pre-built map and a solo 10–15 minute session
the primary integration target. Nothing fires automatically. A later director/map controller chooses the beats.

## Client API

Call `org.unallocated.client.interfacefx.InterfaceEffects.trigger(Request)` on the Minecraft client thread.
It returns `false` when the world, cooldown, settings editor or requested advancement is unavailable.
Invalid parameters throw `IllegalArgumentException`; an off-thread call throws `IllegalStateException`.
The director should treat a refusal as a skipped beat, never retry it every tick.

```java
client.execute(() -> InterfaceEffects.trigger(new InterfaceEffects.Request(
        InterfaceEffects.Effect.FOV, 40, 2, .5,
        InterfaceTexts.LeakLanguage.ES, 17, null)));
```

`Request` takes effect, duration in ticks (50 ms each), generation 0–5, strength −1…1,
leaked-text language, deterministic seed, optional sign block position, and optional advancement ID.
The seven-argument constructor defaults the advancement ID to `null` (invented advancement).
The sign position is required for `SIGN`; it is captured immutably and bound to the current client world.
Other world effects stop when that client world is replaced, including dimension transitions.

Production triggers have a 60 second cooldown per primitive and a 15 second global minimum interval.
These are safety ceilings, not a suggested schedule. The director should normally be substantially rarer.
Only one settings effect can run at once. No setting effect is accepted while the settings editor is open.
`clear()` cancels the toolkit and clears perception overrides; it retains cooldowns. `leaveWorld()` is wired to unload.
No networking, world mutation, inference service, Fabric API runtime dependency or automatic story schedule is added.

| Name | Maximum duration | Behavior and preserved functionality |
| --- | --- | --- |
| `F3` | 60 ticks / 3 s | Replaces only the XYZ display with coordinates beyond the world boundary and an impossible depth. Block/Chunk/Facing, actual position, F3+C clipboard, targeting and physics stay truthful. It neither enables F3 nor changes the player's debug profile. |
| `DEATH` | 600 ticks / 30 s armed window | Rewrites the cause line while an actual vanilla death screen renders. It neither kills the player nor changes respawn buttons, death packets, signed chat or the server's death history. Expiry restores the original line, even while the screen remains open. |
| `SPLASH` | 600 ticks / 30 s | Replaces the title screen's native splash component at render time. Respects the player's Hide Splash Texts option. |
| `LOADING` | 600 ticks / 30 s | Replaces the native LevelLoadingScreen terrain-loading caption. Generations 0–2 leak the goal; generations 3–5 leak log-like failure lines. The real chunk/progress display and narration of numeric loading progress remain intact. |
| `ADVANCEMENT` | 100 ticks / 5 s | Adds a native AdvancementToast without registering an advancement, awarding criteria/rewards, changing progress or sending packets. Toolkit toast titles are clipped to one native line so the title appears below the header immediately, including during the first second. With no ID it composes a deterministic invented title; with an ID it uses an advancement already known to the client's advancement tree. An unknown ID is refused. |
| `SENSITIVITY` | 100 ticks / 5 s | A 0.92…1.08 multiplier applied only when MouseHandler reads sensitivity; actual options stay unchanged. |
| `FOV` | 100 ticks / 5 s | A 0.96…1.04 multiplier applied to Camera.calculateFov before Camera.update stores it and builds the perspective projection, preserving normal camera/spyglass behavior outside the effect. |
| `GAMMA` | 100 ticks / 5 s | Smooth 2 Hz darkness variations, at most 0.08 added to vanilla lightmap darkness. Respects the player's Darkness Effect Scale; never changes gamma or applies a status effect. |
| `SIGN` | 100 ticks / 5 s | A chosen sign's front-side render text uses compact g0…g5 lines in the selected leak language. The sign block entity, back side, packets and world saves stay untouched. The next render returns the original text. |

Durations use a monotonic real-time deadline, so pausing or accelerating game-test ticks cannot extend drift.
Splash/loading arms can span leaving a world so a director can prepare a transition/title beat; they expire normally.
All other effects are cleared on world unload. All effects are cleared on client close.
Owned toasts are hidden on cancellation/expiry through vanilla's normal dismissal animation; real toasts are unaffected.

## Debug/video commands

In a disposable test launcher profile, add JVM argument `-Dunallocated.interface.debug=true`.
The opt-in local command bypasses production cooldowns, while retaining duration/strength limits.
Without the argument, the command is consumed silently and never forwards anything to a server.
`-Dunallocated.interface.debug.quiet=true` suppresses feedback even in debug mode (used for CI captures).

```text
/uinterface <effect> [ticks] [generation] [es|en|mixed] [strength] [seed] [advancement_id]
/uinterface restore
/uinterface help
```

Names are case-insensitive. Defaults: 60 ticks, g2, Spanish, strength 0.5, seed 17.
Use ASCII `-` for negative strengths. For `sign`, aim at a sign first. Actual chat input is intercepted before sending.

Suggested separate shots for the owner:

1. Enable F3 normally, then `/uinterface f3 60` and compare XYZ with the still-correct Block line.
2. `/uinterface death 600 3`, then `/kill` in a disposable cheats-enabled world. This arms a real death, not a fake death screen.
3. `/uinterface splash 600 2 mixed`, then save/leave to the title within 30 seconds.
4. `/uinterface loading 600 3`, then cause an actual loading transition within 30 seconds.
5. `/uinterface advancement 100 5 es 0.5 39` for an invented advancement; repeat with a different seed/generation.
6. `/uinterface advancement 100 2 es 0.5 17 minecraft:story/mine_stone` for a known advancement.
   It must already be visible/known to this client; no progress is granted by this command.
7. Separately try `/uinterface sensitivity 60`, `/uinterface fov 60`, `/uinterface gamma 60`,
   and `/uinterface sign 100 3 mixed` while looking at a sign.
8. `/uinterface restore` is the immediate emergency stop. Opening Options also ends settings drift first.

The developer debug command is not a substitute for the future player-facing beginning/story sequence.
The Spanish resource key `interface.unallocated.warning` is available to the sibling contract package:
brief sensitivity/FOV changes and smooth brightness variations should appear in the actual content warnings.
Its integration belongs to that package/coordinator; this branch does not add a competing consent screen.

## Restoration by construction (adopted D-21 A)

Settings drift now uses `org.unallocated.client.effects.PerceptionOverrides`, following milestone PLAN §4.3.
Its `INSTANCE.apply(Perception kind, float value, int ticks)` accepts `FOV_SCALE` (0.96…1.04),
`SENSITIVITY_SCALE` (0.92…1.08), or `DARKNESS` (0…0.08). `clearAll()` cancels them.
The pure Java state expires by monotonic wall time; darkness follows a smooth 2 Hz pulse.

Only the native mouse consumer, camera FOV calculation and lightmap render state read the overrides. None calls
`OptionInstance.set`, writes `options.txt`, suppresses vanilla saves, changes the selected UI language,
or requires a next-start recovery hook. Opening Options, leaving/replacing the world and client close clear
all perception overrides. A killed process loses all overrides; removing the mod before restarting is safe.
Normal user preferences and vanilla saves continue to work throughout. The obsolete journal, OptionsGuard,
save/load mixin, recovery probe and journal tests have been removed: nothing in this toolkit needs to touch real options.
The `GAMMA` trigger name is retained for callers, but now maps to temporary render darkness rather than gamma mutation.

## Text and the open language decision

All narrative/debug text is in `assets/unallocated/lang/es_es.json`, with the same Spanish-default entries in
`en_us.json` as vanilla's fallback for other UI languages. Keys begin `interface.unallocated.`.
The UI language is never switched globally or reloaded. Each request independently selects `ES`, `EN` or `MIXED`.
The short sign lines, death lines, log lines and goals track g0…g5, with failed acquisition, retained human data,
copying over copies, the frustration of making each attempt worse and finally repetition without meaning.

Invented advancement titles are seeded fragment composition, not a claim of a running LLM or real goal learning.
Generation 5 repeats fragments to make the degradation apparent. Existing advancement titles follow the player's
own vanilla localization; only the invented/leaked text follows the request's leak language.

**Recommendation, not a closed decision:** Spanish for narrative clarity in a short solo session; use `mixed`
sparingly for technical leaks and later degradation. The owner's plan 3.2 language choice remains open.

## 26.3 API investigation

The coordinator's official unobfuscated client JAR is now the authority for Minecraft signatures and injection
sites. [The recorded audit](INTERFACE-API-AUDIT.json) checks Minecraft and resolved Fabric calls, fields, private mixin targets and
actual bytecode references without running Java or Gradle. Its SHA-1 matches `mc-ref/26.3.json`.
This replaces the earlier source-mirror assumptions: the debug reply uses public
`ChatComponent.addClientSystemMessage(Component)`; invented toasts use current `rootDisplay(...)`; death text
redirects the cause field in `visitText(ActiveTextCollector)`, where the real renderer reads it.
Mixins live exclusively in `org.unallocated.client.mixin.interfacefx` and test-only
`org.unallocated.client.mixin.interfacefxsmoke`; runtime logic remains outside both namespaces.

The binary audit now includes client game-test API 6.0.11+131f8d475d, game-test API
4.0.35+3434d6d95d and Fabric Loader 0.20.1 from the coordinator's resolved dependencies.

The implemented hooks target the actual 26.3 classes, rather than older `getGameInformation`/`GuiGraphics` tutorials.
The inspected Mojang class source has the title's SplashRenderer and LevelLoadingScreen's terrain caption with
progress/chunk rendering. No rotating advice/tip list was found in these Java Edition screens; this package uses
the native splash and native loading caption and does not pretend the Bedrock tips screen exists here.

References inspected October 26, 2026 (the source mirror reproduces game class source; it is not Mojang's website):

- [DebugEntryPosition source](https://mc-packet-reference.netlify.app/26.x/source/net/minecraft/client/gui/components/debug/debugentryposition/)
- [SplashRenderer source](https://mc-packet-reference.netlify.app/26.x/source/net/minecraft/client/gui/components/splashrenderer/)
- [LevelLoadingScreen source](https://mc-packet-reference.netlify.app/26.x/source/net/minecraft/client/gui/screens/levelloadingscreen/)
- [DeathScreen source](https://mc-packet-reference.netlify.app/26.x/source/net/minecraft/client/gui/screens/deathscreen/)
- [AbstractSignRenderer source](https://mc-packet-reference.netlify.app/26.x/source/net/minecraft/client/renderer/blockentity/abstractsignrenderer/)
- [Options source](https://mc-packet-reference.netlify.app/26.x/source/net/minecraft/client/options/)
- [Fabric 26.3 game-test context](https://raw.githubusercontent.com/FabricMC/fabric-api/26.3/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/api/client/gametest/v1/context/ClientGameTestContext.java)
- [Fabric 26.3 test-world builder](https://raw.githubusercontent.com/FabricMC/fabric-api/26.3/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/api/client/gametest/v1/world/TestWorldBuilder.java)

Compilation and real-game success must be established by CI, not inferred from source inspection.

## Verification and remaining playthrough

`gradle build clientSmokeJar` runs three JUnit perception-state tests in CI: bounded requests,
wall-time expiry/clear, and fresh process state without drift. No journal or options recovery is needed.

The existing production-client game test now also:

- Renders native splash/loading/F3/advancement/sign/death hooks and takes six named screenshots; the Python watchdog
  requires all six valid PNG signatures and the explicit restoration success marker, in addition to the title smoke.
- Exercises all three leak languages, checks missing keys, request bounds, global cooldown and the local command handler.
- Keeps real F3 navigation lines unchanged, checks a sign remains unmodified on the server, and checks displaying an
  existing advancement does not award progress. A real server `/kill` opens the death screen.
- Requires changed values at all three native perception consumers, calls normal vanilla save while overrides are active,
  and checks that real option values and file bytes remain unchanged. Also checks expiry and opening Options.
  Sensitivity is verified by feeding identical raw deltas into vanilla `MouseHandler.turnPlayer` through a test-only
  accessor and asserting changed player yaw, then the original yaw response after expiry. This bypasses only the
  SDL focus/capture gates that simulated cursor movement does not guarantee under Xvfb.
  FOV is verified on the stored camera value used by `setupPerspective`, rather than by intercepting its getter.
  Darkness is observed after changing `LightmapRenderState.darknessEffectScale`, the value uploaded by `Lightmap.render`.
  Every phase logs its effect name and original/adjusted values; a failed wait names the missing consumer.
- Requires the toast title hook and verifies a disabled debug command emits no chat feedback and dispatches no effect.
- Leaves the world with an override active, checks it cleared and confirms normal saves leave the same option bytes.

The smoke helper and raw-mouse accessor remain outside the player JAR; artifact verification rejects their classes,
test entrypoints, nested dependencies and test mixin config. Dedicated-server smoke still uses only the player JAR.
JUnit XML/HTML and client screenshots/logs are uploaded in diagnostics. Fourteen Python watchdog tests run without Java.

The coordinator confirmed run 40273949425 on f84c8b6 green with actual effect screenshots. The corrected native-consumer
perception probes require a new green CI run; no Java or Gradle was run locally.

No Java or Gradle is run on the shared scaffold host. Python checks and static JSON/diff checks can run locally.
CI result/diagnostics must be reviewed before merging; the sandbox cannot reach the private GitHub API directly.
Human playthrough still needs to judge subtlety, readability at ordinary UI scales, perceived gamma/FOV intensity,
the warning/consent integration and the director/map's spacing across a full 10–15 minute session.
