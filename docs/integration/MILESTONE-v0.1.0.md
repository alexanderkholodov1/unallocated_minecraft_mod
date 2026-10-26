# Milestone v0.1.0 integration

This milestone consolidates the work packages in `origin/v0.1.0`. Package branches remain intact;
the coordinator records merge parents after content integration. Builds and game tests run only in CI.
The milestone remains a 0.x foundation, not the owner's complete-game 1.0.0-alpha release.

## Package reconciliation

- `v0.1.1` (`5f82cc5`): scaffold, pinned toolchain, installation documentation and CI were already
  included in the starting tree. Its tip only corrects package version labels; the integrated artifact
  keeps `0.1.0` and its existing installation instructions. No scaffold behavior was dropped.

- `v0.1.3` (`eca0a83`): the full contract behavior and its input/receipt hardening were already merged. Kept typed and clicked agreement, optional data list, first-title interception, UTF-8 repair, replay, focus handling and two-process smoke coverage. Version-only conflicts resolve to `0.1.0`.
