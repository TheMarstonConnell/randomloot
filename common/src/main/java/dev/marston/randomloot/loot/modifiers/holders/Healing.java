package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class Healing extends AbstractModifier implements HoldModifier {
	/** Shared by every Living item, so wearing several doesn't multiply the whispers. */
	private static final WhisperLimiter WHISPERS = new WhisperLimiter();

	/** What a Living item mutters while it mends itself; one is picked at random per whisper. */
	private static final List<String> WHISPER_LINES = List.of(
			"pssst...",
			"hi...",
			"hissss...",
			"shhhh...",
			"hey...",
			"over here...",
			"...hello?",
			"mmm...");

	private float getPower() {
		// Level 1: 0.5%, Level 2: 1%, Level 3: 2%
		return 0.005f * (float) Math.pow(2, traitLevel() - 1);
	}

	@Override
	public String description() {
		return "While held or worn, " + String.format("%.1f", getPower() * 100) + "% chance per tick to repair itself";
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		float power = getPower();
		float f = level.getRandom().nextFloat();
		if (f < power) {

			if (stack.getDamageValue() == 0) {
				return;
			}

			stack.setDamageValue(Math.max(stack.getDamageValue() - 1, 0));

			if (f < power / 5 && holder instanceof Player p
					&& WHISPERS.tryWhisper(p.getUUID(), Util.getMillis(), Config.LIVING_WHISPERS_PER_MINUTE.get())) {
				MutableComponent comp = Component.empty();

				comp.append(Util.getRandom(WHISPER_LINES, level.getRandom()));
				comp = comp.withStyle(ChatFormatting.GRAY);
				comp = comp.withStyle(ChatFormatting.ITALIC);

				p.sendSystemMessage(comp);
			}

		}

	}
}
