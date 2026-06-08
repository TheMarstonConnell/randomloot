package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.ModifierConstants;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class Nemesis extends AbstractModifier implements EntityHurtModifier {
	private int level;
	private Map<String, Integer> killCounts;

	private static final String KILL_COUNTS = "killCounts";

	public Nemesis(String name, int level, Map<String, Integer> killCounts) {
		this.name = name;
		this.level = level;
		this.killCounts = killCounts;
	}

	public Nemesis() {
		this.name = "Nemesis";
		this.level = 1;
		this.killCounts = new HashMap<>();
	}

	public Modifier clone() {
		return new Nemesis();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);
		tag.putInt(ModifierConstants.LEVEL, level);

		CompoundTag killCountsTag = new CompoundTag();
		for (Map.Entry<String, Integer> entry : killCounts.entrySet()) {
			killCountsTag.putInt(entry.getKey(), entry.getValue());
		}
		tag.put(KILL_COUNTS, killCountsTag);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		String name = tag.getStringOr(NAME, "Nemesis");
		int level = ModifierConstants.getLevel(tag, 1);

		Map<String, Integer> killCounts = new HashMap<>();
		CompoundTag killCountsTag = tag.getCompoundOrEmpty(KILL_COUNTS);
		for (String key : killCountsTag.keySet()) {
			killCounts.put(key, killCountsTag.getIntOr(key, 0));
		}

		return new Nemesis(name, level, killCounts);
	}

	@Override
	public String name() {
		if (level == 1) {
			return name;
		}
		return name + " " + LootUtils.roman(level - 1);
	}

	@Override
	public String tagName() {
		return "nemesis";
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_RED.getName();
	}

	@Override
	public String description() {
		return "Tracks mob kills and deals " + String.format("%.0f", bonusDamagePercent() * 100) + "% bonus damage to your most killed mob type.";
	}

	public float bonusDamagePercent() {
		return this.level * 0.05f;
	}

	private String getMostKilledEntity() {
		String mostKilled = null;
		int maxCount = 0;

		for (Map.Entry<String, Integer> entry : killCounts.entrySet()) {
			if (entry.getValue() > maxCount) {
				maxCount = entry.getValue();
				mostKilled = entry.getKey();
			}
		}

		return mostKilled;
	}

	private int getMostKilledCount() {
		String mostKilled = getMostKilledEntity();
		if (mostKilled == null) {
			return 0;
		}
		return killCounts.getOrDefault(mostKilled, 0);
	}

	private String getEntityDisplayName(String registryName) {
		// Extract simple name from registry name (e.g., "minecraft:zombie" -> "Zombie")
		String simpleName = registryName;
		if (simpleName.contains(":")) {
			simpleName = simpleName.substring(simpleName.indexOf(":") + 1);
		}
		// Replace underscores with spaces and capitalize each word
		String[] words = simpleName.split("_");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (!word.isEmpty()) {
				if (result.length() > 0) {
					result.append(" ");
				}
				result.append(Character.toUpperCase(word.charAt(0)));
				if (word.length() > 1) {
					result.append(word.substring(1));
				}
			}
		}
		return result.toString();
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		String mostKilled = getMostKilledEntity();
		if (mostKilled == null) {
			return Modifier.makeComp("No nemesis yet", ChatFormatting.GRAY);
		}

		String entityName = getEntityDisplayName(mostKilled);
		int bonusPercent = (int) (bonusDamagePercent() * 100);

		return Modifier.makeComp(entityName + " +" + bonusPercent + "%", ChatFormatting.GRAY);
	}

	@Override
	public boolean forTool(ToolType type) {
		return isWeapon(type);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		// Only track kills on server side
		if (hurtee.level().isClientSide()) {
			return false;
		}

		String entityKey = EntityType.getKey(hurtee.getType()).toString();
		String mostKilled = getMostKilledEntity();

		// Apply bonus damage if attacking most-killed type
		if (mostKilled != null && mostKilled.equals(entityKey)) {
			float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
			float bonusDamage = baseDamage * bonusDamagePercent();
			hurtee.hurt(hurter.damageSources().mobAttack(hurter), bonusDamage);
		}

		// Track kill if the entity is dead
		if (hurtee.getHealth() <= 0) {
			int currentCount = killCounts.getOrDefault(entityKey, 0);
			killCounts.put(entityKey, currentCount + 1);

			// Save updated kill counts to the item
			LootUtils.updateModifier(itemstack, this);
		}

		return false;
	}

	@Override
	public boolean canLevel() {
		return this.level < 3;
	}

	@Override
	public void levelUp() {
		this.level++;
	}
}
