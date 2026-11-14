package dev.marston.randomloot.loot.modifiers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import dev.marston.randomloot.loot.ToolType;
import net.minecraft.ChatFormatting;

/**
 * Trait declaration: everything about a trait that is not its rule, said once.
 *
 * <p>Its identity (the tag gear stores it under, its display name, its colour), the gear
 * it fits, its wiki groups, how it levels and what it cannot share gear with. One line
 * in {@link ModifierRegistry} per trait:
 *
 * <pre>
 * trait("fragile", "Fragile", WHITE, HAND_HELD, BREAKERS, HURTERS, STATS).levels(1, 3).make(Fragile::new);
 * </pre>
 *
 * <p>The class a declaration {@link #make makes} holds the trait's rule and the fields
 * it stores ({@link Stored}), and nothing about who it is. The registry builds a fresh
 * instance from the declaration for every grant and every load, so no two pieces of
 * gear ever share a trait object.
 */
public final class TraitDeclaration {

	/** The wiki's sections. A trait is listed under each group it declares. */
	public enum Group {
		BREAKERS, USERS, HURTERS, HOLDERS, STATS, WEARERS, MISC
	}

	/** Swords and axes. */
	public static final Set<ToolType> WEAPONS = only(ToolType.SWORD, ToolType.AXE);
	/** Pickaxes, axes and shovels. */
	public static final Set<ToolType> MINING_TOOLS = only(ToolType.PICKAXE, ToolType.AXE, ToolType.SHOVEL);
	/** The four armor pieces. */
	public static final Set<ToolType> ARMOR = only(ToolType.HELMET, ToolType.CHESTPLATE, ToolType.LEGGINGS,
			ToolType.BOOTS);
	/** Mining tools and armor: what is held while working or worn. */
	public static final Set<ToolType> MINING_TOOLS_AND_ARMOR = union(MINING_TOOLS, ARMOR);
	/** Everything that is not armor. */
	public static final Set<ToolType> HAND_HELD = Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.copyOf(ARMOR)));
	/** Any gear at all. */
	public static final Set<ToolType> ANY_GEAR = Collections.unmodifiableSet(EnumSet.allOf(ToolType.class));

	/** Exactly these gear types. */
	public static Set<ToolType> only(ToolType first, ToolType... rest) {
		return Collections.unmodifiableSet(EnumSet.of(first, rest));
	}

	/** Everything in either set. */
	public static Set<ToolType> union(Set<ToolType> one, Set<ToolType> other) {
		EnumSet<ToolType> both = EnumSet.copyOf(one);
		both.addAll(other);
		return Collections.unmodifiableSet(both);
	}

	final String tag;
	final String name;
	final ChatFormatting color;
	final Set<ToolType> fits;
	final Set<Group> groups;
	final Set<String> excludes = new HashSet<>();

	/** Null for a trait that does not level. */
	String levelKey;
	int minLevel;
	int maxLevel;
	boolean capsOnLoad;
	String levelMirrorKey;

	private Supplier<? extends AbstractModifier> factory;

	TraitDeclaration(String tag, String name, ChatFormatting color, Set<ToolType> fits, Group... groups) {
		this.tag = tag;
		this.name = name;
		this.color = color;
		this.fits = fits;
		this.groups = groups.length == 0 ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(List.of(groups)));
	}

	/**
	 * The trait levels from {@code min}, where a fresh grant starts, up to {@code max}.
	 * Stored under {@link ModifierConstants#LEVEL}, and still read from the older
	 * {@link ModifierConstants#LEGACY_LEVEL} key on gear that has it.
	 */
	public TraitDeclaration levels(int min, int max) {
		return levels(ModifierConstants.LEVEL, min, max);
	}

	/** The same, for the traits whose level has always been stored under a key of their own. */
	public TraitDeclaration levels(String key, int min, int max) {
		this.levelKey = key;
		this.minLevel = min;
		this.maxLevel = max;
		return this;
	}

	/** A level above the cap on old gear reads back as the cap, for a trait whose cap came down. */
	public TraitDeclaration capsOnLoad() {
		this.capsOnLoad = true;
		return this;
	}

	/** Also writes the level under an older key, and reads it from there when gear has nothing newer. */
	public TraitDeclaration mirrorsLevelAs(String legacyKey) {
		this.levelMirrorKey = legacyKey;
		return this;
	}

	/**
	 * Traits this one cannot share gear with, by tag. Say it on either trait of the pair:
	 * the registry applies it both ways.
	 */
	public TraitDeclaration excludes(String... tags) {
		this.excludes.addAll(List.of(tags));
		return this;
	}

	/**
	 * Finishes the declaration with the class that holds the trait's rule, registers it
	 * and returns the registry's own instance.
	 */
	public Modifier make(Supplier<? extends AbstractModifier> factory) {
		this.factory = factory;
		return ModifierRegistry.register(this);
	}

	/** A new instance of the trait, as a fresh grant starts out. */
	AbstractModifier create() {
		AbstractModifier trait = this.factory.get();
		trait.bind(this);
		return trait;
	}
}
