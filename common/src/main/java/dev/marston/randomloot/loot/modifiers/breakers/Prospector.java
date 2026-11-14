package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.modifiers.Stored;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
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
	private static final String TOTAL_FINDS = "totalFinds";

	private final Stored<Integer> totalFinds = stored(TOTAL_FINDS, 0);

	private static final float BASE_CHANCE = 0.03f;
	private static final float CHANCE_PER_LEVEL = 0.01f;

	private static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(
			Registries.LOOT_TABLE,
			Identifier.fromNamespaceAndPath("randomloot", "prospector_drops"));

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
	public void startBreak(ItemStack itemstack, BlockState broken, BlockPos pos, LivingEntity entity) {
		Level level = entity.level();

		if (!isStoneBlock(broken)) {
			return;
		}

		float chance = BASE_CHANCE + (traitLevel() * CHANCE_PER_LEVEL);
		if (level.getRandom().nextFloat() > chance) {
			return;
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
			this.totalFinds.set(this.totalFinds.get() + 1);
		}
	}

	@Override
	public String description() {
		int chancePercent = (int) ((BASE_CHANCE + (traitLevel() * CHANCE_PER_LEVEL)) * 100);
		return "Mining stone has a " + chancePercent + "% chance to discover bonus minerals.";
	}

	@Override
	public Component writeDetailsToLore(Level level) {
		if (totalFinds.get() == 0) {
			return Modifier.makeComp("No minerals found yet", ChatFormatting.GRAY);
		}
		return Modifier.makeComp(totalFinds.get() + " minerals found", ChatFormatting.GRAY);
	}

}
