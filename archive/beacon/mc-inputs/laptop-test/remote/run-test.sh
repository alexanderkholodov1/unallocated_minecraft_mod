#!/usr/bin/env bash
# Runs on the laptop (WSL Ubuntu). Usage: run-test.sh <run-name> [install|play|all]
# Everything lives under ~/unallocated-cleantest/; the run directory is fresh per run.
set -uo pipefail
RUN=${1:?run name}; STAGE=${2:-all}
BASE=$HOME/unallocated-cleantest
RUNDIR=$BASE/$RUN; DIST=$RUNDIR/dist; GAME=$RUNDIR/mc; OUT=$RUNDIR/out
CACHE=$BASE/cache; TOOLS=$BASE/tools
DISPLAY_NUM=${DISPLAY_NUM:-77}
WIDTH=854; HEIGHT=480            # the official launcher's default window size
mkdir -p "$OUT" "$CACHE/mc-shared/assets" "$CACHE/mc-shared/libraries"
exec 3>>"$OUT/harness.log"
log() { printf '[%s] %s\n' "$(date -u +%H:%M:%SZ)" "$*" | tee -a "$OUT/harness.log"; }
T0=$(date +%s); declare -A TIMING
mark() { TIMING[$1]=$(( $(date +%s) - T0 )); echo "$1 ${TIMING[$1]}" >> "$OUT/timings.txt"; }
fail() { log "FAIL: $*"; echo "$*" >> "$OUT/failures.txt"; }
xd() { DISPLAY=:$DISPLAY_NUM python3 -I "$TOOLS/xdrive.py" "$@"; }
alive() { kill -0 "$GAME_PID" 2>/dev/null; }
shot() { xd shot "$OUT/$1.png" && log "screenshot $1.png ($(xd stats))"; }

# ---------- values taken from the itch page itself ----------
PAGE=$DIST/itch-page.md
read -r MC LOADER INSTALLER JAVAMAJOR JARNAME PAGEHASH < <(python3 -I - "$PAGE" <<'PY'
import re, sys
t = open(sys.argv[1], encoding="utf-8").read()
mc = re.search(r"exactamente (\S+?)\*\*", t).group(1)
loader = re.search(r"Fabric Loader (\S+) o posterior", t).group(1)
inst = re.search(r"instalador oficial de Fabric\]\([^)]*\) \*\*([\d.]+)\*\*", t).group(1)
java = re.search(r"\*\*Java (\d+)\*\*", t).group(1)
jar = re.search(r"^\| `([^`]+\.jar)` \| El mod", t, re.M).group(1)
h = re.search(r"SHA-256: `([0-9a-f]{64})`", t).group(1)
print(mc, loader, inst, java, jar, h)
PY
)
log "page says: Minecraft=$MC Loader>=$LOADER installer=$INSTALLER java=$JAVAMAJOR jar=$JARNAME"

