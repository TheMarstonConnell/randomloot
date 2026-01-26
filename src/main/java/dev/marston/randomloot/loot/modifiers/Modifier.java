package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Random;

public interface Modifier {

    public static final String MODTAG = "modifiers";
    static final String NAME = "name";

    /**
     * Spawn particles around an entity
     */
    public static void TrackEntityParticle(World world, Entity e, EnumParticleTypes particleType) {
        if (!world.isRemote && world instanceof WorldServer) {
            Random r = new Random();
            WorldServer ws = (WorldServer) world;

            for (int i = 0; i < 32; ++i) {
                double d0 = (double) (r.nextFloat() * 2.0F - 1.0F);
                double d1 = (double) (r.nextFloat() * 2.0F - 1.0F);
                double d2 = (double) (r.nextFloat() * 2.0F - 1.0F);
                if (!(d0 * d0 + d1 * d1 + d2 * d2 > 1.0D)) {
                    double d3 = e.posX + d0 * e.width / 4.0D;
                    double d4 = e.posY + e.height / 2.0D + d1 * e.height / 4.0D;
                    double d5 = e.posZ + d2 * e.width / 4.0D;
                    ws.spawnParticle(particleType, d3, d4, d5, 1, d0, d1 + 0.2D, d2, 0.0D);
                }
            }
        }
    }

    /**
     * Format text with color
     */
    public static String formatText(String text, TextFormatting color) {
        return color + text + TextFormatting.RESET;
    }

    /**
     * Format text with color name
     */
    public static String formatText(String text, String colorName) {
        TextFormatting color = TextFormatting.getValueByName(colorName);
        if (color == null) color = TextFormatting.WHITE;
        return color + text + TextFormatting.RESET;
    }

    /**
     * The internal tag name used for NBT storage
     */
    public String tagName();

    /**
     * Write lore text to the list for tooltip display
     */
    public void writeToLore(List<String> list, boolean shift);

    /**
     * Short description of what this modifier does
     */
    public String description();

    /**
     * Display name of the modifier
     */
    public String name();

    /**
     * Color name for display (lowercase, e.g., "gold", "aqua")
     */
    public String color();

    /**
     * Create a copy of this modifier
     */
    public Modifier clone();

    /**
     * Serialize to NBT
     */
    public NBTTagCompound toNBT();

    /**
     * Deserialize from NBT
     */
    public Modifier fromNBT(NBTTagCompound tag);

    /**
     * Check if this modifier is compatible with the given tool type
     */
    public boolean forTool(ToolType type);

    /**
     * Write additional details to lore (called when shift is held)
     */
    default String writeDetailsToLore(World world) {
        return null;
    }

    /**
     * Check if this modifier is compatible with another modifier
     */
    default boolean compatible(Modifier mod) {
        return true;
    }

    /**
     * Check if this modifier can level up
     */
    default boolean canLevel() {
        return false;
    }

    /**
     * Level up this modifier
     */
    default void levelUp() {
    }
}
