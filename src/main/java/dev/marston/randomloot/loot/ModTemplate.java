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
		ItemStack template = player.getItemInHand(hand);

		Modifier.TrackEntityParticle(level, player, ParticleTypes.CLOUD);

		if (!level.isClientSide()) {

			ItemStack s;

			if (this.add) {
				s = new ItemStack(ModItems.MOD_SUB.get());
			} else {
				s = new ItemStack(ModItems.MOD_ADD.get());
			}

			player.setItemInHand(hand, s);
		}

		return InteractionResult.SUCCESS;
	}

//	@Override
//	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tipList, TooltipFlag flag) {
//
//		MutableComponent comp = Component.empty();
//		comp.append("Right-click to change function");
//		comp = comp.withStyle(ChatFormatting.GRAY);
//
//		tipList.add(comp);
//
//	}

}
