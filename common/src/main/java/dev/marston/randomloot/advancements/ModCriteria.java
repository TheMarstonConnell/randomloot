package dev.marston.randomloot.advancements;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.marston.randomloot.platform.Services;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Registration and fire-helpers for Random Loot's advancement criteria. The
 * helpers are null/side tolerant so call sites can fire-and-forget.
 */
public final class ModCriteria {

	public static final Supplier<CaseOpenedTrigger> CASE_OPENED = Services.REG
			.register(Registries.TRIGGER_TYPE, "case_opened", CaseOpenedTrigger::new);
	public static final Supplier<ToolLeveledTrigger> TOOL_LEVELED = Services.REG
			.register(Registries.TRIGGER_TYPE, "tool_leveled", ToolLeveledTrigger::new);
	public static final Supplier<TraitObtainedTrigger> TRAIT_OBTAINED = Services.REG
			.register(Registries.TRIGGER_TYPE, "trait_obtained", TraitObtainedTrigger::new);
	public static final Supplier<TraitUsedTrigger> TRAIT_USED = Services.REG
			.register(Registries.TRIGGER_TYPE, "trait_used", TraitUsedTrigger::new);

	private ModCriteria() {
	}

	/** Classloads the class so the trigger registrations above run. */
	public static void init() {
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
