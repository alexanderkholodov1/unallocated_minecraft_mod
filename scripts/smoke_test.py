"""CI-only real-game smoke checks. Never invoke Java/Gradle on the scaffold host."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import time
import threading
from urllib.request import Request, urlopen
from zipfile import ZipFile

from verify_jar import matches


ROOT = Path(__file__).resolve().parents[1]
SMOKE = ROOT / '.tmp' / 'smoke'
FATAL = re.compile(
    r'Encountered an unexpected exception|Exception in thread|'
    r'A fatal error has been detected|Crash report saved|Mixin apply.*failed|UNALLOCATED map command failed|UNALLOCATED story invalid|UNALLOCATED story build failed|'
    r'Failed to load sound|Unable to play (?:unknown|empty) soundEvent|Error loading sound', re.I
)
# In-game feature self-tests report failure in the game log; fail at once instead of waiting for the deadline.
SELFTEST_FAILED = re.compile(r'UNALLOCATED bait selftest FAILED[^\r\n]*')


API_POLICY = json.loads((ROOT / 'fabric-api-modules.json').read_text())
API_VERSION = API_POLICY['fabric_api_version']


def java_executable():
    """Resolve the CI-selected JDK without ever falling back to PATH's Java."""
    configured = os.environ.get('JAVA_HOME', '').strip()
    if not configured:
        raise RuntimeError('JAVA_HOME is required for smoke checks; select a Java 25 JDK')
    home = Path(configured).expanduser().resolve()
    try:
        release = (home / 'release').read_text()
    except OSError as error:
        raise RuntimeError(f'JAVA_HOME must point to a Java 25 JDK: cannot read {home / "release"}') from error
    version = re.search(r'^JAVA_VERSION="(\d+)(?:[.+_-][^"\r\n]*)?"$', release, re.M)
    if version is None or version[1] != '25':
        raise RuntimeError(f'JAVA_HOME must point to Java 25; found {version[1] if version else "no valid JAVA_VERSION"} in {home / "release"}')
    executable = home / 'bin' / 'java'
    if not executable.is_file() or not os.access(executable, os.X_OK):
        raise RuntimeError(f'JAVA_HOME has no executable Java 25 launcher: {executable}')
    return str(executable)


def verify_mod_directory(directory, allowed):
    actual = {path.name for path in directory.iterdir() if path.is_file()}
    if actual != allowed:
        raise RuntimeError(f'Mod directory differs from clean installation: {actual} != {allowed}')


def api_version_argument(value):
    if value in (None, 'none', False):
        return None
    if value is True:  # Backwards-compatible pinned-version smoke entry point.
        return API_VERSION
    if value not in API_POLICY['smoke_api_versions']:
        raise ValueError('Unreviewed Fabric API smoke version: ' + str(value))
    return value


def profile_name(side, api_version):
    return side if api_version is None else f'{side}-api-{api_version}'


def expects_rejection(api_version):
    return api_version is not None and matches(
        api_version, '<' + API_POLICY['minimum_external_api_version'])


def rejection_evidence(api_version):
    boundary = re.escape(API_POLICY['minimum_external_api_version'])
    # Loader 0.20.1 Messages.properties: resolution.breaks.invalid / resolution.version.less.
    return [r'Incompatible mods found!',
            rf'(?im)^[^\r\n]*\(unallocated\)[^\r\n]*is incompatible with '
            rf'[^\r\n]*before {boundary}[^\r\n]*\(fabric-api\)'
            rf'[^\r\n]*{re.escape(api_version)}',
            r'(?i)remove|replace']


REJECTION_CRASH = re.compile(
    r'MixinApplyError|MixinTransformerError|Mixin apply.*failed|ClassNotFoundException|'
    r'NoClassDefFoundError|A fatal error has been detected|Crash report saved', re.I)
GAME_STARTED = re.compile(r'UNALLOCATED initialized|Done \([^\r\n]*\)!|'
                          r'UNALLOCATED smoke: title screen reached')


def server_evidence(with_fabric_api=False):
    api_version = api_version_argument(with_fabric_api)
    required = [r'UNALLOCATED initialized \(common\)\.', r'Done \([^\r\n]*\)!']
    required.extend(
        rf'\b{re.escape(module_id)}\s+{re.escape(module["version"])}(?:\s|$)'
        for module_id, module in API_POLICY['modules'].items()
        if module['environment'] != 'client'
    )
    if api_version:
        required.append(rf'\bfabric-api\s+{re.escape(api_version)}(?:\s|$)')
    return required


def properties():
    return dict(
        line.split('=', 1) for line in (ROOT / 'gradle.properties').read_text().splitlines()
        if line and not line.startswith('#') and '=' in line
    )


def read(path):
    return path.read_text(errors='replace') if path.is_file() else ''


