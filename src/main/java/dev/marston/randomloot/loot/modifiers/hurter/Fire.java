package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


public class Fire extends AbstractModifier implements EntityHurtModifier {
	private final static String POINTS = "points";
	/** Seconds of fire at base level; each level-up adds one second up to the max. */
	private final static int BASE_SECONDS = 2;
	private final static int MAX_SECONDS = 5;

	private int points;

	public Fire(String name, int points) {
		this.name = name;
		this.points = points;
	}

	public Fire() {
		this.name = "Flaming";
		this.points = BASE_SECONDS;
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putInt(POINTS, points);
		tag.putString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Fire(tag.getStringOr(NAME, "Flaming"), tag.getIntOr(POINTS, BASE_SECONDS));
	}

	@Override
	public String name() {
		if (points == BASE_SECONDS) {
			return name;
		}
		return name + " " + LootUtils.roman(points - 1);
	}

	@Override
	public String tagName() {
		return "flaming";
	}

	@Override
	public String color() {
		return ChatFormatting.RED.getName();
	}

	@Override
	public String description() {
		return "Sets enemy on fire for " + this.points + " seconds.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isWeapon(type);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		hurtee.setRemainingFireTicks(points * 20);
		return false;
	}

	public boolean canLevel() {
		return this.points < MAX_SECONDS;
	}

	public void levelUp() {
		this.points++;
	}
}
