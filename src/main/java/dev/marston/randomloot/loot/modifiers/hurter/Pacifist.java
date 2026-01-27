package dev.marston.randomloot.loot.modifiers.hurter;

import java.util.List;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.StatsModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Pacifist implements EntityHurtModifier, StatsModifier {

	private String name;

	public Pacifist() {
		this.name = "Pacifist";
	}

	public Pacifist(String name) {
		this.name = name;
	}

	@Override
	public String tagName() {
		return "pacifist";
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public String color() {
		return ChatFormatting.GREEN.getName();
	}

	@Override
	public String description() {
		return "10% faster mining but deals no damage";
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Pacifist(tag.getStringOr(NAME, "Pacifist"));
	}

	@Override
	public Modifier clone() {
		return new Pacifist();
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE) || type.equals(ToolType.SHOVEL) || type.equals(ToolType.AXE);
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.level().isClientSide()) {
			return false;
		}

		// Heal the target by a large amount to negate any damage dealt
		hurtee.heal(1000f);

		return false;
	}

	@Override
	public float getStats(ItemStack itemstack) {
		// 10% faster mining speed
		return 1.10f;
	}
}
