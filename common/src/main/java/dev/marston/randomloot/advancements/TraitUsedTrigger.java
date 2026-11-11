package dev.marston.randomloot.advancements;

import com.google.gson.JsonObject;
import dev.marston.randomloot.RandomLoot;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Optional;

/**
 * Fired when a marquee trait visibly does its thing for a player: an
 * Executioner kill, a Void-Touched teleport, a Charged lightning strike, etc.
 * Carries the trait's tag name.
 */
public class TraitUsedTrigger extends SimpleCriterionTrigger<TraitUsedTrigger.TriggerInstance> {

	public static final ResourceLocation ID = new ResourceLocation(RandomLoot.MODID, "trait_used");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player,
			DeserializationContext context) {
		return new TriggerInstance(player, json.has("trait") ? Optional.of(GsonHelper.getAsString(json, "trait")) : Optional.empty());
	}

	public void trigger(ServerPlayer player, String trait) {
		this.trigger(player, t -> t.trait().isEmpty() || t.trait().get().equals(trait));
	}

	/** 1.20.1 criterion instances are JSON-serialized classes; 1.21.1+ use codec records. */
	public static class TriggerInstance extends AbstractCriterionTriggerInstance {
		private final Optional<String> trait;

		public TriggerInstance(ContextAwarePredicate player, Optional<String> trait) {
			super(ID, player);
			this.trait = trait;
		}

		public Optional<String> trait() {
			return this.trait;
		}

		@Override
		public JsonObject serializeToJson(SerializationContext context) {
			JsonObject json = super.serializeToJson(context);
			this.trait.ifPresent(v -> json.addProperty("trait", v));
			return json;
		}
	}
}
