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
 * Fired when a player opens a Loot Case. Carries the player's lifetime case
 * count (including the case just opened) and the generated tool's type.
 */
public class CaseOpenedTrigger extends SimpleCriterionTrigger<CaseOpenedTrigger.TriggerInstance> {

	public static final ResourceLocation ID = new ResourceLocation(RandomLoot.MODID, "case_opened");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player,
			DeserializationContext context) {
		return new TriggerInstance(player, MinMaxBounds.Ints.fromJson(json.get("count")), json.has("tool_type") ? Optional.of(GsonHelper.getAsString(json, "tool_type")) : Optional.empty());
	}

	public void trigger(ServerPlayer player, int count, String toolType) {
		this.trigger(player, t -> t.matches(count, toolType));
	}

	/** 1.20.1 criterion instances are JSON-serialized classes; 1.21.1+ use codec records. */
	public static class TriggerInstance extends AbstractCriterionTriggerInstance {
		private final MinMaxBounds.Ints count;
		private final Optional<String> toolType;

		public TriggerInstance(ContextAwarePredicate player, MinMaxBounds.Ints count, Optional<String> toolType) {
			super(ID, player);
			this.count = count;
			this.toolType = toolType;
		}

		public MinMaxBounds.Ints count() {
			return this.count;
		}

		public Optional<String> toolType() {
			return this.toolType;
		}
		public boolean matches(int count, String toolType) {
			return this.count.matches(count)
					&& (this.toolType.isEmpty() || this.toolType.get().equalsIgnoreCase(toolType));
		}

		@Override
		public JsonObject serializeToJson(SerializationContext context) {
			JsonObject json = super.serializeToJson(context);
			json.add("count", this.count.serializeToJson());
			this.toolType.ifPresent(v -> json.addProperty("tool_type", v));
			return json;
		}
	}
}
