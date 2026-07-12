package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes damage taken by a wearer to their Random Armor pieces: wearer-hurt
 * traits ({@link WearerHurtModifier}), armor XP (armor levels from absorbing
 * hits, like tools level from swinging), and Unbreaking's chance to skip armor
 * durability loss. Each loader's event shim calls these from its damage
 * events.
 */
public final class ArmorDispatcher {

	private ArmorDispatcher() {
	}

	private static List<ItemStack> wornLootArmor(LivingEntity entity) {
		List<ItemStack> worn = new ArrayList<>();
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
				continue;
			}

			ItemStack stack = entity.getItemBySlot(slot);
			if (stack.getItem() instanceof LootArmorItem) {
				worn.add(stack);
			}
		}
		return worn;
	}

	/** Pre-damage hook: wearer-hurt traits may reduce (or alter) the damage. Returns the new damage. */
	public static float onLivingDamagePre(LivingEntity wearer, DamageSource source, float damage) {
		if (wearer.level().isClientSide()) {
			return damage;
		}

		if (damage <= 0.0f) {
			return damage;
		}

		for (ItemStack stack : wornLootArmor(wearer)) {
			for (Modifier mod : LootUtils.getModifiers(stack)) {
				if (mod instanceof WearerHurtModifier whm && Config.traitEnabled(mod.tagName())) {
					damage = whm.onWearerHurt(stack, wearer, source, damage);
				}
			}
		}

		return Math.max(0.0f, damage);
	}

	/** Post-damage hook: armor earns XP by soaking hits, mirroring tools earning XP per block/swing. */
	public static void onLivingDamagePost(LivingEntity wearer, float inflictedDamage) {
		if (wearer.level().isClientSide()) {
			return;
		}

		if (inflictedDamage <= 0.0f) {
			return;
		}

		int xp = Math.max(1, Math.round(inflictedDamage));
		for (ItemStack stack : wornLootArmor(wearer)) {
			LootUtils.addXp(stack, wearer, xp);
		}
	}

	/**
	 * Durability hook for a single armor piece about to take damage. Returns the
	 * (possibly zeroed) durability damage after Unbreaking rolls.
	 */
	public static float onArmorHurt(LivingEntity wearer, ItemStack stack, float durabilityDamage) {
		if (wearer.level().isClientSide()) {
			return durabilityDamage;
		}

		if (!(stack.getItem() instanceof LootArmorItem)) {
			return durabilityDamage;
		}

		for (Modifier mod : LootUtils.getModifiers(stack)) {
			if (mod instanceof Unbreaking unbreaking && Config.traitEnabled(mod.tagName())
					&& unbreaking.test(wearer.level())) {
				return 0.0f;
			}
		}
		return durabilityDamage;
	}
}
