package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


public class Critical extends AbstractModifier implements EntityHurtModifier {

	public Critical(String name) {
		this.name = name;
	}

	public Critical() {
		this.name = "Critical";
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Critical(tag.getStringOr(NAME, "Critical"));
	}

	@Override
	public String tagName() {
		return "critical";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.GOLD;
	}

	@Override
	public String description() {
		return "Always critically strikes enemy.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isWeapon(type);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.level().isClientSide()) {
			return false;
		}

		float dmg = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));

		Modifier.TrackEntityParticle(hurtee.level(), hurtee, ParticleTypes.CRIT);

		dealBonusDamage(hurtee, hurter, dmg * 0.5f);

		return false;
	}
}
