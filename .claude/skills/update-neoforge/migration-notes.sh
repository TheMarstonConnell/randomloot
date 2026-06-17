#!/usr/bin/env bash
# Print the migration material for a NeoForge/Minecraft bump:
#   - the NeoForged migration primer(s) for every Minecraft minor line crossed
#   - the NeoForge changelog entries newer than the current build
#
# Usage: migration-notes.sh <current_neo_version> <target_neo_version>
#   e.g. migration-notes.sh 26.1.2.68-beta 26.2.0.1-beta
set -euo pipefail

cur="${1:?usage: migration-notes.sh <current_neo> <target_neo>}"
tgt="${2:?usage: migration-notes.sh <current_neo> <target_neo>}"

mc()   { echo "$1" | cut -d. -f1-3; }   # neo -> minecraft (A.B.C)
line() { echo "$1" | cut -d. -f1-2; }   # neo/mc -> minor line (A.B)

cur_line="$(line "$cur")"
tgt_line="$(line "$tgt")"

# version "A.B" comparison: true if $1 > $2
gt() { [ "$1" != "$2" ] && [ "$(printf '%s\n%s\n' "$1" "$2" | sort -V | tail -1)" = "$1" ]; }

echo "############################################################"
echo "# Migration: NeoForge $cur -> $tgt"
echo "#   Minecraft $(mc "$cur") (line $cur_line) -> $(mc "$tgt") (line $tgt_line)"
echo "############################################################"
echo

if [ "$cur_line" = "$tgt_line" ]; then
  echo "Same Minecraft line ($cur_line): no migration primer needed — this is a build bump."
else
  echo "## Migration primer(s)   source: github.com/neoforged/.github/tree/main/primers"
  echo
  primers="$(curl -fsS --max-time 25 'https://api.github.com/repos/neoforged/.github/contents/primers' 2>/dev/null \
    | grep -oE '"name":[[:space:]]*"[0-9]+\.[0-9]+"' | grep -oE '[0-9]+\.[0-9]+' | sort -V || true)"
  shown=0
  for p in $primers; do
    # keep primers strictly above current line and at-or-below target line
    if gt "$p" "$cur_line" && { [ "$p" = "$tgt_line" ] || gt "$tgt_line" "$p"; }; then
      url="https://raw.githubusercontent.com/neoforged/.github/main/primers/$p/index.md"
      echo "===== primer $p :: $url ====="
      curl -fsS --max-time 25 "$url" 2>/dev/null || echo "(could not fetch primer $p)"
      echo; echo
      shown=1
    fi
  done
  [ "$shown" = 0 ] && echo "(no primer found between $cur_line and $tgt_line — check the primers repo by hand)"
fi

echo
echo "## NeoForge changelog since your current build ($cur)"
echo "   source: maven.neoforged.net/.../neoforge/$tgt/neoforge-$tgt-changelog.txt"
echo
chlog="$(curl -fsS --max-time 25 "https://maven.neoforged.net/releases/net/neoforged/neoforge/$tgt/neoforge-$tgt-changelog.txt" 2>/dev/null || true)"
if [ -n "$chlog" ]; then
  # changelog is newest-first; stop once we reach the current build's entry
  echo "$chlog" | awk -v cur="$cur" '$0 ~ ("`" cur "`") { exit } { print }'
else
  echo "(could not fetch changelog for $tgt)"
fi
