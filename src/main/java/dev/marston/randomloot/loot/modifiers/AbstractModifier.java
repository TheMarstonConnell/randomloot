package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Base class for modifiers. Holds the universal {@code name} field and the default
 * {@code name()} / {@code writeToLore(...)} implementations shared by almost every
 * concrete modifier, so leaf classes no longer hand-copy them.
 *
 * <p>Subclasses still provide {@link #color()}, {@link #description()}, {@link #tagName()},
 * {@link #toNBT()}/{@link #fromNBT(net.minecraft.nbt.CompoundTag)}, {@link #clone()} and
 * {@link #forTool(ToolType)}. They may override {@link #name()} (e.g. to append a roman
 * numeral when leveled) or {@link #writeToLore(List, boolean)} (for custom lore lines).
 */
public abstract class AbstractModifier implements Modifier {

	protected String name;

	// Re-declared abstract so Object's protected clone() does not satisfy Modifier's
	// public clone(); every concrete modifier provides its own.
	@Override
	public abstract Modifier clone();

	@Override
	public String name() {
		return name;
	}

	@Override
	public Component displayName() {
		String full = name();
		if (hasDynamicName() || name == null || !full.startsWith(name)) {
			// Custom full names (e.g. Unbreaking's maxed-out "Unbreakable") stay literal so
			// a base-name lang entry can't mask them.
			return Component.literal(full);
		}
		// Translate the base name but keep any level suffix: "Poisonous II" becomes
		// "<translated Poisonous> II".
		return Component.translatableWithFallback("modifier.randomloot." + tagName() + ".name", name)
				.append(full.substring(name.length()));
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		list.add(Modifier.makeComp(this.displayName()).withStyle(ChatFormatting.getByName(this.color())));
	}

	/** Melee tools: swords and axes. */
	public static boolean isWeapon(ToolType type) {
		return type == ToolType.SWORD || type == ToolType.AXE;
	}

	/** Block-breaking tools: pickaxes, axes and shovels. */
	public static boolean isMiningTool(ToolType type) {
		return type == ToolType.PICKAXE || type == ToolType.AXE || type == ToolType.SHOVEL;
	}
}
