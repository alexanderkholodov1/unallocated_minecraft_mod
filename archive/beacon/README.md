# Beacon local archive (2026-11-01)

Everything about the mod that existed only on the Beacon machine, copied as it was, so other machines can reach it.

| Folder here | Source on Beacon | What it is |
|---|---|---|
| `mc-story/` | `~/Workspaces/mc-story` | The designer session: the owner-approved design (DISENO, LORE, GAMEPLAY, MAPA, NARRATIVA, DIRECTOR, BUILD), NOTAS, intent/, the unsent draft, the session brief (CLAUDE.md) |
| `mc-inputs/` | `~/Workspaces/mc-inputs` | The captain/lead records: briefs, plans, reports, dispatches, steers, rehearsal logs, CI evidence, release bundles (JAR, world zip, video, screenshots), the laptop install test |
| `mc-hito-tmp/` | `~/Workspaces/mc-hito/.tmp` (worktree of branch v0.2.0) | Worker scratch: diagnostics, smoke runs, release builds, story evidence, helper scripts, logs |
| `mc-ref/` | `~/Workspaces/mc-ref` | Only the scripts and metadata |

Left out on purpose:
- Mojang's Minecraft client JAR and its disassembly (`mc-ref/client-26.3.jar`, `mc-hito/.tmp/chunk-audit/client.jar`,
  `chunk-audit/*disassembly.txt`). They can't be redistributed in a public repository; the Gradle build downloads them.
- The JDK, Fabric and library JARs in `mc-ref/` (`jdk25/`, `libs/`, `fabric-*.jar`). These are downloaded again.
- Python packages vendored for tests (`check-python/`, `chunk-audit/python/`), `__pycache__`, `.gradle`, `.pytest_cache`.

To fetch only this branch: `git fetch origin archive/beacon-local-2026-11-01 && git switch archive/beacon-local-2026-11-01`.
The curated copies of the design and records are also on `main` under `docs/`.