def crash_reports(directory):
    return [p for p in directory.rglob('*') if p.is_file() and (
        'crash-reports' in p.parts or re.fullmatch(r'hs_err_pid\d+\.log', p.name)
    )]


def monitor(command, cwd, console, required, timeout, stop_when_ready=False,
            shutdown_timeout=30, game_directory=None, command_steps=(), startup_required=None,
            expected_rejection=None, on_log=None):
    """Require clean game evidence or a specific pre-startup Loader dependency rejection."""
    if Path(command[0]).name == 'gradle':
        # Gradle's launcher reads JAVA_HOME; pin its daemon to that same validated JDK.
        home = str(Path(java_executable()).parent.parent)
        command = [command[0], f'-Dorg.gradle.java.home={home}', *command[1:]]
    console.parent.mkdir(parents=True, exist_ok=True)
    game_directory = game_directory or cwd
    latest = game_directory / 'logs' / 'latest.log'

    def reports():
        found = crash_reports(game_directory)
        if cwd != game_directory:
            found.extend(cwd.glob('hs_err_pid*.log'))
        return found

    crash_pattern = REJECTION_CRASH if expected_rejection else FATAL
    deadline = time.monotonic() + timeout
    stop_sent = False
    step_index = 0
    step_sent = False
    step_log_start = 0
    process = None
    try:
        with console.open('w') as output:
            process = subprocess.Popen(
                command, cwd=cwd, stdin=subprocess.PIPE, stdout=output,
                stderr=subprocess.STDOUT, text=True, start_new_session=True,
            )
            while process.poll() is None:
                game_log = read(latest)
                if on_log is not None: on_log(game_log)
                if reports() or crash_pattern.search(game_log + read(console)):
                    raise RuntimeError('Game crash detected')
                failed = SELFTEST_FAILED.search(game_log)
                if failed:
                    raise RuntimeError(f'In-game self-test failed: {failed.group(0)}')
                ready = all(re.search(pattern, game_log) for pattern in (startup_required or required))
                if ready and step_index < len(command_steps):
                    command, evidence = command_steps[step_index]
                    if not step_sent:
                        step_log_start = len(game_log)
                        process.stdin.write(command + '\n')
                        process.stdin.flush()
                        step_sent = True
                    elif all(re.search(pattern, game_log[step_log_start:]) for pattern in evidence):
                        step_index += 1
                        step_sent = False
                if stop_when_ready and not stop_sent and step_index == len(command_steps) and all(
                    re.search(pattern, game_log) for pattern in required
                ):
                    process.stdin.write('stop\n')
                    process.stdin.flush()
                    stop_sent = True
                    deadline = time.monotonic() + shutdown_timeout
                    print('Server reached Done with the mod loaded; sent stop.', flush=True)
                if time.monotonic() >= deadline:
                    raise RuntimeError('Timed out waiting for game startup/test or clean shutdown')
                time.sleep(0.2)
            if expected_rejection and process.returncode == 0:
                raise RuntimeError('Expected Loader rejection but command exited successfully')
            if not expected_rejection and process.returncode != 0:
                raise RuntimeError(f'Game command exited with status {process.returncode}')
        game_log = read(latest)
        if expected_rejection:
            rejection_log = game_log + '\n' + read(console)
            missing = [pattern for pattern in rejection_evidence(expected_rejection)
                       if not re.search(pattern, rejection_log)]
            if missing:
                raise RuntimeError(f'Missing specific Loader incompatibility evidence: {missing}')
            if GAME_STARTED.search(rejection_log) or reports() or crash_pattern.search(rejection_log):
                raise RuntimeError('Game started or crashed instead of Loader dependency rejection')
            print(f'PASS: Loader rejected Fabric API {expected_rejection} before game startup', flush=True)
            return
        if step_index != len(command_steps):
            raise RuntimeError('Server exited before command evidence was acknowledged')
        missing = [pattern for pattern in required if not re.search(pattern, game_log)]
        if missing:
            raise RuntimeError(f'Missing required game log evidence: {missing}')
        if stop_when_ready and (not stop_sent or 'Stopping server' not in game_log):
            raise RuntimeError('Server did not acknowledge our stop command and exit cleanly')
        if reports() or crash_pattern.search(game_log + read(console)):
            raise RuntimeError('Game crash detected after exit')
        print(f'PASS: real-game smoke check in {game_directory.relative_to(ROOT)}', flush=True)
    except BaseException:
        print(f'--- Smoke console: {console} ---\n' + '\n'.join(read(console).splitlines()[-80:]), file=sys.stderr)
        print(f'--- Game log: {latest} ---\n' + '\n'.join(read(latest).splitlines()[-80:]), file=sys.stderr)
        for report in reports():
            print(f'Crash report: {report}', file=sys.stderr)
        raise
    finally:
        if process is not None:
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait(timeout=5)
            if process.stdin is not None:
                process.stdin.close()


