---
name: update-neoforge
description: Update this multiloader mod to the newest Minecraft version (betas included) and its matching NeoForge build + Fabric/build-tool versions, then migrate the code — reading the NeoForge primer/changelog and looping build→fix until both loader jars compile and gametests pass. Use when asked to bump, update, upgrade, or migrate the NeoForge / Fabric / Minecraft version, or "get on the latest Minecraft".
---

# Update NeoForge + Fabric / Minecraft version

This mod is **multiloader**: one shared `common/` codebase ships both a NeoForge jar
and a Fabric jar (see `claude.md`). A version bump therefore touches two families of
dependency — the NeoForge/vanilla side **and** the Fabric side + shared build tooling —
and must leave **both** loader jars building and both gametest suites green.

Bump the mod to a newer Minecraft version, align every dependency to it in
`gradle.properties` + `build.gradle` + doc references, migrate any broken code, and
verify both loaders still build.

## Versioning scheme

Minecraft uses calendar versioning; NeoForge mirrors it (see `claude.md`):

```
NeoForge  A.B.C.D[-suffix]   ==>   Minecraft A.B.C , NeoForge build D
26.1.2.76                    ==>   Minecraft 26.1.2 (stable)
26.2.0.1-beta                ==>   Minecraft 26.2.0 (pre-release: -beta / -rc)
```

The Minecraft version is the first three segments of the NeoForge version — **except
that the vanilla string drops a trailing `.0`**: NeoForge `26.2.0.11` ==> vanilla MC
`26.2` (that is what `minecraft_version`, `neo_form_version`, `fabric_version` and the
Forge Config API Port version all key off — not `26.2.0`). Parchment is **not** used in
26.x (official Mojang names ship in-box) — there are no `parchment_*` fields.

### Which dependency tracks what

| Field (file) | Tracks | Bump rule |
|---|---|---|
| `neo_version` (gradle.properties) | Minecraft + NeoForge build | the driver — chosen in step 2 |
| `minecraft_version` / `_range` | Minecraft | vanilla MC string (trailing `.0` dropped) |
| `neo_form_version` | Minecraft | newest `<mc>-<rev>` for the target MC |
| `fabric_version` (fabric-api) | Minecraft | newest `<api>+<mc>` for the target MC |
| `forge_config_api_port_version` | Minecraft line | newest `<mc-line>.<build>` |
| `fabric_loader_version` | — (loader) | newest stable |
| `net.fabricmc.fabric-loom` (build.gradle) | — (build tool) | newest stable |
| `net.neoforged.moddev` (build.gradle) | — (build tool) | newest |

## Process

### 1. Discover versions

Two helpers. Run the NeoForge one first — it drives the Minecraft target:

```bash
.claude/skills/update-neoforge/check-versions.sh          # NeoForge / MC candidates
```

It prints current versions and four candidates (with `OUT_*` lines): latest stable /
latest beta **on the current MC line**, and latest stable / latest overall **across all
lines**.

### 2. Choose the target

Policy — **maximize the Minecraft version, betas included.** The goal is the newest
Minecraft release, not necessarily the newest stable NeoForge build:

1. Pick the **highest Minecraft version** available (largest `A.B.C`).
2. Within that line, take the **newest NeoForge build, including `-beta` / `-rc`** —
   the helper's `OUT_ANY_NEO` / `OUT_ANY_MC` candidate. Default to it.

Now resolve every other dependency against that MC with the Fabric-side helper (pass the
**vanilla** MC string — e.g. `26.2`, not `26.2.0`; it also accepts and normalises the
`26.2.0` form):

```bash
.claude/skills/update-neoforge/check-fabric-versions.sh <target_mc>
```

It reports current + latest for `fabric_version`, `forge_config_api_port_version`,
`neo_form_version` (all MC-tied) and `fabric_loader_version`, fabric-loom, moddev
(build tooling), with `OUT_*` lines.

> **Multiloader gate:** if the Fabric helper prints `none` for any **MC-tied** row
> (`fabric_version`, `forge_config_api_port_version`, or `neo_form_version`), that
> Minecraft version is **not yet ready** for a full multiloader bump — one loader's
> upstream hasn't shipped for it. (Fabric API often races ahead of Forge Config API Port
> and NeoForm.) Report this and either drop to the highest MC where **all three** resolve,
> or ask the user whether to proceed NeoForge-only. Do not invent/guess a missing version.

Caveat to **report, not block on**: when the target crosses a Minecraft minor/major line
(e.g. `26.1.x` → `26.2.x`), renamed/removed APIs can break the build on either loader —
that migration is expected and step 6 surfaces it. Because this work belongs on its own
branch, do it on a branch named for the new line (e.g. `26.2.x`, matching the repo's
convention). Proceed without asking unless the user said otherwise.

State which target you picked and why (and note any `none` rows) before editing.

### 3. Edit the version fields

**`gradle.properties`** — set consistently to the chosen `<neo>` / `<mc>` and the
resolved Fabric-side versions:

