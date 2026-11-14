# Random Loot

An RPG-style loot mod for Minecraft: players open cases to get randomly generated gear that carries traits. One shared codebase ships for two mod loaders, NeoForge and Fabric.

## Language

### Loot

**Gear**:
A tool or armor piece this mod generates. Every piece has a type (pickaxe, axe, shovel, sword, helmet, chestplate, leggings or boots), a goodness, traits and its own look and name.
_Avoid_: loot item, tool (when armor is included), equipment

**Trait**:
A named ability or quirk carried by a piece of gear, such as Thorny or Veiny. Class names call the same thing a modifier.
_Avoid_: perk, ability, enchantment (enchantments are vanilla's and sit beside traits)

**Case**:
The item a player opens to receive one piece of gear. How many cases a player has opened sets how good their next gear is.
_Avoid_: crate, loot box, chest

**Goodness**:
The one quality number of a piece of gear, from which its speed, damage, defense and durability all derive. It starts from the number of cases the opener has opened and rises each time the gear levels up.
_Avoid_: level (gear has an XP level too), tier, rarity, stats

**Addition Template** and **Subtraction Template**:
The two smithing templates. An Addition Template puts a trait on gear, or levels one it already has, at the cost of the trait's recipe item; a Subtraction Template takes one off.
_Avoid_: mod_add and mod_sub (those are item ids), upgrade template

**Forger**:
The named smith a world credits its gear to. A world has one forger per temperature band (cold, temperate, hot), fixed by its seed, so the same world always names the same one.
_Avoid_: smith, maker, crafter

**Reveal roll**:
The short spin a freshly opened piece of gear goes through, cycling looks with its name and stats hidden, before it shows what it is.
_Avoid_: roll on its own (a roll is also any random draw, such as a trait roll), spin, animation

### Modules

**Loader shim**:
A loader's own code that routes that loader's game events into the shared code. There are two, one per loader, and neither holds game logic.
_Avoid_: glue, bridge, platform code

**Gametest table**:
The one list of the gametests that both loaders run. A shared gametest exists by having a row in it, so it cannot be present on one loader and missing on the other.
_Avoid_: test registry, test list, test manifest

**Trait grant**:
The one operation that puts a trait on a piece of gear or takes one off, and reports what happened: added, leveled, removed, or refused and why. A case roll, a level-up, the smithing table and the admin command all change gear's traits through it.
_Avoid_: add modifier, apply trait, trait change
