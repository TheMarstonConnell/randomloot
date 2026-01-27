package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

@EventBusSubscriber(modid = RandomLoot.MODID)
public class Soulbound implements EntityHurtModifier {

	private String name;

	public Soulbound(String name) {
		this.name = name;
	}

	public Soulbound() {
		this.name = "Soulbound";
	}

	public Modifier clone() {
		return new Soulbound();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Soulbound(tag.getStringOr(NAME, "Soulbound"));
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public String tagName() {
		return "soulbound";
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_PURPLE.getName();
	}

	@Override
	public String description() {
		return "Grants 15% bonus damage and mining speed when wielded by the original owner.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		// Works for all tool types
		return true;
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		// Only apply bonus damage for swords and axes
		ToolType type = LootUtils.getToolType(itemstack);
		if (!type.equals(ToolType.SWORD) && !type.equals(ToolType.AXE)) {
			return false;
		}

		// Check if hurter is the original owner
		if (!isOwner(itemstack, hurter)) {
			return false;
		}

		// Apply 15% bonus damage
		float baseDamage = LootItem.getAttackDamage(itemstack, type);
		float bonusDamage = baseDamage * 0.15f;
		hurtee.hurt(hurtee.damageSources().mobAttack(hurter), bonusDamage);

		return false;
	}

	/**
	 * Check if the given entity is the original owner of the tool.
	 */
	public static boolean isOwner(ItemStack stack, LivingEntity entity) {
		if (entity == null) {
			return false;
		}

		String ownerUUID = LootUtils.getOwnerUUID(stack);
		if (ownerUUID.isEmpty()) {
			return false;
		}

		return ownerUUID.equals(entity.getStringUUID());
	}

	/**
	 * Event handler for mining speed bonus when player is the original owner.
	 */
	@SubscribeEvent
	public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
		Player player = event.getEntity();
		ItemStack stack = player.getMainHandItem();

		// Check if holding a LootItem
		if (!stack.is(ModItems.TOOL.get())) {
			return;
		}

		// Check if the tool has Soulbound modifier
		List<Modifier> mods = LootUtils.getModifiers(stack);
		boolean hasSoulbound = false;
		for (Modifier mod : mods) {
			if (mod.tagName().equals("soulbound")) {
				if (!Config.traitEnabled(mod.tagName())) {
					return;
				}
				hasSoulbound = true;
				break;
			}
		}

		if (!hasSoulbound) {
			return;
		}

		// Check if player is the original owner
		if (!isOwner(stack, player)) {
			return;
		}

		// Apply 15% mining speed bonus
		float currentSpeed = event.getNewSpeed();
		event.setNewSpeed(currentSpeed * 1.15f);
	}
}
