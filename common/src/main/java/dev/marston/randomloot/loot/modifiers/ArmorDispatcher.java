package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.hurter.EarlyBird;
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
 * durability loss. Reached from both loaders through
 * {@link dev.marston.randomloot.platform.GameHooks}, whose {@code damagePre},
 * {@code damagePost} and {@code armorHurt} say what a loader hands each method.
 */
public final class ArmorDispatcher {

	private ArmorDispatcher() {
	}

	/**
	 * The entity's worn Random Armor pieces. Lazily allocated: this runs for every
	 * damage event on every living entity server-wide, and almost none of them
	 * wear loot armor.
	 */
	private static List<ItemStack> wornLootArmor(LivingEntity entity) {
		List<ItemStack> worn = null;
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
				continue;
			}

			ItemStack stack = entity.getItemBySlot(slot);
			if (stack.getItem() instanceof LootArmorItem) {
				if (worn == null) {
					worn = new ArrayList<>(4);
				}
				worn.add(stack);
			}
		}
		return worn == null ? List.of() : worn;
	}

	/**
	 * Pre-damage hook: wearer-hurt traits may reduce (or alter) the damage. Returns the new damage.
	 * {@code damage} is what is about to be taken, after armor, protection enchantments and
	 * Resistance and before absorption, on both loaders.
	 */
	public static float onLivingDamagePre(LivingEntity wearer, DamageSource source, float damage) {
		if (wearer.level().isClientSide()) {
			return damage;
		}

		// This is the one place both loaders call before damage lands on any living
		// entity, so Early Bird records the victim's untouched health from here - for
		// every hit, since each one has to set or clear that record.
		EarlyBird.notePreDamage(wearer);

		if (damage <= 0.0f) {
			return damage;
		}

		// getModifiers already filters config-disabled traits.
		for (ItemStack stack : wornLootArmor(wearer)) {
			for (Modifier mod : LootUtils.getModifiers(stack)) {
				if (mod instanceof WearerHurtModifier whm) {
					damage = whm.onWearerHurt(stack, wearer, source, damage);
				}
			}
		}

		return Math.max(0.0f, damage);
	}

	/**
	 * Post-damage hook: armor earns XP by soaking hits, mirroring tools earning XP per block/swing.
	 * What {@code inflictedDamage} measures is not the same on both loaders yet - the damage
	 * taken on NeoForge, the damage dealt on Fabric; GameHooks.damagePost has the numbers.
	 */
	public static void onLivingDamagePost(LivingEntity wearer, float inflictedDamage) {
		if (wearer.level().isClientSide()) {
			return;
		}

		if (inflictedDamage <= 0.0f) {
			return;
		}

		// One hit teaches the armor at most a full health bar. The reported damage is
		// not capped at the wearer's health: /kill and Executioner's finishing blow pass
		// Float.MAX_VALUE, which used to round to Integer.MAX_VALUE XP.
		int xp = Math.max(1, Math.round(Math.min(inflictedDamage, wearer.getMaxHealth())));
		for (ItemStack stack : wornLootArmor(wearer)) {
			LootUtils.addXp(stack, wearer, xp);
		}
	}

	/**
	 * Durability hook for a single armor piece about to take damage. Returns the
	 * (possibly zeroed) durability damage after Unbreaking rolls.
	 *
	 * <p>The slot gate lives here rather than in the shims because the two loaders
	 * deliver very different scopes: NeoForge's ArmorHurtEvent fires only for armor
	 * slots, while Fabric's stand-in mixin hooks every ItemStack.hurtAndBreak. Without
	 * it, a loot armor piece damaged outside an armor slot got Unbreaking on Fabric
	 * only.
	 */
	public static float onArmorHurt(LivingEntity wearer, ItemStack stack, EquipmentSlot slot,
			float durabilityDamage) {
		if (slot == null || slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
			return durabilityDamage;
		}

		// instanceof next: on Fabric this hooks EVERY ItemStack.hurtAndBreak.
		if (!(stack.getItem() instanceof LootArmorItem)) {
			return durabilityDamage;
		}

		if (wearer.level().isClientSide()) {
			return durabilityDamage;
		}

		// getModifiers already filters config-disabled traits.
		for (Modifier mod : LootUtils.getModifiers(stack)) {
			if (mod instanceof Unbreaking unbreaking && unbreaking.test(wearer.level())) {
				return 0.0f;
			}
		}
		return durabilityDamage;
	}
}
