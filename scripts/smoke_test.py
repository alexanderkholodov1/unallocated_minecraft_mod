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


def server_evidence(with_fabric_api=False):
    required = [r'UNALLOCATED initialized \(common\)\.', r'Done \([^\r\n]*\)!']
    required.extend(
        rf'\b{re.escape(module_id)}\s+{re.escape(module["version"])}(?:\s|$)'
        for module_id, module in API_POLICY['modules'].items()
        if module['environment'] != 'client'
    )
    if with_fabric_api:
        required.append(rf'\bfabric-api\s+{re.escape(API_VERSION)}(?:\s|$)')
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
            shutdown_timeout=30, game_directory=None):
    """Require actual game log evidence, exit zero, and no crash; stop servers via stdin."""
    console.parent.mkdir(parents=True, exist_ok=True)
    game_directory = game_directory or cwd
    latest = game_directory / 'logs' / 'latest.log'

    def reports():
        found = crash_reports(game_directory)
        if cwd != game_directory:
            found.extend(cwd.glob('hs_err_pid*.log'))
        return found

    deadline = time.monotonic() + timeout
    stop_sent = False
    process = None
    try:
        with console.open('w') as output:
            process = subprocess.Popen(
                command, cwd=cwd, stdin=subprocess.PIPE, stdout=output,
                stderr=subprocess.STDOUT, text=True, start_new_session=True,
            )
            while process.poll() is None:
                game_log = read(latest)
                if reports() or FATAL.search(game_log + read(console)):
                    raise RuntimeError('Game crash detected')
                if stop_when_ready and not stop_sent and all(
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
            if process.returncode != 0:
                raise RuntimeError(f'Game command exited with status {process.returncode}')
        game_log = read(latest)
        missing = [pattern for pattern in required if not re.search(pattern, game_log)]
        if missing:
            raise RuntimeError(f'Missing required game log evidence: {missing}')
        if stop_when_ready and (not stop_sent or 'Stopping server' not in game_log):
            raise RuntimeError('Server did not acknowledge our stop command and exit cleanly')
        if reports() or FATAL.search(game_log + read(console)):
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
    props = properties()
    artifact = ROOT / 'build' / 'libs' / f"unallocated-{props['version']}.jar"
    expected = hashlib.sha256(artifact.read_bytes()).hexdigest()
    directory = SMOKE / ('server-with-api' if with_fabric_api else 'server')
    # Fresh directories prevent a stale Done/log line from producing a false pass.
    directory.mkdir(parents=True, exist_ok=False)
    (directory / 'mods').mkdir()
    installed = directory / 'mods' / artifact.name
    shutil.copyfile(artifact, installed)
    assert hashlib.sha256(installed.read_bytes()).hexdigest() == expected
    if with_fabric_api:
        api = ROOT / 'build/smoke/full-api' / f"fabric-api-{API_VERSION}.jar"
        shutil.copyfile(api, directory / 'mods' / api.name)
    verify_mod_directory(directory / 'mods', {artifact.name} | (
        {api.name} if with_fabric_api else set()))
    (directory / 'eula.txt').write_text('# Accepted only for this disposable CI smoke test.\neula=true\n')
    (directory / 'server.properties').write_text(
        'server-ip=127.0.0.1\nserver-port=25565\nonline-mode=false\n'
        'level-name=smoke-world\nlevel-type=minecraft:flat\n'
        'generate-structures=false\nview-distance=2\nsimulation-distance=2\n'
        'max-players=1\nsync-chunk-writes=false\n'
    )
    temp = directory / 'temp'
    temp.mkdir()
    url = (
        'https://meta.fabricmc.net/v2/versions/loader/'
        f"{props['minecraft_version']}/{props['loader_version']}/1.1.2/server/jar"
    )
    print(f'Downloading pinned official Fabric server launcher: {url}', flush=True)
    launcher = directory / 'fabric-server-launch.jar'
    for attempt in range(2):
        try:
            with urlopen(Request(url, headers={'User-Agent': 'UNALLOCATED-CI'}), timeout=30) as response:
                launcher.write_bytes(response.read())
            with ZipFile(launcher) as jar:
                assert jar.testzip() is None, 'Corrupt Fabric server launcher'
            break
        except Exception:
            if attempt == 1:
                raise
    print(f'Installed player JAR SHA-256: {expected}', flush=True)
    monitor(
        ['java', '-Xms256M', '-Xmx1G', f'-Djava.io.tmpdir={temp}', '-jar', str(launcher), 'nogui'],
        directory, SMOKE / ('server-with-api-console.log' if with_fabric_api else 'server-console.log'),
        server_evidence(with_fabric_api),
        timeout=120, stop_when_ready=True,
    )


def verify_screenshot(directory):
    # Fabric can prepend a numeric counter, e.g. 0000_unallocated-title.png.
    screenshots = list(directory.rglob('*unallocated-title*.png'))
    if not any(path.read_bytes().startswith(b'\x89PNG\r\n\x1a\n') for path in screenshots):
        raise RuntimeError('Client exited without a valid title-screen screenshot')


def client():
    directory = SMOKE / 'client'
    directory.mkdir(parents=True, exist_ok=False)
    monitor(
        ['gradle', '--no-daemon', '--console=plain', 'runClientSmoke'],
        ROOT, SMOKE / 'client-console.log', [
            r'UNALLOCATED initialized \(common\)\.',
            r'UNALLOCATED initialized \(client\)\.',
            r'UNALLOCATED smoke: title screen reached from player JAR\.',
            r'UNALLOCATED smoke: six Fabric API modules loaded from player JAR; screen hook verified\.',
        ], timeout=240,
        # The launcher/Gradle command runs at repo root; game evidence lives in runDir.
        game_directory=directory,
    )
    verify_screenshot(directory)
    props = properties()
    verify_mod_directory(directory / 'mods', {
        f"unallocated-{props['version']}.jar",
    })


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('target', choices=['server', 'client', 'server-with-api'])
    args = parser.parse_args()
    if os.environ.get('CI') != 'true':
        parser.error('Real-game smoke checks must run in CI, never on the scaffold host')
    {'server': server, 'client': client, 'server-with-api': lambda: server(True)}[args.target]()


if __name__ == '__main__':
    main()
