#!/usr/bin/env bash
# Discover the current and available NeoForge / Minecraft versions for this mod.
#
# Reads the project's current versions from gradle.properties and queries the
# NeoForged Maven metadata to compute upgrade candidates. Prints a human report
# plus machine-readable KEY=VALUE lines (prefixed "OUT_") that the skill parses.
#
# Versioning scheme (Minecraft calendar versioning, see claude.md):
#   NeoForge "A.B.C.D[-suffix]"  ==>  Minecraft "A.B.C", NeoForge build "D".
#   "stable" == no -suffix (e.g. -beta / -rc); a -suffix means pre-release.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PROPS="$REPO_ROOT/gradle.properties"
META_URL="https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"

prop() { grep -E "^$1=" "$PROPS" | head -1 | cut -d= -f2- | tr -d '[:space:]'; }

cur_neo="$(prop neo_version)"
cur_mc="$(prop minecraft_version)"
cur_line="$(echo "$cur_mc" | cut -d. -f1-3)"

meta="$(curl -fsS --max-time 30 "$META_URL")" || { echo "ERROR: could not fetch $META_URL" >&2; exit 1; }

# All new-scheme (4-segment) NeoForge versions, version-sorted ascending.
all="$(echo "$meta" \
  | grep -oE '<version>[^<]+</version>' \
  | sed -E 's:</?version>::g' \
  | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.]+)?$' \
  | sort -V)"

last() { tail -1; }                       # max of a version-sorted stream ("" if empty)
stable() { grep -vE -- '-'; }             # drop pre-releases (anything with a -suffix)
onLine() { grep -E "^$(echo "$cur_line" | sed 's/\./\\./g')\."; }
mcOf() { cut -d. -f1-3; }                  # NeoForge version -> Minecraft version

neo_line_stable="$(echo "$all" | onLine | stable | last || true)"
neo_line_any="$(echo "$all" | onLine | last || true)"
neo_stable="$(echo "$all" | stable | last || true)"
neo_any="$(echo "$all" | last || true)"

emit() { # name  neo-version
  local name="$1" neo="$2" mc=""
  [ -n "$neo" ] && mc="$(echo "$neo" | mcOf)"
  printf 'OUT_%s_NEO=%s\nOUT_%s_MC=%s\n' "$name" "$neo" "$name" "$mc"
}

echo "=== randomloot :: NeoForge / Minecraft version check ==="
echo "current:                     neo=$cur_neo  mc=$cur_mc  (line $cur_line.x)"
echo "latest stable on this line:  neo=${neo_line_stable:-none}  mc=$([ -n "$neo_line_stable" ] && echo "$neo_line_stable" | mcOf || echo none)"
echo "latest beta on this line:    neo=${neo_line_any:-none}"
echo "latest stable overall:       neo=${neo_stable:-none}  mc=$([ -n "$neo_stable" ] && echo "$neo_stable" | mcOf || echo none)"
echo "latest overall (incl. beta): neo=${neo_any:-none}  mc=$([ -n "$neo_any" ] && echo "$neo_any" | mcOf || echo none)"
echo
echo "# Machine-readable (parse these):"
echo "OUT_CURRENT_NEO=$cur_neo"
echo "OUT_CURRENT_MC=$cur_mc"
emit LINE_STABLE "$neo_line_stable"
emit LINE_ANY    "$neo_line_any"
emit STABLE      "$neo_stable"
emit ANY         "$neo_any"