def dump_gradle_threads(directory):
    """Capture every Gradle JVM before terminating a silent build, including detached daemons."""
    directory.mkdir(parents=True, exist_ok=True)
    jcmd = str(Path(java_executable()).with_name('jcmd'))
    pids = []
    # Preserve daemon logs alongside stacks, keeping diagnostics at one worktree-relative path.
    gradle_home = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle')))
    for source in (gradle_home / 'daemon').glob('*/daemon-*.out.log'):
        shutil.copyfile(source, directory / (source.parent.name + '-' + source.name))
    with (directory / 'jvms.txt').open('w') as output:
        try:
            listing = subprocess.run([jcmd, '-l'], capture_output=True, text=True, timeout=30)
            output.write(listing.stdout + listing.stderr)
            for line in listing.stdout.splitlines():
                if 'gradle' in line.lower() and line.split()[0].isdigit():
                    pid = int(line.split()[0]); pids.append(pid)
                    with (directory / f'gradle-{pid}-threads.txt').open('w') as stacks:
                        try:
                            subprocess.run([jcmd, str(pid), 'Thread.print'], stdout=stacks,
                                           stderr=subprocess.STDOUT, timeout=30)
                        except (OSError, subprocess.TimeoutExpired) as error:
                            stacks.write(str(error))
        except (OSError, subprocess.TimeoutExpired) as error:
            output.write(str(error))
    # ss emits socket rows followed by indented TCP details. Keep both for Gradle PIDs only.
    with (directory / 'gradle-tcp.txt').open('w') as output:
        try:
            sockets = subprocess.run(['ss', '-tnpi'], capture_output=True, text=True, timeout=30)
            keep = False
            for line in sockets.stdout.splitlines():
                if line and not line[0].isspace():
                    keep = any(re.search(r'\bpid=' + str(pid) + r'[,)]', line) for pid in pids)
                if keep: output.write(line + '\n')
            if sockets.stderr: output.write(sockets.stderr)
        except (OSError, subprocess.TimeoutExpired) as error:
            output.write(str(error))
    if any(re.search(r'org\.gradle\..*(?:DownloadAction|ExternalResource|HttpClientHelper|HttpResourceAccessor)',
                     read(path)) for path in directory.glob('gradle-*-threads.txt')):
        print('UNALLOCATED Gradle watchdog: dependency download stalled (infrastructure)', flush=True)
    return pids


def watch_gradle(command, console, silence=720):
    """A complete output line resets the watchdog; partial bytes do not mask a hang."""
    if Path(command[0]).name == 'gradle':
        home = str(Path(java_executable()).parent.parent)
        command = [command[0], f'-Dorg.gradle.java.home={home}', *command[1:]]
    console.parent.mkdir(parents=True, exist_ok=True)
    process = None
    try:
        with console.open('wb') as output, console.open('rb') as incoming:
            process = subprocess.Popen(command, cwd=ROOT, stdout=output,
                                       stderr=subprocess.STDOUT, start_new_session=True)
            last_line = time.monotonic()
            pending = b''
            while True:
                pending += incoming.read()
                if b'\n' in pending:
                    lines, pending = pending.rsplit(b'\n', 1)
                    print(lines.decode(errors='replace'), flush=True)
                    last_line = time.monotonic()
                if process.poll() is not None:
                    if pending: print(pending.decode(errors='replace'), flush=True)
                    if process.returncode:
                        raise RuntimeError(f'Gradle exited with status {process.returncode}')
                    return
                if time.monotonic() - last_line >= silence:
                    print('UNALLOCATED Gradle watchdog: no output for 12 minutes; collecting thread stacks', flush=True)
                    pids = dump_gradle_threads(console.parent / 'gradle-diagnostics')
                    for pid in pids:
                        try: os.kill(pid, signal.SIGTERM)
                        except ProcessLookupError: pass
                    raise RuntimeError('Gradle no-output watchdog expired')
                time.sleep(.2)
    finally:
        if process is not None and process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
            try: process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL); process.wait(timeout=5)


