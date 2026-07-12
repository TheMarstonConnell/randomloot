package dev.marston.randomloot.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fired when a Random Tool levels up while held by a player. Carries the
 * tool's new level.
 */
public class ToolLeveledTrigger extends SimpleCriterionTrigger<ToolLeveledTrigger.TriggerInstance> {

	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player, int level) {
		this.trigger(player, t -> t.level().matches(level));
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player,
			MinMaxBounds.Ints level) implements SimpleCriterionTrigger.SimpleInstance {

		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				MinMaxBounds.Ints.CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::level))
				.apply(i, TriggerInstance::new));
	}
}
