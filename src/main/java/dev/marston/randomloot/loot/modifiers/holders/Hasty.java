package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.LeveledModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Hasty extends LeveledModifier implements HoldModifier {

	/** Legacy NBT key; old items stored the haste amplifier separately, always equal to level. */
	private final static String POWER = "power";

	public Hasty(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Hasty() {
		this("Hasty", 0);
	}

	@Override
	protected int minLevel() {
		return 0;
	}

	@Override
	protected int maxLevel() {
		return 1;
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = super.toNBT();
		// Keep writing the legacy key so older readers still see the haste amplifier.
		tag.putInt(POWER, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Hasty(tag.getStringOr(NAME, "Hasty"),
				ModifierConstants.getLevel(tag, tag.getIntOr(POWER, 0)));
	}

	@Override
	public String tagName() {
		return "hasty";
	}

	@Override
	public String color() {
		return ChatFormatting.BLUE.getName();
	}

	@Override
	public String description() {
		return "While holding or wearing this item, get the Haste " + LootUtils.roman(this.level + 1) + " effect.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type) || type.isArmor();
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		// hold() runs every tick; a too-short duration makes the effect timer flicker.
		MobEffectInstance haste = new MobEffectInstance(MobEffects.HASTE, 40, this.level, true, false);

		if (holder instanceof LivingEntity le) {
			le.addEffect(haste);
		}
	}
}
