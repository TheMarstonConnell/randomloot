package dev.marston.randomloot.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 * Fired when a player ends up with a Random Tool whose trait set changed:
 * either freshly generated from a Loot Case ({@link #SOURCE_GENERATED}) or
 * taken from a smithing table after a trait recipe ({@link #SOURCE_CRAFTED}).
 * Carries the tool's full list of trait tag names.
 */
public class TraitObtainedTrigger extends SimpleCriterionTrigger<TraitObtainedTrigger.TriggerInstance> {

	public static final String SOURCE_GENERATED = "generated";
	public static final String SOURCE_CRAFTED = "crafted";

	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player, List<String> traits, String source) {
		this.trigger(player, t -> t.matches(traits, source));
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<String> trait,
			MinMaxBounds.Ints count, Optional<String> source) implements SimpleCriterionTrigger.SimpleInstance {

		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				Codec.STRING.optionalFieldOf("trait").forGetter(TriggerInstance::trait),
				MinMaxBounds.Ints.CODEC.optionalFieldOf("count", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::count),
				Codec.STRING.optionalFieldOf("source").forGetter(TriggerInstance::source))
				.apply(i, TriggerInstance::new));

		public boolean matches(List<String> traits, String source) {
			if (this.trait.isPresent() && !traits.contains(this.trait.get())) {
				return false;
			}
			if (!this.count.matches(traits.size())) {
				return false;
			}
			return this.source.isEmpty() || this.source.get().equals(source);
		}
	}
}
