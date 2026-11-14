package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.ToolType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for traits. A subclass holds a trait's rule and the fields it stores; who
 * the trait is comes from its {@link TraitDeclaration}, which {@link ModifierRegistry}
 * hands to every instance it makes.
 *
 * <p>So a subclass writes none of {@code tagName}, {@code color}, {@code forTool},
 * {@code toNBT}, {@code fromNBT}, {@code canLevel} or {@code levelUp}, and has no
 * constructor to speak of. It reads its level with {@link #traitLevel()} and declares any
 * other field it keeps on gear with {@link #stored}. Instances come from the registry,
 * never from {@code new}: one built by hand has no declaration and does not know its
 * own tag.
 */
public abstract class AbstractModifier implements Modifier {

	/** Set by the registry on every instance it makes. */
	TraitDeclaration declaration;

	/** What gear shows the trait as. Stored, so gear keeps a name the declaration has since changed. */
	protected String name;

	private int level;
	private List<Stored<?>> stored = List.of();

	/** Called once by the registry, straight after construction. */
	void bind(TraitDeclaration declaration) {
		this.declaration = declaration;
		this.level = declaration.minLevel;
		if (this.name == null) {
			this.name = defaultName();
		}
	}

	/** The name a trait has when the gear stores none. The declared one, unless a trait names itself. */
	protected String defaultName() {
		return this.declaration.name;
	}

	/** The trait's level, between the declared minimum and maximum. The minimum for a trait that does not level. */
	protected final int traitLevel() {
		return this.level;
	}

	/** Declares an int this trait keeps on gear under {@code key}, starting at {@code initial}. */
	protected final Stored<Integer> stored(String key, int initial) {
		return stored(key, initial, CompoundTag::getIntOr, CompoundTag::putInt);
	}

	/** Declares a long this trait keeps on gear under {@code key}, starting at {@code initial}. */
	protected final Stored<Long> stored(String key, long initial) {
		return stored(key, initial, CompoundTag::getLongOr, CompoundTag::putLong);
	}

	/** Declares a float this trait keeps on gear under {@code key}, starting at {@code initial}. */
	protected final Stored<Float> stored(String key, float initial) {
		return stored(key, initial, CompoundTag::getFloatOr, CompoundTag::putFloat);
	}

	/** Declares a field of any other shape, with how to read and write it. */
	protected final <T> Stored<T> stored(String key, T initial, Stored.Reader<T> reader, Stored.Writer<T> writer) {
		Stored<T> field = new Stored<>(key, initial, reader, writer);
		if (this.stored.isEmpty()) {
			this.stored = new ArrayList<>(2);
		}
		this.stored.add(field);
		return field;
	}

	@Override
	public String tagName() {
		return this.declaration.tag;
	}

	@Override
	public ChatFormatting color() {
		return this.declaration.color;
	}

	@Override
	public boolean forTool(ToolType type) {
		return this.declaration.fits.contains(type);
	}

	/**
	 * The stored name, with a roman numeral from the second level up. The house style: a
	 * freshly granted trait shows no numeral, and the first upgrade shows "II".
	 */
	@Override
	public String name() {
		if (this.declaration == null || this.declaration.levelKey == null || this.level == this.declaration.minLevel) {
			return this.name;
		}
		return this.name + " " + LootUtils.roman(this.level - this.declaration.minLevel + 1);
	}

	@Override
	public boolean canLevel() {
		return this.declaration != null && this.declaration.levelKey != null && this.level < this.declaration.maxLevel;
	}

	@Override
	public void levelUp() {
		this.level++;
	}

	/** Everything the trait keeps on gear: its name, its level if it has one, and its declared fields. */
	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, this.name);

		if (this.declaration != null && this.declaration.levelKey != null) {
			tag.putInt(this.declaration.levelKey, this.level);
			if (this.declaration.levelMirrorKey != null) {
				tag.putInt(this.declaration.levelMirrorKey, this.level);
			}
		}

		for (Stored<?> field : this.stored) {
			field.save(tag);
		}
		return tag;
	}

	/** Reads back what {@link #toNBT} wrote. Anything the gear does not have keeps its starting value. */
	void load(CompoundTag tag) {
		String storedName = tag.getStringOr(NAME, null);
		this.name = storedName != null ? storedName : defaultName();

		TraitDeclaration declared = this.declaration;
		if (declared.levelKey != null) {
			int fallback = declared.minLevel;
			if (declared.levelMirrorKey != null) {
				fallback = tag.getIntOr(declared.levelMirrorKey, fallback);
			}
			// Gear from before the level key was unified keeps its level.
			this.level = declared.levelKey.equals(ModifierConstants.LEVEL) ? ModifierConstants.getLevel(tag, fallback)
					: tag.getIntOr(declared.levelKey, fallback);
			if (declared.capsOnLoad) {
				this.level = Math.min(this.level, declared.maxLevel);
			}
		}

		for (Stored<?> field : this.stored) {
			field.load(tag);
		}
	}

	/** A new instance of this trait holding what {@code tag} stores. */
	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return ModifierRegistry.loadModifier(tagName(), tag);
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
