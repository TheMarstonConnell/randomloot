package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.NameGenerator;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public class DirtPlace extends PlaceOnUseModifier {

	public DirtPlace() {
		super(1);
	}

	/**
	 * Gear that stores its Grace without a name gets a forger rolled on the spot, as it
	 * always has. Every grant writes a name, so only a stored tag that lost its own gets
	 * here. The registry's own Grace rolls nothing: it is the declared, plain "Grace".
	 */
	@Override
	protected String nameWhenNotStored() {
		return NameGenerator.generateForger(RandomSource.create(), 0.5f) + "'s Grace";
	}

	@Override
	public Modifier forWorld(long worldSeed) {
		DirtPlace forged = (DirtPlace) fromNBT(toNBT());
		// 0.5f lands in the temperate band, matching the random roll this replaces.
		forged.name = NameGenerator.forgerForWorld(worldSeed, 0.5f) + "'s Grace";
		return forged;
	}

	@Override
	public boolean hasDynamicName() {
		// On gear it is named after a forger ("<Forger>'s Grace"), the world's when the
		// grant knew its world, so there is no one name to translate.
		return true;
	}

	private BlockState getPlacementState(BlockPlaceContext ctx) {
		BlockState blockstate = Blocks.DIRT.getStateForPlacement(ctx);
		return blockstate.canSurvive(ctx.getLevel(), ctx.getClickedPos())
				&& ctx.getLevel().isUnobstructed(blockstate, ctx.getClickedPos(), CollisionContext.of(ctx.getPlayer()))
						? blockstate
						: null;
	}

	@Override
	protected InteractionResult place(UseOnContext ctx) {
		return placeBlock(ctx, this::getPlacementState);
	}

	@Override
	public String description() {
		return "Right clicking on a block while crouching with the tool in hand will place a dirt block and use "
				+ this.damage.get() + " durability points.";
	}
}
