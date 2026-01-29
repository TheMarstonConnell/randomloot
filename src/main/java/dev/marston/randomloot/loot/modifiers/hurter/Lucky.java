package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Random;

public class Lucky implements EntityHurtModifier {

	private String name;
	private final static String LEVEL = "trait_level";
	private int level = 0;
	private static final Random random = new Random();

	public Lucky(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Lucky() {
		this.name = "Lucky";
		this.level = 0;
	}

	public Modifier clone() {
		return new Lucky();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putInt(LEVEL, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Lucky(tag.getStringOr(NAME, "Lucky"), tag.getIntOr(LEVEL, 0));
	}

	@Override
	public String name() {
		if (this.level == 0) {
			return this.name;
		}
		return this.name + " " + LootUtils.roman(this.level + 1);
	}

	@Override
	public String tagName() {
		return "lucky";
	}

	@Override
	public String color() {
		return ChatFormatting.GREEN.getName();
	}

	@Override
	public String description() {
		int chance = getChance();
		return chance + "% chance to deal double damage.";
	}

	private int getChance() {
		// 10%, 15%, 20%, 25% based on level
		return 10 + (level * 5);
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.level().isClientSide()) {
			return false;
		}

		// Check if we get lucky
		if (random.nextInt(100) >= getChance()) {
			return false;
		}

		// Lucky hit! Deal additional damage equal to base damage
		float dmg = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));

		// Visual feedback - enchanted hit particles
		Modifier.TrackEntityParticle(hurtee.level(), hurtee, ParticleTypes.ENCHANTED_HIT);

		if (hurter instanceof Player p) {
			hurtee.hurt(hurter.damageSources().playerAttack(p), dmg);
		} else {
			hurtee.hurt(hurter.damageSources().mobAttack(hurter), dmg);
		}

		return false;
	}

	public boolean canLevel() {
		return level < 3;
	}

	public void levelUp() {
		this.level++;
	}
}
