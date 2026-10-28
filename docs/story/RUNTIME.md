# Story runtime v1

Author JSON according to `docs/plan/milestones.md` §6. The shipped neutral chapter has a
420-second budget and uses the existing SERVER_CLOSED farewell. Invalid data logs each
`UNALLOCATED story invalid file#pointer: [rule] message` and leaves content-free core acts.
The Java loader rejects duplicate JSON keys and parses vanilla commands at SERVER_STARTED
with a permission-level-2 source. `post_effect` is unavailable in this version.

All runtime access and subscriptions belong on the server thread. Register extensions during
mod initialization, before the first server starts. Duplicate registrations fail.

```java
StoryActions.register("my_action", (json, context) -> {
    context.keys(json, "do at value", "value");
    String value = json.get("value").getAsString();
    return (runtime, player) -> {
        runtime.state().addFlag("story.flag." + value);
        return "ok";
    };
});
```

`ActionParser.parse(JsonObject, StoryValidator.Context)` validates and returns an
`Action.execute(StoryRuntime, ServerPlayer)` returning `ok`, `skipped`, or `refused`.
The context exposes `error(suffix, rule, message)`, `keys`, `position`, and `box`.
`StoryTriggers.register(String, TriggerParser)` follows the same pattern and returns a
`Trigger` with `test(ServerPlayer)`, `simulate(ServerPlayer)`, optional `reset()` and
`automatic()` (true for time/flag triggers that autoplay must wait for).
Package 4 owns registration of `copy`, `corrupt`, and `creature`; they are unknown until then.

`StoryRuntime.get(server)` exposes `fire(String)` (guards/repeat limits still apply),
`currentChapter()` (JSON or null), `state()`, `resolvePosition(JsonElement)` and
`resolveBox(JsonElement)`. Positions retain dimension, block coordinates and camera direction;
`center()` adds the half-block X/Z teleport offset. Boxes include both final block corners.
State flags/counters owned by the engine use `story.`; authored flags use `story.flag.<id>`,
counters use `story.counter.<id>`. Pending queues survive save/reload without changing schema 1.

Subscribe through `runtime.events().subscribe(Event.class, listener)`; the returned subscription
is closeable. Events are `StoryBeatFired(String beatId, String chapter)`,
`StoryCapture(String name)`, and `StoryEnding(String id)`. Capture listeners save
`story-<name>.png` in the CI helper. `StoryBuild.run(server)` authors the story's build section,
releasing only forceloads it acquired, even on failure.

Commands (gamemaster/console): `/unallocated story status`, `check`, `build`, `fire <beat>`.
Build explicitly changes world blocks; check is read-only and reloads the files for validation.

CI alone sets `unallocated.story.dir`, `unallocated.story.autoplay=true` and optionally
`unallocated.story.autoplay.run` (default 0). `StoryRuntime.setAutoplayRun(int)` selects the next
world's run and requires the autoplay JVM property. All story delays/budgets use the saved
active-tick clock and scale only under autoplay. Escape is synchronous, as the core API requires:
its actions execute in at/file order before the core advances. A story ending uses the core's
forward jump to EPILOGUE and the existing tagged save/disconnect boundary.

Text travels through existing `story.*` EffectPayload IDs. Language/style and server placeholder
values are serialized; player name, local clock, save folder and contract agreement are resolved
only on the client. Blackout and ending splash are session memory. Interface custom text uses the
normal toolkit refusal/cooldown policy and never mutates options or the receipt.
