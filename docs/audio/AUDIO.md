# Audio v0 integration contract

This package provides playback; the sequencer/director decides when it is appropriate. Nothing starts automatically
on joining an arbitrary world. Call only within the consented UNALLOCATED session. The caller must enforce rare
muffles and the session limit of **at most two screamers**, and respect `effects.screamer` when that feature switch
is integrated. The debug command deliberately permits repeated takes for video; it is not the session director.

Server thread:

```java
AudioSounds.play(player, "ambient", player.getEyePosition());
AudioSounds.play(player, "threads", threadPosition);
AudioSounds.play(player, "muffle", player.getEyePosition());
AudioSounds.play(player, "screamer", baitPosition);
AudioSounds.stop(player);
```

Classes: `org.unallocated.audio.AudioSounds`, `AudioCue`. `play` returns false when the receiving client cannot
receive our packet. Unknown cue names fail immediately. `AudioCue.loud()` marks the startling cue for the contract,
director and capture tools. Client effects can call `org.unallocated.client.effects.SoundEffects.play(id, Vec3)`
on the client thread, `stop(AudioCue)` or `clearAll()`. These calls share the same layer replacement/cleanup rules.

| Cue | Vanilla event aliases | Behavior | Loud |
| --- | --- | --- | --- |
| `ambient` | beacon ambient + cave ambience | Quiet non-positional bed, 2 s fade in; cave layer has 60 s gap between repetitions; runs until stopped | No |
| `threads` | note block harp + amethyst resonance | Two quiet positional layers at caller's position, downward pitch, ≤2.4 s | No |
| `muffle` | beacon deactivate + gain envelope | Global gain ×0.2 for 0.3 s then recovers over 0.3 s, subtle low tone; total ≤0.6 s | No |
| `screamer` | iron golem damage + anvil impact | Dry two-layer hit, gain 0.55/0.30, downward pitch, faded and cut by 0.5 s | **Yes** |

One active instance per cue: repeating a cue replaces its previous layers, including pending manual loop repeats.
All own sounds stop on disconnect, leaving the world, client shutdown or `/unallocated audio stop`. Vanilla music,
sliders and `options.txt` are never changed. Master mute and category mute remain effective. The screamer uses
Hostile Creatures; other cues use Ambient/Environment. Positions affect `threads`; the bed, muffle and hit are
listener-relative so the bait's impact cannot disappear behind the camera. The muffle is volume attenuation,
**not** a low-pass/EQ filter or a real game freeze. The later latency package supplies any bounded world pause.
There are no sleeps, services, microphone access, external OGGs or modified Mojang files in the player JAR.

`assets/unallocated/sounds.json` references vanilla events with `type: event`; each simultaneous layer has its own
registered event (JSON lists otherwise choose alternatives, not layers). Runtime code applies pitch, gain, position,
looping and duration. Seven Spanish subtitles have `en_us` fallback and Spanish locale copies.

For a listening/capture pass, enable commands in a disposable test world and run each separately:
`/unallocated audio ambient`, `threads`, `muffle`, `screamer`, then `stop` (each uses the same command prefix).
**`screamer` produces a sudden strong impact.** No all-cues autoplay command is supplied. Adjust Minecraft's master,
Ambient/Environment and Hostile Creatures sliders as desired; the mod does not raise them.

CI starts an actual singleplayer client world with null OpenAL output, checks all seven events are registered and
resolve to nonempty resources, drives every command through server → audio packet → receiver, requires STARTED
results and active instances, checks spatialization and authored gain bounds, verifies the actual mixed gain via
the SoundEngine method (so a missing muffle mixin fails), waits for expiry, replaces/stops the bed, disconnects with
active audio, and compares both option slider values and `options.txt` bytes. The log watchdog requires every event
and completion marker. This proves engine execution, not that headphones sound good.

Human acceptance remains required: listen on speakers and headphones at ordinary player volume, judge silence and
sustained tension, string/chime subtlety and location, muffle recovery, and the short hit's balance. Capture a take
with sound; CI's null device cannot prove timbre, acoustic loudness, fear or the absence of a loop seam. Test both
OpenGL and Vulkan as in the plan. No new external asset licensing decision is needed.

Integrated playback clamps the resolved sample gain to the current authored fade envelope and
resolved pitch to 0.5–2.0. Vanilla event and resource-pack multipliers are applied before this
boundary; a layer field alone is not the final sound-engine value. The real client smoke checks
all seven resolved layers, their individual gain limits, finite values and both pitch bounds.
