package dev.marston.randomloot.advancements;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Registration and fire-helpers for Random Loot's advancement criteria. The
 * helpers are null/side tolerant so call sites can fire-and-forget.
 */
public final class ModCriteria {

	public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister
			.create(Registries.TRIGGER_TYPE, RandomLoot.MODID);

	public static final Supplier<CaseOpenedTrigger> CASE_OPENED = TRIGGERS.register("case_opened",
			CaseOpenedTrigger::new);
	public static final Supplier<ToolLeveledTrigger> TOOL_LEVELED = TRIGGERS.register("tool_leveled",
			ToolLeveledTrigger::new);
	public static final Supplier<TraitObtainedTrigger> TRAIT_OBTAINED = TRIGGERS.register("trait_obtained",
			TraitObtainedTrigger::new);
	public static final Supplier<TraitUsedTrigger> TRAIT_USED = TRIGGERS.register("trait_used",
			TraitUsedTrigger::new);

	private ModCriteria() {
	}

	public static void register(IEventBus eventBus) {
		TRIGGERS.register(eventBus);
	}

	public static void caseOpened(ServerPlayer player, int count, ToolType type) {
		CASE_OPENED.get().trigger(player, count, type.name().toLowerCase(Locale.ROOT));
	}

	public static void toolLeveled(LivingEntity holder, int level) {
		if (holder instanceof ServerPlayer player) {
			TOOL_LEVELED.get().trigger(player, level);
		}
	}

	public static void traitsObtained(ServerPlayer player, ItemStack tool, String source) {
		List<String> traits = LootUtils.getModifiers(tool).stream().map(Modifier::tagName).toList();
		TRAIT_OBTAINED.get().trigger(player, traits, source);
	}

	public static void traitUsed(LivingEntity user, Modifier mod) {
		if (user instanceof ServerPlayer player) {
			TRAIT_USED.get().trigger(player, mod.tagName());
		}
	}
}
