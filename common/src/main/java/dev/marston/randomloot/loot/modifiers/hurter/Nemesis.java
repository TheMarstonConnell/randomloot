package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.EntityKillModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Stored;
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

public class Nemesis extends AbstractModifier implements EntityHurtModifier, EntityKillModifier {
	private static final String KILL_COUNTS = "killCounts";

	/** How many of each entity type this weapon has killed, by registry key. */
	private final Stored<Map<String, Integer>> killCounts = stored(KILL_COUNTS, new HashMap<>(), Nemesis::readCounts,
			Nemesis::writeCounts);

	private static Map<String, Integer> readCounts(CompoundTag tag, String key, Map<String, Integer> none) {
		Map<String, Integer> counts = new HashMap<>();
		CompoundTag stored = tag.getCompoundOrEmpty(key);
		for (String entity : stored.keySet()) {
			counts.put(entity, stored.getIntOr(entity, 0));
		}
		return counts;
	}

	private static void writeCounts(CompoundTag tag, String key, Map<String, Integer> counts) {
		CompoundTag stored = new CompoundTag();
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			stored.putInt(entry.getKey(), entry.getValue());
		}
		tag.put(key, stored);
	}

	@Override
	public String description() {
		return "Tracks mob kills and deals " + String.format("%.0f", bonusDamagePercent() * 100) + "% bonus damage to your most killed mob type.";
	}

	public float bonusDamagePercent() {
		return traitLevel() * 0.05f;
	}

	private String getMostKilledEntity() {
		String mostKilled = null;
		int maxCount = 0;

		for (Map.Entry<String, Integer> entry : killCounts.get().entrySet()) {
			if (entry.getValue() > maxCount) {
				maxCount = entry.getValue();
				mostKilled = entry.getKey();
			}
		}

		return mostKilled;
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		String mostKilled = getMostKilledEntity();
		if (mostKilled == null) {
			return Modifier.makeComp("No nemesis yet", ChatFormatting.GRAY);
		}

		String entityName = LootUtils.titleCase(mostKilled);
		int bonusPercent = (int) (bonusDamagePercent() * 100);

		return Modifier.makeComp(entityName + " +" + bonusPercent + "%", ChatFormatting.GRAY);
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		String entityKey = EntityType.getKey(hurtee.getType()).toString();
		String mostKilled = getMostKilledEntity();

		// Apply bonus damage if attacking most-killed type
		if (mostKilled != null && mostKilled.equals(entityKey)) {
			dealBonusDamage(hurtee, hurter, attackDamage(itemstack) * bonusDamagePercent());
		}
	}

	@Override
	public void onKill(ItemStack itemstack, LivingEntity victim, LivingEntity killer) {
		String entityKey = EntityType.getKey(victim.getType()).toString();

		killCounts.get().merge(entityKey, 1, Integer::sum);
	}
}