```properties
minecraft_version=<mc>
minecraft_version_range=[<mc>]
neo_version=<neo>
neo_version_range=[<neo>,)
neo_form_version=<neoform>                    # OUT_NEOFORM
fabric_version=<fabric-api>                    # OUT_FABRIC_API
fabric_loader_version=<fabric-loader>          # OUT_FABRIC_LOADER
forge_config_api_port_version=<fcap>           # OUT_FCAP
```

Leave `loader_version_range` unchanged (it bounds only the FML major version).

**`build.gradle`** — the shared plugin block pins the two build tools; bump if the
helper shows a newer stable:

```groovy
id 'net.fabricmc.fabric-loom' version '<loom>' apply false      # OUT_LOOM
id 'net.neoforged.moddev'     version '<moddev>' apply false     # OUT_MODDEV
```

A loom bump is often **required** on a Minecraft line jump (older loom won't map the new
MC); a moddev bump is usually optional — take it when convenient.

### 4. Sync doc references

If the **Minecraft line** changed, update human-facing version mentions so they don't go
stale:

- `README.md` — intentionally carries **no** version/loader string; leave it that way
  (don't reintroduce one).
- `claude.md` — the **Current Version** block lists every one of these versions
  (Minecraft, NeoForge, ModDevGradle, Fabric loader, fabric-api, fabric-loom, Forge
  Config API Port, NeoForm). Update each field you changed, plus the versioning-note
  example if it now reads as wrong.

Skip this step for a same-line build bump (prose usually only names the MC line, e.g.
"26.2", not the build).

### 5. Read the migration guide

When the bump crosses a Minecraft line, pull the breaking-change notes **before** building
so you can recognise renames in the compile errors:

```bash
.claude/skills/update-neoforge/migration-notes.sh <old_neo> <new_neo>
```

- **Primer** (`primers/<A.B>/index.md`) is the authoritative list of renamed/removed/moved
  vanilla + NeoForge APIs. Multi-line jumps print each line's primer in order — apply in
  sequence. Vanilla API renames in the primer hit **common/** code, so they affect the
  Fabric jar just as much as the NeoForge jar.
- **Changelog** is commit-level context for anything the primer doesn't spell out.

**Fabric-specific migration** the primer won't cover — check these on a line jump:

- **Access widener** (`fabric/src/main/resources/randomloot.accesswidener`) references
  vanilla members by name (`RangeSelectItemModelProperties.ID_MAPPER`, `AxeItem.STRIPPABLES`,
  `ShovelItem.FLATTENABLES`) in the `official` namespace (26.x). If a widened member was
  renamed/moved in vanilla, the AW entry must follow or loom fails at apply time.
- **Mixins** (`fabric/.../mixin/`, 3 of them: `PlayerMixin`, `LivingEntityMixin`,
  `ItemStackMixin`) target vanilla method signatures — a renamed target throws a mixin
  apply error at Fabric runtime/gametest, not at compile.
- **Fabric API deprecations** — a fabric-api bump occasionally moves an event class; the
  fabric-api javadoc/changelog is the reference. Loom itself may change the AW namespace or
  run-config API on a major bump.

Keep all of this handy as your lookup table during the fix loop. Skip this step for a
same-line build bump.

### 6. Build → fix → repeat until BOTH loaders compile and tests pass

Drive the migration as a loop. Do **not** stop at the first error list — keep going until
both loader jars build and both gametest suites pass, or until a genuine blocker.

```bash
./gradlew --refresh-dependencies build          # builds BOTH neoforge + fabric jars (+ neoforge unit tests)
./gradlew :neoforge:runGameTestServer           # NeoForge in-world gametests (headless)
./gradlew :fabric:runGametest                   # Fabric in-world gametests (headless)
```

For a wiki-regen run (only when needed): `RL_PROD=false RL_WIKI_DIR=$PWD ./gradlew
:neoforge:runGameTestServer`.

Each iteration:

1. Run `build`. It compiles the common sources into **both** loader jars, so a common-code
   error fails both and a loader-specific error fails only one — read which project the
   error is under (`:common` errors surface via the loader projects; `:fabric` / `:neoforge`
   prefixes tell you the seam).
2. Collect failures (compile errors first; one missing symbol cascades — re-read after each
   fix). Read the full `gradlew` output, not just the summary.
3. For each failure, find the cause: match the symbol against the **primer** (renamed/moved/
   removed API), then the **changelog**, then the actual class via IDE/jar. Apply the
   **smallest** fix that restores original behavior — update the import/call to the new name
   or signature; do not rewrite logic or change mod behavior to dodge an error. **Keep the
   fix in `common/` when the API is vanilla** (both loaders share it); only touch a loader
   dir for a genuine platform-seam / mixin / AW issue. Match surrounding style.
4. Re-run. Once `build` is green, run **both** gametest suites — a fix can compile on both
   loaders yet only one mixin/AW path breaks at runtime. Repeat.

**Loop discipline:**

- Track the error count across iterations. If it isn't trending down — same error persists
  after a fix, or the count plateaus for ~2 iterations — stop looping and report; you're
  likely guessing.
- **Stop and ask** (don't guess) when a fix needs a real decision: an API removed with no
  drop-in replacement, changed behavior semantics, or a primer-noted redesign. Summarise the
  options instead of inventing an implementation.
- A test *assertion* failure (logic) differs from a *compile/API* failure. Fix API breakage
  freely; for a genuine behavior-test failure, confirm it's migration-caused before changing
  test or code, and flag it.
- If the user only wanted the version bump (not the migration work), or you hit a blocker,
  leave the tree clean (or revert) and report exactly what remains.

Report the real outcome each run — never claim green without the passing output, and say it
per loader (NeoForge build/tests, Fabric build/tests).

### 6b. Provisioning Gradle in a cloud sandbox

A local `./gradlew` run is the preferred loop — it's the fastest way to see a compile error,
so make it run even in the cloud sandbox. The wrapper's `distributionUrl` 307-redirects from
`services.gradle.org` to a `gradle/gradle-distributions` GitHub release asset, and the
sandbox's GitHub proxy 403s any repo not attached to the routine (`GitHub access to this
repository is not enabled`). But the sandbox has open egress to non-GitHub hosts, so fetch
the distribution from a direct mirror instead of giving up on the local build. (Dependency
resolution — `maven.neoforged.net`, `maven.fabricmc.net`, Mojang's `piston-*` /
`libraries.minecraft.net` / `resources.download.minecraft.net`, `api.foojay.io` for the Java
25 toolchain — is not GitHub-proxied and already works.)

Provision Gradle from a mirror, integrity-pinned to the official checksum, then revert so the
committed wrapper stays on the canonical URL (local dev and CI reach GitHub fine and this
keeps their download fast):

```bash
GWP=gradle/wrapper/gradle-wrapper.properties
VER=$(sed -n 's/.*gradle-\(.*\)-bin\.zip/\1/p' "$GWP")
# official checksum — this endpoint is NOT GitHub and is reachable:
SUM=$(curl -sL "https://services.gradle.org/distributions/gradle-$VER-bin.zip.sha256")
# repoint at a direct mirror (bytes verified == official) + pin the checksum.
# rewrite via a temp file so it's portable (the cloud sandbox is Linux, not BSD sed)
# and idempotent (drops any existing url/sum line before re-adding):
{ grep -vE '^distribution(Url|Sha256Sum)=' "$GWP"
  echo "distributionUrl=https\\://mirrors.cloud.tencent.com/gradle/gradle-$VER-bin.zip"
  echo "distributionSha256Sum=$SUM"
} > "$GWP.tmp" && mv "$GWP.tmp" "$GWP"
# ...run the step-6 build/gametest loop normally; ./gradlew now downloads from the mirror...
git checkout -- "$GWP"   # BEFORE committing — keep the PR on the canonical URL
```

Fallback mirror if Tencent is unreachable:
`https://mirrors.aliyun.com/macports/distfiles/gradle/gradle-$VER-bin.zip` (same bytes). The
`distributionSha256Sum` pin means a tampered or truncated mirror download fails the build
rather than compiling something unverified, so trusting a mirror is safe.

Only if BOTH mirrors are unreachable — never merely because the GitHub path 403s — fall back
to CI as the verification of record. **Do not report a bump as unverified in that case, and
never claim green from inspection.** `.github/workflows/gradle.yml` runs exactly the three
commands from step 6, on both loaders, on a properly provisioned runner. Push and wait on it:

```bash
git push -u origin <branch>
gh run watch <run-id> --exit-status    # blocks; non-zero exit if CI fails
gh pr checks <pr>                      # per-check summary
```

Then read the actual CI log — not just the green check — and pull the real numbers out of it,
the same way you would read local output:

```bash
gh run view <run-id> --log | grep -E "BUILD SUCCESSFUL|BUILD FAILED|tests are now running|error:"
```

The gametest step prints `N tests are now running` per loader; quote those counts in the
report. If CI fails, that's a real failure — resume the step-6 loop, fix, push again. Only the
*location* of the build moves; the "don't stop until both loaders are green" discipline is
unchanged. Prefix `gh` with `env -u GITHUB_TOKEN` if it 401s.

### 7. Wrap up

Summarize: old → new versions (all fields in the step-3 table), files changed, migration
fixes applied (grouped by primer change, and noting which were common vs loader-specific),
and the final build/gametest result **for each loader**, saying where it ran (local or CI,
with the run URL) and quoting the gametest counts. A CI-verified bump is fully verified — do
not caveat it as unverified. For opening a PR, follow the
project's PR workflow (target the matching `26.x.x` branch — for a line jump, the new
`26.2.x`; a bot pushes wiki-regen commits, so rebase before pushing) — see the project
memory.
