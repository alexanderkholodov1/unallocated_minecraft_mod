Hito v0.2.0: the complete jam build of UNALLOCATED.

## What it contains
- Skeleton (packages 0.2.1–0.2.5): story format v1 and its validators, the data-driven story runtime with autoplay,
  the UNALLOCATED title button opening a world template carried in the JAR (plus `UNALLOCATED-world.zip`), the pocket
  dimension (the Heap), copy/corruption, creature bodies, the release kit (itch page template, checklist, bundle script).
- The story (0.2.0.6), from the approved design in the owner's `mc-story` documents: five chapters (presa → heap →
  puppet → vuelta → kernel) with the three endings COMPLETE / POISON / CONSUMED; the contract's data-use list updated
  to be true for this build, with Subject #4114 and the protocol fine print.

## Proof
- Real CI on the final commit: four Fabric API profiles; the `play` phase opens the shipped world from the button and
  plays the shipped story to each of its three endings; release artifact `unallocated-release-<sha>`.
- Clean install on the owner's laptop from the release files following the itch page (harness in mc-inputs).

## Known gaps (technical, reported to the owner)
Explorer arrival only (no random variants), no fake server list / Tab list, fixed (not per-world) secrets, director by
fixed triggers, respawn button keeps vanilla's label, POISON closes to the farewell instead of leaving the game open.
