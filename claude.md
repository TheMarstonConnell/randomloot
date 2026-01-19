# Random Loot 2 - Minecraft Mod

> **Claude: Whenever you discover something important during development (API quirks, patterns, gotchas, or useful snippets), automatically add it to this file so you'll remember it in future sessions.**

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
3. Register in `ModifierRegistry.java` (add to both the static field AND the appropriate Set like `HURTERS`)
4. Add recipe JSON in `data/randomloot/recipe/trait_<tagname>.json`
5. Add to config in `Config.java` if toggleable

### Hurter Modifier Pattern (EntityHurtModifier)
```java
public class MyModifier implements EntityHurtModifier {
    // Required fields
    private String name;
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
