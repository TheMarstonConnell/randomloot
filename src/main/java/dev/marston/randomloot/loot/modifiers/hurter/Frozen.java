package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class Frozen implements EntityHurtModifier, HoldModifier, BiomeRestrictedModifier {

	private String name;
	private int level = 0;
	private final static String LEVEL = "trait_level";

	public Frozen(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Frozen() {
		this.name = "Frozen";
		this.level = 0;
	}

	public Modifier clone() {
		return new Frozen();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putInt(LEVEL, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Frozen(tag.getStringOr(NAME, "Frozen"), tag.getIntOr(LEVEL, 0));
	}

	@Override
	public String name() {
		if (this.level == 0) {
			return this.name;
		}
		return this.name + " " + LootUtils.roman(this.level + 1);
	}

	@Override
	public String tagName() {
		return "frozen";
	}

	@Override
	public String color() {
		return ChatFormatting.AQUA.getName();
	}

	@Override
	public String description() {
		int radius = 3 + this.level;
		return "Slows enemies on hit. Creates " + radius + " block radius of frosted ice on water.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		int duration = 3 * 20;
		hurtee.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, this.level + 1, false, true));
		return false;
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (!(holder instanceof Player player)) return;

		// Check every 2 ticks for smoother ice generation
		if (level.getGameTime() % 2 != 0) return;

		BlockPos centerPos = player.blockPosition().below();

		// Radius scales with level: 3.0, 4.0, 5.0
		double radius = 3.0 + this.level;

		// Create circular pattern of frosted ice (like Frost Walker but wider)
		int radiusInt = (int) Math.ceil(radius);
		for (int xOffset = -radiusInt; xOffset <= radiusInt; xOffset++) {
			for (int zOffset = -radiusInt; zOffset <= radiusInt; zOffset++) {
				// Use floating-point distance for smoother circle
				double distance = Math.sqrt(xOffset * xOffset + zOffset * zOffset);
				if (distance > radius) {
					continue; // Skip blocks outside the circle
				}

				BlockPos pos = centerPos.offset(xOffset, 0, zOffset);
				BlockState below = level.getBlockState(pos);

				// Create frosted ice on water surface (mimic Frost Walker)
				if (below.is(Blocks.WATER) && below.getValue(LiquidBlock.LEVEL) == 0) {
					level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
				}
			}
		}
	}

	@Override
	public boolean canLevel() {
		return level < 2; // Max level 3 (0, 1, 2)
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	@Override
	public boolean canSpawnInBiome(String biomeKey, float temperature, String dimension) {
		return temperature <= 0.15f;
	}
}
