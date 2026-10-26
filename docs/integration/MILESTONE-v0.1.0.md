# Milestone v0.1.0 integration

This milestone consolidates the work packages in `origin/v0.1.0`. Package branches remain intact;
the coordinator records merge parents after content integration. Builds and game tests run only in CI.
The milestone remains a 0.x foundation, not the owner's complete-game 1.0.0-alpha release.

## Package reconciliation

- `v0.1.1` (`5f82cc5`): scaffold, pinned toolchain, installation documentation and CI were already
  included in the starting tree. Its tip only corrects package version labels; the integrated artifact
  keeps `0.1.0` and its existing installation instructions. No scaffold behavior was dropped.

- `v0.1.3` (`eca0a83`): the full contract behavior and its input/receipt hardening were already merged. Kept typed and clicked agreement, optional data list, first-title interception, UTF-8 repair, replay, focus handling and two-process smoke coverage. Version-only conflicts resolve to `0.1.0`.

- `v0.1.6` (`846df01`): added independent include-input hashes, dependency predicates, registry synchronization, exact Minecraft pin, external API floor and all four server/client compatibility profiles. Resolved workflow/build/smoke conflicts by retaining contract first/restart phases and core command/reload checks alongside the matrix. Preserved contract helper methods while adopting dynamic module expectations; updated the watchdog fixture to find its phase by option name. README/toolchain retain installation, contract and core details with the packaging policy. Local validation: 45 Python tests and Appendix C synchronization pass.

- `v0.1.7` (`13271c5`): integrated dormant-world guards, armed JOIN, callback isolation, build arming, rescue cooldown and saved session fixes with all unit tests and the real-player smoke. Core documentation takes the reviewed tip; packaging documentation retains the newer API matrix. Build classpath conflict keeps equivalent split-source wiring. The single title entrypoint runs core checks after contract acceptance on the first phase; restart remains the receipt persistence check. Added explicit admin arming to accepted server profiles. Local validation: 45 Python tests pass.

- `v0.1.14` (`fffb216`): added the complete story/design proposal. The plan, verification notes and work-package document were already identical to this tip. Resolved DECISIONS.md by copying the entire branch file byte for byte, including every owner answer. The proposal remains a proposal; consolidation does not turn unapproved future endings, shaders, audio assets or option mutation choices into new behavior.

- `v0.1.15` (`ff3862c`): integrated image-keyed headless graphics package caching and the installer script, plus Gradle daemon reuse for build and every client phase. Resolved shared core/package files in favor of the already integrated reviewed implementations; retained API matrix properties and two-process contract coverage when adopting daemon reuse. No duplicate core checks or entrypoints. Local validation: 45 Python tests pass; installer remains CI-only.

- `v0.1.4` (`e083a05`): added every interface effect, dedicated mixin namespace, perception primitive, API audit, documentation, unit test and rendered client smoke assertions/screenshots. Unioned language keys with the contract text and registered both mixin configurations. Resolved Gradle/workflow conflicts by keeping the modern packaging checks and adding interface debug flags only to the CI helper run. One title smoke entrypoint sequences core and interface checks after agreement; the Python watchdog requires all six effect captures and restoration evidence. Local validation: 47 Python tests and contract synchronization pass.

- `v0.1.5` (`5b77d83`): integrated the bait figure, view/zoom logic, fallback, screamer, commands, mixins, all unit tests, real-server self-test and four real-client screenshots. Resolved metadata by unioning mixins and keeping one smoke entrypoint; the title helper explicitly invokes the bait scenario after contract/core/interface checks. Folded self-test execution into the fresh-evidence command watchdog, retaining immediate failure detection. Fixed directorCanForceAStepAndTheScare at its cause: successful PLACE no longer throttles the first unseen MOVE; failed MOVE retries remain throttled and now have a regression test. This commit also repairs unresolved merge text accidentally captured when the preceding relay request overlapped preparation of this package; all subsequent relay requests are awaited before edits. Initial local validation found two bait fixtures still calling the old watchdog interface; the follow-up fix adapts them to the fresh-evidence command API. Java tests await CI.

- Bait integration follow-up: retained both console self-test success and immediate-failure fixtures, adapting their old ready/ready_commands calls to startup_required/command_steps. All 49 Python tests now pass, including immediate failure and command-before-stop assertions. No check was removed.

- `v0.1.8` (`14294ca`): integrated all five map sections, biome, authoring API, commands, verification, unit tests and nine-checkpoint client tour. Preserved the tip persistence fix: dedicated mode/difficulty are stored in server.properties as well as level data. Core build-all arming decorates the map command; normal worlds remain dormant. Unified server build/rebuild/bait/status and post-restart map verification; rejection profiles return before world assertions. Unioned map screenshots into the first client phase and retained exact-name/list screenshot checks. Latest package CI 40353707138 passed persistence but timed out in MapTourSmokeTest.waitForChunksRender; added missing-chunk/render diagnostics without weakening the wait. Local validation: 50 Python tests pass.
