package dev.marston.randomloot.loot.modifiers;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
	 * <p>The trait yields when the off-hand item would act on the click: anything usable
	 * in the air (shield, food, bow, ender pearl, snowball, bucket, ...), or anything
	 * usable on a block (a block to place, flint and steel, bone meal, ...) while the
	 * player is aiming at one. {@link OffhandUse} decides what counts.
	 */
	static boolean yieldsToOffhand(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return false;
		}

		ItemStack offhand = player.getOffhandItem();
		if (offhand.isEmpty()) {
			return false;
		}

		if (OffhandUse.actsInAir(offhand, player)) {
			return true;
		}

		return OffhandUse.actsOnBlock(offhand)
				&& player.pick(player.blockInteractionRange(), 1.0f, false).getType() == HitResult.Type.BLOCK;
	}
}
