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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Synergy implements EntityHurtModifier {

	private String name;
	private static final float BONUS_PER_MODIFIER = 0.08f; // 8% per other modifier

	public Synergy() {
		this("Synergy");
	}

	public Synergy(String name) {
		this.name = name;
	}

	public Modifier clone() {
		return new Synergy();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Synergy(tag.getStringOr(NAME, "Synergy"));
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "synergy";
	}

	@Override
	public String color() {
		return ChatFormatting.LIGHT_PURPLE.getName();
	}

	@Override
	public String description() {
		return "Gains 8% bonus damage for each other modifier on this tool.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		return null; // Dynamic bonus calculated per hit
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	private float calculateBonus(ItemStack itemstack) {
		// Count other modifiers on this tool (excluding self)
		List<Modifier> modifiers = LootUtils.getModifiers(itemstack);
		int otherModifiers = Math.max(0, modifiers.size() - 1); // -1 to exclude this modifier

		return otherModifiers * BONUS_PER_MODIFIER;
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		Level level = hurtee.level();

		float bonusPercent = calculateBonus(itemstack);

		if (bonusPercent > 0) {
			float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
			float bonusDamage = baseDamage * bonusPercent;

			if (hurter instanceof net.minecraft.world.entity.player.Player p) {
				hurtee.hurt(hurter.damageSources().playerAttack(p), bonusDamage);
			} else {
				hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
			}

			// Visual feedback - purple particles when synergy activates
			if (!level.isClientSide()) {
				Modifier.TrackEntityParticle(level, hurtee, ParticleTypes.ENCHANT);
			}
		}

		return false;
	}

	@Override
	public boolean compatible(Modifier mod) {
		// Synergy works with everything!
		return true;
	}
}
