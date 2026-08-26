package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.NbtCompat;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.ChargeTracker;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Combo extends AbstractModifier implements EntityHurtModifier {
	private int points;
	private long charged;

	public Combo(String name, int points, long charged) {
		this.name = name;
		this.points = points;
		this.charged = charged;
	}

	public Combo() {
		this.name = "Dexterous";
		this.points = 2;
		this.charged = 0;
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putInt(ModifierConstants.POINTS, points);
		tag.putString(ModifierConstants.NAME, name);
		tag.putLong(ModifierConstants.CHARGED, charged);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Combo(NbtCompat.getStringOr(tag, ModifierConstants.NAME, "Dexterous"), NbtCompat.getIntOr(tag, ModifierConstants.POINTS, 2), NbtCompat.getLongOr(tag, ModifierConstants.CHARGED, 0L));
	}

	@Override
	public String tagName() {
		return "combo";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.YELLOW;
	}

	@Override
	public String description() {
		return "Hitting enemies within " + this.points + " seconds after hitting them deals an extra 25% damage.";
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (level != null) {
			float charge = ChargeTracker.getCharge(level, charged, points);

			String s = "Not Ready";
			if (charge < 1.0f) {
				s = "Ready";
			}

			return Modifier.makeComp(s, ChatFormatting.RED);
		}

		return null;
	}

	@Override
	public boolean forTool(ToolType type) {
		return isWeapon(type);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {

		Level level = hurtee.level();

		if (level.isClientSide()) {
			return false;
		}

		long time = level.getGameTime();

		if (ChargeTracker.getCharge(level, charged, points) < 1.0f) {

			float damage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));

			dealBonusDamage(hurtee, hurter, damage * 0.25f);

			Modifier.TrackEntityParticle(level, hurtee, ParticleTypes.CRIT);

		}

		this.charged = time;
		LootUtils.updateModifier(itemstack, this);

		return false;
	}
}