def server(with_fabric_api=False):
    java = java_executable()
    api_version = api_version_argument(with_fabric_api)
    props = properties()
    artifact = ROOT / 'build' / 'libs' / f"unallocated-{props['version']}.jar"
    expected = hashlib.sha256(artifact.read_bytes()).hexdigest()
    name = profile_name('server', api_version)
    directory = SMOKE / name
    # Fresh directories prevent a stale Done/log line from producing a false pass.
    directory.mkdir(parents=True, exist_ok=False)
    (directory / 'mods').mkdir()
    installed = directory / 'mods' / artifact.name
    shutil.copyfile(artifact, installed)
    assert hashlib.sha256(installed.read_bytes()).hexdigest() == expected
    if api_version:
        api = ROOT / 'build/smoke/full-api' / f"fabric-api-{api_version}.jar"
        shutil.copyfile(api, directory / 'mods' / api.name)
    verify_mod_directory(directory / 'mods', {artifact.name} | (
        {api.name} if api_version else set()))
    (directory / 'eula.txt').write_text('# Accepted only for this disposable CI smoke test.\neula=true\n')
    (directory / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25565\nonline-mode=false\n'
        'level-name=smoke-world\nlevel-type=minecraft:flat\n'
        'generator-settings={"layers":[{"block":"minecraft:air","height":1}],"biome":"unallocated:lobby","structure_overrides":[]}\n'
        'generate-structures=false\nview-distance=2\nsimulation-distance=2\n'
        'max-players=1\nsync-chunk-writes=false\npause-when-empty-seconds=-1\n'
    )
    temp = directory / 'temp'
    temp.mkdir()
    # Cache only distribution files. Worlds, mods, logs, and Loader's processed mod
    # cache remain private to each fresh smoke installation, including coexistence.
    downloads = ROOT / '.tmp' / 'smoke-downloads' / (
        f"{props['minecraft_version']}-{props['loader_version']}-1.1.2"
    )
    downloads.mkdir(parents=True, exist_ok=True)
    (directory / '.fabric').mkdir()
    for relative in ('.fabric/server', 'libraries', 'versions'):
        shared = downloads / relative
        shared.mkdir(parents=True, exist_ok=True)
        (directory / relative).symlink_to(shared, target_is_directory=True)
    url = (
        'https://meta.fabricmc.net/v2/versions/loader/'
        f"{props['minecraft_version']}/{props['loader_version']}/1.1.2/server/jar"
    )
    print(f'Using pinned official Fabric server launcher (download on cache miss): {url}', flush=True)
    launcher = downloads / 'fabric-server-launch.jar'
    for attempt in range(2):
        try:
            if not launcher.is_file():
                with urlopen(Request(url, headers={'User-Agent': 'UNALLOCATED-CI'}), timeout=30) as response:
                    launcher.write_bytes(response.read())
            with ZipFile(launcher) as jar:
                assert jar.testzip() is None, 'Corrupt Fabric server launcher'
            break
        except Exception:
            launcher.unlink(missing_ok=True)
            if attempt == 1:
                raise
    print(f'Installed player JAR SHA-256: {expected}', flush=True)
    command = [java, '-Xms256M', '-Xmx1G', '-Djava.awt.headless=true', f'-Djava.io.tmpdir={temp}',
               '-Dunallocated.story.dir=' + str(ROOT / 'src/clientSmoke/resources/story-regression'),
               '-jar', str(launcher), 'nogui']
    startup = server_evidence(with_fabric_api)
    entered = r'UNALLOCATED act=LOBBY enter t=0'
    status = ''
    if not expects_rejection(api_version):
        story_dir = ROOT / 'src/clientSmoke/resources/story-regression'
        story = json.loads((story_dir / 'story.json').read_text())
        chapters = [json.loads((story_dir / 'chapters' / f'{name}.json').read_text()) for name in story['chapters']]
        startup = server_evidence(with_fabric_api) + [rf'UNALLOCATED story loaded chapters={len(chapters)} beats={sum(len(c["beats"]) for c in chapters)} endings={len(story["endings"])} format=1']
        console_prefix = 'server-with-api' if with_fabric_api else 'server'
        entered = r'UNALLOCATED act=LOBBY enter t=0'
        status = (r'UNALLOCATED status act=LOBBY started=true finished=false elapsedTicks=0 '
                  rf'budgetTicks={int(chapters[0]["budget_seconds"] * 20)} checkpoint={re.escape(str(chapters[0]["checkpoint"]).replace(chr(39), chr(34)))}')
    monitor(
        command, directory, SMOKE / f'{name}-console.log', startup + [entered, status, r'UNALLOCATED bait selftest passed'],
        timeout=120, stop_when_ready=not expects_rejection(api_version), startup_required=startup,
        command_steps=[] if expects_rejection(api_version) else [
            ('unallocated build all', [r'UNALLOCATED map built section=all', r'UNALLOCATED map verified section=all']),
            ('unallocated mapcheck rebuild', [r'UNALLOCATED map regeneration verified sections=5']),
            ('unallocated act LOBBY', [entered]), ('unallocated status', [status]),
            ('unallocated story check', [r'UNALLOCATED story check ok']),
            ('ubait selftest', [r'UNALLOCATED bait selftest passed'])],
        expected_rejection=api_version if expects_rejection(api_version) else None,
    )
    if expects_rejection(api_version):
        return
    # Dedicated startup overrides world mode/difficulty from this file. Authoring must persist it too.
    saved_properties = dict(line.split('=', 1) for line in read(directory / 'server.properties').splitlines()
                            if '=' in line and not line.startswith('#'))
    for key, expected_value in {'gamemode': 'adventure', 'difficulty': 'normal'}.items():
        if saved_properties.get(key) != expected_value:
            raise RuntimeError(f'Map setting not persisted: {key} expected={expected_value} found={saved_properties.get(key)}')
    if with_fabric_api:
        return  # Coexistence check stays a single process; clean one-JAR smoke proves reload.
    # A second real server process must load SavedData, not a leftover Java/static object.
    # Preserve the first log as evidence and require entirely fresh startup/reload markers.
    latest = directory / 'logs' / 'latest.log'
    shutil.copyfile(latest, directory / 'logs' / 'core-before-reload.log')
    latest.unlink()
    monitor(
        command, directory, SMOKE / 'server-reload-console.log', startup + [status],
        timeout=120, stop_when_ready=True, startup_required=startup,
        command_steps=[('unallocated mapcheck', [r'UNALLOCATED map verified section=all']),
                       ('unallocated status', [status])],
    )
    assert 'UNALLOCATED act=LOBBY enter' not in read(latest), 'Reload unexpectedly restarted the act'
    latest.unlink()
    example_command = [part for part in command if not part.startswith('-Dunallocated.story.dir=')]
    example_command = example_command[:1] + ['-Dunallocated.story.dir=' + str(ROOT / 'src/test/resources/story-fixtures/valid/example')] + example_command[1:]
    monitor(example_command, directory, SMOKE / 'server-story-example-console.log',
            server_evidence() + [r'UNALLOCATED story loaded chapters=2 beats=7 endings=2 format=1', r'UNALLOCATED story check ok'],
            timeout=120, stop_when_ready=True, startup_required=server_evidence(),
            command_steps=[('unallocated story check', [r'UNALLOCATED story check ok'])])


