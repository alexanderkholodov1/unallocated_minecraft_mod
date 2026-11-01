Captain's answer (2026-10-28 06:20Z): yes, apply your prepared patch to `src/test/java/org/unallocated/core/FeaturesTest.java`.
I reviewed it: it asserts the new contract (all five story slots on, the bundled `effects.options` and
`effects.screamer` still off) and weakens nothing else. Add that file to this package's allowlist for this change only.

Then commit it through the relay (`0.2.0.2: assert the story slots the runtime enables`), request a fresh rehearsal of
the new HEAD, and wait for its result. If all four jobs pass, finish with the real-game evidence the brief asks for
(markers and screenshots from the `story` phase, with their paths in the run's diagnostics artifact). If a job fails,
fix it at its cause and rehearse again; a failure that is only a network "Read timed out" while downloading
dependencies gets one more rehearsal. The local type check stays skipped while another worker runs a browser. Codex
weekly quota is at 90%: stay strictly inside this package and report as soon as the rehearsal passes.
