# The spyglass bait (work package v0.1.5; plan v0.1.8)

The owner's words it implements:

- Brainstorm, "Detalles": "cada vez que usas el catalejo (nativo de Minecraft) a la distancia, la figura-cebo está un
  poco más cerca. Es un cebo lejano con propósito."
- Plan 4.3, step 5: "un cebo lejano, asemejandose a tu amigo o a ti, se aleja, llamando asi tu atencion e invitandolo
  a seguir (Reclaimer lo hace como un anglerfish). Detalle: catalejo, cada vez que lo usas la figura está un poco más
  cerca (sin moverse entre intentos) hasta un screamer estando demasiado cerca."
- Plan 3.3, "Artefactos": "El Reclaimer los puede usar de **cebo** (anglerfish)"; "g1 casi perfectos y parecidos al
  jugador original (los más inquietantes)".

## What the player experiences

1. **It appears out of view.** A beat (or the director) starts it; after an optional delay ("en un momento inesperado")
   the figure is placed beside or behind the player, so it is *discovered*, never spawned in front of them. It prefers
   higher ground: a person-shaped silhouette against the sky, about 90 blocks away.
2. **It is you, slightly wrong** (the owner's g1 artefact). A vanilla mannequin wearing the player's own profile:
   their skin, mirrored (it holds its spyglass in the other hand, like a reflection), 6 % too tall, no name tag. It is
   always looking back at the player **through its own spyglass**: from far away the raised arm reads as "someone is
   watching me with a spyglass", which teaches the mechanic without a single line of text.
3. **Each spyglass use brings it closer, and it is never seen moving.** A use counts when the figure stays in the
   scope for half a second. The figure moves only while the player cannot see it: out of view, behind terrain,
   outside the spyglass circle, or during the first three ticks of raising the spyglass, when the zoom-in animation
   itself hides the jump (then it moves exactly along the line of sight, so it reads as part of the zoom). Default
   distances: 92, 64, 44, 28 blocks for four calm sightings, then 14.
4. **Too close: the screamer.** The next look at the 14-block figure holds for 0.7–1.2 s of silence (seeded jitter, so
   the moment is not predictable), then the figure is thrown into the player's face — about 3.4 blocks ahead through
   the scope, where its face fills the circle, or about 1 block ahead without it. The client cuts to black for 0.18 s
   (hiding the lunge itself); as the face appears, three vanilla sounds hit at once, pitched down: the enderman's
   stare, **the player's own hurt voice**, and the elder guardian's curse (the game's own screamer); a red stain and
   three torn scanlines fade by 0.8 s. Then the figure is gone for good.
5. **Silence.** Nothing about the bait makes a sound until the screamer.

How it behaves when the player does something else:

| Player does | Bait does |
| --- | --- |
| Walks toward it | It keeps its distance: while unseen it recedes along the same bearing (or toward the `lure` point, where it finally waits). "Invitandolo a seguir": a speck that leads. |
| Stares and walks up to it without looking away | Never moves; once closer than the last distance it is "too close", and the next steady look is the screamer. |
| Walks away | It follows, unseen, and is there at the same distance when they look back. |
| Looks away / lowers the spyglass | That is when it moves (one step per counted use). |
| Never uses the spyglass | For 90 s it only lures. Then plain looks (a steady 1.2 s look at it) also bring it one step closer each, so nobody leaves without the scare (plan §3.3, "nadie se va sin vivirlo"). The act budget can also force steps or the scare. |
| Shoots or hits it | The screamer at once (or a quiet vanish with the scare off). |
| Goes underground / to another dimension | It waits for a spot it can stand on with a line of sight; after 30 s missing it gives up. |
| Quits the world | The figure is never written to the save (`Entity.shouldBeSaved` is false for it); stray figures are removed whenever loaded. The player's world is unchanged. |
| Has a short render distance | The whole approach shrinks proportionally to fit 80 % of the view distance. |

## For story beats and the director (`org.unallocated.bait`, server thread)

```java
SpyglassBait.place(player, SpyglassBaitSpec.defaults());                  // out of view, defaults above
SpyglassBait.place(player, spec, lureBlockPos, false);                    // leads toward e.g. MapLayout "hill.bait_path"
SpyglassBaitSpec spec = SpyglassBaitSpec.defaults()
        .withDistances(SpyglassBaitSpec.geometric(4, 80, 12))           // steps; the last is "too close"
        .withScare(features.screamer())                                   // off: it just vanishes after the last look
        .withTiming(10, 14, 10)                                           // dwell, scare delay, jitter (ticks)
        .withFallbackAfterTicks(1800).withAppearDelayTicks(400).withMaxLifetimeTicks(6000).withSeed(seed);
SpyglassBait.forceStep(player);   SpyglassBait.forceScare(player);       // act budget ("tope del acto")
SpyglassBait.clear(player, "act-advanced");   SpyglassBait.isActive(player);   SpyglassBait.status(player);
```

`SpyglassBaitLogic` holds every rule and has no Minecraft types; `SpyglassBaitFigure` is the world adapter
(placement, visibility raycasts, the mannequin). Log markers: `UNALLOCATED bait start|place|step=N/M distance= cause=
sightings=|scare cause=|gone cause=` and, on the client, `UNALLOCATED bait screamer shown`.

## Debug command (cheats on)

```
/ubait start [steps first closest scare]   as a beat would: out of view
/ubait ahead [steps first closest scare]   straight ahead in plain view, for the video and CI
/ubait step | scare | clear | status
/ubait selftest                            dedicated-server self-test (server console)
```

Example for the video: `/ubait ahead 4 70 12 true`, then use the spyglass four times.

## Content warnings

The screamer uses exactly what the contract (work package 1.3) warns about: "sonidos fuertes" and "sustos"; its black
cut is a single flash, never a strobe. It lasts 0.8 s and happens once per bait. `scare=false` gives a version without
sound or image for any reduced-intensity choice the owner may want later. In multiplayer (secondary) only the host
sees the image; remote players get the lunge without sound or image.

**For the coordinator when merging with 1.3:** the contract's data-use list currently says this build reads no skins.
The bait shows the player's own skin on a figure, taken from the profile the game already has; nothing is stored or
sent. Suggested line: "El catalejo: usamos tu propia skin, la que el juego ya cargó, para dar forma a una figura en tu
mundo. No se guarda ni sale de tu equipo."

## No Fabric API, no registered content

Everything is vanilla content driven by our code: `EntityTypes.MANNEQUIN`, vanilla sounds, the skin the client already
has. Six small mixins (`org.unallocated.mixin.bait`, `org.unallocated.client.mixin.bait`): server tick, command
registration (same injection point as Fabric's command API in 26.3), three private mannequin setters, the save
exclusion, the HUD overlay and mannequin render distance (vanilla culls body-sized entities at ~64 blocks). Both mixin
configs are `required: false`: if a future 26.3.x hotfix moves a target, the bait degrades instead of crashing the
judge's game, and CI's self-test and game test fail loudly instead.

## Tests in CI

- `SpyglassBaitLogicTest` (JUnit, `gradle build`): appearance delay, one sighting per use, no move while seen,
  zoom-mask step, panning away, full sequence to the screamer and removal, scare off, receding and following, walking
  up while watching, the never-uses-the-spyglass fallback, lifetime, hurt, lost figure, forced step/scare, seeded scare
  moment, spec validation and render-distance fitting, and a randomized check over 200 runs that the figure is never
  moved while seen.
- Dedicated server (`scripts/smoke_test.py server`): types `ubait selftest`; requires `UNALLOCATED bait selftest passed`
  (figure made, accessor mixin applied, dressed, on the ground, found in the level, spyglass raised, not saveable,
  moved, removed).
- Real client (`SpyglassBaitSmokeTest`): single-player world, `/ubait ahead 3 40 12 true`, three spyglass uses with real
  input, server state checked after each, screamer, figure gone. Screenshots in the CI artifact:
  `unallocated-bait-1-plain-view`, `-2-first-sighting`, `-3-closer`, `-4-screamer`.

## Needs a human playthrough

Whether it is frightening rather than silly: the distances, the silence, the 0.7–1.2 s wait, the loudness of the three
sounds together, the lunge reading as a jump-cut rather than a slide, the mirrored figure holding a spyglass, the
receding "speck" when followed on the real map's terrain, and how often players find the spyglass on their own.

## Open questions for the owner

1. **"Se aleja".** Read here as: it leads you (recedes when you approach, unseen) and is never seen moving. Should it
   instead be seen walking away once, at the very first sighting?
2. **Whose figure.** The brief and "a ti" use the player's own skin; plan §3.3 says "un artefacto de otra víctima, aún no
   tú". Another victim needs a skin we ship (an artefact skin). Own skin now; switchable later.
3. **The fallback.** After 90 s without the spyglass, plain looks also advance it (plan's "nadie se va sin vivirlo"). Too
   generous or too slow?
4. **Second scare channel.** A loud sound plus a black cut and red stain, no full-screen picture: the face is the
   figure itself in the world. Is that the "imagen repentina" you want, or a drawn image as well?
