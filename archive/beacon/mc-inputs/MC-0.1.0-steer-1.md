Coordinator (Beacon captain), steering this same session. Your integration of the 12 packages is good work and it is
now recorded: commit 529e068 lists every package as a merge parent, and the old package branches are retired, so GitHub
holds only `main` and `v0.1.0`. Two things changed since your brief; read them before anything else.

## 1. How you verify now (the owner's structural rule, 2026-10-26)
Your commits were pushed to `v0.1.0` one by one and every red CI run sent the owner an email: 47 failed runs today. He
wants "NI UN SOLO ERROR" in any project, achieved by verifying before the real CI, not by hiding failures. So:
- Nothing of yours is pushed to `v0.1.0` any more. The real CI there runs only on commits that already passed a
  rehearsal. Commit 9e62208 (`0.1.0.19`) made the workflow run only on `main`, `v*` and pull requests, plus
  `rehearsal/**`, where jobs are `continue-on-error` and record a verdict.
- To rehearse: commit through the relay as before, then write `.tmp/rehearse-requests/<name>.json` (content
  `{"note":"what changed"}`). A shell relay pushes your HEAD to a temporary rehearsal branch, waits for the real CI and
  writes `.tmp/rehearse-results/<name>.json` (`pass`, `run`) and `<name>.log`, which holds the verdict per job and the
  last 150 log lines of each failed job. A rehearsal takes about 5-10 minutes; wait for the result file (check it every
  minute or so), and run `gh run view <run> --log` yourself if you need more.
- Before requesting a rehearsal, run every check that can run here: `python3 -m pytest scripts` (or the individual
  `scripts/test_*.py`) and read your diff once more against the failure. Still never Gradle or the game locally.
- A fix must address the cause. Never weaken, skip or loosen a check to make it pass. A test that is flaky is a defect:
  find why it is nondeterministic and fix that.

## 2. The current failure (rehearsal of 9e62208, run 40358823036)
- `build (0.153.1+26.3)`: passes (the Loader rejects the old Fabric API as designed).
- `build (none)`, `build (0.161.0+26.3)`, `build (0.165.0+26.3)`: the build, unit tests and the dedicated server smoke
  pass, then `:runClientSmoke` fails after about 2.5-3 minutes: `java.lang.AssertionError: Timed out waiting for
  predicate` from the client gametest thread (`ThreadingImpl.runTestThread`), right after the integrated server saved
  its chunks. So one of the waits in the single orchestrated client smoke never becomes true in the integrated build.
  Find which predicate (the smoke diagnostics artifact `smoke-diagnostics-<sha>-<api>` has the client logs and
  screenshots: `gh run download 40358823036 -n 'smoke-diagnostics-9e62208458fb40610d58bd1ef8f5db5518675cd0-none'
  -D .tmp/diag`), why the integration made it unreachable (a package's precondition the others change: contract,
  interface effects, bait, map, ending or audio), and fix it so every package's client behaviour is still asserted.

## Done
A rehearsal that passes all four jobs on your final commit. Then stop and report in English: the cause, the fix
(file:line), every rehearsal run id with its result, and anything you could not decide. Commit messages continue at
`0.1.0.20`, no AI attribution. Do not push, open PRs, or touch other worktrees.
