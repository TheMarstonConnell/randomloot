package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class Prospector extends AbstractModifier implements BlockBreakModifier {

	private int level;
	private int totalFinds;

	private static final String LEVEL = "trait_level";
	private static final String TOTAL_FINDS = "totalFinds";
	private static final int MAX_LEVEL = 10;
	private static final float BASE_CHANCE = 0.03f;
	private static final float CHANCE_PER_LEVEL = 0.01f;

	private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(
			Registries.LOOT_TABLE,
			Identifier.fromNamespaceAndPath("randomloot", "prospector_drops"));

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

	private boolean isStoneBlock(BlockState state) {
		return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER);
	}

	private List<ItemStack> getDropsFromLootTable(ServerLevel serverLevel, BlockPos pos) {
		LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(LOOT_TABLE);

		LootParams params = new LootParams.Builder(serverLevel)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
				.create(LootContextParamSets.EMPTY);

		return lootTable.getRandomItems(params);
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity entity) {
		Level level = entity.level();
		if (level.isClientSide()) {
			return false;
		}

		BlockState state = level.getBlockState(pos);
		if (!isStoneBlock(state)) {
			return false;
		}

		float chance = BASE_CHANCE + (this.level * CHANCE_PER_LEVEL);
		if (level.getRandom().nextFloat() > chance) {
			return false;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		List<ItemStack> drops = getDropsFromLootTable(serverLevel, pos);

		for (ItemStack drop : drops) {
			if (!drop.isEmpty()) {
				ItemEntity itemEntity = new ItemEntity(level,
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
				level.addFreshEntity(itemEntity);
			}
		}

		if (!drops.isEmpty()) {
			level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5f, 1.2f);
			this.totalFinds++;
			LootUtils.updateModifier(itemstack, this);
		}

		return false;
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putInt(LEVEL, level);
		tag.putInt(TOTAL_FINDS, totalFinds);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Prospector(
				tag.getStringOr(NAME, "Prospector"),
				tag.getIntOr(LEVEL, tag.getIntOr("level", 1)),
				tag.getIntOr(TOTAL_FINDS, 0));
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
		return ChatFormatting.GOLD.getName();
	}

	@Override
	public String description() {
		int chancePercent = (int) ((BASE_CHANCE + (this.level * CHANCE_PER_LEVEL)) * 100);
		return "Mining stone has a " + chancePercent + "% chance to discover bonus minerals.";
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (totalFinds == 0) {
			return Modifier.makeComp("No minerals found yet", ChatFormatting.GRAY);
		}
		return Modifier.makeComp(totalFinds + " minerals found", ChatFormatting.GRAY);
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
