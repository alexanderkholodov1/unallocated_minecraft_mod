# Interface toolkit — 1.0.0-alpha.1.4

This package implements the brainstorm's “el idioma de Minecraft como arma” and plan 3.2, 3.5, 4.5,
5.2 and 5.7. The owner's October 26 update makes the pre-built map and a solo 10–15 minute session
the primary integration target. Nothing fires automatically. A later director/map controller chooses the beats.

## Client API

Call `org.unallocated.client.interfacefx.InterfaceEffects.trigger(Request)` on the Minecraft client thread.
It returns `false` when the world, cooldown, settings editor, requested advancement or durable journal is unavailable.
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
`clear()` cancels the toolkit and restores options; it retains cooldowns. `leaveWorld()` is wired to unload.
No networking, world mutation, inference service, Fabric API runtime dependency or automatic story schedule is added.

| Name | Maximum duration | Behavior and preserved functionality |
| --- | --- | --- |
| `F3` | 60 ticks / 3 s | Replaces only the XYZ display with coordinates beyond the world boundary and an impossible depth. Block/Chunk/Facing, actual position, F3+C clipboard, targeting and physics stay truthful. It neither enables F3 nor changes the player's debug profile. |
| `DEATH` | 600 ticks / 30 s armed window | Rewrites the cause line while an actual vanilla death screen renders. It neither kills the player nor changes respawn buttons, death packets, signed chat or the server's death history. Expiry restores the original line, even while the screen remains open. |
| `SPLASH` | 600 ticks / 30 s | Replaces the title screen's native splash component at render time. Respects the player's Hide Splash Texts option. |
| `LOADING` | 600 ticks / 30 s | Replaces the native LevelLoadingScreen terrain-loading caption. The real chunk/progress display and narration of numeric loading progress remain intact. |
| `ADVANCEMENT` | 100 ticks / 5 s | Adds a native AdvancementToast without registering an advancement, awarding criteria/rewards, changing progress or sending packets. With no ID it composes a deterministic invented title; with an ID it uses an advancement already known to the client's advancement tree. An unknown ID is refused. |
| `SENSITIVITY` | 100 ticks / 5 s | At most ±0.04 on vanilla's 0…1 sensitivity range, clamped to that range. |
| `FOV` | 100 ticks / 5 s | At most ±3 degrees, clamped to vanilla's 30…110 degree range. |
| `GAMMA` | 100 ticks / 5 s | Smooth 2 Hz variations, at most 0.08 on vanilla's 0…1 gamma range, toward the middle of the range. This is not a full-screen flashing overlay. |
| `SIGN` | 100 ticks / 5 s | A chosen sign's front-side render text uses compact g0…g5 lines in the selected leak language. The sign block entity, back side, packets and world saves stay untouched. The next render returns the original text. |
| `LOG` | 100 ticks / 5 s request | A local chat line resembling a failed server/model operation, without forging a JVM/server log entry. Like ordinary chat, it remains in session chat history. |

Durations use a monotonic real-time deadline, so pausing or accelerating game-test ticks cannot extend drift.
Splash/loading arms can span leaving a world so a director can prepare a transition/title beat; they expire normally.
All other effects are cleared on world unload. All effects are cleared on client close.
Owned toasts are hidden on cancellation/expiry through vanilla's normal dismissal animation; real toasts are unaffected.

## Debug/video commands

In a disposable test launcher profile, add JVM argument `-Dunallocated.interface.debug=true`.
The opt-in local command bypasses production cooldowns, while retaining duration/strength limits and journal checks.
Without the argument, the command explains how to enable debugging and never forwards anything to a server.

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

## Restoration contract

`InterfaceOptionsJournal` is pure Java; it does not parse/rewrite `options.txt`.
At the first accepted settings trigger it snapshots the **entire current file**, including unknown options,
comments, encoding and newline bytes, or records that the file did not exist. This is the pre-effect baseline;
vanilla may already have saved startup state earlier in the launch.

