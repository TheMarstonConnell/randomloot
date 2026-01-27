package dev.marston.randomloot.loot.modifiers.misc;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Territorial implements HoldModifier {
	private String name;
	private BlockPos territoryCenter;
	private long ticksInTerritory;
	private float maxBonusPercent;
	private int territoryRadius;
	private final static String TERRITORY_CENTER_X = "territory_center_x";
	private final static String TERRITORY_CENTER_Y = "territory_center_y";
	private final static String TERRITORY_CENTER_Z = "territory_center_z";
	private final static String TICKS_IN_TERRITORY = "ticks_in_territory";
	private final static String MAX_BONUS = "max_bonus_percent";
	private final static String TERRITORY_RADIUS = "territory_radius";

	public Territorial(String name, BlockPos center, long ticks, float maxBonus, int radius) {
		this.name = name;
		this.territoryCenter = center;
		this.ticksInTerritory = ticks;
		this.maxBonusPercent = maxBonus;
		this.territoryRadius = radius;
	}

	public Territorial() {
		this.name = "Territorial";
		this.territoryCenter = null;
		this.ticksInTerritory = 0;
		this.maxBonusPercent = 50.0f; // 50% max bonus
		this.territoryRadius = 32; // 32 block radius
	}

	public Modifier clone() {
		return new Territorial();
	}

	private boolean isInTerritory(BlockPos playerPos) {
		if (territoryCenter == null) return false;
		
		double distanceSq = territoryCenter.distSqr(playerPos);
		return distanceSq <= (territoryRadius * territoryRadius);
	}

	private float getCurrentBonus() {
		// 1 hour = 72,000 ticks (20 ticks/sec * 3600 sec/hour)
		// Max bonus at 36 hours = 2,592,000 ticks
		long maxTicks = 2592000L; // 36 hours worth of ticks
		
		float progress = Math.min(1.0f, (float) ticksInTerritory / maxTicks);
		return progress * maxBonusPercent;
	}

	public float getEfficiencyMultiplier() {
		return 1.0f + (getCurrentBonus() / 100.0f);
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (!(holder instanceof Player)) {
			return;
		}

		Player player = (Player) holder;
		BlockPos currentPos = player.blockPosition();

		if (territoryCenter == null) {
			// First time using tool - establish territory
			territoryCenter = currentPos;
			ticksInTerritory = 1;
			return;
		}

		if (isInTerritory(currentPos)) {
			// Player is in their territory - gain time
			ticksInTerritory++;
		} else {
			// Player left territory - reset if far enough away
			double distance = Math.sqrt(territoryCenter.distSqr(currentPos));
			if (distance > territoryRadius * 2) { // Double radius to give some buffer
				// Reset territory to current location
				territoryCenter = currentPos;
				ticksInTerritory = 1;
			}
		}
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putLong(TICKS_IN_TERRITORY, ticksInTerritory);
		tag.putFloat(MAX_BONUS, maxBonusPercent);
		tag.putInt(TERRITORY_RADIUS, territoryRadius);
		
		if (territoryCenter != null) {
			tag.putInt(TERRITORY_CENTER_X, territoryCenter.getX());
			tag.putInt(TERRITORY_CENTER_Y, territoryCenter.getY());
			tag.putInt(TERRITORY_CENTER_Z, territoryCenter.getZ());
		}
		
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		BlockPos center = null;
		if (tag.contains(TERRITORY_CENTER_X)) {
			center = new BlockPos(
				tag.getIntOr(TERRITORY_CENTER_X, 0),
				tag.getIntOr(TERRITORY_CENTER_Y, 0),
				tag.getIntOr(TERRITORY_CENTER_Z, 0)
			);
		}
		
		return new Territorial(
			tag.getStringOr(NAME, "Territorial"),
			center,
			tag.getLongOr(TICKS_IN_TERRITORY, 0),
			tag.getFloatOr(MAX_BONUS, 50.0f),
			tag.getIntOr(TERRITORY_RADIUS, 32)
		);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "territorial";
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_GREEN.name();
	}

	@Override
	public String description() {
		return "Tool efficiency increases the longer you stay in a " + territoryRadius + 
		       "-block territory. Max " + (int)maxBonusPercent + "% bonus after extended residence.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);

		if (shift) {
			float currentBonus = getCurrentBonus();
			long hours = ticksInTerritory / 72000; // Convert ticks to hours
			long minutes = (ticksInTerritory % 72000) / 1200; // Remaining minutes
			
			list.add(Component.literal("  ").append(
				Component.literal("Territory: " + territoryRadius + " block radius").withStyle(ChatFormatting.GRAY)
			));
			
			if (territoryCenter != null) {
				list.add(Component.literal("  ").append(
					Component.literal("Center: " + territoryCenter.getX() + ", " + 
					                  territoryCenter.getY() + ", " + territoryCenter.getZ())
					.withStyle(ChatFormatting.GRAY)
				));
			}
			
			list.add(Component.literal("  ").append(
				Component.literal("Time established: " + hours + "h " + minutes + "m")
				.withStyle(ChatFormatting.AQUA)
			));
			
			list.add(Component.literal("  ").append(
				Component.literal("Current bonus: " + String.format("%.1f", currentBonus) + "%")
				.withStyle(currentBonus > 25 ? ChatFormatting.GOLD : ChatFormatting.GREEN)
			));
		}
	}

	@Override
	public boolean compatible(Modifier mod) {
		return true;
	}

	@Override
	public boolean forTool(ToolType type) {
		return true; // Works for all tool types
	}
}