BAIT_SCREENSHOTS = (
    'unallocated-bait-1-plain-view', 'unallocated-bait-2-first-sighting',
    'unallocated-bait-3-closer', 'unallocated-bait-4-screamer',
)


def verify_screenshot(directory, name='unallocated-title'):
    # Fabric can prepend a numeric counter; one capture cannot substitute for another.
    names = (name,) if isinstance(name, str) else name
    for expected in names:
        screenshots = list(directory.rglob(f'*{expected}.png'))
        if not any(path.read_bytes().startswith(b'\x89PNG\r\n\x1a\n') for path in screenshots):
            raise RuntimeError(f'Client exited without a valid screenshot: {expected}')


def audio_evidence():
    required = [r'UNALLOCATED audio: registered 7 sound events; loud=screamer\.']
    layers = {
        'ambient': ('ambient_drone', 'ambient_cave'),
        'threads': ('thread_string', 'thread_chime'),
        'muffle': ('breath',),
        'screamer': ('screamer_impact', 'screamer_crack'),
    }
    for cue, events in layers.items():
        required.append(rf'UNALLOCATED smoke: audio cue={cue} registered, resolved and active\.')
        for event in events:
            required.append(rf'UNALLOCATED audio: play cue={cue} event=unallocated:audio\.{event} '
                            rf'loud={str(cue == "screamer").lower()} result=STARTED(?:\s|$)')
    required.append(r'UNALLOCATED smoke: audio attenuation, expiry, replacement, stop, disconnect and options verified\.')
    return required


def play_checks(story_directory=None):
    """Derive content expectations, without encoding chapter/ending/capture names in CI."""
    directory = story_directory or ROOT / 'src/main/resources/data/unallocated/story'
    story = json.loads((directory / 'story.json').read_text())
    chapters = [json.loads((directory / 'chapters' / f'{name}.json').read_text())
                for name in story['chapters']]
    runs = story['autoplay']['runs']
    evidence = [r'UNALLOCATED smoke: play all shipped runs verified',
                r'UNALLOCATED smoke: play finished save preserved and new copy opened']
    screenshots = ['unallocated-title-button', 'unallocated-connecting']
    for index, run in enumerate(runs):
        evidence += [r'UNALLOCATED smoke: world authored template pocket exists',
                     rf'UNALLOCATED story autoplay run={index} ending={re.escape(run["ending"])} complete',
                     rf'UNALLOCATED smoke: play run={index} armed unstarted Adventure cheats=false first={re.escape(story["chapters"][0])}',
                     rf'UNALLOCATED smoke: play run={index} farewell and title verified']
        screenshots += [f'unallocated-play-farewell-{index}', f'unallocated-play-title-{index}']
    capture_names = sorted({beat['capture']['name'] for chapter in chapters
                            for beat in chapter['beats'] if 'capture' in beat})
    screenshots += ['story-' + name for name in capture_names]
    evidence += [rf'UNALLOCATED story capture={re.escape(name)}(?:\s|$)' for name in capture_names]
    seconds = sum(chapter['budget_seconds'] for chapter in chapters) * story['autoplay']['time_scale']
    return {'evidence': evidence, 'screenshots': screenshots,
            'timeout': int(120 + len(runs) * (seconds + 180))}


