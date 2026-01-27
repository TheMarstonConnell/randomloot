package dev.marston.randomloot.items;

import net.minecraft.item.Item;

/**
 * Placeholder item for legacy items from the old 1.12 Random Loot mod.
 * These items exist only to catch old items when players upgrade.
 * When held in hand, they are converted to cases via LegacyMigrationHandler.
 */
public class LegacyItem extends Item {
    private final String legacyType;

    public LegacyItem(String legacyType) {
        this.legacyType = legacyType;
        this.setMaxStackSize(1);
    }

    public String getLegacyType() {
        return legacyType;
    }
}