1. Acquire a process-lifetime lock for this game directory. Two clients cannot own the same transaction.
2. Before any `OptionInstance.set`, write the original bytes, existence flag, schema version and SHA-256 checksum
   into a staging file; force the file and publish the journal with an atomic same-filesystem rename.
   There is no non-atomic fallback: a failed journal write refuses the effect.
3. Save the original in-memory sensitivity/FOV/gamma and apply bounded drift.
4. Suppress vanilla `Options.save` while the transaction is protected. Therefore even saves made while drift is
   visible cannot serialize modified options, normalize the original file or truncate it mid-write.
5. On expiry, emergency stop, world exit and before closing, restore the original in-memory values and original file.
   Keep the journal and save protection until shutdown completes: late passive vanilla saves must not replace the bytes.
6. After vanilla close, restore once more and retire the journal. A JVM shutdown hook also replays disk restoration;
   it never calls render-thread option callbacks. If killed without a hook, the journal remains.
7. On the next start, inject at the **head of Options.load**, acquire the same lock and restore before vanilla reads
   the file. Keep save protection across startup's own save calls, so recovery is not immediately overwritten.
8. Explicitly opening Options/OptionsSubScreen first restores and retires the transaction, then permits normal user
   preference edits. A subsequent effect journals those new preferences. Changes made deliberately in the options UI
   are thus a new baseline; passive vanilla saves cannot silently become one.

An incomplete staging file is never treated as a published journal. A corrupt published journal is retained and
settings effects are disabled instead of overwriting recovery evidence. Restore is replayable after interruption;
the journal is deleted only after restoring succeeds. Existing/missing options are both restored exactly.
The lock sidecar is deliberately not deleted (deleting a locked inode permits a second writer to race).
No original settings, journal payload or user identifiers are sent over the network.

This protects ordinary process crashes/kills. Filesystem destruction, journal deletion by another program and
removing the mod before recovery cannot be repaired by software that is no longer running. On a recovery I/O error,
the original journal stays in place and the client logs the problem without intentionally crashing the game.

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

`gradle build clientSmokeJar` runs seven JUnit journal tests in CI: byte-identical change/crash/restart,
repeated triggers retaining the first snapshot, replay after restore/late save, originally missing options,
checksum failure retaining evidence, competing owner, abandoned staging, and an abruptly halted child JVM.
The child actually calls `Runtime.halt`, bypassing finally blocks and shutdown hooks.

The existing production-client game test now also:

- Starts with a seeded pending journal and changed options. A test-only observer at Options.load return requires
  original nondefault sensitivity, FOV and gamma already loaded and the original CRLF file byte-for-byte restored.
- Renders native splash/loading/F3/advancement/sign/death hooks and takes six named screenshots; the Python watchdog
  requires all six valid PNG signatures and the explicit restoration success marker, in addition to the title smoke.
- Exercises all three leak languages, checks missing keys, request bounds, global cooldown and the local command handler.
- Keeps real F3 navigation lines unchanged, checks a sign remains unmodified on the server, and checks displaying an
  existing advancement does not award progress. A real server `/kill` opens the death screen.
- Changes each numerical setting, calls vanilla save while active, observes the changed value, waits for wall-time
  expiry, and compares all three original values and the complete options file. It also opens Options during drift.
- Leaves the world with drift active, checks restoration and calls a late vanilla save to prove it cannot win.

The smoke helper and recovery observer remain outside the player JAR; artifact verification rejects their classes,
test entrypoints, nested dependencies and test mixin config. Dedicated-server smoke still uses only the player JAR.
JUnit XML/HTML and client screenshots/logs are uploaded in diagnostics. Fourteen Python watchdog tests run without Java.

No Java or Gradle is run on the shared scaffold host. Python checks and static JSON/diff checks can run locally.
CI result/diagnostics must be reviewed before merging; the sandbox cannot reach the private GitHub API directly.
Human playthrough still needs to judge subtlety, readability at ordinary UI scales, perceived gamma/FOV intensity,
the warning/consent integration and the director/map's spacing across a full 10–15 minute session.