def client_phases():
    """Four real client processes share the accepted contract and options."""
    title = r'UNALLOCATED smoke: title screen reached from player JAR\.'
    modules = r'UNALLOCATED smoke: bundled Fabric API modules verified; screen hook verified\.'
    return {
        # The first start meets the contract before any title screen; package checks follow it.
        'first': {
            'evidence': [
                r'UNALLOCATED smoke: contract first start, data list, typed and clicked acceptance verified\.',
                modules, title, *audio_evidence(),
                r'UNALLOCATED smoke: core armed join, no-cheats rescue guards, client act sync and single-player reload verified\.',
                r'UNALLOCATED interface smoke: effects rendered; options untouched byte-for-byte after world exit\.',
                r'UNALLOCATED bait step=2/2',
                r'UNALLOCATED bait screamer shown',
                r'UNALLOCATED spyglass smoke: three looks, closer each time, screamer, figure gone\.',
                r'UNALLOCATED smoke: map tour completed\.',
                r'UNALLOCATED smoke: launcher GUI scales, continue and new copies verified',
                r'UNALLOCATED ending=SERVER_CLOSED',
                r'UNALLOCATED smoke: ending save reloaded as finished; options and receipt unchanged; splash reset\.',
                r'UNALLOCATED smoke: ending command showed vanilla disconnect and returned to title with changed splash\.',
            ],
            'screenshots': ['unallocated-contract', 'unallocated-contract-uses', 'unallocated-title-first',
                            'unallocated-ending-disconnect', 'unallocated-ending-title',
                            'unallocated-ending-command-disconnect', 'unallocated-ending-command-title',
                            'map-lobby-spawn', 'map-lobby-board', 'map-hill-bait_path', 'map-hill-view', 'map-path-midpoint',
                            'map-base-entrance', 'map-base-theft_zone', 'map-mine-entrance', 'map-mine-threshold',
                            *[f'unallocated-interface-{effect}' for effect in
                              ('splash', 'loading', 'f3', 'advancement', 'sign', 'death')],
                            *BAIT_SCREENSHOTS],
            'timeout': 480,
        },
        'restart': {
            'evidence': [
                r'UNALLOCATED smoke: contract skipped on second process with saved acceptance\.',
                modules, title,
            ],
            'screenshots': ['unallocated-title-restart'],
            'timeout': 240,
        },
        'story': {
            'evidence': [modules, title,
                r'UNALLOCATED smoke: contract skipped on second process with saved acceptance\.',
                r'UNALLOCATED story autoplay run=0 ending=EXAMPLE_A complete',
                r'UNALLOCATED story autoplay run=1 ending=EXAMPLE_B complete',
                r'UNALLOCATED smoke: story both endings, captures, options and receipt verified\.',
                r'UNALLOCATED story autoplay run=0 ending=SERVER_CLOSED complete',
                r'UNALLOCATED smoke: world fresh pocket, copy blocks, block entities and force tickets verified',
                r'UNALLOCATED smoke: world corruption job and cursor restored',
                r'UNALLOCATED smoke: world pocket round trip and corruption completion verified',
                r'UNALLOCATED smoke: world fixture ending, pocket capture and reload verified',
            ],
            'screenshots': ['story-example-sign', 'story-example-end',
                            'unallocated-story-farewell-a', 'unallocated-story-farewell-b',
                            'story-world-pocket', 'unallocated-story-world-farewell'],
            'timeout': 300,
        },
        'play': play_checks(),
    }


def verify_interface_screenshots(directory):
    for effect in ('splash', 'loading', 'f3', 'advancement', 'sign', 'death'):
        screenshots = list(directory.rglob(f'*unallocated-interface-{effect}*.png'))
        if not any(path.read_bytes().startswith(b'\x89PNG\r\n\x1a\n') for path in screenshots):
            raise RuntimeError(f'Client exited without a valid interface screenshot: {effect}')


def xvfb_capture_environment(arguments):
    """xvfb-run's cookie belongs to that server, not the parent Python process."""
    display = next((os.fsdecode(arg) for arg in arguments if re.fullmatch(rb':\d+', arg)), None)
    environment = dict(os.environ)
    if display: environment['DISPLAY'] = display
    if b'-auth' in arguments:
        index = arguments.index(b'-auth')
        if index + 1 >= len(arguments): raise ValueError('Xvfb -auth has no path')
        environment['XAUTHORITY'] = os.fsdecode(arguments[index + 1])
    return display, environment


