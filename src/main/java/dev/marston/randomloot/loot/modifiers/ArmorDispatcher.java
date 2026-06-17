package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootArmorItem;
import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Routes damage taken by a wearer to their Random Armor pieces: wearer-hurt
 * traits ({@link WearerHurtModifier}), armor XP (armor levels from absorbing
 * hits, like tools level from swinging), and Unbreaking's chance to skip armor
 * durability loss.
 */
@EventBusSubscriber(modid = RandomLoot.MODID)
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

	@SubscribeEvent
	public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
		LivingEntity wearer = event.getEntity();
		if (wearer.level().isClientSide()) {
			return;
		}

		float damage = event.getNewDamage();
		if (damage <= 0.0f) {
			return;
		}

		for (ItemStack stack : wornLootArmor(wearer)) {
			for (Modifier mod : LootUtils.getModifiers(stack)) {
				if (mod instanceof WearerHurtModifier whm && Config.traitEnabled(mod.tagName())) {
					damage = whm.onWearerHurt(stack, wearer, event.getSource(), damage);
				}
			}
		}

		event.setNewDamage(Math.max(0.0f, damage));
	}

	@SubscribeEvent
	public static void onLivingDamagePost(LivingDamageEvent.Post event) {
		LivingEntity wearer = event.getEntity();
		if (wearer.level().isClientSide()) {
			return;
		}

		float damage = event.getInflictedDamage();
		if (damage <= 0.0f) {
			return;
		}

		// Armor earns XP by soaking hits, mirroring tools earning XP per block/swing.
		int xp = Math.max(1, Math.round(damage));
		for (ItemStack stack : wornLootArmor(wearer)) {
			LootUtils.addXp(stack, wearer, xp);
		}
	}

	@SubscribeEvent
	public static void onArmorHurt(ArmorHurtEvent event) {
		LivingEntity wearer = event.getEntity();
		if (wearer.level().isClientSide()) {
			return;
		}

		for (Map.Entry<EquipmentSlot, ArmorHurtEvent.ArmorEntry> entry : event.getArmorMap().entrySet()) {
			ItemStack stack = entry.getValue().armorItemStack;
			if (!(stack.getItem() instanceof LootArmorItem)) {
				continue;
			}

			for (Modifier mod : LootUtils.getModifiers(stack)) {
				if (mod instanceof Unbreaking unbreaking && Config.traitEnabled(mod.tagName())
						&& unbreaking.test(wearer.level())) {
					event.setNewDamage(entry.getKey(), 0.0f);
				}
			}
		}
	}
}