if [[ $STAGE == install || $STAGE == all ]]; then
  # 0. verify the download as the page instructs (sha256sum on Linux)
  actual=$(sha256sum "$DIST/$JARNAME" | cut -d' ' -f1)
  [[ $actual == "$PAGEHASH" ]] && log "SHA-256 matches the page ($actual)" || { fail "SHA-256 mismatch: page $PAGEHASH file $actual"; exit 1; }
  mark sha_ok

  # 1. Java 25 (Temurin) - the Linux row says to install it if the system has none
  JAVA=$CACHE/jdk$JAVAMAJOR/bin/java
  if [[ ! -x $JAVA ]]; then
    log "downloading Temurin $JAVAMAJOR (cached afterwards)"
    curl -fsSL --retry 3 -o "$CACHE/temurin$JAVAMAJOR.tar.gz" \
      "https://api.adoptium.net/v3/binary/latest/$JAVAMAJOR/ga/linux/x64/jdk/hotspot/normal/eclipse" || { fail "Temurin download"; exit 1; }
    mkdir -p "$CACHE/jdk$JAVAMAJOR" && tar -xzf "$CACHE/temurin$JAVAMAJOR.tar.gz" -C "$CACHE/jdk$JAVAMAJOR" --strip-components=1 || { fail "Temurin extract"; exit 1; }
  fi
  "$JAVA" -version 2>&1 | tee "$OUT/java-version.txt" | head -1 | tee -a "$OUT/harness.log"
  mark java_ready

  # 2. official Fabric installer, client mode, clean game directory
  INST=$CACHE/fabric-installer-$INSTALLER.jar
  [[ -f $INST ]] || curl -fsSL --retry 3 -o "$INST" "https://maven.fabricmc.net/net/fabricmc/fabric-installer/$INSTALLER/fabric-installer-$INSTALLER.jar" || { fail "installer download"; exit 1; }
  mkdir -p "$GAME"
  log "running Fabric installer $INSTALLER: client -mcversion $MC -loader $LOADER -dir $GAME"
  "$JAVA" -jar "$INST" client -dir "$GAME" -mcversion "$MC" -loader "$LOADER" </dev/null > "$OUT/fabric-installer.log" 2>&1
  rc=$?; cat "$OUT/fabric-installer.log" | tail -5 | sed 's/^/  installer: /'
  if (( rc != 0 )) && [[ ! -f $GAME/launcher_profiles.json ]]; then
    log "installer refused (no launcher_profiles.json: the real launcher writes it in page step 1); creating the minimal file and retrying"
    echo "SUBSTITUTION: launcher_profiles.json created by harness" >> "$OUT/substitutions.txt"
    echo '{"profiles":{},"settings":{},"version":3}' > "$GAME/launcher_profiles.json"
    "$JAVA" -jar "$INST" client -dir "$GAME" -mcversion "$MC" -loader "$LOADER" </dev/null > "$OUT/fabric-installer.log" 2>&1; rc=$?
  fi
  (( rc == 0 )) || { fail "Fabric installer exit $rc"; exit 1; }
  VERSION_ID=$(ls "$GAME/versions" | grep '^fabric-loader-' | head -1)
  [[ -n $VERSION_ID ]] || { fail "no fabric version directory written"; exit 1; }
  echo "$VERSION_ID" > "$OUT/version-id.txt"
  log "installer wrote profile version $VERSION_ID"
  mark fabric_installed

  # 3. launcher substitute: vanilla $MC + libraries + assets (assets/libraries are a shared download cache)
  ln -sfn "$CACHE/mc-shared/assets" "$GAME/assets"; ln -sfn "$CACHE/mc-shared/libraries" "$GAME/libraries"
  python3 -I "$TOOLS/launch.py" prepare --game-dir "$GAME" --version-id "$VERSION_ID" 2>&1 | tee -a "$OUT/harness.log" || { fail "launcher prepare"; exit 1; }
  mark vanilla_ready

  # 4. the mod: only the JAR, into mods/
  mkdir -p "$GAME/mods" && cp "$DIST/$JARNAME" "$GAME/mods/" && ls -la "$GAME/mods" | tee "$OUT/mods-dir.txt"
  mark mod_copied
fi

