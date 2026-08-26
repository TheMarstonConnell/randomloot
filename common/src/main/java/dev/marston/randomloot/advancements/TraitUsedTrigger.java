package dev.marston.randomloot.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fired when a marquee trait visibly does its thing for a player: an
 * Executioner kill, a Void-Touched teleport, a Charged lightning strike, etc.
 * Carries the trait's tag name.
 */
public class TraitUsedTrigger extends SimpleCriterionTrigger<TraitUsedTrigger.TriggerInstance> {

	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player, String trait) {
		this.trigger(player, t -> t.trait().isEmpty() || t.trait().get().equals(trait));
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player,
			Optional<String> trait) implements SimpleCriterionTrigger.SimpleInstance {

		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				Codec.STRING.optionalFieldOf("trait").forGetter(TriggerInstance::trait))
				.apply(i, TriggerInstance::new));
	}
}