class VideoRecorder:
    """Optional Xvfb capture. Graphics/test failures still belong to the normal smoke gate."""
    def __init__(self, directory):
        self.directory = SMOKE / 'video'
        self.process = None
        self.done = threading.Event()
        self.thread = None
        self.output = None
        self.stopped = False

    def unavailable(self, reason):
        message = 'UNALLOCATED video unavailable: ' + str(reason)
        print(message, flush=True)
        (self.directory / 'unavailable.log').write_text(message + '\n')

    def start(self):
        self.directory.mkdir(parents=True, exist_ok=True)
        def capture():
            try:
                ffmpeg = shutil.which('ffmpeg')
                if not ffmpeg:
                    self.unavailable('ffmpeg is missing'); return
                display = None
                deadline = time.monotonic() + 90
                while not self.done.is_set() and time.monotonic() < deadline:
                    for cmdline in Path('/proc').glob('[0-9]*/cmdline'):
                        try: args = cmdline.read_bytes().split(b'\0')
                        except OSError: continue
                        if args and Path(os.fsdecode(args[0])).name == 'Xvfb':
                            display, environment = xvfb_capture_environment(args)
                            if display: break
                    if display: break
                    self.done.wait(.25)
                if not display:
                    self.unavailable('Xvfb display was not found'); return
                self.output = (self.directory / 'ffmpeg.log').open('w')
                self.process = subprocess.Popen([ffmpeg, '-y', '-f', 'x11grab', '-framerate', '8',
                    '-i', display, '-vf', 'scale=640:-2', '-c:v', 'libx264', '-preset', 'ultrafast',
                    '-crf', '30', '-maxrate', '200k', '-bufsize', '400k', '-pix_fmt', 'yuv420p',
                    '-fs', '24000000', '-t', '1800', '-movflags', '+faststart', str(self.directory / 'gameplay.mp4')],
                    stdin=subprocess.PIPE, stdout=self.output, stderr=subprocess.STDOUT, text=True, env=environment)
            except Exception as error: self.unavailable(error)
        self.thread = threading.Thread(target=capture, daemon=True); self.thread.start()

    def stop(self):
        if self.stopped: return
        self.stopped = True
        self.done.set()
        self.thread.join(timeout=5)
        try:
            if self.process is not None:
                if self.process.poll() is None:
                    self.process.communicate('q\n', timeout=15)
                if self.process.returncode != 0: raise RuntimeError('ffmpeg exited ' + str(self.process.returncode))
            path = self.directory / 'gameplay.mp4'
            if not path.is_file() or path.stat().st_size < 1024: raise RuntimeError('no playable recording')
            if path.stat().st_size > 25_000_000: raise RuntimeError('recording exceeds 25 MB')
            print(f'UNALLOCATED video bytes={path.stat().st_size}', flush=True)
        except Exception as error:
            if self.process is not None and self.process.poll() is None:
                self.process.kill(); self.process.wait(timeout=5)
            (self.directory / 'gameplay.mp4').unlink(missing_ok=True)
            self.unavailable(error)
        finally:
            if self.output: self.output.close()


