package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

import java.util.List;
import java.util.Random;

public class Prospector implements BlockBreakModifier {

	private String name;
	private int level;
	private int totalFinds;

	private static final String LEVEL = "level";
	private static final String TOTAL_FINDS = "totalFinds";
	private static final int MAX_LEVEL = 10;
	private static final float BASE_CHANCE = 0.03f;
	private static final float CHANCE_PER_LEVEL = 0.01f;

	private static final ResourceLocation LOOT_TABLE = new ResourceLocation("randomloot", "prospector_drops");

	public Prospector(String name, int level, int totalFinds) {
		this.name = name;
		this.level = level;
		this.totalFinds = totalFinds;
	}

	public Prospector() {
		this.name = "Prospector";
		this.level = 1;
		this.totalFinds = 0;
	}

	public Modifier clone() {
		return new Prospector();
	}

	private boolean isStoneBlock(net.minecraft.block.state.IBlockState state) {
		net.minecraft.block.Block block = state.getBlock();
		return block == net.minecraft.init.Blocks.STONE ||
			   block == net.minecraft.init.Blocks.COBBLESTONE ||
			   block == net.minecraft.init.Blocks.NETHERRACK;
	}

	private List<ItemStack> getDropsFromLootTable(WorldServer serverLevel, BlockPos pos) {
		LootTable lootTable = serverLevel.getLootTableManager().getLootTableFromLocation(LOOT_TABLE);
		LootContext.Builder builder = new LootContext.Builder(serverLevel);
		return lootTable.generateLootForPools(serverLevel.rand, builder.build());
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, EntityLivingBase entity) {
		World world = entity.world;
		if (world.isRemote) {
			return false;
		}

		net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
		if (!isStoneBlock(state)) {
			return false;
		}

		Random random = new Random();
		float chance = BASE_CHANCE + (this.level * CHANCE_PER_LEVEL);
		if (random.nextFloat() > chance) {
			return false;
		}

		WorldServer serverLevel = (WorldServer) world;
		List<ItemStack> drops = getDropsFromLootTable(serverLevel, pos);

		for (ItemStack drop : drops) {
			if (!drop.isEmpty()) {
				EntityItem itemEntity = new EntityItem(world,
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
				world.spawnEntity(itemEntity);
			}
		}

		if (!drops.isEmpty()) {
			world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.5f, 1.2f);
			this.totalFinds++;
			LootUtils.updateModifier(itemstack, this);
		}

		return false;
	}

	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString(NAME, name);
		tag.setInteger(LEVEL, level);
		tag.setInteger(TOTAL_FINDS, totalFinds);
		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new Prospector(
				tag.hasKey(NAME) ? tag.getString(NAME) : "Prospector",
				tag.hasKey(LEVEL) ? tag.getInteger(LEVEL) : 1,
				tag.hasKey(TOTAL_FINDS) ? tag.getInteger(TOTAL_FINDS) : 0);
	}

	@Override
	public String name() {
		if (level == 1) {
			return name;
		}
		return name + " " + LootUtils.roman(level);
	}

	@Override
	public String tagName() {
		return "prospector";
	}

	@Override
	public String color() {
		return TextFormatting.GOLD.getFriendlyName();
	}

	@Override
	public String description() {
		int chancePercent = (int) ((BASE_CHANCE + (this.level * CHANCE_PER_LEVEL)) * 100);
		return "Mining stone has a " + chancePercent + "% chance to discover bonus minerals.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public String writeDetailsToLore(World world) {
		if (totalFinds == 0) {
			return Modifier.formatText("No minerals found yet", TextFormatting.GRAY);
		}
		return Modifier.formatText(totalFinds + " minerals found", TextFormatting.GRAY);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE);
	}

	@Override
	public boolean canLevel() {
		return this.level < MAX_LEVEL;
	}

	@Override
	public void levelUp() {
		this.level++;
	}
}
