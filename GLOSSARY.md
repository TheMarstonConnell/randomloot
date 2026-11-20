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

**Loader hook contract**:
The list of game events the shared code needs from a loader, each with what a loader shim must hand over: at which moment of the event, on which side, and for which cases. Both shims answer to the same contract, so what an event means for gear is decided once, behind it.
_Avoid_: event API, hook registry, bindings

**Gametest table**:
The one list of the gametests that both loaders run. A shared gametest exists by having a row in it, so it cannot be present on one loader and missing on the other.
_Avoid_: test registry, test list, test manifest

**Trait declaration**:
The one statement of what a trait is apart from its rule: the tag gear stores it under, its name and colour, the gear types it fits, the traits it will not share gear with, how far it levels and which wiki groups it sits in. Whatever needs one of those facts reads it from there.
_Avoid_: trait definition, trait config (config is the toml file), registration

**Trait registry**:
The module that holds every trait declaration and makes trait instances from them, a new one for each grant and for each trait read off gear. It owns how a trait is stored and read back, the one levelling scheme, and it applies a declared conflict to both traits of the pair.
_Avoid_: modifier list, role sets, prototype (the registry's own instance of a trait is never put on gear)

**Trait grant**:
The one operation that puts a trait on a piece of gear or takes one off, and reports what happened: added, leveled, removed, or refused and why. A case roll, a level-up, the smithing table and the admin command all change gear's traits through it.
_Avoid_: add modifier, apply trait, trait change

**Trait dispatch**:
The one path a game event takes to the traits on a piece of gear: a hit, a broken block, a right-click, a tick, a kill, damage to the wearer. It settles what every trait shares, such as which ones run, in what order, what gets saved and what durability is spent, so a trait only states its own rule.
_Avoid_: trait loop, event bus, handler

**Gear roll**:
The decision of what a case produces: the gear's type, traits, name, lore and look, made from plain facts and chance before any item exists. Stamping is the single step that then makes the item.
_Avoid_: generation, gen tool, loot roll

**XP step**:
What a gain of XP does to a piece of gear's level: the level it ends on, the XP left over and how many levels it gained. Each level gained is then applied as one level-up.
_Avoid_: level-up loop, XP loop

**Config setting**:
One value a player or pack maker can change in the config file, such as the chance of finding a case or whether a trait is switched on. Each is declared once, and the file, its row in the config guide and the value the game reads all come from that declaration.
_Avoid_: option, config value, property

**Wiki generation**:
Producing the player docs in the repo (the trait list, config guide, progression, names, loot and biome pages) from the game's own loaded data, so a page cannot state something the game does not do. It refuses to produce a page with a gap in it, such as a trait with no recipe.
_Avoid_: docs build, doc gen, wiki export