def release_bundle():
    """Stage the exact interface consumed by release-kit, after successful clean-profile smoke."""
    from author_template import TEMPLATE_ENTRY, WORLD_ZIP
    props = properties(); jar = ROOT / 'build/libs' / f'unallocated-{props["version"]}.jar'
    checksum = jar.with_suffix('.jar.sha256')
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    if checksum.read_text() != f'{digest}  {jar.name}\n': raise RuntimeError('Final JAR checksum changed during smoke')
    with ZipFile(jar) as archive:
        if archive.read(TEMPLATE_ENTRY) != WORLD_ZIP.read_bytes(): raise RuntimeError('Release template differs from JAR')
    output = ROOT / 'build/release'; output.mkdir(parents=True, exist_ok=False)
    for source in (jar, checksum, WORLD_ZIP): shutil.copyfile(source, output / source.name)
    screenshots = output / 'screenshots'; screenshots.mkdir()
    for source in sorted((SMOKE / 'client').rglob('*.png')):
        match = re.search(r'(story-|map-|unallocated-title-button|unallocated-connecting).*\.png$', source.name)
        if match:
            target = screenshots / match.group()
            if not target.exists() or 'screenshots-before-play' not in source.parts:
                shutil.copyfile(source, target)
    video = SMOKE / 'video/gameplay.mp4'
    if video.is_file():
        (output / 'video').mkdir(); shutil.copyfile(video, output / 'video/gameplay.mp4')
    manifest = {'version': props['version'], 'minecraft': props['minecraft_version'],
                'loader': props['loader_version'], 'fabric_api_bundled': True,
                'jar': jar.name, 'sha256': digest, 'world_zip': WORLD_ZIP.name,
                'commit': os.environ['GITHUB_SHA'], 'run': os.environ['GITHUB_RUN_ID']}
    (output / 'release.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('UNALLOCATED release staged: ' + json.dumps(manifest), flush=True)


def client(with_fabric_api=None, *, include_story=False):
    api_version = api_version_argument(with_fabric_api)
    name = profile_name('client', api_version)
    directory = SMOKE / name
    directory.mkdir(parents=True, exist_ok=False)
    if expects_rejection(api_version):
        monitor(
            ['gradle', '--daemon', '--console=plain', 'runClientSmoke',
             f'-PsmokeApiVersion={api_version}', '-PsmokeExpectRejection=true'],
            ROOT, SMOKE / f'{name}-console.log', [], timeout=240,
            game_directory=directory, expected_rejection=api_version,
        )
        return
    receipt = directory / 'unallocated' / 'contract.properties'
    original = None
    for phase, checks in client_phases().items():
        if phase in ('story', 'play') and not include_story:
            continue  # Preserve the reusable two-process contract check; CI explicitly runs all phases.
        if phase == 'play':
            # A fixture capture with the same name must never satisfy shipped-story evidence.
            (directory / 'screenshots').rename(directory / 'screenshots-before-play')
        recorder = VideoRecorder(directory) if phase == 'play' and api_version is None else None
        if recorder: recorder.start()
        try:
            monitor(
            ['gradle', '--daemon', '--console=plain', 'runClientSmoke', f'-PcontractSmokePhase={phase}',
             f'-PsmokeApiVersion={api_version or "none"}'],
            ROOT, SMOKE / f'{name}-{phase}-console.log', [
                r'UNALLOCATED initialized \(common\)\.',
                r'UNALLOCATED initialized \(client\)\.',
                *checks['evidence'],
                *([rf'UNALLOCATED smoke: external Fabric API {re.escape(api_version)} loaded\.']
                  if api_version else []),
            ], timeout=checks['timeout'],
            # The launcher/Gradle command runs at repo root; game evidence lives in runDir.
            # Same real game directory, new JVM, no stale log evidence.
            game_directory=directory,
            on_log=(lambda log: recorder.stop() if 'UNALLOCATED smoke: play all shipped runs verified' in log else None) if recorder else None,
            )
        finally:
            if recorder: recorder.stop()
        if include_story:  # The CI pipeline has packaged the JAR; contract-only fixtures have no build.
            props = properties()
            artifact = ROOT / 'build/libs' / f'unallocated-{props["version"]}.jar'
            expected = hashlib.sha256(artifact.read_bytes()).hexdigest()
            if artifact.with_suffix('.jar.sha256').read_text() != f'{expected}  {artifact.name}\n':
                raise RuntimeError('Gradle changed the verified player JAR during smoke')
        for screenshot in checks['screenshots']:
            if phase == 'first' and screenshot.startswith('unallocated-contract'):
                if not any(path.read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
                           for path in directory.rglob(f'*{screenshot}.png')):
                    raise RuntimeError(f'Missing contract screenshot: {screenshot}')
            else:
                verify_screenshot(directory / 'screenshots' if phase == 'play' else directory, screenshot)
        if phase == 'first':
            original = receipt.read_bytes()
            (directory / 'logs').rename(directory / 'logs-first')
        elif receipt.read_bytes() != original:
            raise RuntimeError('Second start rewrote the original acceptance')
        if phase in ('restart', 'story') and include_story:
            (directory / 'logs').rename(directory / f'logs-{phase}')
    props = properties()
    verify_mod_directory(directory / 'mods', {
        f"unallocated-{props['version']}.jar",
    } | ({f'fabric-api-{api_version}.jar'} if api_version else set()))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('target', choices=['server', 'client', 'server-with-api', 'gradle', 'release'])
    parser.add_argument('--fabric-api-version', choices=['none'] + API_POLICY['smoke_api_versions'],
                        default='none')
    args, extra = parser.parse_known_args()
    if extra and args.target != 'gradle': parser.error('Unexpected arguments')
    if os.environ.get('CI') != 'true':
        parser.error('Real-game smoke checks must run in CI, never on the scaffold host')
    if args.target == 'release':
        release_bundle(); return
    if args.target == 'gradle':
        watch_gradle(['gradle', '--daemon', '--console=plain', *[x for x in extra if x != '--']],
                     SMOKE / 'build-console.log')
        return
    version = api_version_argument(args.fabric_api_version)
    if args.target == 'server-with-api':
        version = version or API_VERSION
    if args.target == 'client':
        client(version, include_story=True)
    else:
        server(version)


if __name__ == '__main__':
    main()
