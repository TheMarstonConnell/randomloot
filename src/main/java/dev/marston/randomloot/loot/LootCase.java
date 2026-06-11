package dev.marston.randomloot.loot;

import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.jetbrains.annotations.NotNull;

public class LootCase extends Item {

	public static void initDispenser() {
		DispenserBlock.registerBehavior(ModItems.CASE.get(), new DefaultDispenseItemBehavior() {
			public @NotNull ItemStack execute(@NotNull BlockSource source, @NotNull ItemStack stack) {
				Direction direction = source.state().getValue(DispenserBlock.FACING);
				Position position = DispenserBlock.getDispensePosition(source);

				ItemStack tool = LootUtils.genTool(null, source.level(), source.pos());

				spawnTool(source.level(), tool, 6, direction, position);

				return ItemStack.EMPTY;
			}
		});
	}

	public static void spawnTool(Level level, ItemStack stack, int speed, Direction direction, Position pos) {
		double d0 = pos.x();
		double d1 = pos.y();
		double d2 = pos.z();
		if (direction.getAxis() == Direction.Axis.Y) {
			d1 -= 0.125D;
		} else {
			d1 -= 0.15625D;
		}

		ItemEntity itementity = new ItemEntity(level, d0, d1, d2, stack);
		double d3 = level.getRandom().nextDouble() * 0.1D + 0.2D;
		itementity.setDeltaMovement(
				level.getRandom().triangle((double) direction.getStepX() * d3, 0.0172275D * (double) speed),
				level.getRandom().triangle(0.2D, 0.0172275D * (double) speed),
				level.getRandom().triangle((double) direction.getStepZ() * d3, 0.0172275D * (double) speed));
		level.addFreshEntity(itementity);
	}

	public LootCase(Properties p) {
		super(p.stacksTo(1));
	}

	@Override
	public boolean isFoil(@NotNull ItemStack stack) {
		return true;
	}

	@Override
	public @NotNull InteractionResult use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
		ItemStack lootCase = player.getItemInHand(hand);
		boolean consumeCase = !player.getAbilities().instabuild;

		Modifier.TrackEntityParticle(level, player, ParticleTypes.CLOUD);

		if (!level.isClientSide() && player instanceof ServerPlayer sPlayer) {
			ItemStack tool = LootUtils.generateTool(sPlayer, level);
			// The gear is fully decided here; the roll only delays the reveal while the
			// texture wheel spins. Dispensed gear (initDispenser above) skips the roll:
			// item entities never inventory-tick, so they could not settle on the ground.
			LootUtils.startRoll(tool, level.getGameTime() + LootUtils.ROLL_TICKS);
			if (consumeCase) {
				// The case stacks to 1, so the new tool takes its slot directly
				// instead of landing in the first free inventory slot.
				player.setItemInHand(hand, tool);
			} else if (!player.getInventory().add(tool)) {
				// Creative keeps the case in hand, so the tool goes to the inventory.
				player.drop(tool, false);
			}
		} else if (consumeCase) {
			lootCase.shrink(1); // client prediction; the server replaces the stack above
		}

		// Awards the single ITEM_USED stat for the case. genTool() reads this exact
		// stat to size the loot "goodness" curve, so it must only be counted once.
		player.awardStat(Stats.ITEM_USED.get(this));

		return InteractionResult.SUCCESS;
	}

//	@Override
//	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tipList, TooltipFlag flag) {
//
//		MutableComponent comp = Component.empty();
//		comp.append("Right-click for loot!");
//		comp = comp.withStyle(ChatFormatting.GRAY);
//
//		tipList.add(comp);
//
//	}

}
