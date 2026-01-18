package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Charging implements EntityHurtModifier {
	private String name;
	private int points;
	private final static String POINTS = "points";
	private long charged;
	private final static String CHARGED = "charged";

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

	public Modifier clone() {
		return new Charging();
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putInt(POINTS, points);
		tag.putString(NAME, name);
		tag.putLong(CHARGED, charged);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Charging(tag.getStringOr(NAME, "Charged"), tag.getIntOr(POINTS, 10), tag.getLongOr(CHARGED, 0L));
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "charged";
	}

	@Override
	public String color() {
		return ChatFormatting.YELLOW.getName();
	}

	@Override
	public String description() {
		return "After " + this.points
				+ " seconds, hitting and enemy will summon a lightning bolt and empty the charge meter.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	private float getCharge(Level level) {
		if (level != null) {
			long time = level.getGameTime();

			long diff = time - charged;

			float rate = (float) (diff) / (float) (points * 20);
			if (rate > 1.0f) {
				rate = 1.0f;
			}

			return rate;
		}

		return 0.0f;
	}

	@Override
	public Component writeDetailsToLore(Level level) {

		if (level != null) {
			float charge = getCharge(level);

			String perc = String.format("%.0f%% Charged", charge * 100.0f);

			return Modifier.makeComp(perc, ChatFormatting.GREEN);
		}

		return null;
	}

	@Override
	public boolean compatible(Modifier mod) {
		return true;
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {

		Level level = hurtee.level();

		long time = level.getGameTime();

		if (getCharge(level) >= 1.0f) {
			LightningBolt lb = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
			lb.setPos(hurtee.position());
			if (hurter instanceof ServerPlayer) {
				lb.setCause((ServerPlayer) hurter);
			}


			level.addFreshEntity(lb);

			this.charged = time;
			LootUtils.updateModifier(itemstack, this);
		}

		return false;

	}

	public boolean canLevel() {
		return false;
	}

	public void levelUp() {
		return;
	}

}
