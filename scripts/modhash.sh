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
import re, sys, os
root = sys.argv[1]

# Blank only the version-declaration lines; a bare substring replace would also
# mangle unrelated ranges the version happens to be a substring of (e.g. mod
# version 1.1.0 inside neoforge versionRange="[21.1.0,)").
def norm(path, pattern, repl):
    if os.path.exists(path):
        s = open(path).read()
        open(path, "w").write(re.sub(pattern, repl, s, flags=re.M))

norm(os.path.join(root, "fabric.mod.json"),
     r'"version": "[^"]+"', '"version": "__VERSION__"')
norm(os.path.join(root, "META-INF", "neoforge.mods.toml"),
     r'^version="[^"]+"', 'version="__VERSION__"')
norm(os.path.join(root, "META-INF", "MANIFEST.MF"),
     r'^(Implementation-Version|Specification-Version): .+$', r'\1: __VERSION__')
PY

if command -v sha256sum >/dev/null; then SHA=sha256sum; else SHA="shasum -a 256"; fi
(cd "$tmp" && find . -type f | LC_ALL=C sort | xargs $SHA | $SHA | cut -d' ' -f1)
