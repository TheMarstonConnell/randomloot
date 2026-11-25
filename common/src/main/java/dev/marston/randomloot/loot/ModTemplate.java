package dev.marston.randomloot.loot;

import dev.marston.randomloot.items.ModItems;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ModTemplate extends Item {

	final boolean add;

	public ModTemplate(Properties p, boolean additional) {
		super(p.stacksTo(1));
		this.add = additional;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		Modifier.TrackEntityParticle(level, player, ParticleTypes.CLOUD);

		// Swaps the template for the other one.
		if (!level.isClientSide()) {
			player.setItemInHand(hand, new ItemStack(this.add ? ModItems.MOD_SUB.get() : ModItems.MOD_ADD.get()));
		}

		return InteractionResult.SUCCESS;
	}
}
