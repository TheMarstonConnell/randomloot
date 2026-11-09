package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public interface UseModifier extends Modifier {
	public InteractionResult use(UseOnContext ctx);

	public boolean use(Level level, Player player, InteractionHand hand);

	public boolean useAnywhere();

	/**
	 * Whether a right-click-anywhere trait on the main-hand tool should stand down and
	 * let the off-hand item have the click. The client always predicts PASS for these
	 * traits, so it goes on to use the off-hand item as well; without this a shield
	 * raise also threw a fireball, and placing an off-hand torch also teleported.
	 *
	 * <p>The trait yields when the off-hand holds something with a use action (shield,
	 * food, bow, spyglass) or a block the player is aiming to place.
	 */
	static boolean yieldsToOffhand(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return false;
		}

		ItemStack offhand = player.getOffhandItem();
		if (offhand.isEmpty()) {
			return false;
		}

		if (offhand.getUseAnimation() != ItemUseAnimation.NONE) {
			return true;
		}

		return offhand.getItem() instanceof BlockItem
				&& player.pick(player.blockInteractionRange(), 1.0f, false).getType() == HitResult.Type.BLOCK;
	}
}