if [[ $STAGE == play || $STAGE == all ]]; then
  JAVA=$CACHE/jdk$JAVAMAJOR/bin/java; VERSION_ID=$(cat "$OUT/version-id.txt")
  LATEST=$GAME/logs/latest.log
  # window geometry -> GUI scale (auto) -> button positions
  SCALE=$(( WIDTH/320 < HEIGHT/240 ? WIDTH/320 : HEIGHT/240 ))
  CX=$(( WIDTH/2 ))
  ACCEPT_Y=$(( HEIGHT - 17*SCALE ))                 # contract: accept button, 27..7px above the bottom (gui px)
  PLAY_Y=$(( ( HEIGHT/SCALE/4 + 48 - 14 ) * SCALE )) # title: UNALLOCATED sits 24 above Singleplayer (row HEIGHT/4+48)

  Xvfb :$DISPLAY_NUM -screen 0 ${WIDTH}x${HEIGHT}x24 -nolisten tcp >"$OUT/xvfb.log" 2>&1 &
  XVFB_PID=$!; sleep 2
  trap 'kill ${GAME_PID:-0} $XVFB_PID 2>/dev/null' EXIT
  alive_x() { kill -0 $XVFB_PID 2>/dev/null; }
  alive_x || { fail "Xvfb did not start"; exit 1; }

  log "starting client (Xvfb :$DISPLAY_NUM ${WIDTH}x${HEIGHT}, software GL, OpenAL null device)"
  T_START=$(date +%s)
  ( unset WAYLAND_DISPLAY XDG_SESSION_TYPE PULSE_SERVER
    export DISPLAY=:$DISPLAY_NUM LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null
    exec python3 -I "$TOOLS/launch.py" run --game-dir "$GAME" --version-id "$VERSION_ID" --java "$JAVA" --width $WIDTH --height $HEIGHT ) \
    </dev/null >"$OUT/game.stdout" 2>&1 &
  GAME_PID=$!

  waitlog() { # pattern timeout
    local end=$(( $(date +%s) + $2 ))
    while (( $(date +%s) < end )); do
      grep -qE -- "$1" "$LATEST" 2>/dev/null && return 0
      alive || return 2
      sleep 1
    done; return 1
  }
  waitstable() { # wait until the frame is non-black and unchanged for 2 consecutive samples
    local end=$(( $(date +%s) + $1 )) prev="" cur mean
    while (( $(date +%s) < end )); do
      cur=$(xd stats); mean=${cur#mean=}; mean=${mean%% *}
      if [[ $cur == "$prev" ]] && awk "BEGIN{exit !($mean > 3)}"; then return 0; fi
      prev=$cur; alive || return 2; sleep 1.5
    done; return 1
  }

  waitlog 'UNALLOCATED initialized \(client\)\.' 240; rc=$?
  (( rc == 0 )) && { mark client_initialized; log "client initialized after $(( $(date +%s)-T_START ))s"; } || fail "client never initialized (rc=$rc)"

  if alive; then
    waitstable 90 && log "frame stable" || log "frame never stabilised"
    # A profile that never ran vanilla shows Minecraft's own Narrator/accessibility welcome first (page step 1
    # makes judges dismiss it in vanilla). Dismiss it here by its Continue button, then wait for the contract.
    CONTINUE_Y=$(( HEIGHT - 16*SCALE - 1 ))
    for i in 1 2 3; do
      [[ $(xd pixel 5 $(( HEIGHT/2 ))) == "17 21 25" ]] && break   # contract background colour 0xFF111519
      shot 00-first-run-welcome-$i; xd click $CX $CONTINUE_Y; log "dismissed Minecraft first-run welcome screen ($i)"; sleep 2; waitstable 30
    done
    [[ $(xd pixel 5 $(( HEIGHT/2 ))) == "17 21 25" ]] || fail "contract screen not detected (background pixel is $(xd pixel 5 $(( HEIGHT/2 )))"
    shot 01-contract
    # a judge types the phrase into the focused field and confirms
    xd type "De acuerdo"; sleep 1; shot 01b-contract-typed
    xd click $CX $ACCEPT_Y; log "clicked accept at $CX,$ACCEPT_Y"; mark contract_accepted
    sleep 6; shot 02-title-button   # the title panorama animates, so no stable-frame wait here
    xd click $CX $PLAY_Y; log "clicked UNALLOCATED at $CX,$PLAY_Y"; T_CLICK=$(date +%s); mark button_clicked
    # connecting/loading screens are short: sample for a while, keep every frame
    for i in 1 2 3 4 5 6 7 8; do sleep 0.7; xd shot "$OUT/seq-$i.png"; done
    cp "$OUT/seq-1.png" "$OUT/03-connecting.png"; cp "$OUT/seq-6.png" "$OUT/03b-loading.png"   # first frame = the mod's "Conectando al servidor…"
    waitlog 'UNALLOCATED story chapter=\S+ slot=LOBBY enter' 180; rc=$?
    if (( rc == 0 )); then mark chapter_entered; log "chapter entered after $(( $(date +%s)-T_CLICK ))s from click"; else fail "no LOBBY chapter entry (rc=$rc)"; fi
    sleep 4; shot 04-in-world
    if alive; then
      for t in $(seq 1 12); do sleep 5; alive || { fail "game died during the 60 s in-world hold at ~$((t*5))s"; break; }; done
      alive && { shot 05-in-world-60s; mark held_60s; log "game still running after 60 s in-world"; }
    else fail "game exited before the in-world hold"; fi
    if alive; then
      xd close | tee -a "$OUT/harness.log"
      for t in $(seq 1 30); do alive || break; sleep 1; done
      if alive; then fail "window close did not stop the game in 30s; sending SIGTERM"; kill "$GAME_PID"; sleep 5; echo "unclean" > "$OUT/exit-kind.txt"
      else echo "clean" > "$OUT/exit-kind.txt"; mark quit_clean; fi
    fi
  fi
  alive && kill -9 "$GAME_PID" 2>/dev/null
  wait "$GAME_PID" 2>/dev/null; echo "game exit status: $?" | tee -a "$OUT/harness.log"
  kill $XVFB_PID 2>/dev/null

  # ---------- evidence + verdict ----------
  cp "$LATEST" "$OUT/latest.log" 2>/dev/null
  cp "$GAME/launcher_profiles.json" "$OUT/launcher_profiles.json" 2>/dev/null
  ls "$GAME/crash-reports" 2>/dev/null > "$OUT/crash-report-names.txt" && cp -r "$GAME/crash-reports" "$OUT/" 2>/dev/null
  ls "$GAME/hs_err_pid"* "$RUNDIR"/hs_err_pid* 2>/dev/null | while read -r f; do cp "$f" "$OUT/"; done
  L=$OUT/latest.log
  chk() { if grep -qE -- "$2" "$L" 2>/dev/null; then echo "PASS  $1"; else echo "FAIL  $1"; fi; }
  {
    chk "log: initialized (common)" 'UNALLOCATED initialized \(common\)\.'
    chk "log: initialized (client)" 'UNALLOCATED initialized \(client\)\.'
    chk "log: story loaded" 'UNALLOCATED story loaded'
    chk "log: chapter entry slot=LOBBY" 'UNALLOCATED story chapter=\S+ slot=LOBBY enter'
    if grep -q 'UNALLOCATED story invalid' "$L" 2>/dev/null; then echo "FAIL  no 'story invalid' (found)"; else echo "PASS  no 'story invalid'"; fi
    if [[ -n $(ls "$OUT/crash-reports" "$OUT"/hs_err_pid* 2>/dev/null) ]]; then echo "FAIL  no crash report (found)"; else echo "PASS  no crash report"; fi
    [[ -f $OUT/timings.txt ]] && grep -q '^chapter_entered' "$OUT/timings.txt" && grep -q '^held_60s' "$OUT/timings.txt" && echo "PASS  game ran 60 s in-world" || echo "FAIL  game ran 60 s in-world"
    [[ $(cat "$OUT/exit-kind.txt" 2>/dev/null) == clean ]] && echo "PASS  clean quit" || echo "FAIL  clean quit"
  } | tee "$OUT/checks.txt"
  # page claims worth confirming
  { [[ -f $GAME/unallocated/contract.properties ]] && echo "page claim OK: unallocated/contract.properties exists" || echo "page claim NOT met: unallocated/contract.properties missing"
    ls "$GAME/saves" 2>/dev/null | sed 's/^/saves\/ has: /'
    python3 -I - "$GAME/saves/UNALLOCATED/level.dat" "$GAME/launcher_profiles.json" <<'PY' 2>&1
import gzip, json, re, sys
try:
    d = gzip.open(sys.argv[1]).read()
    gt = re.search(rb"\x03\x00\x08GameType(.{4})", d, re.S); ac = re.search(rb"\x01\x00\rallowCommands(.)", d, re.S)
    print("page claim", "OK" if gt and int.from_bytes(gt.group(1), "big") == 2 else "NOT met", "- world GameType:", int.from_bytes(gt.group(1), "big") if gt else "?", "(2 = Adventure)")
    print("page claim", "OK" if ac and ac.group(1) == b"\x00" else "NOT met", "- allowCommands:", ac.group(1)[0] if ac else "?", "(0 = cheats off)")
except Exception as e:
    print("world check failed:", e)
try:
    print("installer profile name(s):", [p.get("name") for p in json.load(open(sys.argv[2]))["profiles"].values()])
except Exception as e:
    print("profile check failed:", e)
PY
    grep -E '^\[[0-9:]+\] \[[^]]*/(ERROR|FATAL)\]' "$L" 2>/dev/null | cut -c1-160 | sort | uniq -c | sed 's/^/ERROR line (x count): /'
    grep -E '/(WARN|ERROR)\]: UNALLOCATED' "$L" 2>/dev/null | cut -c1-200 | sed 's/^/mod warning\/error: /'; } | tee "$OUT/extra-checks.txt"
  grep -q '^FAIL' "$OUT/checks.txt" && echo FAIL > "$OUT/verdict.txt" || echo PASS > "$OUT/verdict.txt"
  log "VERDICT: $(cat "$OUT/verdict.txt")"
fi
