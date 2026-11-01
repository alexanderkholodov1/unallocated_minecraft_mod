#!/bin/bash
# javacheck.sh [worktree] — compile every Java source of an UNALLOCATED worktree against the real Minecraft 26.3,
# Fabric Loader 0.20.1, Mixin, Fabric API 0.165.0+26.3 (incl. game-test modules) and JUnit, with JDK 25. It only
# type-checks (no jar, no Gradle, no game run): use it before every commit; CI remains the proof that the game starts.
# Light on purpose: one run at a time on this machine (lock), heap capped, output under <worktree>/.tmp/javacheck.
set -u
wt=$(cd "${1:-.}" && pwd); R=$(cd "$(dirname "$0")" && pwd)
# The shared lock lives outside sandboxed worktrees; if it cannot be opened, the memory check below still protects us.
if exec 9>"$R/.javacheck.lock" 2>/dev/null; then flock -w 600 9 || { echo "javacheck: another check is running; try again" >&2; exit 75; }; fi
avail=$(awk '/MemAvailable/{print int($2/1024)}' /proc/meminfo)
[ "$avail" -ge 900 ] || { echo "javacheck: only ${avail} MB available; wait a minute and retry" >&2; exit 75; }
# A browser test or the Firestore emulator needs the memory javac would take. The bracketed first letters keep pgrep
# from matching the shell that runs this script (a plain pattern matches its own command line and always refuses).
if pgrep -f '[c]hrom|[f]irestore-emulator' >/dev/null; then
  echo "javacheck: a browser or the Firestore emulator is running; try again in a few minutes" >&2; exit 75
fi
cp="$R/client-26.3.jar:$R/fabric-loader-0.20.1.jar:$R/fabric-client-gametest-api-v1-6.0.11+131f8d475d.jar:$R/fabric-gametest-api-v1-4.0.35+3434d6d95d.jar"
for j in "$R"/libs/*.jar; do cp="$cp:$j"; done
out="$wt/.tmp/javacheck"; rm -rf "$out"; mkdir -p "$out"
find "$wt/src" -name '*.java' > "$out/sources.txt"
n=$(wc -l < "$out/sources.txt")
TMPDIR="$out" "$R/jdk25/bin/javac" -J-Xmx448m -J-Djava.io.tmpdir="$out" -proc:none -encoding UTF-8 --release 25 \
  -Xmaxerrs 200 -Xlint:removal -nowarn -d "$out/classes" -cp "$cp" @"$out/sources.txt" 2>&1 | sed "s#$wt/##"
rc=${PIPESTATUS[0]}
echo "javacheck: $n source files, javac exit $rc ($([ $rc = 0 ] && echo OK || echo ERRORS))"
exit $rc
