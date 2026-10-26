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
