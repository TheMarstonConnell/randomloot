package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

public interface Modifier {

	public static MutableComponent makeComp(String text, ChatFormatting color) {
		MutableComponent comp = Component.empty();
		comp.append(text);
		comp = comp.withStyle(color);

		return comp;
	}

	public static MutableComponent makeComp(String text, String color) {
		MutableComponent comp = Component.empty();
		comp.append(text);
		comp = comp.withStyle(ChatFormatting.getByName(color));

		return comp;
	}

	public static MutableComponent makeComp(Component compIn) {
		MutableComponent comp = Component.empty();
		comp.append(compIn);
		return comp;
	}

	public static void TrackEntityParticle(Level level, Entity e, ParticleOptions particleType) {
		if (!level.isClientSide()) {
			Random r = new Random();

			ServerLevel sl = ((ServerLevel) level);

			for (int i = 0; i < 32; ++i) {
				double d0 = (double) (r.nextFloat() * 2.0F - 1.0F);
				double d1 = (double) (r.nextFloat() * 2.0F - 1.0F);
				double d2 = (double) (r.nextFloat() * 2.0F - 1.0F);
				if (!(d0 * d0 + d1 * d1 + d2 * d2 > 1.0D)) {
					double d3 = e.getX(d0 / 4.0D);
					double d4 = e.getY(0.5D + d1 / 4.0D);
					double d5 = e.getZ(d2 / 4.0D);
					sl.sendParticles(particleType, d3, d4, d5, 1, d0, d1 + 0.2D, d2, 0.0D);
				}
			}

		}
	}

	public static final String MODTAG = "modifiers";

	static final String NAME = "name";

	public String tagName();

	public void writeToLore(List<Component> list, boolean shift);

	public String description();

	public String name();

	/**
	 * True for modifiers whose name is generated per instance (e.g. DirtPlace's random
	 * "<Forger>'s Grace"). These get no lang entry and always display their stored name.
	 */
	default boolean hasDynamicName() {
		return false;
	}

	/**
	 * Translatable display name, keyed {@code modifier.randomloot.<tag>.name} with the
	 * raw English {@link #name()} as fallback. Use this for anything player-facing.
	 */
	default Component displayName() {
		return Component.translatableWithFallback("modifier.randomloot." + tagName() + ".name", name());
	}

	/**
	 * Translatable description, keyed {@code modifier.randomloot.<tag>.description} with
	 * {@link #description()} as fallback. Leveling traits ship no lang entry on purpose:
	 * their descriptions bake in live power/duration values, so the dynamic English
	 * fallback is more accurate than a frozen translation.
	 */
	default Component displayDescription() {
		return Component.translatableWithFallback("modifier.randomloot." + tagName() + ".description", description());
	}

	public String color();

	public CompoundTag toNBT();

	public Modifier fromNBT(CompoundTag tag);

	public boolean forTool(ToolType type);

	default Component writeDetailsToLore(Level level) {
		return null;
	}

	default boolean compatible(Modifier mod) {
		return true;
	}

	/**
	 * Returns the variant of this modifier for the given world, or {@code this} when the
	 * modifier has no world-dependent state. Used by traits with world-constant flavor
	 * (e.g. DirtPlace's "&lt;Forger&gt;'s Grace" name) so the same world always produces
	 * the same variant.
	 */
	default Modifier forWorld(long worldSeed) {
		return this;
	}

	default boolean canLevel() {
		return false;
	}

	default void levelUp() {
	}
}
