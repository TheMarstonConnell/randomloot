package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Healing implements HoldModifier {

	private static final String LEVEL = "level";
	private static final int MAX_LEVEL = 3;

	private String name;
	private int level;

	public Healing(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Healing() {
		this.name = "Living";
		this.level = 1;
	}

	public Modifier clone() {
		return new Healing();
	}

	@Override
	public boolean canLevel() {
		return level < MAX_LEVEL;
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	private float getPower() {
		// Level 1: 0.5%, Level 2: 1%, Level 3: 2%
		return 0.005f * (float) Math.pow(2, level - 1);
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putInt(LEVEL, level);
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Healing(tag.getStringOr(NAME, "Living"), tag.getIntOr(LEVEL, 1));
	}

	@Override
	public String name() {
		if (level == 1) {
			return name;
		}
		return name + " " + LootUtils.roman(level);
	}

	@Override
	public String tagName() {
		return "living";
	}

	@Override
	public String color() {
		return ChatFormatting.GREEN.getName();
	}

	@Override
	public String description() {
		return "While holding, " + String.format("%.1f", getPower() * 100) + "% chance per tick to repair itself";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {

		MutableComponent comp = Modifier.makeComp(this.name(), this.color());

		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return true;
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		float power = getPower();
		float f = level.getRandom().nextFloat();
		if (f < power) {

			if (stack.getDamageValue() == 0) {
				return;
			}

			stack.setDamageValue(Math.max(stack.getDamageValue() - 1, 0));

			if (f < power / 5) {
				MutableComponent comp = Component.empty();

				comp.append("pssst...");
				comp = comp.withStyle(ChatFormatting.GRAY);
				comp = comp.withStyle(ChatFormatting.ITALIC);

				if (holder instanceof Player) {
					Player p = (Player) holder;
					p.displayClientMessage(comp, false);
				}
			}

		}

	}
}
