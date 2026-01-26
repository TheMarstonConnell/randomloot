package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BiomeRestrictedModifier;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

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
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString(NAME, name);
		tag.setInteger(LEVEL, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Frozen(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Frozen",
			tag.hasKey(LEVEL) ? tag.getInteger(LEVEL) : 0);
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
		return TextFormatting.AQUA.getFriendlyName();
	}

	@Override
	public String description() {
		int radius = 3 + this.level;
		return "Slows enemies on hit. Creates " + radius + " block radius of ice on water.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, EntityLivingBase hurtee, EntityLivingBase hurter) {
		int duration = 3 * 20;
		hurtee.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, duration, this.level + 1, false, true));
		return false;
	}

	@Override
	public void hold(ItemStack stack, World world, Entity holder) {
		if (!(holder instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) holder;

		// Check every 2 ticks for smoother ice generation
		if (world.getTotalWorldTime() % 2 != 0) return;

		BlockPos centerPos = player.getPosition().down();

		// Radius scales with level: 3.0, 4.0, 5.0
		double radius = 3.0 + this.level;

		// Create circular pattern of ice (like Frost Walker but wider)
		int radiusInt = (int) Math.ceil(radius);
		for (int xOffset = -radiusInt; xOffset <= radiusInt; xOffset++) {
			for (int zOffset = -radiusInt; zOffset <= radiusInt; zOffset++) {
				// Use floating-point distance for smoother circle
				double distance = Math.sqrt(xOffset * xOffset + zOffset * zOffset);
				if (distance > radius) {
					continue; // Skip blocks outside the circle
				}

				BlockPos pos = centerPos.add(xOffset, 0, zOffset);
				IBlockState below = world.getBlockState(pos);

				// Create ice on water surface (mimic Frost Walker)
				if (below.getBlock() == Blocks.WATER && below.getValue(BlockLiquid.LEVEL) == 0) {
					// Use packed ice instead of frosted ice (1.12.2 doesn't have frosted ice)
					world.setBlockState(pos, Blocks.ICE.getDefaultState());
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
