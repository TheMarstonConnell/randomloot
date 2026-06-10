package dev.marston.randomloot.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fired when a player opens a Loot Case. Carries the player's lifetime case
 * count (including the case just opened) and the generated tool's type.
 */
public class CaseOpenedTrigger extends SimpleCriterionTrigger<CaseOpenedTrigger.TriggerInstance> {

	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player, int count, String toolType) {
		this.trigger(player, t -> t.matches(count, toolType));
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints count,
			Optional<String> toolType) implements SimpleCriterionTrigger.SimpleInstance {

		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				MinMaxBounds.Ints.CODEC.optionalFieldOf("count", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::count),
				Codec.STRING.optionalFieldOf("tool_type").forGetter(TriggerInstance::toolType))
				.apply(i, TriggerInstance::new));

		public boolean matches(int count, String toolType) {
			return this.count.matches(count)
					&& (this.toolType.isEmpty() || this.toolType.get().equalsIgnoreCase(toolType));
		}
	}
}
