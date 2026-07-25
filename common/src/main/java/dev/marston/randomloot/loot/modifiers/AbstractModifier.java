package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.ToolType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Base class for modifiers. Holds the universal {@code name} field and the default
 * {@code name()} / {@code toNBT()} / {@code writeToLore(...)} implementations shared by
 * almost every concrete modifier, so leaf classes no longer hand-copy them.
 *
 * <p>Subclasses still provide {@link #color()}, {@link #description()}, {@link #tagName()},
 * {@link #fromNBT(net.minecraft.nbt.CompoundTag)} and {@link #forTool(ToolType)}. Stateful
 * modifiers extend {@link #toNBT()} via {@code super.toNBT()}; leveled ones extend
 * {@link LeveledModifier} instead.
 */
public abstract class AbstractModifier implements Modifier {

	protected String name;

	@Override
	public String name() {
		return name;
	}

	/** Serializes the shared {@code name} field; stateful subclasses add to this tag. */
	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		return tag;
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
		list.add(Modifier.makeComp(this.displayName()).withStyle(this.color()));
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
