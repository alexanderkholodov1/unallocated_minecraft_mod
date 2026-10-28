"""Build the itch.io upload bundle from a downloaded CI release artifact.

Usage:
    python3 scripts/release_bundle.py ARTIFACT_DIR [--out dist]
        [--design FILE] [--known-issues FILE]
"""

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import sys

ROOT = Path(__file__).resolve().parent.parent
TEMPLATE = ROOT / "docs" / "release" / "ITCH-PAGE.md"
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
RELEASE_KEYS = (
    "version",
    "minecraft",
    "loader",
    "fabric_api_bundled",
    "jar",
    "sha256",
    "world_zip",
    "commit",
    "run",
)
GENERATED_TOKENS = (
    "VERSION",
    "MINECRAFT",
    "LOADER",
    "FABRIC_API",
    "JAR",
    "SHA256",
    "WORLD_ZIP",
    "COMMIT",
    "KNOWN_ISSUES",
)
TOKEN_PATTERN = re.compile(r"\{\{[^{}]+\}\}")
HASH_PATTERN = re.compile(r"[0-9a-fA-F]{64}")


class BundleError(Exception):
    """A problem that must stop the bundle with exit status 1."""


class ArgumentParser(argparse.ArgumentParser):
    def error(self, message):
        self.print_usage(sys.stderr)
        self.exit(1, f"{self.prog}: error: {message}\n")


