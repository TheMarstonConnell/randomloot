package dev.marston.randomloot.loot.modifiers.stats;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Momentum implements StatsModifier, BlockBreakModifier, EntityHurtModifier {

	private String name;
	private long lastAction;
	private int stack;
	private static final int MAX_STACK = 5;
	private static final int TIMEOUT_SECONDS = 3;
	private static final float BONUS_PER_STACK = 0.05f; // 5% per stack

	public Momentum() {
		this("Momentum");
	}

	public Momentum(String name) {
		this(name, 0L, 0);
	}

	public Momentum(String name, long lastAction, int stack) {
		this.name = name;
		this.lastAction = lastAction;
		this.stack = stack;
	}

	public Modifier clone() {
		return new Momentum();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putLong("lastAction", lastAction);
		tag.putInt("stack", stack);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Momentum(
			tag.getStringOr(NAME, "Momentum"),
			tag.getLongOr("lastAction", 0L),
			tag.getIntOr("stack", 0)
		);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "momentum";
	}

	@Override
	public String color() {
		return ChatFormatting.GOLD.getName();
	}

	@Override
	public String description() {
		return "Each consecutive block broken or enemy hit within " + TIMEOUT_SECONDS + 
			   " seconds increases speed/damage by 5% (max 25%). Resets when idle.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (level != null) {
			updateStack(level);
			if (stack > 0) {
				int percent = (int) (stack * BONUS_PER_STACK * 100);
				return Modifier.makeComp("+" + percent + "% (" + stack + "/" + MAX_STACK + ")", ChatFormatting.GOLD);
			} else {
				return Modifier.makeComp("No Stack", ChatFormatting.GRAY);
			}
		}
		return null;
	}

	@Override
	public boolean forTool(ToolType type) {
		return true; // Works on all tools
	}

	@Override
	public float getStats(ItemStack itemstack) {
		// Returns the multiplier (1.0 = no bonus, 1.25 = 25% bonus)
		return 1.0f + (stack * BONUS_PER_STACK);
	}

	private void updateStack(Level level) {
		if (level == null) return;
		
		long currentTime = level.getGameTime();
		long timeSinceAction = currentTime - lastAction;
		
		// If more than TIMEOUT_SECONDS have passed, reset stack
		if (timeSinceAction > TIMEOUT_SECONDS * 20L) {
			stack = 0;
		}
	}

	private void incrementStack(Level level, ItemStack itemstack) {
		if (level == null) return;
		
		long currentTime = level.getGameTime();
		long timeSinceAction = currentTime - lastAction;
		
		// If within timeout window, increment stack
		if (timeSinceAction <= TIMEOUT_SECONDS * 20L) {
			if (stack < MAX_STACK) {
				stack++;
			}
		} else {
			// Reset and start new stack
			stack = 1;
		}
		
		lastAction = currentTime;
		LootUtils.updateModifier(itemstack, this);
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity player) {
		Level level = player.level();
		incrementStack(level, itemstack);
		
		// Visual feedback - show particles when stacking
		if (stack > 1 && !level.isClientSide()) {
			Modifier.TrackEntityParticle(level, player, ParticleTypes.FLAME);
		}
		
		return false;
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		Level level = hurtee.level();
		incrementStack(level, itemstack);
		
		// Visual feedback - show particles when stacking
		if (stack > 1 && !level.isClientSide()) {
			Modifier.TrackEntityParticle(level, hurtee, ParticleTypes.FLAME);
		}
		
		return false;
	}
}
