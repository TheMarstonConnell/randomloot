package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.NbtCompat;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Healing extends LeveledModifier implements HoldModifier {

	public Healing(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Healing() {
		this("Living", 1);
	}

	@Override
	protected int minLevel() {
		return 1;
	}

	@Override
	protected int maxLevel() {
		return 3;
	}

	private float getPower() {
		// Level 1: 0.5%, Level 2: 1%, Level 3: 2%
		return 0.005f * (float) Math.pow(2, level - 1);
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Healing(NbtCompat.getStringOr(tag, NAME, "Living"), ModifierConstants.getLevel(tag, 1));
	}

	@Override
	public String tagName() {
		return "living";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.GREEN;
	}

	@Override
	public String description() {
		return "While held or worn, " + String.format("%.1f", getPower() * 100) + "% chance per tick to repair itself";
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
					p.sendSystemMessage(comp);
				}
			}

		}

	}
}
