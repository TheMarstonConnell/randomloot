package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootNBT;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = RandomLoot.MODID)
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
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString(NAME, name);
		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Soulbound(tag.hasKey(NAME) ? tag.getString(NAME) : "Soulbound");
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
		return TextFormatting.DARK_PURPLE.getFriendlyName();
	}

	@Override
	public String description() {
		return "Grants 15% bonus damage and mining speed when wielded by the original owner.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		// Works for all tool types
		return true;
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, EntityLivingBase hurtee, EntityLivingBase hurter) {
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
		hurtee.attackEntityFrom(DamageSource.causeMobDamage(hurter), bonusDamage);

		return false;
	}

	/**
	 * Check if the given entity is the original owner of the tool.
	 */
	public static boolean isOwner(ItemStack stack, EntityLivingBase entity) {
		if (entity == null) {
			return false;
		}

		String ownerUUID = LootNBT.getOwnerUUID(stack);
		if (ownerUUID.isEmpty()) {
			return false;
		}

		return ownerUUID.equals(entity.getUniqueID().toString());
	}

	/**
	 * Event handler for mining speed bonus when player is the original owner.
	 */
	@SubscribeEvent
	public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
		EntityPlayer player = event.getEntityPlayer();
		ItemStack stack = player.getHeldItemMainhand();

		// Check if holding a LootItem
		if (stack.getItem() != ModItems.TOOL) {
			return;
		}

		// Check if the tool has Soulbound modifier
		List<Modifier> mods = LootNBT.getModifiers(stack);
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
