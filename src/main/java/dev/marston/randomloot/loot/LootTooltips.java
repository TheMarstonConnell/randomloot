package dev.marston.randomloot.loot;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Shared tooltip body for {@link LootItem} and {@link LootArmorItem}. The two items
 * render identical tooltips except for the shift-expanded stats block (speed/damage vs
 * armor/toughness), which the caller supplies via {@code statsBlock}.
 */
final class LootTooltips {

	private LootTooltips() {
	}

	static MutableComponent makeComp(String text, ChatFormatting color) {
		MutableComponent comp = Component.empty();
		comp.append(text);
		return comp.withStyle(color);
	}

	private static void newLine(Consumer<Component> tipList) {
		tipList.accept(makeComp("", ChatFormatting.GRAY));
	}

	/**
	 * Whether the given GLFW key pair is held. Client-only code reached via fully
	 * qualified names so this common class never imports client-only types; callers
	 * must not invoke this on a dedicated server.
	 */
	private static boolean isKeyDown(int leftKey, int rightKey) {
		com.mojang.blaze3d.platform.Window window = net.minecraft.client.Minecraft.getInstance().getWindow();
		return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, leftKey)
				|| com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, rightKey);
	}

	/**
	 * Appends the shared tooltip: type, lore, level/XP, sorted trait list with optional
	 * details/descriptions, the item's stats block, and the shift/ctrl hints.
	 */
	static void appendHoverText(ItemStack item, @Nullable Level level, Consumer<Component> tipList,
			BiConsumer<ToolType, Consumer<Component>> statsBlock) {

		// Tooltips are only ever built with key state on the client; the level check
		// keeps Minecraft.getInstance() unreachable on a dedicated server.
		boolean onClient = level != null && level.isClientSide();
		boolean show = onClient && isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT,
				com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT);
		boolean showDescription = onClient && isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL,
				com.mojang.blaze3d.platform.InputConstants.KEY_RCONTROL);

		ToolType tt = LootUtils.getToolType(item);

		if (show) {
			tipList.accept(Component.empty().append(tt.displayName()).withStyle(ChatFormatting.BLUE));
		}

		tipList.accept(makeComp(LootUtils.getItemLore(item), ChatFormatting.GRAY));

		if (show) {
			newLine(tipList);
			int itemLevel = LootUtils.getLevel(item);
			tipList.accept(Component.translatableWithFallback("tooltip.randomloot.level", "Level: %s", itemLevel)
					.withStyle(ChatFormatting.GRAY));
			tipList.accept(Component.translatableWithFallback("tooltip.randomloot.xp", "XP: %s / %s",
					LootUtils.getXP(item), LootUtils.getMaxXP(itemLevel)).withStyle(ChatFormatting.GRAY));
		}

		newLine(tipList);

		List<Modifier> mods = LootUtils.getModifiers(item);
		mods.sort(Comparator.comparing(Modifier::tagName));

		for (Modifier modifier : mods) {
			// Wrapper to bridge List<Component> interface to Consumer<Component>
			List<Component> tempList = new ArrayList<>();
			modifier.writeToLore(tempList, show);
			tempList.forEach(tipList);
			if (show) {
				Component details = modifier.writeDetailsToLore(level);

				if (details != null) {
					MutableComponent detailComp = makeComp(" - ", ChatFormatting.GRAY);
					detailComp.append(details);
					tipList.accept(detailComp);
				}
			}
			if (showDescription) {
				MutableComponent detailComp = makeComp("", ChatFormatting.GRAY);
				detailComp.append(modifier.displayDescription());
				tipList.accept(detailComp);
			}
		}

		if (show) {
			newLine(tipList);
			statsBlock.accept(tt, tipList);
		}

		if (!show && !showDescription) {
			newLine(tipList);
			tipList.accept(Component.translatableWithFallback("tooltip.randomloot.shift_hint", "[Shift for more]")
					.withStyle(ChatFormatting.GRAY));
			tipList.accept(Component.translatableWithFallback("tooltip.randomloot.ctrl_hint", "[Ctrl for trait info]")
					.withStyle(ChatFormatting.GRAY));
		}
	}
}
