---
name: update-neoforge
description: Update this mod to the newest Minecraft version (betas included) and its matching NeoForge build, then migrate the code — reading the NeoForge primer/changelog and looping build→fix until it compiles and gametests pass. Use when asked to bump, update, upgrade, or migrate the NeoForge / Minecraft version, or "get on the latest Minecraft".
---

# Update NeoForge / Minecraft version

Bump this mod to a newer NeoForge build (and its matching Minecraft version),
update the version fields in `gradle.properties` + doc references, and verify the
build still passes.

## Versioning scheme

Minecraft uses calendar versioning and NeoForge mirrors it (see `claude.md`):

```
NeoForge  A.B.C.D[-suffix]   ==>   Minecraft A.B.C , NeoForge build D
26.1.2.76                    ==>   Minecraft 26.1.2 (stable)
26.2.0.1-beta                ==>   Minecraft 26.2.0 (pre-release: -beta / -rc)
```

The Minecraft version is always the first three segments of the NeoForge version.
Parchment is **not** used in 26.x (official Mojang param names ship in-box) — there
are no `parchment_*` fields to touch.

## Process

### 1. Discover versions

Run the helper — it reads the current versions from `gradle.properties` and queries
the NeoForged Maven metadata:

```bash
.claude/skills/update-neoforge/check-versions.sh
```

It prints the current versions and four candidates (and their machine-readable
`OUT_*` lines): latest stable / latest beta **on the current MC line**, and latest
stable / latest overall **across all lines**.

### 2. Choose the target

Policy — **maximize the Minecraft version, betas included.** The goal is to be on
the newest Minecraft release, not necessarily the newest stable NeoForge build:

1. Pick the **highest Minecraft version** available (the largest `A.B.C`).
2. Within that line, take the **newest NeoForge build, including `-beta` / `-rc`**.

This is exactly the helper's `OUT_ANY_NEO` / `OUT_ANY_MC` candidate (latest overall,
pre-releases included). Default to it.

Caveat to **report, not to block on**: when the target crosses a Minecraft
minor/major line (e.g. `26.1.x` → `26.2.x`), renamed/removed APIs can break the
build — that migration is expected and the verify step (5) will surface it. Because
this work belongs on its own branch, do it on a branch named for the new line (e.g.
`26.2.x`, matching the repo's branch convention) rather than committing the jump
onto the current line's branch. Proceed without asking unless the user said
otherwise.

State which target you picked and why before editing.

### 3. Edit `gradle.properties`

Set all four fields consistently to the chosen `<neo>` / `<mc>`:

```properties
minecraft_version=<mc>
minecraft_version_range=[<mc>]
neo_version=<neo>
neo_version_range=[<neo>,)
```

Leave `loader_version_range` unchanged (it bounds only the FML major version).

### 4. Sync doc references

If the **Minecraft line** changed, update human-facing version mentions so they don't
go stale:

- `README.md` — the "This branch targets **Minecraft 26.1 on NeoForge**" line.
- `claude.md` — the versioning-note example, only if it now reads as wrong.

Skip this step for a same-line build bump (the prose usually only names the MC
line, e.g. "26.1", not the build).

### 5. Read the migration guide

When the bump crosses a Minecraft line, pull the breaking-change notes **before**
building so you can recognize renames in the compile errors. The helper fetches the
NeoForged migration primer(s) for every line crossed plus the changelog since the
current build:

```bash
.claude/skills/update-neoforge/migration-notes.sh <old_neo> <new_neo>
```

- **Primer** (`primers/<A.B>/index.md`) is the authoritative list of renamed/removed/
  moved vanilla + NeoForge APIs — e.g. "`26.1.x -> 26.2` Mod Migration Primer". If the
  jump spans several lines, the helper prints each line's primer in order; apply them
  in sequence.
- **Changelog** is commit-level context for anything the primer doesn't spell out.

Keep these notes handy — they are your lookup table during the fix loop. Skip this
step for a same-line build bump (no breaking changes).

### 6. Build → fix → repeat until it compiles and tests pass

Drive the migration as a loop. Do **not** stop at the first error list — keep going
until the build is green and gametests pass, or until you hit a genuine blocker.

```bash
./gradlew --refresh-dependencies build           # compile + assemble
RL_PROD=false RL_WIKI_DIR=$PWD ./gradlew runGameTestServer   # gametests
```

Each iteration:

1. Run the build. If it compiles, run the gametest server.
2. Collect the failures (compile errors first; a single missing symbol can cascade,
   so re-read after each fix). Read the full `gradlew` output, not just the summary.
3. For each failure, find the cause: match the symbol against the **primer** (renamed/
   moved/removed API), then the **changelog**, then the actual class via the IDE/jar
   if needed. Apply the **smallest** fix that restores the original behavior — update
   the import/call to the new name or signature; do not rewrite logic or change mod
   behavior to dodge an error. Match the surrounding code's style.
4. Re-run. Repeat.

**Loop discipline:**

- Track the error count across iterations. If it isn't trending down — the same error
  persists after a fix, or the count plateaus for ~2 iterations — stop looping and
  report; you're likely guessing.
- **Stop and ask** (don't guess) when a fix needs a real decision: an API was removed
  with no drop-in replacement, behavior semantics changed, or the primer says a
  feature was redesigned. Summarize the options instead of inventing an implementation.
- A test *assertion* failure (logic) is different from a *compile/API* failure. Fix
  API breakage freely; for a genuine behavior-test failure, confirm it's migration-
  caused before changing test or code, and flag it.
- If the user only wanted the version bump (not the migration work), or you hit a
  blocker, leave the tree in a clean state (or revert) and report exactly what remains.

Report the real outcome each run — never claim green without the passing output.

### 7. Wrap up

Summarize: old → new versions, files changed, the migration fixes applied (grouped by
primer change), and the final build/gametest result. For opening a PR, follow the
project's PR workflow (target the matching `26.x.x` branch — for a line jump, the new
`26.2.x`; a bot pushes wiki-regen commits, so rebase before pushing) — see the project
memory.
