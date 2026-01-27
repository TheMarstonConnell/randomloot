package dev.marston.randomloot.items;

import net.minecraft.world.item.Item;

/**
 * Placeholder item for legacy items from the 1.12 version of Random Loot.
 * These items exist only to catch old items when players upgrade.
 * When held in hand, they are converted to cases via LegacyMigrationHandler.
 */
public class LegacyItem extends Item {
    private final String legacyType;

    public LegacyItem(Properties props, String legacyType) {
        super(props.stacksTo(1));
        this.legacyType = legacyType;
    }

    public String getLegacyType() {
        return legacyType;
    }
}
