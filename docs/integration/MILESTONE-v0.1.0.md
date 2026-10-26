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

- `v0.1.8` (`14294ca`): integrated all five map sections, biome, authoring API, commands, verification, unit tests and nine-checkpoint client tour. Preserved the tip persistence fix: dedicated mode/difficulty are stored in server.properties as well as level data. Core build-all arming decorates the map command; normal worlds remain dormant. Unified server build/rebuild/bait/status and post-restart map verification; rejection profiles return before world assertions. Unioned map screenshots into the first client phase and retained exact-name/list screenshot checks. Latest package CI 40353707138 passed persistence but timed out in MapTourSmokeTest.waitForChunksRender; added missing-chunk/render diagnostics without weakening the wait. Local validation: 51 Python tests pass.

- `v0.1.10` (`97d7acc`): registered EPILOGUE before core initialization, installed ending client hooks and mixins, and integrated save-before-close, terminal reload, idempotence, farewell/splash and all unit/client tests. Common/client initializers retain map/core/contract wiring. The ending fixtures now explicitly arm disposable worlds to obey the reviewed core guard; core-only terminal transitions use no participant so the separate real-player ending test owns disconnect verification. The integrated farewell must quote the actual typed agreement, with options/receipt unchanged. Retained all four ending screenshots and both completion markers in the single first-phase orchestration. Local validation: 52 Python tests pass.

- `v0.1.11` (`23325de`): integrated every registered cue, alias/subtitle, network payload, command, client lifecycle cleanup, attenuation mixin and audio smoke/packaging fixtures. Unioned common/client initializers, language keys, all mixins and fatal-error patterns; retained independent include hashes and the API matrix. Audio checks run in the same first client process after the other packages. Fixed authored gain/pitch failure at the final resolved sound boundary: AbstractSoundInstance multiplies layer fields by vanilla sample values, so playback now clamps resolved gain to its current fade envelope and pitch to 0.5–2.0. The real-engine check now also enforces each layer gain, finite values and the upper pitch bound. README describes the integrated 0.1.0 artifact. Local validation: 55 Python tests and contract synchronization pass.

- CI follow-up: run 40356016322 exposed a real-server self-test race: addFreshEntity can precede chunk/entity visibility, so the self-test now waits across ticks for that exact entity before asserting pose/save/move/removal, with the original bounded timeout and cleanup. Runs 40356393129/40356646310 exposed incomplete cached Debian dependency closures; the graphics installer still tries the fast cache first and resolves missing packages from official Ubuntu on a miss/failure. Deduplicated the identical registry-sync declarations introduced by independent packaging/audio additions. Local validation: all source JSON has unique keys, shell syntax passes, and 55 Python tests pass.

- Final wiring audit reconstructed metadata from every package and found a duplicate-key merge had hidden the contract/interface/bait mixin registrations when ending was added. Restored all six production configurations with correct sides. Added source JSON uniqueness, exact production config registration, single smoke orchestrator and packaged unregistered/duplicate config checks so the failure cannot silently recur. Every package-added path is present; DECISIONS.md still matches v0.1.14 byte for byte. Local validation: 60 Python tests and contract synchronization pass.

- Documentation reconciliation: README now describes the actual integrated feature set, manual authoring/test entrypoints, single orchestrated client, four-profile CI, cache behavior, credits and remaining full-game work. Toolchain/core notes distinguish dated package evidence from the integrated seven-module/hash policy; ending fixtures document arming. Restored the interface branch safeguard rejecting helper accessor classes in the player JAR, with a leakage regression fixture. No intent document or owner answer changed. Local validation: 61 Python tests and contract synchronization pass.

- Rehearsal correction (`0.1.0.20`): run 40358823036 localized the remaining client timeout to
  the map tour's download predicate. Fabric's square wait conflicts with vanilla 26.3's rounded
  radius-8 tracking view; the four missing corners are deliberately not streamed. The tour now
  requires every chunk in the real server tracking view (329, including the outer buffer),
  verifies radius/center, and retains the full light/render completion wait at every checkpoint.
  Added direct vanilla footprint regression tests. All package behavior and screenshots remain.
  Verification uses the coordinator's rehearsal relay; no unverified commit is pushed to the hito.

- `0.1.0.21`: rehearsal 40361615824 passed the corrected map tour in all three accepted
  profiles, then exposed its temporary radius-8 setting leaking into the audio scenario.
  The tour now restores render distance, bobbing and FOV in `finally`; no audio wait or
  product behavior is removed. The rehearsal also exposed a separate ending disconnect
  reason publication race, addressed in the next fix before another rehearsal.

- Local validation for 0.1.0.21: 61 Python tests and 70 subtests pass. Rehearsal remains required on the final fix commit; none is pushed to the hito.

- `0.1.0.22`: rehearsal 40361615824 exposed a vanilla 26.3 disconnect race: channel closure
  becomes visible before its reason field is assigned, allowing the render thread to consume
  the generic fallback. Publish only UNALLOCATED's tagged ending reason before closing; keep
  ordinary disconnects untouched. A helper-only injection probes the real close window on both
  ending paths, so the fix is asserted deterministically alongside all farewell/save checks.
  Expanded helper leakage checks to cover the new dedicated ending smoke namespace.

- Local validation for 0.1.0.22: 61 Python tests and 73 subtests pass; contract text and diff whitespace checks pass. Read the fix against rehearsal 40361615824 and official 26.3 disconnect bytecode. No local Java/Gradle/game. Final rehearsal required before delivery.

- `0.1.0.23`: rehearsal 40363658359 passed map and ending (including the deterministic
  close-window probe) in all accepted profiles, then exposed retired ambient sounds
  receiving another engine tick after vanilla stop() clears their looping flag. That
  mistakenly selected a zero-duration finite fade and produced negative volume. Stopped
  layers now remain silent/inert; fades use the authored cue and bounded factors. The
  client smoke deliberately late-ticks all retired layers and asserts zero gain.

- Local validation for 0.1.0.23: 61 Python tests and 73 subtests pass, contract synchronization and diff checks pass. Reviewed against the exact negative-volume failure in rehearsal 40363658359 and vanilla stop() bytecode. All previous map/ending fixes retained; final four-profile rehearsal required.
