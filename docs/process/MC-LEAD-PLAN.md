# MC lead plan — ship the jam build by 04:15Z (written ~01:58Z, 2026-10-29)

Base: `v0.2.0` @ 9498b53 (skeleton green, run 40700637219). Design: ~/Workspaces/mc-story (LORE, GAMEPLAY, MAPA,
NARRATIVA, DIRECTOR, BUILD), read whole. Content is data in story format v1 (docs/plan/milestones.md §6) plus
vanilla commands; no Java unless a blocker appears.

## What fits, in order
1. **Story content, written by me (Opus, lead), 02:00–02:50Z.** Five chapters on the existing map + the Heap (pocket):
   1. `presa` (Overworld, ~6 min): forum line on arrival; lobby → hill spyglass bait → path (lost time, rewritten chat,
      `<nick> no corras…`) → predecessor's base (signs NO USES EL CATALEJO / LA MINA DA VUELTAS / EL RELOJ MIENTE,
      chest with shears) → mine loop (seamless relative teleport, twice) → floor rots → fall into the Heap.
   2. `heap` (~2 min): rotting copy of the island; the spider body (scaled vanilla spider) stalks then chases; caught →
      death screen written by him (`subject connection: still open. retaining (+fear)`), Trial 1.
   3. `puppet` (~3 min): respawn as Puppet in the Heap; threads (tripwire/cobweb) appear; shears that only cut thread;
      `processing` bar; three thread clusters on the Kernel route, each cut leaks a fact (UNALLOCATED pool, PID,
      original spec value) and blinds a sensor; all cut → sample rejected → spat back alive (slow path: time).
   4. `vuelta` (Overworld, ~1–2 min): escalated danger; death returns you as Puppet at the Kernel approach.
   5. `kernel` (~3 min): threshold warning (verbatim), credential rotation if sensors intact, Shell through chat
      (`help`, `ps`, `params`, `kill -STOP <pid>`), silence + watchdog countdown (bossbar), then COMPLETE
      (`set success_threshold <spec>`) / POISON (name into dataset) / CONSUMED (timeout or death inside).
   Endings with LORE §4 texts verbatim. Name rule: typing "reclaimer" earlier → fury line (verbatim) + scare.
   Proof (local): `python3 scripts/story_check.py` 0 errors; unit tests pass.
2. **Rehearsal #1 by ~02:55Z** (CI play phase autoplays all three endings, captures screenshots + video).
   Lane: CI via `~/caspian-kb/tools/rehearse.sh`. I read the captures and the video myself (that is the playthrough).
3. **Fix pass + rehearsal #2 by ~03:40Z.**
4. **Release by 04:15Z:** promote to `v0.2.0` real CI, `release_bundle.py` → `dist/`, itch page text filled
   (description/warnings/credits from LORE §0 and §6 verbatim; team = owner's studio), checklist, report to captain.
   Itch fill can go to an OpenCode worker in parallel once the run id exists; I verify.
5. **Clean install on the laptop before calling it ready (captain, 01:50Z):** install from `dist/` on a clean profile
   following the itch page's own instructions, start the story to the first chapter at least. Harness built now by a
   worker (`MC-laptop-install-test.md`, claude-lab Sonnet high, launched 01:50Z; proven on 9498b53 by 03:15Z); I rerun
   it on the final dist at ~04:00Z and read its screenshots/log. A failure here blocks "ready".
6. If time remains: contract fine print (subject #4114 + spec value) via CONTRACT-TEXT.md; laptop real-client play.

## Lanes
- Me (Opus, USFQ TEAM lab): all story data, verification, release. Codex: only if a Java blocker appears (tight brief).
- OpenCode: mechanical itch-text fill from my values. No other workers: coordination would cost more than it saves.

## Cuts (technical, never the story's endings or arc), in order if time runs short
1. Arrival variants (3 random) → Explorer only (runtime has no random branch). 2. Fake server list / Tab list with
   victims, respawn button labelled "De acuerdo" (Java). 3. Per-world secrets (fixed values instead). 4. Director by
   telemetry (fixed triggers, as DIRECTOR §7 allows). 5. Second deliberate-death loop (one Puppet trip + one return).
6. Contract fine print (spec value lives in the leak instead). Never cut: three endings, first death, Puppet, Kernel.

## Already known deviations (reported to the owner)
- POISON closes to the farewell screen with the degraded cascade instead of leaving the player in the world forever.
- Completar's "reopen message replaces the contract" → shown as the title splash after the ending.
- Wording not given verbatim by the design (Shell output, leak lines) is written only from LORE facts and listed in
  the report for his review.
