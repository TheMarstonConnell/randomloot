# Random Loot 2 - Minecraft Mod

> **Claude: Whenever you discover something important during development (API quirks, patterns, gotchas, or useful snippets), automatically add it to this file so you'll remember it in future sessions.**

## Overview
An RPG-style loot system mod for Minecraft that generates randomized tools with modifiers/traits. **Multiloader**: one shared codebase (`common/`) shipping both a NeoForge jar and a Fabric jar.

**Vocabulary**: `GLOSSARY.md` (repo root) defines the project's words - gear, trait, case, goodness, forger, reveal roll - and names its modules (loader shim, loader hook contract, gametest table, trait grant, trait dispatch, gear roll, XP step, trait declaration, trait registry, config setting, wiki generation). Use those words in code comments and PRs, and when you introduce a module, add its name there.

## Current Version
- **Minecraft**: 26.3 (fabric/NeoForm artifacts use `26.3`; NeoForge builds are `26.3.0.x`)
- **NeoForge**: 26.3.0.76 · **ModDevGradle**: 2.0.148
- **Fabric**: loader 0.19.5, fabric-api 0.162.0+26.3, fabric-loom 1.18.4
- **Forge Config API Port**: 26.3.1 (NeoForge config API on Fabric; bundled jar-in-jar)
- **Gradle**: 9.8.1 (wrapper) — fabric-loom 1.18 requires Gradle ≥9.7 and a Java 25 launcher JVM
- **Java**: 25 (toolchain auto-provisioned via foojay-resolver-convention 1.0.0; loom 1.18 also requires the Gradle launcher JVM itself to be 25)
- **Mod ID**: `randomloot`
- **Package**: `dev.marston.randomloot`

> **Versioning note:** Minecraft moved to calendar versioning. NeoForge `26.3.0.76` = MC `26.3`, build `76`. The vanilla version string has NO trailing `.0` — `com.mojang:minecraft:26.3`, NeoForm `26.3-1`. Parchment is no longer used: MC ships deobfuscated with official Mojang names, which is also why Fabric needs no intermediary remapping anymore (loom has no `mappings`/`modImplementation` — use plain `implementation`).

