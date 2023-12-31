package dev.marston.randomloot.loot;

import java.util.List;

import javax.annotation.Nullable;

import dev.marston.randomloot.RandomLootMod;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class LootCase extends Item {

	public LootCase() {
		super(new Properties().stacksTo(1));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack lootCase = player.getItemInHand(hand);

		if (player instanceof ServerPlayer) {
			ServerPlayer sPlayer = (ServerPlayer) player;
			StatType<Item> itemUsed = Stats.ITEM_USED;

			sPlayer.getStats().increment(sPlayer, itemUsed.get(LootRegistry.CaseItem), 1);
		}

		Modifier.TrackEntityParticle(level, player, ParticleTypes.CLOUD);

		if (!level.isClientSide) {
			RandomLootMod.LOGGER.info("Generating tool...");
			boolean success = LootUtils.generateTool(player, level); // generate tool and give it to the player
			if (success) {
				RandomLootMod.LOGGER.info("tool generation complete.");
			}
//			Thread thread = new Thread() {
//				public void run() {
//					RandomLootMod.LOGGER.info("Starting tool generation thread...");
//					try {
//						Thread.sleep(100);
//					} catch (InterruptedException e) {
//						RandomLootMod.LOGGER.error(e.getStackTrace().toString());
//					}
//					boolean success = LootUtils.generateTool(player, level); // generate tool and give it to the player
//					if (success) {
//						RandomLootMod.LOGGER.info("tool generation complete.");
//					}
//				}
//			};
//
//			thread.start();
		}

		player.awardStat(Stats.ITEM_USED.get(this));
		if (!player.getAbilities().instabuild) {
			lootCase.shrink(1);
		}

		return InteractionResultHolder.sidedSuccess(lootCase, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tipList, TooltipFlag flag) {

		MutableComponent comp = MutableComponent.create(ComponentContents.EMPTY);
		comp.append("Right-click for loot!");
		comp = comp.withStyle(ChatFormatting.GRAY);

		tipList.add(comp);

	}

}
