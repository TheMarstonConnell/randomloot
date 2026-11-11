package dev.marston.randomloot.advancements;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Registration and fire-helpers for Random Loot's advancement criteria. The
 * helpers are null/side tolerant so call sites can fire-and-forget.
 *
 * <p>1.20.1 has no trigger registry, so these go through
 * {@code RegHelper.registerCriterion} and are plain fields rather than suppliers.
 */
public final class ModCriteria {

	public static final CaseOpenedTrigger CASE_OPENED = Services.REG.registerCriterion(new CaseOpenedTrigger());
	public static final ToolLeveledTrigger TOOL_LEVELED = Services.REG.registerCriterion(new ToolLeveledTrigger());
	public static final TraitObtainedTrigger TRAIT_OBTAINED = Services.REG.registerCriterion(new TraitObtainedTrigger());
	public static final TraitUsedTrigger TRAIT_USED = Services.REG.registerCriterion(new TraitUsedTrigger());

	private ModCriteria() {
	}

	/** Classloads the class so the trigger registrations above run. */
	public static void init() {
	}

	public static void caseOpened(ServerPlayer player, int count, ToolType type) {
		CASE_OPENED.trigger(player, count, type.name().toLowerCase(Locale.ROOT));
	}

	public static void toolLeveled(LivingEntity holder, int level) {
		if (holder instanceof ServerPlayer player) {
			TOOL_LEVELED.trigger(player, level);
		}
	}

	public static void traitsObtained(ServerPlayer player, ItemStack tool, String source) {
		List<String> traits = LootUtils.getModifiers(tool).stream().map(Modifier::tagName).toList();
		TRAIT_OBTAINED.trigger(player, traits, source);
	}

	public static void traitUsed(LivingEntity user, Modifier mod) {
		if (user instanceof ServerPlayer player) {
			TRAIT_USED.trigger(player, mod.tagName());
		}
	}
}
