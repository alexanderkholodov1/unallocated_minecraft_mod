"""One command for a real playthrough on a desktop machine (Windows or Linux), in CI's order.

1. gradle build clientSmokeJar   (player JAR with the nested Fabric API modules, and the test helper)
2. author_template.py            (authors the template world and injects it into the player JAR)
3. a saved contract receipt      (the play phase starts from the title screen, like a returning player)
4. gradle runClientSmoke -PcontractSmokePhase=play  (autoplays every ending; screenshots in .tmp/smoke/client/screenshots)

Usage: python scripts/local_play.py [--gradle PATH] [--skip-build]
Needs JAVA_HOME pointing to a Java 25 JDK. Never touches the player's own .minecraft.
"""
import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / '.tmp' / 'smoke' / 'client'


def run(command, **extra):
    print('>', ' '.join(str(part) for part in command), flush=True)
    subprocess.run(command, cwd=ROOT, check=True, **extra)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--gradle', default=os.environ.get('GRADLE', shutil.which('gradle') or 'gradle'))
    parser.add_argument('--skip-build', action='store_true', help='reuse the JAR already authored in build/libs')
    parser.add_argument('--jdk', help='use only this JDK for compiling and for the game (e.g. a Temurin 25 when another JDK crashes)')
    parser.add_argument('--phase', default='play', choices=['play', 'idle'], help='play: autoplay every ending; idle: a player who never moves must still reach an ending')
    args = parser.parse_args()
    if args.jdk:
        os.environ['JAVA_HOME'] = args.jdk
    if not os.environ.get('JAVA_HOME'):
        sys.exit('Set JAVA_HOME to a Java 25 JDK first (or pass --jdk)')
    gradle = [args.gradle, '--console=plain']
    if args.jdk:
        gradle += ['-Porg.gradle.java.installations.auto-detect=false', f'-Porg.gradle.java.installations.paths={args.jdk}']
    if not args.skip_build:
        run(gradle + ['build', 'clientSmokeJar', '-PsmokeApiVersion=none', '-x', 'test'])
        shutil.rmtree(ROOT / '.tmp' / 'smoke' / 'template', ignore_errors=True)
        run([sys.executable, 'scripts/author_template.py'], env={**os.environ, 'CI': 'true'})
    for stale in ('screenshots', 'saves', 'logs'):
        shutil.rmtree(CLIENT / stale, ignore_errors=True)
    receipt = CLIENT / 'unallocated' / 'contract.properties'
    receipt.parent.mkdir(parents=True, exist_ok=True)
    receipt.write_text('schemaVersion=1\ncontractVersion=1\nacceptedAt=2026-11-01T18\\:00\\:00Z\nmethod=TYPED\ntypedText=De acuerdo\n',
                       encoding='utf-8')
    run(gradle + ['runClientSmoke', '-PsmokeApiVersion=none', f'-PcontractSmokePhase={args.phase}'])
    print('Screenshots:', CLIENT / 'screenshots')


if __name__ == '__main__':
    main()
