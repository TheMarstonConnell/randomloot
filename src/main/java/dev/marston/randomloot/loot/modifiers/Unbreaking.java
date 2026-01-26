package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class Unbreaking implements Modifier {

    final String name;
    int level;

    final static String LEVEL = "trait_level";

    public Unbreaking(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public Unbreaking() {
        this("Unbreaking", 0);
    }

    public String tagName() {
        return "unbreaking";
    }

    public void writeToLore(List<String> list, boolean shift) {
        list.add(Modifier.formatText(this.name(), this.color()));
    }

    public String description() {
        return "This tool has a " + String.format("%.0f", chance() * 100) + "% chance of not taking damage.";
    }

    public String name() {
        if (this.level == 0) {
            return this.name;
        }

        if (!this.canLevel()) {
            return "Unbreakable";
        }

        return this.name + " " + LootUtils.roman(this.level + 1);
    }

    public String color() {
        return TextFormatting.AQUA.getFriendlyName();
    }

    public Modifier clone() {
        return new Unbreaking(this.name, this.level);
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();

        tag.setString(NAME, name);
        tag.setInteger(LEVEL, level);

        return tag;
    }

    public Modifier fromNBT(NBTTagCompound tag) {
        String n = tag.hasKey(NAME) ? tag.getString(NAME) : "Unbreaking";
        int l = tag.hasKey(LEVEL) ? tag.getInteger(LEVEL) : 0;
        return new Unbreaking(n, l);
    }

    public boolean forTool(ToolType type) {
        return true;
    }

    public boolean canLevel() {
        return this.level < 4;
    }

    public void levelUp() {
        this.level++;
    }

    private float chance() {
        return 0.2f * (float) (this.level + 1);
    }

    public boolean test(World world) {
        Random r = new Random();
        float f = r.nextFloat();
        return f <= chance();
    }
}
