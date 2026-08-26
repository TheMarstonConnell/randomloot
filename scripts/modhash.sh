#!/usr/bin/env bash
# Prints a version-normalized content hash of a mod jar.
#
# The release workflow stamps the release version into every jar's manifests, so
# two builds of identical code still differ byte-wise. This hash unpacks the jar,
# blanks the mod version out of the loader manifests, and hashes the sorted file
# contents - two releases with no code changes for a Minecraft version produce
# the same hash, letting the workflow skip CurseForge/Modrinth for that jar.
set -euo pipefail

jar=$1
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
unzip -qq "$jar" -d "$tmp"

python3 - "$tmp" <<'PY'
import json, re, sys, os
root = sys.argv[1]

version = None
fmj = os.path.join(root, "fabric.mod.json")
toml = os.path.join(root, "META-INF", "neoforge.mods.toml")
if os.path.exists(fmj):
    version = json.load(open(fmj))["version"]
elif os.path.exists(toml):
    m = re.search(r'^version="([^"]+)"', open(toml).read(), re.M)
    version = m.group(1) if m else None

if version:
    targets = [fmj, toml, os.path.join(root, "META-INF", "MANIFEST.MF")]
    for f in targets:
        if os.path.exists(f):
            s = open(f).read()
            open(f, "w").write(s.replace(version, "__VERSION__"))
PY

if command -v sha256sum >/dev/null; then SHA=sha256sum; else SHA="shasum -a 256"; fi
(cd "$tmp" && find . -type f | LC_ALL=C sort | xargs $SHA | $SHA | cut -d' ' -f1)