def sha256_file(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalize_hash(value, source):
    match = HASH_PATTERN.search(str(value))
    if match is None:
        raise BundleError(f"{source} does not contain a SHA-256 hash")
    return match.group(0).lower()


def read_release(artifact):
    path = artifact / "release.json"
    if not path.is_file():
        raise BundleError(f"missing release.json in {artifact}")
    try:
        release = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        raise BundleError(f"cannot read release.json: {error}") from error
    if not isinstance(release, dict):
        raise BundleError("release.json must contain a JSON object")
    missing = [key for key in RELEASE_KEYS if key not in release]
    if missing:
        raise BundleError("release.json is missing: " + ", ".join(missing))
    for key in ("version", "minecraft", "loader", "fabric_api_bundled", "jar",
                "sha256", "world_zip", "commit"):
        if not isinstance(release[key], str) or not release[key].strip():
            raise BundleError(f"release.json field {key!r} must be a non-empty string")
    for key in ("jar", "world_zip"):
        name = release[key]
        if Path(name).name != name or name in (".", ".."):
            raise BundleError(f"release.json field {key!r} must be a plain file name")
    return release


def verify_artifact(artifact, release):
    jar = artifact / release["jar"]
    if not jar.is_file():
        raise BundleError(f"missing JAR: {jar}")
    sidecar = artifact / (release["jar"] + ".sha256")
    if not sidecar.is_file():
        raise BundleError(f"missing SHA-256 file: {sidecar}")
    world_zip = artifact / release["world_zip"]
    if not world_zip.is_file():
        raise BundleError(f"missing world ZIP: {world_zip}")

    declared = normalize_hash(release["sha256"], "release.json sha256")
    sidecar_hash = normalize_hash(sidecar.read_text(encoding="utf-8"), sidecar.name)
    actual = sha256_file(jar)
    if actual != declared:
        raise BundleError(
            f"JAR SHA-256 {actual} does not match release.json {declared}")
    if actual != sidecar_hash:
        raise BundleError(
            f"JAR SHA-256 {actual} does not match {sidecar.name} {sidecar_hash}")

    screenshots_dir = artifact / "screenshots"
    if not screenshots_dir.is_dir():
        raise BundleError(f"missing screenshots directory: {screenshots_dir}")
    screenshots = sorted(path for path in screenshots_dir.rglob("*.png")
                         if path.is_file())
    if not screenshots:
        raise BundleError(f"no PNG screenshots in {screenshots_dir}")
    for path in screenshots:
        with path.open("rb") as handle:
            signature = handle.read(len(PNG_SIGNATURE))
        if signature != PNG_SIGNATURE:
            raise BundleError(f"invalid PNG file: {path}")

    video = None
    for candidate in (artifact / "video" / "gameplay.mp4",
                      artifact / "gameplay.mp4"):
        if candidate.is_file():
            video = candidate
            break
    return jar, sidecar, world_zip, screenshots, video


def prepare_output(out):
    if out.exists():
        if not out.is_dir():
            raise BundleError(f"output path is not a directory: {out}")
        if any(out.iterdir()):
            raise BundleError(f"refusing to overwrite non-empty directory: {out}")
    out.mkdir(parents=True, exist_ok=True)


def load_design(path):
    if path is None:
        return {}
    try:
        design = json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        raise BundleError(f"cannot read design file {path}: {error}") from error
    if not isinstance(design, dict):
        raise BundleError("design file must contain a JSON object")
    tokens = {}
    for key, value in design.items():
        name = str(key).strip()
        if name.lower().startswith("design:"):
            name = name[len("design:"):]
        if not name:
            continue
        tokens[name] = "" if value is None else str(value)
    return tokens


def load_known_issues(path):
    if path is None:
        return None
    try:
        return Path(path).read_text(encoding="utf-8").strip()
    except (OSError, UnicodeDecodeError) as error:
        raise BundleError(f"cannot read known-issues file {path}: {error}") from error


def render_page(release, design, known_issues):
    if not TEMPLATE.is_file():
        raise BundleError(f"missing page template: {TEMPLATE}")
    page = TEMPLATE.read_text(encoding="utf-8")
    generated = {
        "VERSION": release["version"],
        "MINECRAFT": release["minecraft"],
        "LOADER": release["loader"],
        "FABRIC_API": release["fabric_api_bundled"],
        "JAR": release["jar"],
        "SHA256": normalize_hash(release["sha256"], "release.json sha256"),
        "WORLD_ZIP": release["world_zip"],
        "COMMIT": release["commit"],
    }
    if known_issues is not None:
        generated["KNOWN_ISSUES"] = known_issues
    for name in GENERATED_TOKENS:
        if name in generated:
            page = page.replace("{{" + name + "}}", generated[name])
    for name, value in design.items():
        page = page.replace("{{DESIGN:" + name + "}}", value)
    return page


def remaining_tokens(page):
    tokens = []
    for match in TOKEN_PATTERN.finditer(page):
        token = match.group(0)
        if token not in tokens:
            tokens.append(token)
    return tokens


def build(artifact, out, design_path, known_issues_path):
    release = read_release(artifact)
    jar, sidecar, world_zip, screenshots, video = verify_artifact(artifact, release)
    design = load_design(design_path)
    known_issues = load_known_issues(known_issues_path)

    prepare_output(out)
    shutil.copy2(jar, out / jar.name)
    shutil.copy2(sidecar, out / sidecar.name)
    shutil.copy2(world_zip, out / world_zip.name)
    screenshots_out = out / "screenshots"
    for source in screenshots:
        destination = screenshots_out / source.relative_to(artifact / "screenshots")
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
    if video is not None:
        shutil.copy2(video, out / "gameplay.mp4")

    page = render_page(release, design, known_issues)
    (out / "itch-page.md").write_text(page, encoding="utf-8")

    tokens = remaining_tokens(page)
    if tokens:
        print("Unfilled tokens:")
        for token in tokens:
            print(token)
        return 2
    print(f"Bundle complete: {out}")
    return 0


def main(argv=None):
    parser = ArgumentParser(description=__doc__)
    parser.add_argument("artifact_dir", type=Path,
                        help="directory holding the downloaded release artifact")
    parser.add_argument("--out", type=Path, default=Path("dist"),
                        help="output directory (default: dist)")
    parser.add_argument("--design", type=Path,
                        help="JSON object with the DESIGN token values")
    parser.add_argument("--known-issues", type=Path,
                        help="UTF-8 file with the known-issue text")
    args = parser.parse_args(argv)
    try:
        return build(args.artifact_dir, args.out, args.design, args.known_issues)
    except BundleError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
