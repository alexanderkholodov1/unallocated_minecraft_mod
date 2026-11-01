#!/usr/bin/env bash
# Clean-install test of the UNALLOCATED release files on the owner's laptop (WSL Ubuntu).
# Run from Beacon (nothing heavy runs here):
#   laptop-install-test.sh <dist-dir> [--stage all|install|play] [--run NAME]
# <dist-dir> = output of scripts/release_bundle.py (jar, .sha256, world zip, itch-page.md).
# Results: ~/Workspaces/mc-inputs/laptop-test/results/<run>/ (PNGs, latest.log, RESULT.md, ...).
# Exit status: 0 = PASS, 1 = FAIL, 3 = harness/infrastructure error.
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
DIST=${1:?usage: $0 <dist-dir> [--stage all|install|play] [--run NAME]}; shift
STAGE=all; RUN=run-$(date -u +%Y%m%dT%H%M%SZ)
while (( $# )); do case $1 in --stage) STAGE=$2; shift 2;; --run) RUN=$2; shift 2;; *) echo "bad arg $1" >&2; exit 3;; esac; done
DIST=$(cd "$DIST" && pwd)
RESULTS=$HERE/results/$RUN
SSH=(ssh -o ServerAliveInterval=15 -o ServerAliveCountMax=40 -o LogLevel=ERROR ak-laptop)
WSL='wsl -d Ubuntu -e bash -l -c'
mkdir -p "$RESULTS"

if [[ $STAGE == all || $STAGE == install ]]; then
  jar=$(cd "$DIST" && ls unallocated-*.jar | head -1)
  for f in "$jar" "$jar.sha256" UNALLOCATED-world.zip itch-page.md; do [[ -f $DIST/$f ]] || { echo "missing $DIST/$f" >&2; exit 3; }; done
  echo "== copying release files to a fresh laptop run directory ~/unallocated-cleantest/$RUN"
  tar -C "$DIST" -cf - "$jar" "$jar.sha256" UNALLOCATED-world.zip itch-page.md \
    | "${SSH[@]}" "$WSL \"mkdir -p ~/unallocated-cleantest/$RUN/dist && tar -C ~/unallocated-cleantest/$RUN/dist -xf -\"" || exit 3
fi
echo "== installing harness tools"
tar -C "$HERE/remote" -cf - xdrive.py launch.py run-test.sh \
  | "${SSH[@]}" "$WSL \"mkdir -p ~/unallocated-cleantest/tools && tar -C ~/unallocated-cleantest/tools -xf -\"" || exit 3

echo "== running stage '$STAGE' on the laptop (foreground)"
START=$(date +%s)
"${SSH[@]}" "$WSL \"bash ~/unallocated-cleantest/tools/run-test.sh $RUN $STAGE\"" </dev/null 2>&1 | tee "$RESULTS/console.log"
echo "== elapsed $(( $(date +%s) - START ))s; copying evidence back"
"${SSH[@]}" "$WSL \"tar -C ~/unallocated-cleantest/$RUN/out -cf - .\"" | tar -C "$RESULTS" -xf - || { echo "copy-back failed" >&2; exit 3; }

python3 -I "$HERE/make_result.py" "$RESULTS" "$DIST" "$RUN" "$STAGE"
verdict=$(cat "$RESULTS/verdict.txt" 2>/dev/null || echo NONE)
echo "== VERDICT: $verdict  (see $RESULTS/RESULT.md)"
[[ $verdict == PASS ]] && exit 0 || { [[ $verdict == FAIL ]] && exit 1 || exit 3; }
