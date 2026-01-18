# Random Loot 2 - Minecraft Mod

## Overview
An RPG-style loot system mod for Minecraft that generates randomized tools with modifiers/traits. Built for NeoForge.

## Current Version
- **Minecraft**: 1.21.11
- **NeoForge**: 21.11.34-beta
- **ModDevGradle**: 2.0.131
- **Mod ID**: `randomloot`
- **Package**: `dev.marston.randomloot`

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
src/main/java/dev/marston/randomloot/
├── RandomLoot.java          # Main mod class
├── Config.java              # Mod configuration
├── GenWiki.java             # Wiki generation utility
├── ModLootModifiers.java    # Loot table modifiers
├── component/               # Data components
├── items/                   # Custom items (Tool, Case, Templates)
├── loot/                    # Core loot system
│   ├── LootItem.java        # Main tool item class
│   ├── LootUtils.java       # Utility functions
│   ├── LootCase.java        # Loot case item
│   └── modifiers/           # Trait/modifier system
│       ├── breakers/        # Block breaking traits
│       ├── holders/         # Held item traits
│       ├── hurters/         # Combat traits
│       ├── stats/           # Stat modifiers
│       └── users/           # Right-click traits
└── recipes/                 # Custom recipes
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

## Build Commands
```bash
./gradlew runClient          # Run client
./gradlew runServer          # Run server
./gradlew build              # Build mod jar
./gradlew downloadAssets     # Download MC assets
```

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
- `Item.hurtEnemy()` → `Item.postHurtEnemy()` with void return type
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

## Testing
- Use `/give @p randomloot:case` to get a loot case
- Right-click case to generate random tool
- Modifier templates can add/remove traits via smithing table
- Use `/give @p randomloot:mod_add` for addition template
- Use `/give @p randomloot:mod_sub` for subtraction template

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

## Adding New Modifiers
1. Create class in appropriate `modifiers/` subdirectory
2. Implement relevant interface (`BlockBreakModifier`, `HoldModifier`, etc.)
3. Register in `ModifierRegistry.java`
4. Add recipe JSON in `data/randomloot/recipe/`
5. Add to config in `Config.java` if toggleable
