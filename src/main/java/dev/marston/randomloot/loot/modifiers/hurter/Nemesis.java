package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Nemesis implements EntityHurtModifier {
	private String name;
	private int level;
	private Map<String, Integer> killCounts;

	private static final String LEVEL = "level";
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
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();

		tag.setString(NAME, name);
		tag.setInteger(LEVEL, level);

		NBTTagCompound killCountsTag = new NBTTagCompound();
		for (Map.Entry<String, Integer> entry : killCounts.entrySet()) {
			killCountsTag.setInteger(entry.getKey(), entry.getValue());
		}
		tag.setTag(KILL_COUNTS, killCountsTag);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		String name = tag.hasKey(NAME) ? tag.getString(NAME) : "Nemesis";
		int level = tag.hasKey(LEVEL) ? tag.getInteger(LEVEL) : 1;

		Map<String, Integer> killCounts = new HashMap<>();
		if (tag.hasKey(KILL_COUNTS)) {
			NBTTagCompound killCountsTag = tag.getCompoundTag(KILL_COUNTS);
			for (String key : killCountsTag.getKeySet()) {
				killCounts.put(key, killCountsTag.getInteger(key));
			}
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
		return TextFormatting.DARK_RED.getFriendlyName();
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
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public String writeDetailsToLore(World world) {
		String mostKilled = getMostKilledEntity();
		if (mostKilled == null) {
			return Modifier.formatText("No nemesis yet", TextFormatting.GRAY);
		}

		String entityName = getEntityDisplayName(mostKilled);
		int bonusPercent = (int) (bonusDamagePercent() * 100);

		return Modifier.formatText(entityName + " +" + bonusPercent + "%", TextFormatting.GRAY);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, EntityLivingBase hurtee, EntityLivingBase hurter) {
		// Only track kills on server side
		if (hurtee.world.isRemote) {
			return false;
		}

		ResourceLocation entityRL = EntityList.getKey(hurtee);
		String entityKey = entityRL != null ? entityRL.toString() : "unknown";
		String mostKilled = getMostKilledEntity();

		// Apply bonus damage if attacking most-killed type
		if (mostKilled != null && mostKilled.equals(entityKey)) {
			float baseDamage = LootItem.getAttackDamage(itemstack, LootUtils.getToolType(itemstack));
			float bonusDamage = baseDamage * bonusDamagePercent();
			hurtee.attackEntityFrom(DamageSource.causeMobDamage(hurter), bonusDamage);
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