## Multiloader Architecture
- `common/` — 95% of the code; compiles against vanilla only (ModDevGradle `neoFormVersion`) + a `compileOnly` stub of `fuzs.forgeconfigapiport:forgeconfigapiport-common-neoforgeapi` so `Config` (ModConfigSpec) lives here. "Vanilla only" includes vanilla's access levels: a member that NeoForge's patched jar or a Fabric API access widener makes public is still private or protected here, even though the same line compiles in `neoforge/` and `fabric/`. `CreativeModeTabs.TOOLS_AND_UTILITIES` (private) and `CreativeModeTab.Output` (protected) are two; that is why `ModItems.CREATIVE_TAB` is built from the tab's id and `GameHooks.creativeTab` takes a `Consumer<ItemStack>`.
- `neoforge/`, `fabric/` — loader projects compile the **common sources** into their jars (`commonJava`/`commonResources` configurations from `build-logic/`), so each shipped jar is self-contained.
- **Platform seam** (`dev.marston.randomloot.platform`): `Services.PLATFORM`/`Services.REG` resolved via Java `ServiceLoader` (bindings in each loader's `META-INF/services/`). `RegHelper` = registration (NeoForge: DeferredRegisters attached in the mod ctor; Fabric: immediate `Registry.register`). `IPlatformHelper` = item factories (NeoForge subclasses add `canPerformAction`/`supportsEnchantment`/`isCombineRepairable` extension overrides), `canHarvestBlock`, `tooltipLevel`. (Strip/scrape/wax/flatten resolution is **not** a platform method — since 26.3 it's vanilla-only and lives in common `LootItem`, reading the datapack `BLOCK_TRANSFORMER` registry.)
- **Event seam = the loader hook contract** (`dev.marston.randomloot.platform`): `GameHook` names every game event the mod needs from a loader, and `GameHooks` has one static method per hook. **A loader shim calls nothing in common but `GameHooks`.** Each method's javadoc is the contract for its hook - the stage of the game event the shim must call at, the side, the scope - and the method forwards to the code that owns the behaviour (`KillDispatcher.onLivingDeath`, `ArmorDispatcher.onLivingDamagePre/Post/onArmorHurt`, `Soulbound.modifyBreakSpeed`, `LootItem.afterUseItem/afterUseItemOn`, `BlockHighlighter.onServerTick/onServerStopping`, `ModCommands.register`, `Config.onLoad`, `LootInjection`, `TraitAdditionRecipe.onTake`), or decides in place where the rule is a line or two (the anvil-combine predicate, the enchant gate's default, the creative tab's items). A shim method is an unpacking of the loader's event plus that one call: no guard, no null-to-default mapping, no loop over the mod's items. If a shim needs an `if` about gear, the `if` belongs behind `GameHooks`. NeoForge shim: `neoforge/.../NeoForgeEvents` (events), `mixin/ServerPlayerGameModeMixin` (AFTER_USE_ITEM), the `supportsEnchantment` overrides on `NeoForgeLootItem`/`NeoForgeLootArmorItem` (ENCHANT_GATE), `loot/CaseLootModifier` (LOOT_INJECTION). Fabric shim: `RandomLootFabric.registerHooks` (Fabric API events) + mixins for the seven hooks Fabric API has no event for, or none at the right stage (`LivingEntityMixin` + `PlayerMixin` DAMAGE_PRE and DAMAGE_POST, `ItemStackMixin` ARMOR_HURT, `PlayerMixin` BREAK_SPEED, `ServerPlayerGameModeMixin` AFTER_USE_ITEM, `AnvilMenuMixin` ANVIL_COMBINE, `SmithingMenuMixin` SMITHING_TAKE). Not a hook, but also once per loader: the client registration of the `randomloot:cosmetic` range property (`NeoForgeClientEvents`, `RandomLootFabricClient`).
- **Derived data components**: per-stack attributes (attack / armor+toughness) and MAX_DAMAGE are vanilla components rebuilt by `LootUtils.refreshDerivedComponents(stack)`. **`GearTags.mutate` calls it for you** — that is the whole point of the seam, so never pair a raw read with a raw write. `GearTags.write` is the escape hatch that skips the refresh. It is for a caller that writes several namespaces and then refreshes once itself: `LootUtils.stamp` (every namespace of freshly generated gear, one refresh at the end) and `LootUtils.startRoll`. The migration gametest uses it for the opposite reason, to build a stack that never had its components stamped. Everything else goes through `mutate`; if you build a gear stack by hand, call `refreshDerivedComponents` yourself like `CloneItem` does. `migrateDerivedComponents` in inventoryTick upgrades pre-component items but **bails as soon as ATTRIBUTE_MODIFIERS exists**, so it cannot rescue a stack that was stamped once and then mutated. These replaced NeoForge's dynamic `getDefaultAttributeModifiers`/`getMaxDamage` item overrides — never reintroduce loader-only dynamic item methods for stats.
- **Hook delivery check**: calling a `GameHooks` method records its hook as delivered; nothing is declared by hand (the old `GameHooks.bind` list is gone - it stayed "bound" after its handler was deleted). The `loader_hooks_all_bound` gametest makes the game do the thing behind each hook (a hit on an armored player, a kill, a destroy-speed query, an item use, a creative tab build, an anvil, an enchanting-table roll, a chest roll, a smithing take) and fails naming any hook that never arrived, so it covers a mixin missing from the mixins json and an item override or loot modifier file that is not there, not only a forgotten listener. Add a hook -> the `GameHook` constant, its `GameHooks` method with the contract in its javadoc, a call from **both** shims, and a step in that gametest. `SERVER_STOPPING` is the one hook it cannot see (a gametest ends before the server stops).
- **DAMAGE_POST hands over the damage taken, on both loaders**: armor XP comes from what the wearer actually took - after a shield, armor, enchantments, Resistance and the wearer-hurt traits, before absorption - delivered at the end of `hurtServer`, for a killing hit too (after the entity died and dropped its loot, so a dead player's dropped armor earns nothing). NeoForge's `LivingDamageEvent.Post.getInflictedDamage()` is that number. Fabric API's `AFTER_DAMAGE` is **not**: it passes `hurtServer`'s own `damage` parameter, which vanilla only reassigns for the shield, freezing and helmet steps, so it is the hit before armor, and it never fires for a killing hit. The Fabric shim therefore does not use it: `LivingEntityMixin`/`PlayerMixin` keep what `GameHooks.damagePre` returned on the entity (`DamageTakenCarrier`) and `LivingEntityMixin` hands it to `GameHooks.damagePost` at `hurtServer` TAIL. Until that was done Fabric gave 10 XP where NeoForge gave 4 (hit for 10 against 20 armor), 0 for 2 (half-blocked), 6 for 0 (fall that maxed Featherweight cancels) and 0 for 20 (killing hit). Regression net: `armor_xp_per_hit`, seven cases with exact XP, one of them a bigger hit inside the first one's invulnerability, which takes the other path through `hurtServer`.
- **Smithing take gotcha** (verified against 26.3 `ItemCombinerMenu.quickMoveStack`): on a shift-click the result is moved into the inventory first and `SmithingMenu.onTake` is then handed an *empty* stack, so `ItemStack.onCraftedBy` resolves to air and an item's `onCraftedBy` never runs. Anything that must happen on every take belongs on `GameHook.SMITHING_TAKE` (`TraitAdditionRecipe.onTake`: charges a trait recipe's extra `count - 1`, since vanilla only shrinks each input by one, and - for the Addition Template only - fires the `trait_obtained` criteria behind `add_trait`), not on the result item. The hook cannot reach the taken stack either - on a shift-click NeoForge's `ItemSmithedEvent.getResult()` is a copy and Fabric's mixin gets the empty one - so it re-assembles the result from the inputs, which are still whole at that point, and only reads it. Anything that has to be written *onto* the gear goes on when the result is built: `TraitAdditionRecipe.assemble` applies the world-forger stamp (through `TraitGrant`), using the seed its last server-side `matches` saw because `assemble` is handed no level. Regression nets: `smithing_craft_sequence` (click), `smithing_shift_click_consumes_count` (shift-click).
- **Loot injection**: `loot/LootInjection` owns which tables, what chance, and the roll itself; a shim says which table rolled and where the stacks go. The loaders see a roll differently and each keeps its scope (documented on `LootInjection`): NeoForge = GLM (`CaseLootModifier` + `data/randomloot/loot_modifiers/`, lives in `neoforge/`), run once per injected item on a finished top-level roll with the queried table's id (`rollItem`); Fabric = `LootTableEvents.MODIFY_DROPS`, fired for every table with an id as it rolls, nested ones included (they share the parent's `LootContext`), so `rollOncePerContext` injects once per context - adding a pool to every matching table via `MODIFY` multiplied trial-vault payouts, whose `reward` table rolls `reward_common/_rare/_unique`. No vanilla 26.3 roll comes out differently (all 1,447 tables checked: none nests a matching table inside a non-matching one). A datapack or mod that does nest one gets the injection on Fabric and not on NeoForge. `loot_injection_rolls_once_per_chest` pins exactly one case and one template per roll with both chances forced to 1.
- **Config**: same `randomloot-common.toml` on both loaders (FCAP on Fabric). Register: NeoForge `modContainer.registerConfig`, Fabric `ModConfigEvents.loading/reloading` listeners and then `ConfigRegistry.INSTANCE.register`, **in that order** (in `RandomLootFabric.onInitialize`, `registerHooks()` runs before the config is registered): Forge Config API Port loads a COMMON config inside `register`, so a loading listener added after it never hears the first load. That was the order until 26.3, and Fabric ran on the defaults whatever the file said (`caseChance = 0.5` in the file: NeoForge 0.5, Fabric 0.25) unless the file was edited while the game was running. Nothing caught it because every setting starts out holding its default, so a generated file reads the same loaded or not. Two gametests hold it now. The Fabric-only `configLoads` checks a value that differs: `seedGametestConfig` in `fabric/build.gradle` writes `livingWhispersPerMinute = 7` into `fabric/runs/gametest/config/` before every `:fabric:runGametest` (so every Fabric gametest runs with 7, not the default). And `loader_hooks_all_bound`, which found the gap, requires CONFIG to have been delivered on Fabric as on NeoForge; its Fabric-only excuse is gone. Both fail with the order swapped back (measured, two runs of two: `runs with 1` and `never delivered these hooks: [CONFIG]`). `configLoads` used to pass in that state when the shared tests ran first, because a test helper called `Config.onLoad()` itself; tests now substitute a setting instead (see **Changing config in a test** under Testing), so nothing but the loader's event reads the file. So do not put the Fabric excuse back.
- **Config settings** (`Config.java`): a setting is one `Config.Setting` constant - key, default, range, comment and `Group` - and the spec entry, the CONFIG.md row and the live value all derive from it. Adding a setting is that one line, above `SPEC` (one declared below it fails class init with a message saying so). Read it with `Config.CASE_CHANCE.get()`: the default until the file loads, then whatever the file said at the last load/reload event. A `Group` is both the toml section and the CONFIG.md table (`PROGRESSION` is the section the file has always called `Misc`). Per-trait toggles are declared from `ModifierRegistry`, one `<tag>_enabled` each: `Config.traitEnabled(tag)`. **The generated file is what players already have** - never rename a key or section, reorder, or reword a default, range or comment as a side effect; diff a freshly generated `neoforge/run/config/randomloot-common.toml` against one generated on the commit before (move the old file aside first: an existing one is corrected in place). Nets: `ConfigSettingTest`, gametest `disabled_trait_is_filtered`.
- **Anvil and enchanting, per loader**: combining two pieces of gear on an anvil is refused on both (`GameHooks.blocksAnvilCombine`; NeoForge cancels `AnvilUpdateEvent`, Fabric's `AnvilMenuMixin` empties the result; `anvil_cannot_combine_loot_gear`). Enchant gating is `GameHooks.enchantGate` on both: NeoForge reaches it through the `supportsEnchantment` item extension, Fabric through `EnchantmentEvents.ALLOW_ENCHANTING`, which Fabric API fires from its own enchantability check rather than from `ItemStack.supportsEnchantment` (NeoForge-only). The table path (`enchanting_table_filters_by_type`) and the anvil-with-a-book path (`anvil_books_filter_by_type`) are pinned on both.
- **Known Fabric gaps** (measured on 26.3; the startup config gap once listed here is closed, see **Config**): **two pieces of gear still combine outside the anvil** (not fixed; it changes what Fabric players get, so it needs the author's go-ahead): NeoForge's `isCombineRepairable=false` also switches off vanilla's crafting-grid repair and the grindstone merge, and Fabric has nothing for those. Two damaged Random Tools in a crafting grid match `minecraft:repair_item` and give a `randomloot:tool` with no type or traits; in a grindstone they give the first one fully repaired with its traits, eating the second.
- **`mineBlock` ordering differs by loader** (verified against 26.3 sources): vanilla/Fabric `ServerPlayerGameMode.destroyBlock` calls `level.removeBlock` *before* `Item.mineBlock`; NeoForge patches it to call `mineBlock` *first*. So re-reading `level.getBlockState(pos)` from a block-break trait gives the real block on NeoForge but air on Fabric - Veiny, Lumbering and Prospector were silently dead on Fabric for that reason. `BlockBreakModifier.startBreak(stack, broken, pos, player)` now receives the broken state; use it, and treat the origin as already gone when walking outward from it. Regression nets: `lumbering_fells_tree`, `veiny_breaks_vein` (they go through `gameMode.destroyBlock`).
- **Hand swap animation**: the client replays the re-equip animation whenever a component on the held stack changes, unless its type was built with `.ignoreSwapAnimation()` (vanilla only flags `damage`). `tool_mod` changes on nearly every use (XP), so it is flagged too. Singleplayer never showed the problem: the in-memory connection hands the client the server's own component objects, and `GearTags.mutate` used to edit the stored `CompoundTag` in place, so old and new stacks always compared equal. A dedicated server serializes, so test component-change behaviour there.
- **Fabric access widener** (`fabric/src/main/resources/randomloot.accesswidener`, namespace `official` — NOT `named` — since 26.x): `RangeSelectItemModelProperties.ID_MAPPER` (texture property registration). (26.3 removed the `AxeItem.STRIPPABLES`/`ShovelItem.FLATTENABLES` maps — strip/flatten now resolve through the datapack `BLOCK_TRANSFORMER` registry in common `LootItem` (vanilla API, no longer a platform method), so those AW entries are gone.)
- **GameTests**: a shared test is two edits, both in common, and nothing per loader: (1) a body in `common/.../gametest/GameTestBodies.java` (`public static void name(GameTestHelper)`, vanilla + common APIs only), (2) one row in the **gametest table**, `common/.../gametest/GameTestTable.java`: `row("snake_name", GameTestBodies::name)` (200 ticks; write `new Row(name, body, ticks)` for a longer limit). Each loader has an adapter that loops `GameTestTable.rows()`, so a shared test has the id `randomloot:<snake_name>`, the `randomloot:empty` 3x3x3 structure and the `randomloot:default_env` environment on both, and cannot exist on one loader only. A body with no row never runs; the JUnit `GameTestTableTest` fails on that (it compares counts, so keep helpers in `GameTestBodies` private). A test for one loader only stays in that loader's adapter class: NeoForge `loot_modifiers_load`, `enchant_type_filtering`, `armor_enchant_filtering`; Fabric `configLoads`, `gametestTableRegistered`. Two rows are **wiki generation** (`wiki_generates`, `wiki_needs_every_trait_recipe`; see the GenWiki paragraphs at the end of this file). A run's total is table rows + that loader's own tests + vanilla's `minecraft:always_pass`. Unit tests: `./gradlew :neoforge:test` (73 across `GearStatsTest`, `GearRollTest`, `TraitGrantTest`, `TraitEligibilityTest`, `ArmorTraitGatingTest`, `ModifierLevelTest`, `LootUtilsMathTest`, `ForgerWorldConstantTest`, `LootInjectionTableTest`, `WhisperLimiterTest`, `GameTestTableTest`, `TraitFixtureTest`, `GraceNameTest`, `ConfigSettingTest`).
- **Gametest table adapters** (how the rows reach each loader's test registry): NeoForge `RandomLootGameTests.onRegisterGameTests` registers an `RLTestInstance` per row through `RegisterGameTestsEvent`, which NeoForge only fires when `GameTestHooks.isGametestEnabled()`. Fabric `RandomLootFabricGameTests.registerShared` (called from mod init) does what Fabric API's `@GameTest` support does, by hand: each body goes into `BuiltInRegistries.TEST_FUNCTION` during mod init, then a vanilla `FunctionGameTestInstance` per row goes into the dynamic `TEST_INSTANCE` registry from registry-sync's `DynamicRegistrySetupCallback` (verified on fabric-api 0.166.2: it fires server-side only, after the empty registries are created and before any datapack entry loads; the view for other registry groups has no test registries, so check `getOptional`). Fabric API registers its own instances just before the freeze from a mixin with no public hook - do not mixin into that lambda. Gate: `-Dfabric-api.gametest` or a dev environment, the same gate Fabric API uses. Nothing would fail by itself if that callback stopped firing after a Fabric API bump - the run would pass with the shared tests gone - so the Fabric-only `gametestTableRegistered`, registered by the annotation route, asserts every row is in the registry. Fabric's `@GameTest` methods get the id `randomloot:random_loot_fabric_game_tests_<name>` and an 8x8x8 structure, which is why only the Fabric-only tests still use the annotation. To list the ids a Fabric run executed: `JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.report-file=<path> ./gradlew :fabric:runGametest` (JUnit XML).

## Useful Links
- [NeoForge Versions](https://projects.neoforged.net/neoforged/neoforge) - Find latest NeoForge versions
- [Parchment Mappings](https://parchmentmc.org/docs/getting-started) - Better parameter names
- [ModDevGradle](https://github.com/neoforged/ModDevGradle) - Build plugin documentation
- [NeoForge Docs](https://docs.neoforged.net/) - Official NeoForge documentation

## Updating Dependencies
When updating NeoForge version in `gradle.properties`:
```bash
./gradlew --refresh-dependencies    # Download new dependencies
./gradlew build                      # Or just build (auto-downloads)
```

If you encounter cache issues:
```bash
rm -rf ~/.gradle/caches/neoformruntime
./gradlew build
```

## Project Structure
```
build-logic/                     # Gradle convention plugins (multiloader-common / multiloader-loader)
common/src/main/java/dev/marston/randomloot/
├── RandomLoot.java              # Loader-neutral core: MODID, LOGGER, init(), commonSetup()
├── Config.java                  # Config settings, one declaration each (FCAP stub makes this common)
├── GenWiki.java                 # Wiki generation: the root *.md docs + lang, from loaded data (runs as a gametest)
├── platform/                    # Services, IPlatformHelper, RegHelper, ToolAction
├── component/                   # Data components
├── items/                       # ModItems (registers via Services.REG/PLATFORM)
├── gametest/                    # GameTestBodies (shared bodies) + GameTestTable (one row per shared test)
├── commands/  advancements/     # Plain-method dispatch, loader shims call in
├── loot/                        # Core loot system + modifiers/ (breakers, holders, hurters, stats, users)
│   ├── LootGearItem.java        #   shared base for LootItem + LootArmorItem
│   ├── ToolType.java            #   the 8 gear types (top-level, NOT nested in LootItem)
│   ├── GearStats.java           #   pure stat/XP/goodness formulas over (goodness, ToolType), and the XP step
│   ├── GearRoll.java            #   pure decision of what a case produces; LootUtils.stamp makes the item
│   ├── GearTags.java            #   per-stack tag namespaces; mutate() refreshes components
│   ├── TraitEligibility.java    #   the one "can this trait go on this gear" rule
│   ├── TraitGrant.java          #   puts a trait on gear or takes it off; returns what happened
│   ├── modifiers/TraitDispatch.java #   carries a game event to the traits on a piece of gear
│   └── modifiers/ModifierRegistry.java #   the trait registry: one TraitDeclaration line per trait
└── recipes/                     # Custom recipes
common/src/main/resources/       # All assets + data (loader-agnostic)
neoforge/src/main/java/…/neoforge/   # @Mod entry, event shims, reg helper, item subclasses
neoforge/src/main/java/…/{ModLootModifiers,loot/CaseLootModifier,gametest/RandomLootGameTests}.java
neoforge/src/main/resources/     # neoforge.mods.toml, META-INF/services, data/randomloot/loot_modifiers
neoforge/src/test/                # JUnit unit tests (ModDevGradle unitTest)
fabric/src/main/java/…/fabric/   # ModInitializer, client init, reg/platform helpers, mixins, gametests
fabric/src/main/resources/       # fabric.mod.json, mixins json, accesswidener, META-INF/services
```

## Key Systems

### Gear Roll
Gear is decided first and written second.
- **Decide**: `GearRoll.roll(goodness, scene, armorChance, random)` (or `GearRoll.ofType(type, ...)` for a set type) returns a `GearRoll.Rolled`: type, goodness, traits, name and colour, lore, texture, place and owner. It takes no `ItemStack` and no `Level`, and the one thing it reads from outside its arguments is which traits are switched on (the gate reads those toggles), so `GearRollTest` checks the armor share, the trait picks and the texture range with a seed.
- **Stamp**: `LootUtils.stamp(rolled)` is the one place generated gear is written. Every namespace goes on raw and the derived components are built once.
- **Entry points** (`LootUtils`), which read the world, the config and the opener's stat and then call the two above: `openCase(player)` (a player's case; fires `case_opened` and `trait_obtained`), `dispenseCase(level, pos)` (a dispenser; no owner, no stats, no criteria), `buildTool(player, level, pos, type, goodness)` (a set type; `/randomloot give` at goodness 0).
- Goodness scales with the opener's cases-opened stat (`GearStats.goodnessForCases`). The stat is awarded *after* the case is opened, so `openCase` reads "opened before this one" once and passes `+ 1` to the criteria; do not read the stat a second time.
- Name colour comes from the dimension, the adjective from the biome's temperature and the weather, the forger in the lore from the world seed.

### Modifier System
- A trait is one declaration line in `ModifierRegistry` plus a class that holds its rule (see "Adding New Modifiers")
- Trait classes implement the hook interfaces for the events they react to: `BlockBreakModifier`, `HoldModifier`, `EntityHurtModifier`, `UseModifier`, `StatsModifier`
- Stored in item's `ToolModifier` data component
- Can be added/removed via smithing table recipes

### Tool Types
- PICKAXE, AXE, SHOVEL, SWORD (defined in `LootItem.ToolType`)

### Biome-Specific Traits
Some modifiers are restricted to tools generated in specific biomes. These implement `BiomeRestrictedModifier` interface.

#### How It Works
1. When a tool is generated, biome data is stored in the tool's NBT under "info" tag:
   - `biomeTemp` - Temperature value (0.0 to 2.0+)
   - `biomeKey` - Full registry key (e.g., "minecraft:ocean")
   - `dimension` - Dimension key (e.g., "minecraft:the_nether")
2. Biome-restricted modifiers only spawn on tools generated in matching biomes
3. Smithing table recipes also check biome restrictions - you cannot add a biome trait to a tool from an incompatible biome

#### Biome-Restricted Modifiers

| Modifier | Biome Restriction | Recipe Item | Effects |
|----------|-------------------|-------------|---------|
| **Aquatic** | Ocean/river biomes (key contains "ocean" or "river") | Prismarine Shard | Water breathing + Haste underwater |
| **Frozen** | Cold biomes (temp ≤ 0.15) | Packed Ice | Slowness on hit, frost walker effect |
| **Scorched** | Hot biomes (temp ≥ 1.0) or Nether | Blaze Powder | Fire damage on hit, fire resistance |
| **Overgrown** | Jungle/swamp/bamboo biomes | Vine | Extra arthropod damage, poison immunity |
| **Void-Touched** | The End dimension only | Ender Pearl | Right-click teleport (8-16 blocks) |

#### Adding a New Biome-Restricted Modifier
```java
public class MyBiomeModifier extends AbstractModifier implements EntityHurtModifier, BiomeRestrictedModifier {
    // ... the trait's rule, as for any other trait ...

    @Override
    public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
        // Example: Desert biomes only
        return biomeKey != null && biomeKey.contains("desert");

        // Or temperature-based: return temperature >= 1.5f;
        // Or dimension-based: return dimension.equals("minecraft:the_nether");
    }
}
```

## Build Commands
```bash
./gradlew build                        # Build both loader jars (+ runs neoforge unit tests)
./gradlew :neoforge:runClient          # NeoForge client
./gradlew :neoforge:runServer          # NeoForge server
./gradlew :neoforge:runGameTestServer  # NeoForge in-world gametests (headless)
./gradlew :neoforge:test               # JUnit unit tests
./gradlew :fabric:runClient            # Fabric client
./gradlew :fabric:runServer            # Fabric server
./gradlew :fabric:runGametest          # Fabric in-world gametests (headless, -Dfabric-api.gametest)
```
Jars land in `neoforge/build/libs/` and `fabric/build/libs/` (FCAP nested in the fabric jar).

## Migration Notes (1.21.4 → 1.21.11)

### API Changes Applied
- `ResourceLocation` → `Identifier`
- `CompoundTag.getInt/getString/getFloat/getCompound()` → `getIntOr/getStringOr/getFloatOr/getCompoundOrEmpty()`
- `CompoundTag.getAllKeys()` → `keySet()`
- `Level.isClientSide` → `Level.isClientSide()`
- `LargeFireball` moved to `net.minecraft.world.entity.projectile.hurtingprojectile`
- `@EventBusSubscriber` no longer has `bus` parameter (auto-routes by event type)
- `MobEffects.DIG_SPEED` → `MobEffects.HASTE`
- `MobEffects.DAMAGE_RESISTANCE` → `MobEffects.RESISTANCE`
- `ItemEntity.copy()` removed - create new `ItemEntity` with constructor and `setDeltaMovement()`
- `Item.hurtEnemy()` - both `hurtEnemy` and `postHurtEnemy` exist, but `hurtEnemy` is the one called during attacks
- `Item.appendHoverText()` signature changed: `List<Component>` → `Consumer<Component>`, added `TooltipDisplay` parameter
- `Item.inventoryTick()` signature changed: `Level` → `ServerLevel`, slot int/boolean → `EquipmentSlot`
- `Screen.hasShiftDown()/hasControlDown()` - use GLFW directly: `GLFW.glfwGetKey(window.handle(), GLFW_KEY_LEFT_SHIFT)`
- `Window.getHandle()` → `Window.handle()`
- `RangeSelectItemModelProperty.get()` - third parameter is `net.minecraft.world.entity.ItemOwner` (not `ItemModel.ItemOwner`)
- `SmithingRecipe.templateIngredient()` returns `Optional<Ingredient>`
- `SmithingRecipe.baseIngredient()` returns `Ingredient` (not Optional)
- `SmithingRecipe.additionIngredient()` returns `Optional<Ingredient>`
- `Ingredient.EMPTY` removed - use `Optional.empty()` for optional ingredients
- `PlacementInfo.createFromOptionals(List<Optional<Ingredient>>)` for mixed ingredients
- `Ingredient.optionalIngredientToDisplay()` for display from optionals

## Migration Notes (1.21.11 → 26.1)

Big jump — Minecraft adopted calendar versioning and shipped deobfuscated. See NeoForge primer: https://docs.neoforged.net/primer/docs/26.1/

### Toolchain
- **Java 21 → 25**; **Gradle 8.12 → 9.1.0** (wrapper); **ModDevGradle 2.0.131 → 2.0.141**.
- **foojay-resolver-convention 0.9.0 → 1.0.0** — 0.9.0 references `JvmVendorSpec.IBM_SEMERU`, removed in Gradle 9, causing a config-time failure. 1.0.0 fixes it and auto-provisions JDK 25.
- **Parchment removed** entirely (build.gradle block + `parchment_*` props in gradle.properties) — official Mojang param names ship in-box.

### API Changes Applied
- `Level.random` field is now `protected` → use `level.getRandom()`.
- **RecipeSerializer is now a `record`**: `new RecipeSerializer<>(MapCodec, StreamCodec)`. Inner `Serializer` classes are gone; `CustomRecipe.Serializer<>` removed. Hoist `CODEC`/`STREAM_CODEC` to fields on the recipe and register via `() -> new RecipeSerializer<>(Recipe.CODEC, Recipe.STREAM_CODEC)`.
- `Recipe#assemble()` lost the `HolderLookup.Provider` param → `assemble(T input)`.
- `Recipe#group()` and `Recipe#showNotification()` are no longer default — must be implemented (`SmithingRecipe` does NOT provide them; it does provide `getType()` and `recipeBookCategory()`).
- `CustomRecipe` constructor takes **no args** now (no `CraftingBookCategory`); it provides `group()`/`category()`/`showNotification()`/`placementInfo()`. For a no-data custom recipe, mirror vanilla `RepairItemRecipe`: a singleton `INSTANCE` + `MapCodec.unit(INSTANCE)` + `StreamCodec.unit(INSTANCE)` (share the same instance — `StreamCodec.unit` does a reference-equality check on encode).
- NeoForge `LootModifier` constructor is now `(LootItemCondition[] conditions, int priority)` and `codecStart()` returns a P2 that adds an optional `"priority"` int → subclass constructor needs a `priority` param: `super(conditions, priority)`.
- `IItemExtension#canPerformAction` first param changed `ItemStack` → `ItemInstance` (the shared read-only interface implemented by both `ItemStack` and `ItemStackTemplate`). Cast to `ItemStack` inside if you need stack-only utils.
- `Player.displayClientMessage(Component, boolean)` removed → `player.sendSystemMessage(Component)`.
- `ItemStackTemplate` is the new immutable stack for data/recipe contexts; `ItemStack.CODEC`/`STREAM_CODEC` still exist and work for recipe deserialization (registries are loaded by recipe-load time).

## Testing

> **Gametest gotcha — damaging a mock player** (26.x): `makeMockServerPlayerInLevel()` players are effectively invulnerable until you (1) set `abilities.instabuild/invulnerable = false`, (2) mark their fake connection loaded — `player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket())` — because `ServerPlayer.isInvulnerableTo` returns true until `hasClientLoaded()`. Mob damage also difficulty-scales to 0 on the gametest server's Peaceful setting, and player-vs-player damage needs the `pvp` **game rule** (moved to `GameRules.PVP` in 26.x) enabled. See `GameTestBodies.mockVulnerablePlayer` + `thornyReflectsForPlayer`.

> **JUnit cannot build an `ItemStack`** (26.x): the unit-test environment loads the mod and its registries, so `ModItems.TOOL.get()` works, but item components are only bound when a server loads datapacks and `new ItemStack(...)` throws `NullPointerException: Components not bound yet`. Anything that needs a gear stack is a gametest; JUnit is for code behind a stack-free seam (`GearStats`, `GearRoll`, `TraitEligibility.check(GearContext, ...)`, the tag form of `TraitGrant.grant`). `RandomSource.create(seed)` works there, but draw many rolls from one source: sources made from consecutive seeds give nearly the same first float.

> **Changing config in a test**: substitute the setting, `try (Config.Substitution off = Config.traitToggle("critical").substitute(false)) { ... }`, and likewise `Config.CASE_CHANCE.substitute(1.0)` for any other setting. Closing the block puts the previous value back; nothing is written to the toml and the loaded value is untouched. Every thread sees a substitution, so open and close it inside one synchronous stretch of a test body and no other test sees it. This is the one way: no test sets a `SPEC` value or calls `Config.onLoad()` (the loaders' config event is the only caller).

> **Fabric mixin gotcha**: an injection into a `LivingEntity` method that `Player`/`ServerPlayer` overrides (e.g. `actuallyHurt`, which `Player` re-implements without calling super) silently won't run for players. Either wrap the call sites in the caller (`hurtServer`) with MixinExtras `@WrapOperation`, which catches the virtual dispatch, or - when the hook has to sit *inside* the method - inject into both classes. The wearer-hurt hook does the second: it must see damage after armor and enchantments like NeoForge's `LivingDamageEvent.Pre`, so `LivingEntityMixin` and `PlayerMixin` each wrap the `getDamageAfterMagicAbsorb` call in their own `actuallyHurt`. Two tempting shortcuts are both wrong: wrapping the `actuallyHurt` call hands traits the raw hit (against an armored wearer Thorny reflected 7.5 on Fabric and 3.0 on NeoForge), and one injection into `getDamageAfterMagicAbsorb` itself runs before a subclass's own override finishes (Witch takes 85% off magic after `super`; Thorny reflected 7.5 against 1.125). Regression net: `thorny_reflects_for_player` (a player wearer and a witch wearer).

> **Codex CI sandbox gotcha (Ubuntu 24.04)**: installing `bubblewrap` alone is not enough on GitHub-hosted runners. Install `apparmor-profiles`, copy `/usr/share/apparmor/extra-profiles/bwrap-userns-restrict` to `/etc/apparmor.d/`, and load it with `apparmor_parser -r`. Otherwise every sandboxed command can fail with `bwrap: loopback: Failed RTM_NEWADDR` while `codex exec` still exits successfully; require and verify the reviewed HEAD SHA before accepting the output.
- Use `/give @p randomloot:case` to get a loot case
- Right-click case to generate random tool
- Modifier templates can add/remove traits via smithing table
- Use `/give @p randomloot:mod_add` for addition template
- Use `/give @p randomloot:mod_sub` for subtraction template

### Admin commands (`commands/ModCommands.java`, gamemaster-gated)
- `/randomloot give <type>` - spawn a base (0 goodness, no traits) tool/armor piece of the given type
- `/randomloot trait add <trait>` - add a trait to the held gear (levels it if present; enforces forTool, biome restrictions, compatibility, and config gating)
- `/randomloot trait remove <trait>` - remove a trait from the held gear (works on config-disabled traits too)
- `/randomloot xp <amount>` - add XP to the held gear
- 26.1 gotcha: command permission gating is `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` (PermissionCheck/PermissionSet), not the old `source.hasPermission(int)`

## Key Files
- `gradle.properties` - Version configuration, mod metadata
- `src/main/resources/META-INF/neoforge.mods.toml` - Mod manifest
- `src/main/resources/data/randomloot/recipe/` - Recipe JSON files
- `src/main/resources/assets/randomloot/` - Textures, models, lang files
- `run/config/randomloot-common.toml` - Runtime mod configuration (generated)

## Configuration (gradle.properties)
Key properties that can be changed:
- `neo_version` - NeoForge version (must match Minecraft version)
- `minecraft_version` - Target Minecraft version
- `mod_version` - Your mod's version number
- `parchment_mappings_version` - Mappings for readable parameter names

## Troubleshooting

### Java Version Issues
Requires Java 21. Check with `java -version`. Error "Unsupported major.minor version 65.0" means wrong Java version.

### Missing Assets
```bash
./gradlew downloadAssets
./gradlew runClient
```

### Corrupt Cache
```bash
rm -rf ~/.gradle/caches/neoformruntime
rm -rf build/
./gradlew build
```

### IDE Not Finding Classes
Refresh Gradle project in IDE (IntelliJ: click elephant icon with refresh arrows)

## Git Worktree Workflow for New Modifiers

> **IMPORTANT: When creating a new modifier (including in plan mode), ALWAYS use a git worktree to isolate development from the main directory.**

This keeps the main `randomloot` directory clean and allows parallel development of multiple features.

### Creating a Worktree for a New Modifier
```bash
# From the main randomloot directory
git worktree add ../randomloot-feature/<modifier-name> -b feature/<modifier-name>

# Example: Creating an "Excavator" modifier
git worktree add ../randomloot-feature/excavator -b feature/excavator
```

### Worktree Directory Structure
```
~/Documents/Github/
├── randomloot/                    # Main directory (keep clean!)
└── randomloot-feature/
    ├── excavator/                 # Worktree for excavator modifier
    ├── lightning/                 # Worktree for lightning modifier
    └── ...
```

### Development Workflow
1. **Create worktree** before writing any code for a new modifier
2. **Do all development** in the worktree directory (e.g., `../randomloot-feature/excavator/`)
3. **Build and test** from the worktree: `cd ../randomloot-feature/excavator && ./gradlew runClient`
4. **When complete**, push the branch and create a PR:
   ```bash
   cd ../randomloot-feature/excavator
   git push -u origin feature/excavator
   gh pr create --base main --head feature/excavator
   ```
5. **Clean up** the worktree after the PR is merged:
   ```bash
   git worktree remove ../randomloot-feature/excavator
   git branch -d feature/excavator
   ```

### Listing Active Worktrees
```bash
git worktree list
```

### Why Worktrees?
- Main directory stays on the stable branch
- Multiple features can be developed in parallel
- Easy to abandon/restart features without affecting main
- Clear separation between stable code and work-in-progress

## Adding New Modifiers
> **Multiloader rule:** modifiers live entirely in `common/` and must only use vanilla + common APIs. If a new trait needs a game event the mod does not get yet, add a hook to the loader hook contract: a `GameHook` constant, a `GameHooks` method that says what the shim must deliver, a call from BOTH loader shims (`NeoForgeEvents` + `RandomLootFabric`/a fabric mixin), and a step in `loader_hooks_all_bound`. The dispatcher it reaches hands the gear to a new event on `TraitDispatch` rather than loop over the traits itself. If it changes stats/durability inputs, make sure the path runs through a `LootUtils` mutator so `refreshDerivedComponents` fires.

1. **Create a git worktree first** (see above section)
2. **Write the rule**: a class in the matching `modifiers/` subdirectory that `extends AbstractModifier` (or `PlaceOnUseModifier`/`BlockHighlighter` for those families) and implements `description()` plus the hook interface(s) for the events it reacts to (`BlockBreakModifier`, `HoldModifier`, `EntityHurtModifier`, …). It has no constructor and writes none of `tagName`/`color`/`forTool`/`compatible`/`canLevel`/`levelUp`/`toNBT`/`fromNBT`: those are final in the base and answered from the declaration. Read the level with `traitLevel()`, and declare a field the gear must keep with `stored(key, initial)`.
3. **Declare it**: one line in `ModifierRegistry`, `trait(tag, name, colour, fits, groups...)` then `.levels(min, max)` and `.excludes("other_tag")` if it needs them, then `.make(MyTrait::new)`. That line is the registration, the role sets, the wiki group and the `<tag>_enabled` config toggle (`Config` loops the registry), so there is no Set to add it to and nothing to add in `Config.java`.
4. Add the recipe JSON in `data/randomloot/recipe/trait_<tagname>.json`
5. **Regenerate** the docs (the GenWiki gotcha at the end of this file; it writes the trait's `MODIFIERS.md` entry and its `lang/en_us.json` keys) and the trait fixtures (`TraitFixtureTest`, below). Read both diffs: they should show the new trait and nothing else.

### Deep modules in `loot/` — reach for these before writing new logic
- **`GearStats`** — every derived number as a pure function of `(goodness, ToolType[, statMultiplier])`. No `ItemStack`, so it is **unit-testable** (`GearStatsTest`). It also holds the **XP step**: `xpStep(level, xp, amount)` returns the new level, the XP left over and the levels gained, and `LootUtils.addXp` applies one level-up per level gained (goodness bump, sound, bonus-trait chance) before writing the new level. Put new stat maths here, not on an item class; `TraitDispatch.statMultiplier(stack)` folds the `StatsModifier` traits.
- **`GearTags`** — the per-stack tag store. `read`/`has`/`mutate`/`clear`, plus named namespace constants (`INFO`, `STATS`, `COSMETICS`, `XP`, `LORE`, `ROLLING`; traits use `Modifier.MODTAG`). Never write a bare namespace literal.
- **`GearRoll`** — what a case produces, decided before any item exists (see "Gear Roll"). `pickTrait(GearContext, random)` is the one place a random trait is chosen, uniform among what `TraitEligibility` allows; the case roll and the level-up bonus (`bonusTrait(gear, chance, random)`) both use it. The config's numbers are arguments (goodness, armor chance, level-up chance): read them in the `LootUtils` entry points, never inside the decision. Which traits are switched on is the one thing the roll reads from the config, and it does so through the gate, which owns that question for every caller. To change what generated gear carries, change `Rolled` and `LootUtils.stamp` together; `stamp_writes_what_the_gear_roll_decided` reads every field back off the stack. A roll is seedable all the way down: the same seed decides the same gear, Chaotic's seed included, because a grant hands the trait the roll's random source (`Modifier.granted(random)`).
- **`TraitEligibility`** — the single gate for "may this trait go on this gear". It asks `compatible()` once per trait already on the gear: the trait registry applies a declared conflict to both traits of the pair. Works off a `GearContext` so it is unit-testable (`TraitEligibilityTest`); `Result.reason(trait)` renders the player-facing refusal. Ask it directly only when the question is all you need (tab-completion, a recipe's `matches`, picking which traits a roll may choose from); to put the trait on, call `TraitGrant`. **Never add another copy of the forTool/biome/config/compatible/canLevel checks.**
- **`TraitGrant`** — the one way a trait goes on gear or comes off it. `grant(gear, trait, worldSeed)` asks the gate, resolves the trait for the world (`forWorld`, the forger stamp), adds it or levels the gear's own copy, and returns an `Outcome`: `ADDED`, `LEVELED`, or `REFUSED` with the gate's reason. `remove(gear, trait)` returns `REMOVED` or `NOT_PRESENT` and has no gate, so a config-disabled or mismatched trait still comes off. The four callers - the case roll (inside `GearRoll`) and the level-up bonus (`LootUtils.addXp`), which both pick with `GearRoll.pickTrait`, `TraitAdditionRecipe` and `/randomloot trait add|remove` - only render the outcome; `outcome.refusal()` is the player-facing reason, and `outcome.obtainedBy(player, source)` fires `trait_obtained` only when something was granted, so a removal cannot count as an add. Two extras, each for one user: `restamp(gear, seed)` is the smithing table's sweep that re-forges every forger-named trait on its result (`grant` stamps only the trait it adds - both behaviours predate the module and were kept), and `force(gear, trait)` skips the gate for test setup (a Thorny sword). A new trait is stored as `trait.granted(random)`, a fresh instance from the trait registry, never the registry's own. A stack-free form, `grant(GearContext, CompoundTag traits, trait, seed, random)`, does the same to the tag of traits a piece of gear will store; `GearRoll` uses it so a rolled trait is stored exactly as a granted one (`TraitGrantTest`, and `trait_grant_adds_then_levels` checks the two forms agree). `LootUtils.addModifier`/`removeModifier`/`applyWorldForgers` are gone; `LootUtils.updateModifier` stays for a trait saving its own state. **Never write to `Modifier.MODTAG` to add or remove a trait.** Tests: the `trait_grant_*` gametests - a gear stack cannot be built in JUnit (see Testing).
- **`LootGearItem`** — shared base for tools and armor. Subclass hooks: `buildAttributeModifiers`, `holdSlot`, `appendStatLines`, `supportsEnchantmentCommon`.
- **`BiomeRestrictedModifier.describeRestriction()`** — biome traits state their own restriction so `BIOMES.md` doesn't restate thresholds. Implement it on any new biome trait.

### Shared modifier infrastructure
- **`TraitDispatch`** (`loot/modifiers/`) — the one path a game event takes to the traits on a piece of gear. A caller names the event and passes its arguments: `hit`, `blockBroken`, `useOn`, `use` (+ `usesAnywhere`), `tick`, `kill`, `wearerHurt`, `beforeDamage`, `statMultiplier`, `spendDurability`, `armorDurability`. It owns what the events share, so neither a caller nor a trait class repeats it: traits are loaded once per event (config-disabled ones already dropped by `getModifiers`; a trait that charges durability of its own through `spendDurability` loads them again there, to find Unbreaking); gear in its reveal roll is inert, in every event; a `PreHitModifier` runs before the weapon's other hit traits; hooks run on the server only (`useOn` and `statMultiplier` are the two that also run on the client - a placing trait predicts its block there); a trait whose stored state changed in its hook is saved, and one that changed nothing writes nothing (it compares `toNBT()` before and after, so `toNBT()` of a loaded trait must be stable - `TraitFixtureTest` asserts it for every trait at every level); and `hit`/`blockBroken` spend the event's one durability with Unbreaking's roll. So a hook has **no `isClientSide()` guard, no `LootUtils.isRolling` check, no `LootUtils.updateModifier(stack, this)` and no return value for durability**. `LootItem`, `LootGearItem`, `KillDispatcher` and `ArmorDispatcher` keep only what is not about traits (XP, vanilla tool actions, finding the weapon or the worn pieces) and call in; the last two stay the loader seam's entry points. "Rolling gear is inert" has no exception: kill traits and the two armor events (`wearerHurt`, `armorDurability`) used to run for rolling gear and were brought into line. A rolling piece can only reach an armor slot by command; vanilla still damages it there, its Unbreaking just gets no roll. Never add a `for (Modifier mod : LootUtils.getModifiers(stack)) if (mod instanceof ...)` loop. Regression nets: `trait_dispatch_runs_pre_hit_traits_first`, `trait_dispatch_leaves_rolling_gear_inert` (hit, break, right-click, tick, kill, stats, a direct durability charge, and worn: damage to the wearer and to the piece), `trait_dispatch_saves_only_changed_traits`.
- **`TraitDeclaration`** (`loot/modifiers/`) — everything about a trait that is not its rule, said once, as a line in `ModifierRegistry`: `trait("fragile", "Fragile", ChatFormatting.WHITE, HAND_HELD, Group.BREAKERS, Group.HURTERS, Group.STATS).levels(1, 3).make(Fragile::new)`. In order: the tag gear stores the trait under (frozen once shipped: it is also the recipe's `trait`, the config key and the lang key), the display name, the colour, the gear it fits (`WEAPONS`, `MINING_TOOLS`, `ARMOR`, `MINING_TOOLS_AND_ARMOR`, `HAND_HELD`, `ANY_GEAR`, or `only(...)`/`union(...)`) and its wiki groups. Then, if needed: `.levels(min, max)` (without it the trait does not level) and `.excludes("tag", ...)` for a conflict. `.levels(key, min, max)`, `.capsOnLoad()` and `.mirrorsLevelAs(key)` exist for gear already saved (effects store their level as `power`, Fire and Draining as `points`, Hasty under two keys); a new trait uses none of them.
- **Trait registry** (`ModifierRegistry`) — the table of declarations and the one place a trait instance is made. `loadModifier(tag, stored)` builds a new instance for every trait read off gear, and `Modifier.granted(random)` a new one for every grant, so no two pieces of gear share a trait object. The registry keeps one instance of each trait for questions about the trait itself: `getModifiers()`, the static fields (`ModifierRegistry.THORNY`) and the role sets, which the gate, the wiki and the config toggles read. The role sets `BREAKERS`/`USERS`/`HURTERS`/`HOLDERS`/`STATS`/`WEARERS`/`MISC` are derived from the declared groups, so there is nothing to keep in step by hand. A conflict is declared on one trait and holds for both; the `USERS` group is one-a-piece (gear answers a right-click once), so two right-click traits never share gear without either naming the other.
- **`AbstractModifier`** (`loot/modifiers/`) — base for every trait class. It answers identity, fit and compatibility from the declaration and owns the round-trip: `toNBT()` writes the name, the level and each field declared with `stored(...)`, and loading reads them back, a missing key keeping the field's starting value. So a tuning value (Combo's `points`, Explosive's `power`) lives on the gear and old gear keeps the number it was made with. `stored(key, initial)` covers ints, longs and floats; `stored(key, initial, reader, writer)` anything else (Nemesis's kill counts). Instances come from the registry, never from `new`: one built by hand has no declaration. In a test take `ModifierRegistry.getModifier(tag)`, or `.fromNBT(tag)` of it for a copy to level.
- **One levelling scheme** (in `AbstractModifier`) — the level runs from the declared minimum to the maximum; a fresh grant is at the minimum and shows no numeral, the first upgrade shows "II" (`roman(level - min + 1)`), and `canLevel()` is `level < max`. It is stored under `ModifierConstants.LEVEL` (`"trait_level"`); gear saved under the old `"level"` key still loads. Override `name()` only for a name of its own at the top level (Unbreaking's "Unbreakable", Featherweight).
- **`TraitFixtureTest`** — the saved-gear freeze. `neoforge/src/test/resources/trait-fixtures/` records, for all 60 traits, what a grant stores at each level, how saved tags load (legacy keys, levels beyond a cap, tuned values, stray keys), everything a trait renders, which gear it fits, the role sets, and the gate's verdict for every pair of traits. A refactor leaves the five files byte-identical. For a new trait or a deliberate change, regenerate with `RL_WRITE_TRAIT_FIXTURES=$PWD/neoforge/src/test/resources/trait-fixtures ./gradlew :neoforge:test --tests '*TraitFixtureTest' --rerun` and review the diff: it is the list of what changed for saved gear.
- **`PlaceOnUseModifier`** (`loot/modifiers/users/`) — base for crouch-right-click placement traits (DirtPlace/TorchPlace/FirePlace); owns the crouch gate, DAMAGE NBT and the on-SUCCESS durability charge; subclasses implement `place(UseOnContext)`.
- **`BlockHighlighter`** (`loot/modifiers/holders/`) — base for glowing-marker finder traits (OreFinder/TreasureFinder); owns the throttled 20³ block scan and shulker marker spawn/lifecycle/cleanup; subclasses implement `isTarget(BlockState)`.
- **`EffectModifier`** — the eight potion-effect traits share two classes (`Effect` while held, `HurtEffect` on hit); each is one `heldEffect(...)`/`hitEffect(...)` line in `ModifierRegistry`. `EffectModifier.maxPower(effect)` keeps a trait at level I when vanilla only calls `hasEffect` for its effect and never reads the amplifier (Blindness, Fire Resistance), since a further level would cost the recipe again and change nothing. Add any new such effect there.
- **`LootTooltips`** (`loot/`) — shared `appendHoverText` body for `LootItem`/`LootArmorItem`; only the shift-expanded stats block differs, passed as a lambda.
- **`LootUtils.getModifiers`** already filters config-disabled traits — never re-check `Config.traitEnabled` on its results. Use it to read or list traits (tooltips, the gate); to run their hooks, go through `TraitDispatch`.
- **`EntityHurtModifier.dealBonusDamage(hurtee, hurter, amount)`** — use this for any post-hit bonus melee damage. It resets `invulnerableTime` (otherwise the bonus is swallowed by i-frames) and picks the correct `playerAttack`/`mobAttack` source. Never call `hurtee.hurt(...)` directly for a follow-up bonus.
- **`LootUtils.breakBlockAsPlayer(stack, pos, player, level, state)`** — breaks a block as the player (drops + stats) and returns whether it was actually destroyed; only spend durability when it returns `true`. It passes the block entity to the drop logic like vanilla does - block loot that copies from it (shulker box contents) drops empty otherwise - so never hand-roll `playerDestroy(..., null, ...)`.
- **`TraitDispatch.spendDurability(stack, points, holder, slot)`** — the only way a tool spends durability outside the hit and the block break the dispatch charges itself (strip/path, right-click traits, area-break extras, Fragile's surcharge). It gives the gear's Unbreaking trait a roll per point, which is what makes a maxed "Unbreakable" tool actually unbreakable. Never call `stack.hurtAndBreak` on loot gear directly. (Worn armor is damaged inside vanilla, which asks `TraitDispatch.armorDurability` through `ArmorDispatcher.onArmorHurt`; the rule is the same roll per point, `Unbreaking.remaining`. Regression net: `unbreaking_rolls_per_durability_point`.)
- **`GearTags.read` returns the stored tag, not a copy** — treat it as read-only. `GearTags.mutate` edits a copy; editing in place also changed every `ItemStack.copy()` of the stack, including the copy the server diffs against to decide whether to re-send a slot, so trait-only changes never reached remote clients.
- **Hold traits must not write to the stack on a timer** — a component change on the held stack resets the block being mined. Pace them off `level.getGameTime() % interval` like `Naturalist`, `BlockHighlighter` and `Frozen` do. `TraitDispatch` saves a trait whenever a hook changes a field it stores, so a hold trait that counts ticks in a stored field writes every tick.
- **Off-hand arbitration** (`LootItem.use` / `LootItem.afterUseItem`) — a right-click-anywhere trait (`useAnywhere()`: `VoidTouched`, `FireBall`) in the main hand gives a non-empty off hand the click first. The client predicts PASS for these traits and goes on to use the off-hand item too, so firing straight away did both. Nothing can say whether an item *would* act without using it (a use animation misses ender pearls; a `use` override also covers an empty bucket aimed at the sky), so `LootItem.use` defers instead and the trait fires from `afterUseItem` when the off-hand `useItem` that follows returns a non-consuming result; an off-hand block use that succeeds or fails ends the click and drops the deferral in `afterUseItemOn`. That needs the results of `ServerPlayerGameMode.useItem`/`useItemOn`, which no loader has an event for: `GameHook.AFTER_USE_ITEM` is wired by a `ServerPlayerGameModeMixin` on **both** loaders - NeoForge's only mixin, declared via `[[mixins]]` in `neoforge.mods.toml` + `randomloot.neoforge.mixins.json`. A new right-click-anywhere trait gets all this for free. Gametests must replay the click in client order (see `rightClick` in `GameTestBodies`).
- **Block tags** (`data/randomloot/tags/block/`): `detectable_ores` / `undetectable_ores` drive Detecting (never match blocks by display name - it is translated with the integrated server's language), `naturalist_targets` drives Naturalist.
- **Marker entities** (the shulkers `BlockHighlighter.spawnMarker` creates) are unsaved (`shouldBeSaved() == false`) and removed with `discard()`. Never "kill" an entity by teleporting it away with zero health: moved into an unloaded chunk it stops ticking and is saved there instead.
- **Pre-hit state for hurt traits**: `hurtEnemy` runs after the hit has landed. If a trait needs the target's state from before the hit, implement `PreHitModifier` the way `EarlyBird` does: its `beforeDamage(victim)` is called for every living damage event (both loaders route them through `ArmorDispatcher.onLivingDamagePre`), on the registry's instance, so keep the note in static state - and set *or clear* it on every call so it describes the latest hit, not an earlier one in the same tick. Other traits' bonus hits are damage events too, so `TraitDispatch.hit` runs every `PreHitModifier` before the tool's other traits. Implementing the interface is the whole declaration; no caller names the trait.
- **Random state belongs to the grant**: a trait that needs a random value of its own overrides `granted(RandomSource)` and draws from the source it is given, as `Chaotic` does for its seed; use `forWorld(seed)` when the value should be constant per world (`DirtPlace`). Never draw from `new Random()` there - it makes the gear roll unseedable. A grant starts as a copy of the registry's own instance, so that instance must hold nothing random: whatever it rolled would be the same for every grant of a launch and different on the next. Grace's used to be named after a forger rolled at launch, which also put a different name in the comment above `dirt_place_enabled` every time the config was written; it is plain "Grace" now (`GraceNameTest`), a grant that knows its world gets the world's forger from `forWorld`, and one that does not stores "Grace". Regression net: `chaotic_grants_roll_own_seeds`.

### Hurter Modifier Pattern (EntityHurtModifier)
```java
// ModifierRegistry: the whole declaration. Tag, name, colour, fit, wiki group, levels.
public static Modifier TALLY = trait("tally", "Tallying", ChatFormatting.RED, WEAPONS, Group.HURTERS)
        .levels(1, 3).make(Tally::new);

// The class: the rule, and the fields the gear keeps.
public class Tally extends AbstractModifier implements EntityHurtModifier {
    private final Stored<Integer> hits = stored("hits", 0);

    @Override
    public String description() {
        return "Every tenth hit deals " + traitLevel() + " bonus damage.";
    }

    // TraitDispatch.hit calls this on the server, for enabled traits on gear that is not
    // rolling. It spends the hit's durability and saves this trait if a stored field changed.
    @Override
    public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        // Just change the field: no LootUtils.updateModifier(itemstack, this) here.
        hits.set(hits.get() + 1);
        if (hits.get() % 10 == 0) {
            dealBonusDamage(hurtee, hurter, traitLevel());
        }

        // For kills implement EntityKillModifier.onKill instead of checking health here:
        // the hit hook never sees indirect kills.
    }
}
```

### Recipe JSON Format
```json
{
  "type": "randomloot:trait_change",
  "item": {
    "count": 1,
    "id": "minecraft:ender_eye"
  },
  "trait": "mytrait"
}
```
`count` (optional, default 1, at most the item's max stack size) is how many of the item the Addition Template needs in the slot and consumes; a leveled trait pays it again per level. The Subtraction Template always takes one. Changing a count is a balance change - regenerate the docs afterwards (`MODIFIERS.md` and `LOOT.md` print it).

## Useful Code Patterns

### Send Chat Message to Player
```java
if (entity instanceof Player player) {
    player.displayClientMessage(Component.literal("Message here"), false);
}
```

### Get Entity Display Name from Registry Key
```java
String registryName = "minecraft:zombie";
String simpleName = registryName.substring(registryName.indexOf(":") + 1);
// Convert "zombie_villager" to "Zombie Villager"
String[] words = simpleName.split("_");
StringBuilder result = new StringBuilder();
for (String word : words) {
    if (result.length() > 0) result.append(" ");
    result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
}
```

### Roman Numerals
```java
LootUtils.roman(1); // Returns "I"
LootUtils.roman(2); // Returns "II"
```

## Armor System (Random Armor)

One registered item (`randomloot:armor`, `LootArmorItem`) covers all four pieces, mirroring the
single-tool design: the piece type (HELMET/CHESTPLATE/LEGGINGS/BOOTS, part of `LootItem.ToolType`)
lives in the "info" tag, and the equipment slot + worn texture live in the per-stack vanilla
`EQUIPPABLE` data component. `LootUtils.updateEquippable(stack)` rebuilds that component from the
piece type + cosmetic texture index; it is called from `setTexture` and `CloneItem`, so anything
that changes the texture or clones armor keeps the worn look in sync.

Key wiring:
- **Stats**: defense/toughness derive from "goodness" in `LootArmorItem.getDefense/getToughness`
  (per-piece scale: chest 1.4x, legs 1.1x, helmet/boots 0.6x; toughness = stats * 0.25).
  `StatsModifier` traits multiply defense like they multiply dig speed on tools.
- **XP**: armor levels from damage taken - `ArmorDispatcher` (NeoForge `LivingDamageEvent.Post`; Fabric
  hands over the same amount by mixin, see "DAMAGE_POST" under Multiloader Architecture).
  Unbreaking rolls per point of armor durability via `ArmorHurtEvent`, the same rule as for tools (it used to get one roll for the whole hit). New `WearerHurtModifier` interface
  dispatches on `LivingDamageEvent.Pre` (Thorny, Featherweight, Bulwark, Adrenaline): after
  armor, protection enchantments and Resistance, before absorption - on Fabric too, by mixin.
- **Worn textures**: 10 sets. `assets/randomloot/equipment/setN.json` +
  `textures/entity/equipment/humanoid{,_leggings}/setN.png`. Item sprites are
  `textures/item/<piece>N.png` dispatched via `items/armor.json` range_dispatch on the same
  `randomloot:cosmetic` property as tools (offsets: helmet .5, chest .6, legs .7, boots .8).
  All armor art is generated by `assets/randomloot/gen_armor.py` (gradient-maps the old mod's
  titanium/heavy art into 10 palettes; old repo checked out at /tmp/randomlootmod).
- **Gear roll**: `GearRoll.roll` picks armor with probability `Config.ARMOR_CHANCE` (default 0.15), then one of the four pieces.
- **Smithing**: `TraitAdditionRecipe` accepts armor bases; MOD_ADD on armor is gated by
  `modifier.forTool(pieceType)` (tools keep their historical anything-goes behavior).
- **Enchants**: armor item sits in all `minecraft:enchantable/*_armor` tags; per-piece filtering in
  `LootArmorItem.supportsEnchantment` via `data/randomloot/tags/enchantment/{all_armor,helmets,chestplates,leggings,boots}.json`.
- **Enchantable-tag gotcha (26.x)**: vanilla dropped `enchantable/sword` and added
  `melee_weapon` (the `primary_items` gate for sharpness/smite/bane/fire_aspect — controls the
  enchanting TABLE, while `supported_items` alone covers the anvil), `sweeping`, `vanishing`,
  `equippable`, `lunge` (spears; not joined). When bumping MC versions, diff our
  `data/minecraft/tags/item/enchantable/` against the vanilla jar's — a missing primary tag
  silently removes enchants from the table only, which tests missed for a whole version. The
  `enchanting_table_filters_by_type` gametest is the regression net.
- **Repair**: anvil material repair via `data/randomloot/tags/item/armor_repair_materials.json` (diamond).

**Wiki generation** (`GenWiki`) renders the six root docs (`MODIFIERS.md`, `CONFIG.md`, `PROGRESSION.md`, `NAMES.md`, `LOOT.md`, `BIOMES.md`) and the trait entries of `lang/en_us.json` from what the game holds, not from restated text: config rows and the defaults quoted in `LOOT.md`/`PROGRESSION.md` come from `Config.settings(Group)` and the `Setting` constants (declare a whole-number setting with `whole(...)`, not `decimal(...)`, so CONFIG.md doesn't render it as `1.0`), the progression tables from `GearStats`, texture counts from `LootUtils.textureCount`, the biome table from `describeRestriction()`, and **recipes from the server's loaded recipe manager** (`randomloot:trait_<tag>`, a `TraitAdditionRecipe`). It no longer parses the recipe JSON itself; the only source file it opens is the lang file, which it rewrites in place and must read back to keep hand-written keys.

It runs as the `wiki_generates` row of the gametest table (`GenWiki.generate(recipes)`), not from mod construction, because recipes only exist once a server has loaded its datapacks. Every gametest run on either loader renders the docs in memory (`GenWiki.render`), so a trait in `MODIFIERS.md` with no loaded recipe fails the run, naming the trait (`wiki_needs_every_trait_recipe` is the net for that check itself). Nothing is written unless the run was started with `RL_PROD=false`:
`RL_PROD=false RL_WIKI_DIR=$PWD ./gradlew :neoforge:runGameTestServer`
(this is what `.github/workflows/wiki.yml` runs). `RL_WIKI_DIR` must be the repo root: without it the root is `..` of the run dir (`neoforge/`), and the run now fails saying so instead of writing docs there. A failed render or write fails the gametest, so the exit code is trustworthy; still read the diff, since a doc can be wrong without being incomplete.

Gotchas: (1) in 26.3 a recipe JSON that fails to parse (unknown item id) stops the server at "Registry Loading" before any test runs; the case wiki generation catches is a trait with no `trait_<tag>.json` at all. (2) Generation runs after other gametests may have run, and renders the registry's own trait instances - a test that changed one of those (rather than a copy made with `fromNBT`) would change the rendered descriptions. (3) On gear Grace (`dirt_place`) carries its world's forger name and the registry's own instance is plain "Grace"; `docName` prints it as "Forger's Grace", which the docs have always called it.
