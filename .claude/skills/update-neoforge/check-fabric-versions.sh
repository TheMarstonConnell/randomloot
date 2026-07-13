#!/usr/bin/env bash
# Discover the current and available Fabric-side / build-tool versions for this
# multiloader mod. Run this AFTER check-versions.sh has fixed the target Minecraft
# version — this script aligns every non-NeoForge dependency to that MC.
#
# It reads the project's current versions from gradle.properties + build.gradle and
# queries the upstream Maven metadata to compute candidates. Prints a human report
# plus machine-readable KEY=VALUE lines (prefixed "OUT_") that the skill parses.
#
# Usage: check-fabric-versions.sh [target_mc]
#   target_mc  the vanilla Minecraft string to align to (e.g. "26.2", "26.3").
#              Defaults to the current minecraft_version in gradle.properties.
#              Accepts the NeoForge-derived "26.2.0" form too — a trailing ".0"
#              is stripped to the vanilla string ("26.2") that Fabric/NeoForm/FCAP use.
#
# Dependency map (what tracks what):
#   fabric_version               (fabric-api)      MC-tied  -> newest "<api>+<mc>"
#   forge_config_api_port_version                  MC-tied  -> newest "<mc-line>.<build>"
#   neo_form_version                               MC-tied  -> newest "<mc>-<rev>" (no snapshots)
#   fabric_loader_version                          MC-free  -> newest stable
#   fabric-loom (build.gradle plugin)              MC-free  -> newest stable (no -alpha/-beta/-rc)
#   net.neoforged.moddev (build.gradle plugin)     MC-free  -> newest
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PROPS="$REPO_ROOT/gradle.properties"
BUILD="$REPO_ROOT/build.gradle"

prop() { grep -E "^$1=" "$PROPS" | head -1 | cut -d= -f2- | tr -d '[:space:]'; }
# current version of a Gradle plugin from build.gradle: pluginVer <plugin-id>
pluginVer() { grep -E "id '$1'" "$BUILD" | grep -oE "version '[^']+'" | head -1 | cut -d"'" -f2; }

cur_fabric_api="$(prop fabric_version)"
cur_fabric_loader="$(prop fabric_loader_version)"
cur_fcap="$(prop forge_config_api_port_version)"
cur_neoform="$(prop neo_form_version)"
cur_loom="$(pluginVer net.fabricmc.fabric-loom)"
cur_moddev="$(pluginVer net.neoforged.moddev)"

cur_mc="$(prop minecraft_version)"
# Target MC: arg or current; normalise a trailing ".0" (NeoForge "26.2.0" -> "26.2").
tgt_mc="${1:-$cur_mc}"
tgt_mc="${tgt_mc%.0}"
mc_line="$(echo "$tgt_mc" | cut -d. -f1-2)"   # e.g. 26.2  (FCAP keys off this)

esc() { echo "$1" | sed 's/\./\\./g'; }

# Fetch <version> entries from a maven-metadata.xml, version-sorted ascending.
versions() { curl -fsS --max-time 30 "$1" 2>/dev/null \
  | grep -oE '<version>[^<]+</version>' | sed -E 's:</?version>::g' | sort -V; }

last()    { tail -1; }                 # max of a version-sorted stream ("" if empty)
stable()  { grep -viE -- '-(alpha|beta|rc|pre|snapshot)'; }  # drop pre-releases

FABRIC_META="https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml"
LOADER_META="https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml"
LOOM_META="https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/maven-metadata.xml"
NEOFORM_META="https://maven.neoforged.net/releases/net/neoforged/neoform/maven-metadata.xml"
MODDEV_META="https://maven.neoforged.net/releases/net/neoforged/moddev/net.neoforged.moddev.gradle.plugin/maven-metadata.xml"
FCAP_META="https://raw.githubusercontent.com/Fuzss/modresources/main/maven/fuzs/forgeconfigapiport/forgeconfigapiport-common/maven-metadata.xml"

# MC-tied: newest fabric-api built for exactly this MC ("<api>+<mc>").
fabric_api="$(versions "$FABRIC_META" | grep -E "\+$(esc "$tgt_mc")\$" | last || true)"
# MC-tied: newest Forge Config API Port on this MC line ("<mc-line>.<build>").
fcap="$(versions "$FCAP_META" | grep -E "^$(esc "$mc_line")\." | last || true)"
# MC-tied: newest NeoForm rev for this MC ("<mc>-<rev>"), no snapshots.
neoform="$(versions "$NEOFORM_META" | grep -E "^$(esc "$tgt_mc")-[0-9]" | last || true)"
# MC-free build tooling: newest stable.
fabric_loader="$(versions "$LOADER_META" | stable | last || true)"
loom="$(versions "$LOOM_META" | stable | last || true)"
moddev="$(versions "$MODDEV_META" | stable | last || true)"

row() { printf '  %-26s current=%-16s latest=%s\n' "$1" "$2" "${3:-none}"; }

echo "=== randomloot :: Fabric / build-tool version check ==="
echo "target Minecraft: $tgt_mc  (line $mc_line.x)   [current minecraft_version=$cur_mc]"
echo
echo "MC-tied (must match the target Minecraft):"
row "fabric_version (api)"        "$cur_fabric_api"    "$fabric_api"
row "forge_config_api_port"       "$cur_fcap"          "$fcap"
row "neo_form_version"            "$cur_neoform"       "$neoform"
echo
echo "MC-independent (bump to newest stable):"
row "fabric_loader_version"       "$cur_fabric_loader" "$fabric_loader"
row "fabric-loom (build.gradle)"  "$cur_loom"          "$loom"
row "moddev (build.gradle)"       "$cur_moddev"        "$moddev"
echo
echo "# Machine-readable (parse these):"
echo "OUT_TARGET_MC=$tgt_mc"
echo "OUT_CURRENT_FABRIC_API=$cur_fabric_api"
echo "OUT_CURRENT_FABRIC_LOADER=$cur_fabric_loader"
echo "OUT_CURRENT_FCAP=$cur_fcap"
echo "OUT_CURRENT_NEOFORM=$cur_neoform"
echo "OUT_CURRENT_LOOM=$cur_loom"
echo "OUT_CURRENT_MODDEV=$cur_moddev"
echo "OUT_FABRIC_API=$fabric_api"
echo "OUT_FCAP=$fcap"
echo "OUT_NEOFORM=$neoform"
echo "OUT_FABRIC_LOADER=$fabric_loader"
echo "OUT_LOOM=$loom"
echo "OUT_MODDEV=$moddev"
