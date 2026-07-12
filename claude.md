# Random Loot 2 - Minecraft Mod

> **Claude: Whenever you discover something important during development (API quirks, patterns, gotchas, or useful snippets), automatically add it to this file so you'll remember it in future sessions.**

## Overview
An RPG-style loot system mod for Minecraft that generates randomized tools with modifiers/traits. **Multiloader**: one shared codebase (`common/`) shipping both a NeoForge jar and a Fabric jar.

## Current Version
- **Minecraft**: 26.2 (fabric/NeoForm artifacts use `26.2`; NeoForge builds are `26.2.0.x`)
- **NeoForge**: 26.2.0.11-beta · **ModDevGradle**: 2.0.141
- **Fabric**: loader 0.19.3, fabric-api 0.152.1+26.2, fabric-loom 1.16.3
- **Forge Config API Port**: 26.2.1 (NeoForge config API on Fabric; bundled jar-in-jar)
- **Gradle**: 9.5.0 (wrapper) — fabric-loom 1.16 requires ≥9.4
- **Java**: 25 (toolchain auto-provisioned via foojay-resolver-convention 1.0.0)
- **Mod ID**: `randomloot`
- **Package**: `dev.marston.randomloot`

> **Versioning note:** Minecraft moved to calendar versioning. NeoForge `26.2.0.11-beta` = MC `26.2`, build `11`. The vanilla version string has NO trailing `.0` — `com.mojang:minecraft:26.2`, NeoForm `26.2-1`. Parchment is no longer used: MC ships deobfuscated with official Mojang names, which is also why Fabric needs no intermediary remapping anymore (loom has no `mappings`/`modImplementation` — use plain `implementation`).

