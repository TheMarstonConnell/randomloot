package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.advancements.ModCriteria;
import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.ChargeTracker;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class Charging extends AbstractModifier implements EntityHurtModifier {
	private int points;
	private long charged;

	public Charging(String name, int points, long charged) {
		this.name = name;
		this.points = points;
		this.charged = charged;
	}

	public Charging() {
		this.name = "Charged";
		this.points = 10;
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
		return new Charging(tag.getStringOr(ModifierConstants.NAME, "Charged"), tag.getIntOr(ModifierConstants.POINTS, 10), tag.getLongOr(ModifierConstants.CHARGED, 0L));
	}

	@Override
	public String tagName() {
		return "charged";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.YELLOW;
	}

	@Override
	public String description() {
		return "After " + this.points
				+ " seconds, hitting and enemy will summon a lightning bolt and empty the charge meter.";
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (level != null) {
			float charge = ChargeTracker.getCharge(level, charged, points);

			String perc = String.format("%.0f%% Charged", charge * 100.0f);

			return Modifier.makeComp(perc, ChatFormatting.GREEN);
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

		if (ChargeTracker.getCharge(level, charged, points) >= 1.0f) {
			LightningBolt lb = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
			lb.setPos(hurtee.position());
			if (hurter instanceof ServerPlayer) {
				lb.setCause((ServerPlayer) hurter);
			}


			level.addFreshEntity(lb);

			ModCriteria.traitUsed(hurter, this);

			this.charged = time;
			LootUtils.updateModifier(itemstack, this);
		}

		return false;

	}
}
