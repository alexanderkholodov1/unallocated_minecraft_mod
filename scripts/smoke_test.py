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
from urllib.request import Request, urlopen
from zipfile import ZipFile

from verify_jar import matches


ROOT = Path(__file__).resolve().parents[1]
SMOKE = ROOT / '.tmp' / 'smoke'
FATAL = re.compile(
    r'Encountered an unexpected exception|Exception in thread|'
    r'A fatal error has been detected|Crash report saved|Mixin apply.*failed', re.I
)


API_POLICY = json.loads((ROOT / 'fabric-api-modules.json').read_text())
API_VERSION = API_POLICY['fabric_api_version']


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
            expected_rejection=None):
    """Require clean game evidence or a specific pre-startup Loader dependency rejection."""
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
                if reports() or crash_pattern.search(game_log + read(console)):
                    raise RuntimeError('Game crash detected')
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


def server(with_fabric_api=False):
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
        'generate-structures=false\nview-distance=2\nsimulation-distance=2\n'
        'max-players=1\nsync-chunk-writes=false\n'
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
    command = ['java', '-Xms256M', '-Xmx1G', '-Djava.awt.headless=true', f'-Djava.io.tmpdir={temp}', '-jar', str(launcher), 'nogui']
    startup = server_evidence(with_fabric_api)
    console_prefix = 'server-with-api' if with_fabric_api else 'server'
    entered = r'UNALLOCATED act=LOBBY enter t=0'
    status = (r'UNALLOCATED status act=LOBBY started=true finished=false elapsedTicks=0 '
              r'budgetTicks=6000 checkpoint=lobby\.spawn')
    monitor(
        command, directory, SMOKE / f'{name}-console.log', startup + [entered, status],
        timeout=120, stop_when_ready=not expects_rejection(api_version), startup_required=startup,
        command_steps=[] if expects_rejection(api_version) else [
            ('unallocated arm', [r'UNALLOCATED session armed']),
            ('unallocated act LOBBY', [entered]), ('unallocated status', [status])],
        expected_rejection=api_version if expects_rejection(api_version) else None,
    )
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
        command_steps=[('unallocated status', [status])],
    )
    assert 'UNALLOCATED act=LOBBY enter' not in read(latest), 'Reload unexpectedly restarted the act'


def verify_screenshot(directory, name='unallocated-title'):
    # Fabric can prepend a numeric counter, e.g. 0000_unallocated-title.png.
    screenshots = list(directory.rglob(f'*{name}*.png'))
    if not any(path.read_bytes().startswith(b'\x89PNG\r\n\x1a\n') for path in screenshots):
        raise RuntimeError(f'Client exited without a valid screenshot: {name}')


def client_phases():
    """Two real client processes share one game directory: a first start, then a restart."""
    title = r'UNALLOCATED smoke: title screen reached from player JAR\.'
    modules = r'UNALLOCATED smoke: bundled Fabric API modules verified; screen hook verified\.'
    return {
        # The first start meets the contract before any title screen; package checks follow it.
        'first': {
            'evidence': [
                r'UNALLOCATED smoke: contract first start, data list, typed and clicked acceptance verified\.',
                modules, title,
                r'UNALLOCATED smoke: core armed join, no-cheats rescue guards, client act sync and single-player reload verified\.',
            ],
            'screenshots': ['unallocated-contract', 'unallocated-contract-uses', 'unallocated-title-first'],
            'timeout': 240,
        },
        'restart': {
            'evidence': [
                r'UNALLOCATED smoke: contract skipped on second process with saved acceptance\.',
                modules, title,
            ],
            'screenshots': ['unallocated-title-restart'],
            'timeout': 240,
        },
    }


def client(with_fabric_api=None):
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
        )
        for screenshot in checks['screenshots']:
            if phase == 'first' and screenshot.startswith('unallocated-contract'):
                if not any(path.read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
                           for path in directory.rglob(f'*{screenshot}.png')):
                    raise RuntimeError(f'Missing contract screenshot: {screenshot}')
            else:
                verify_screenshot(directory, screenshot)
        if phase == 'first':
            original = receipt.read_bytes()
            (directory / 'logs').rename(directory / 'logs-first')
        elif receipt.read_bytes() != original:
            raise RuntimeError('Second start rewrote the original acceptance')
    props = properties()
    verify_mod_directory(directory / 'mods', {
        f"unallocated-{props['version']}.jar",
    } | ({f'fabric-api-{api_version}.jar'} if api_version else set()))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('target', choices=['server', 'client', 'server-with-api'])
    parser.add_argument('--fabric-api-version', choices=['none'] + API_POLICY['smoke_api_versions'],
                        default='none')
    args = parser.parse_args()
    if os.environ.get('CI') != 'true':
        parser.error('Real-game smoke checks must run in CI, never on the scaffold host')
    version = api_version_argument(args.fabric_api_version)
    if args.target == 'server-with-api':
        version = version or API_VERSION
    {'server': server, 'client': client, 'server-with-api': server}[args.target](version)


if __name__ == '__main__':
    main()
