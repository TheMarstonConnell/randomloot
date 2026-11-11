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
 * Fired when a Random Tool levels up while held by a player. Carries the
 * tool's new level.
 */
public class ToolLeveledTrigger extends SimpleCriterionTrigger<ToolLeveledTrigger.TriggerInstance> {

	public static final ResourceLocation ID = new ResourceLocation(RandomLoot.MODID, "tool_leveled");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player,
			DeserializationContext context) {
		return new TriggerInstance(player, MinMaxBounds.Ints.fromJson(json.get("level")));
	}

	public void trigger(ServerPlayer player, int level) {
		this.trigger(player, t -> t.level().matches(level));
	}

	/** 1.20.1 criterion instances are JSON-serialized classes; 1.21.1+ use codec records. */
	public static class TriggerInstance extends AbstractCriterionTriggerInstance {
		private final MinMaxBounds.Ints level;

		public TriggerInstance(ContextAwarePredicate player, MinMaxBounds.Ints level) {
			super(ID, player);
			this.level = level;
		}

		public MinMaxBounds.Ints level() {
			return this.level;
		}

		@Override
		public JsonObject serializeToJson(SerializationContext context) {
			JsonObject json = super.serializeToJson(context);
			json.add("level", this.level.serializeToJson());
			return json;
		}
	}
}
