package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Base for modifiers that apply a vanilla {@link MobEffect} and level up by raising the
 * effect's amplifier ("power"). Holds the power/duration/color fields, NBT shape,
 * roman-numeral naming and leveling rules that {@code holders.Effect} and
 * {@code hurter.HurtEffect} used to copy from each other.
 *
 * <p>Subclasses provide {@link #description()}, {@code forTool(...)},
 * {@code fromNBT(...)} and the behavior hook that actually applies
 * {@link #makeInstance()}.
 */
public abstract class EffectModifier extends AbstractModifier {

	protected static final String POWER = "power";
	/** Effects level from power 0 (shown as I) to 4 (shown as V). */
	protected static final int MAX_POWER = 4;

	protected final String tagname;
	protected final Holder<MobEffect> effect;
	protected final int duration;
	protected final ChatFormatting format;
	protected int power;

	protected EffectModifier(String name, String tagname, int power, int duration, Holder<MobEffect> effect,
			ChatFormatting format) {
		this.name = name;
		this.tagname = tagname;
		this.power = power;
		this.duration = duration;
		this.effect = effect;
		this.format = format;
	}

	/** The effect instance this modifier applies: duration is in seconds, no particles. */
	protected MobEffectInstance makeInstance() {
		return new MobEffectInstance(effect, duration * 20, power, false, false);
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);
		tag.putInt(POWER, power);

		return tag;
	}

	@Override
	public String color() {
		return format.getName();
	}

	@Override
	public String name() {
		if (this.power == 0) {
			return name;
		}
		return name + " " + LootUtils.roman(this.power + 1);
	}

	@Override
	public String tagName() {
		return tagname;
	}

	@Override
	public boolean canLevel() {
		return this.power < MAX_POWER;
	}

	@Override
	public void levelUp() {
		this.power++;
	}
}
