#!/usr/bin/env bash
# Keep the runner's installed graphics stack; only fetch packages it lacks.
set -euo pipefail

missing=()
for package in xvfb xauth libgl1-mesa-dri mesa-vulkan-drivers; do
    if [[ $(dpkg-query -W -f='${Status}' "$package" 2>/dev/null || true) != 'install ok installed' ]]; then
        missing+=("$package")
    fi
done
if (( ${#missing[@]} == 0 )); then
    echo 'All headless graphics packages are already installed.'
    exit 0
fi

archives="$PWD/.tmp/headless-debs"
mkdir -p "$archives/partial"
shopt -s nullglob
debs=("$archives"/*.deb)
if (( ${#debs[@]} )); then
    # The cache is scoped to the runner image and this script. Install real packages,
    # including their dependencies, rather than overlaying cached system libraries.
    sudo apt-get install -y --no-download --no-install-recommends "${debs[@]}"
else
    # Avoid the slow Azure mirror and avoid updating unrelated third-party repos.
    source_file="$PWD/.tmp/headless.sources"
    cat > "$source_file" <<'SOURCES'
Types: deb
URIs: https://archive.ubuntu.com/ubuntu
Suites: noble noble-updates noble-security
Components: main universe
Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg
SOURCES
    options=(-o "Dir::Etc::sourcelist=$source_file" -o 'Dir::Etc::sourceparts=-'
             -o "Dir::Cache::archives=$archives" -o 'APT::Keep-Downloaded-Packages=true'
             -o 'Acquire::Retries=2' -o 'Acquire::https::Timeout=30')
    sudo apt-get "${options[@]}" update -qq
    sudo apt-get "${options[@]}" install -y --no-install-recommends --no-upgrade "${missing[@]}"
fi

for package in xvfb xauth libgl1-mesa-dri mesa-vulkan-drivers; do
    [[ $(dpkg-query -W -f='${Status}' "$package") == 'install ok installed' ]]
done