## Multiloader Architecture
- `common/` — 95% of the code; compiles against vanilla only (ModDevGradle `neoFormVersion`) + a `compileOnly` stub of `fuzs.forgeconfigapiport:forgeconfigapiport-common-neoforgeapi` so `Config` (ModConfigSpec) lives here.
- `neoforge/`, `fabric/` — loader projects compile the **common sources** into their jars (`commonJava`/`commonResources` configurations from `build-logic/`), so each shipped jar is self-contained.
- **Platform seam** (`dev.marston.randomloot.platform`): `Services.PLATFORM`/`Services.REG` resolved via Java `ServiceLoader` (bindings in each loader's `META-INF/services/`). `RegHelper` = registration (NeoForge: DeferredRegisters attached in the mod ctor; Fabric: immediate `Registry.register`). `IPlatformHelper` = item factories (NeoForge subclasses add `canPerformAction`/`supportsEnchantment`/`isCombineRepairable` extension overrides), `getToolModifiedState` (strip/scrape/wax/flatten), `canHarvestBlock`, `tooltipLevel`.
- **Event seam**: dispatchers in common are plain static methods (`KillDispatcher.onLivingDeath`, `ArmorDispatcher.onLivingDamagePre/Post/onArmorHurt`, `Soulbound.modifyBreakSpeed`, `BlockHighlighter.onServerTick/onServerStopping`, `ModCommands.register`, `Config.onLoad`). NeoForge shim: `neoforge/.../NeoForgeEvents` (+`NeoForgeClientEvents`). Fabric shim: `RandomLootFabric` (Fabric API events) + 3 mixins for hooks Fabric lacks (`PlayerMixin` break speed, `LivingEntityMixin` pre-damage armor traits, `ItemStackMixin` armor durability skip).
- **Derived data components**: per-stack attributes (attack / armor+toughness) and MAX_DAMAGE are vanilla components rebuilt by `LootUtils.refreshDerivedComponents(stack)` — called from every mutator (setStats/setToolType/add/remove/updateModifier/addXp/startRoll/finishRoll); `migrateDerivedComponents` in inventoryTick upgrades pre-component items. They replaced NeoForge's dynamic `getDefaultAttributeModifiers`/`getMaxDamage` item overrides — never reintroduce loader-only dynamic item methods for stats.
- **Loot injection**: NeoForge = GLM (`CaseLootModifier` + `data/randomloot/loot_modifiers/`, lives in `neoforge/`); Fabric = `LootTableEvents.MODIFY` pools in `RandomLootFabric` (chances baked at datapack load; `/reload` picks up config changes).
- **Config**: same `randomloot-common.toml` on both loaders (FCAP on Fabric). Register: NeoForge `modContainer.registerConfig`, Fabric `ConfigRegistry.INSTANCE.register` + `ModConfigEvents.loading/reloading`.
- **Known Fabric gaps**: anvil-combining two loot items isn't blocked (NeoForge `isCombineRepairable=false` has no Fabric hook); enchant gating goes through `EnchantmentEvents.ALLOW_ENCHANTING` (hooks EnchantmentHelper paths, not `ItemStack.supportsEnchantment` which is NeoForge-only).
- **Fabric access widener** (`fabric/src/main/resources/randomloot.accesswidener`, namespace `official` — NOT `named` — since 26.x): `RangeSelectItemModelProperties.ID_MAPPER` (texture property registration), `AxeItem.STRIPPABLES`, `ShovelItem.FLATTENABLES`.
- **GameTests**: bodies shared in `common/.../gametest/GameTestBodies.java` (vanilla APIs only). NeoForge registers via `RegisterGameTestsEvent`+`RLTestInstance` (26 tests incl. GLM + supportsEnchantment tests); Fabric via `@GameTest` methods in `RandomLootFabricGameTests` + `fabric-gametest` entrypoint (24 tests incl. loot-injection test).

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
├── Config.java                  # ModConfigSpec config (FCAP stub makes this common)
├── GenWiki.java                 # Wiki generation utility
├── platform/                    # Services, IPlatformHelper, RegHelper, ToolAction
├── component/                   # Data components
├── items/                       # ModItems (registers via Services.REG/PLATFORM)
├── gametest/GameTestBodies.java # Shared gametest bodies (both loaders run them)
├── commands/  advancements/     # Plain-method dispatch, loader shims call in
├── loot/                        # Core loot system + modifiers/ (breakers, holders, hurters, stats, users)
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

### Tool Generation
- Tools are generated via `LootUtils.genTool()`
- Stats scale with player progression (cases opened)
- Random texture selection per tool type
- Name generation based on biome temperature

### Modifier System
- Modifiers implement interfaces: `BlockBreakModifier`, `HoldModifier`, `EntityHurtModifier`, `UseModifier`, `StatsModifier`
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
public class MyBiomeModifier implements EntityHurtModifier, BiomeRestrictedModifier {
    // ... standard modifier fields and methods ...

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
> **Multiloader rule:** modifiers live entirely in `common/` and must only use vanilla + common APIs. If a new trait needs a game event, add a plain static method to the relevant dispatcher in common and wire it in BOTH loader shims (`NeoForgeEvents` + `RandomLootFabric`/a fabric mixin). If it changes stats/durability inputs, make sure the path runs through a `LootUtils` mutator so `refreshDerivedComponents` fires.

1. **Create a git worktree first** (see above section)
2. Create class in appropriate `modifiers/` subdirectory (under `common/src/main/java/...`)
3. **`extends AbstractModifier`** (or `LeveledModifier` if it levels, `PlaceOnUseModifier`/`BlockHighlighter` for those families) and implement the relevant interface(s) (`BlockBreakModifier`, `HoldModifier`, `EntityHurtModifier`, …). The base provides the shared `name` field and default `name()` / `writeToLore()` / `toNBT()`; do NOT write a `clone()` (the interface no longer has one — registry prototypes are the factories via `fromNBT`). Use `isWeapon(type)` / `isMiningTool(type)` for `forTool(...)`.
4. Register in `ModifierRegistry.java` (add to both the static field AND the appropriate Set like `HURTERS`)
5. Add recipe JSON in `data/randomloot/recipe/trait_<tagname>.json`
6. Add to config in `Config.java` if toggleable

### Shared modifier infrastructure
- **`AbstractModifier`** (`loot/modifiers/`) — base for every modifier; holds `name` + default `name()`/`writeToLore()`/`toNBT()` (name-only; stateful classes extend via `super.toNBT()`) and the `isWeapon`/`isMiningTool` tool-group helpers.
- **`LeveledModifier`** (`loot/modifiers/`) — base for smithing-leveled traits; holds `level`, the roman-numeral `name()` (first upgrade displays "II"), `canLevel()`/`levelUp()` and LEVEL serialization. Subclasses supply `minLevel()` (0 or 1) + `maxLevel()` and read the level back in `fromNBT` via `ModifierConstants.getLevel`.
- **`PlaceOnUseModifier`** (`loot/modifiers/users/`) — base for crouch-right-click placement traits (DirtPlace/TorchPlace/FirePlace); owns the crouch gate, DAMAGE NBT and the on-SUCCESS durability charge; subclasses implement `place(UseOnContext)`.
- **`BlockHighlighter`** (`loot/modifiers/holders/`) — base for glowing-marker finder traits (OreFinder/TreasureFinder); owns the throttled 20³ block scan and shulker marker spawn/lifecycle/cleanup; subclasses implement `isTarget(Block)`.
- **`EffectModifier`** — also holds the trait's `ChatFormatting` color; pass it in the constructor (see the `Effect`/`HurtEffect` registrations in `ModifierRegistry`).
- **`LootTooltips`** (`loot/`) — shared `appendHoverText` body for `LootItem`/`LootArmorItem`; only the shift-expanded stats block differs, passed as a lambda.
- **`LootUtils.getModifiers`** already filters config-disabled traits — never re-check `Config.traitEnabled` on its results.
- **`EntityHurtModifier.dealBonusDamage(hurtee, hurter, amount)`** — use this for any post-hit bonus melee damage. It resets `invulnerableTime` (otherwise the bonus is swallowed by i-frames) and picks the correct `playerAttack`/`mobAttack` source. Never call `hurtee.hurt(...)` directly for a follow-up bonus.
- **`LootUtils.breakBlockAsPlayer(stack, pos, player, level, state)`** — breaks a block as the player (drops + stats) and returns whether it was actually destroyed; only spend durability when it returns `true`.
- **Leveled traits** persist their level under `ModifierConstants.LEVEL` (`"trait_level"`); classes migrated from the old `"level"` key read both for back-compat.

### Hurter Modifier Pattern (EntityHurtModifier)
```java
public class MyModifier extends AbstractModifier implements EntityHurtModifier {
    // `name` is inherited from AbstractModifier; declare only trait-specific state
    private int level;

    // For stateful modifiers (tracking data between uses)
    private Map<String, Integer> myData = new HashMap<>();

    @Override
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
        // Server-side only for state changes
        if (hurtee.level().isClientSide()) {
            return false;
        }

        // Get entity registry key
        String entityKey = EntityType.getKey(hurtee.getType()).toString(); // e.g., "minecraft:zombie"

        // Check if entity is dead (after damage applied)
        if (hurtee.getHealth() <= 0) {
            // Update state and save
            LootUtils.updateModifier(itemstack, this);
        }

        return false; // return true to skip durability damage
    }

    @Override
    public boolean forTool(ToolType type) {
        return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
    }

    // For leveling: canLevel() returns true if can level up, levelUp() increments level
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
- **XP**: armor levels from damage taken - `ArmorDispatcher` (NeoForge `LivingDamageEvent.Post`).
  Unbreaking skips armor durability via `ArmorHurtEvent`. New `WearerHurtModifier` interface
  dispatches on `LivingDamageEvent.Pre` (Thorny, Featherweight, Bulwark, Adrenaline).
- **Worn textures**: 10 sets. `assets/randomloot/equipment/setN.json` +
  `textures/entity/equipment/humanoid{,_leggings}/setN.png`. Item sprites are
  `textures/item/<piece>N.png` dispatched via `items/armor.json` range_dispatch on the same
  `randomloot:cosmetic` property as tools (offsets: helmet .5, chest .6, legs .7, boots .8).
  All armor art is generated by `assets/randomloot/gen_armor.py` (gradient-maps the old mod's
  titanium/heavy art into 10 palettes; old repo checked out at /tmp/randomlootmod).
- **Generation**: `genTool` rolls armor with probability `Config.ArmorChance` (default 0.15).
- **Smithing**: `TraitAdditionRecipe` accepts armor bases; MOD_ADD on armor is gated by
  `modifier.forTool(pieceType)` (tools keep their historical anything-goes behavior).
- **Enchants**: armor item sits in all `minecraft:enchantable/*_armor` tags; per-piece filtering in
  `LootArmorItem.supportsEnchantment` via `data/randomloot/tags/enchantment/{all_armor,helmets,chestplates,leggings,boots}.json`.
- **Repair**: anvil material repair via `data/randomloot/tags/item/armor_repair_materials.json` (diamond).

Gotcha: GenWiki only regenerates the root *.md docs when run with env `RL_PROD=false` (and
optionally `RL_WIKI_DIR=<repo root>`), e.g.
`RL_PROD=false RL_WIKI_DIR=$PWD ./gradlew runGameTestServer`.
