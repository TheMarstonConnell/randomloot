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
 * Fired when a player ends up with a Random Tool whose trait set changed:
 * either freshly generated from a Loot Case ({@link #SOURCE_GENERATED}) or
 * taken from a smithing table after a trait recipe ({@link #SOURCE_CRAFTED}).
 * Carries the tool's full list of trait tag names.
 */
public class TraitObtainedTrigger extends SimpleCriterionTrigger<TraitObtainedTrigger.TriggerInstance> {

	public static final ResourceLocation ID = new ResourceLocation(RandomLoot.MODID, "trait_obtained");

	public static final String SOURCE_GENERATED = "generated";
	public static final String SOURCE_CRAFTED = "crafted";

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player,
			DeserializationContext context) {
		return new TriggerInstance(player, json.has("trait") ? Optional.of(GsonHelper.getAsString(json, "trait")) : Optional.empty(), MinMaxBounds.Ints.fromJson(json.get("count")), json.has("source") ? Optional.of(GsonHelper.getAsString(json, "source")) : Optional.empty());
	}

	public void trigger(ServerPlayer player, List<String> traits, String source) {
		this.trigger(player, t -> t.matches(traits, source));
	}

	/** 1.20.1 criterion instances are JSON-serialized classes; 1.21.1+ use codec records. */
	public static class TriggerInstance extends AbstractCriterionTriggerInstance {
		private final Optional<String> trait;
		private final MinMaxBounds.Ints count;
		private final Optional<String> source;

		public TriggerInstance(ContextAwarePredicate player, Optional<String> trait, MinMaxBounds.Ints count, Optional<String> source) {
			super(ID, player);
			this.trait = trait;
			this.count = count;
			this.source = source;
		}

		public Optional<String> trait() {
			return this.trait;
		}

		public MinMaxBounds.Ints count() {
			return this.count;
		}

		public Optional<String> source() {
			return this.source;
		}
		public boolean matches(List<String> traits, String source) {
			if (this.trait.isPresent() && !traits.contains(this.trait.get())) {
				return false;
			}
			if (!this.count.matches(traits.size())) {
				return false;
			}
			return this.source.isEmpty() || this.source.get().equals(source);
		}

		@Override
		public JsonObject serializeToJson(SerializationContext context) {
			JsonObject json = super.serializeToJson(context);
			this.trait.ifPresent(v -> json.addProperty("trait", v));
			json.add("count", this.count.serializeToJson());
			this.source.ifPresent(v -> json.addProperty("source", v));
			return json;
		}
	}
}
