package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public interface UseModifier extends Modifier {
	/**
	 * The gear was right-clicked on a block ({@link TraitDispatch#useOn}). Runs on the
	 * client as well as the server, so a placing trait can predict its block.
	 */
	default InteractionResult use(UseOnContext ctx) {
		return InteractionResult.PASS;
	}

	/**
	 * The gear was right-clicked at nothing in particular ({@link TraitDispatch#use}), on
	 * the server only, and only for a trait that {@link #useAnywhere}. True when it acted.
	 */
	default boolean use(Level level, Player player, InteractionHand hand) {
		return false;
	}

	default boolean useAnywhere() {
		return false;
	}
}
