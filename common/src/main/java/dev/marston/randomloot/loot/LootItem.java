package dev.marston.randomloot.loot;

import dev.marston.randomloot.loot.modifiers.TraitDispatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import dev.marston.randomloot.platform.ToolAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class LootItem extends LootGearItem {

	public LootItem(Properties p) {
		super(p);
	}

	public static float getDigSpeed(ItemStack stack, ToolType type) {
		return GearStats.digSpeed(LootUtils.getStats(stack), type, TraitDispatch.statMultiplier(stack));
	}

	public static float getAttackDamage(ItemStack stack, ToolType type) {
		return GearStats.attackDamage(LootUtils.getStats(stack), type);
	}

	@Override
	public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState block) {

		// A rolling tool hasn't been revealed yet and digs like a bare hand.
		if (LootUtils.isRolling(stack)) {
			return 1.0f;
		}

		ToolType type = LootUtils.getToolType(stack);

		if (type == ToolType.SWORD) {
			// Cobwebs - instant break (vanilla sword behavior)
			if (block.is(Blocks.COBWEB)) {
				return 15.0f;
			}

			// Bamboo - instant break (vanilla sword behavior)
			if (block.is(Blocks.BAMBOO) || block.is(Blocks.BAMBOO_SAPLING)) {
				return 100.0f;
			}

			// Leaves - faster than fists (vanilla sword behavior)
			return block.is(BlockTags.LEAVES) ? 1.5f : 1.0f;
		}

		TagKey<Block> mineable = mineable(type);
		return mineable != null && block.is(mineable) ? getDigSpeed(stack, type) : 1.0F;
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		if (LootUtils.isRolling(stack)) {
			return false;
		}
		TagKey<Block> mineable = mineable(LootUtils.getToolType(stack));
		return mineable != null && state.is(mineable);
	}

	/** The blocks the type mines, or null for a type that mines nothing (sword, armor). */
	@Nullable
	private static TagKey<Block> mineable(ToolType type) {
		return switch (type) {
			case PICKAXE -> BlockTags.MINEABLE_WITH_PICKAXE;
			case AXE -> BlockTags.MINEABLE_WITH_AXE;
			case SHOVEL -> BlockTags.MINEABLE_WITH_SHOVEL;
			default -> null;
		};
	}


	/**
	 * The stack's attack attributes, derived from its stats. Stored on the
	 * stack as the vanilla ATTRIBUTE_MODIFIERS component by
	 * {@link LootUtils#refreshDerivedComponents} whenever the inputs change.
	 */
	@Override
	public ItemAttributeModifiers buildAttributeModifiers(ItemStack stack) {
		ToolType tt = LootUtils.getToolType(stack);

		float attack = getAttackDamage(stack, tt);
		float speed = GearStats.attackSpeed(tt);

		return ItemAttributeModifiers.builder().add(
				Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attack, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND).build();
	}

	/** Loader-neutral form of NeoForge's canPerformAction; loader subclasses hook it into their APIs. */
	public boolean canPerform(ItemStack itemStack, ToolAction action) {
		if (LootUtils.isRolling(itemStack)) {
			return false;
		}
		return switch (LootUtils.getToolType(itemStack)) {
			case AXE -> action == ToolAction.AXE_STRIP || action == ToolAction.AXE_SCRAPE
					|| action == ToolAction.AXE_WAX_OFF;
			case SHOVEL -> action == ToolAction.SHOVEL_FLATTEN;
			default -> false;
		};
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {

		// A rolling weapon earns no XP either; its traits are the dispatch's to skip.
		if (LootUtils.isRolling(itemstack)) {
			return;
		}

		ToolType type = LootUtils.getToolType(itemstack);

		// Before the traits: a trait this XP rolls onto the weapon joins the same hit.
		if (type == ToolType.AXE || type == ToolType.SWORD) {
			LootUtils.addXp(itemstack, hurter, 1);
		}

		TraitDispatch.hit(itemstack, hurtee, hurter);
	}

	@Override
	public boolean mineBlock(@NotNull ItemStack stack, Level level, @NotNull BlockState blockState, @NotNull BlockPos pos, @NotNull LivingEntity player) {
		if (LootUtils.isRolling(stack)) {
			return true;
		}

		if (!level.isClientSide() && blockState.getDestroySpeed(level, pos) != 0.0F) {
			TraitDispatch.blockBroken(stack, blockState, pos, player);
			LootUtils.addXp(stack, player, 1);
		}

		return true;
	}



	@Override
	public @NotNull InteractionResult useOn(UseOnContext ctx) {

		ItemStack itemstack = ctx.getItemInHand();
		if (LootUtils.isRolling(itemstack)) {
			return InteractionResult.PASS;
		}
		ToolType type = LootUtils.getToolType(itemstack);

		// Traits get the click first.
		InteractionResult traitResult = TraitDispatch.useOn(ctx);
		if (traitResult.consumesAction()) {
			return traitResult;
		}

		// No trait consumed it - try vanilla tool behaviors
		return switch (type) {
			case AXE -> tryAxeActions(ctx);
			case SHOVEL -> tryShovelFlatten(ctx);
			default -> InteractionResult.PASS;
		};
	}

	private InteractionResult tryAxeActions(UseOnContext ctx) {
		// Strip logs, else scrape oxidation, else remove wax. Each state is only worked
		// out when the one before did not apply: stripping draws from the level's random.
		InteractionResult result = transform(ctx, getToolModifiedState(ctx, ToolAction.AXE_STRIP),
				SoundEvents.AXE_STRIP.value());
		if (!result.consumesAction()) {
			result = transform(ctx, getToolModifiedState(ctx, ToolAction.AXE_SCRAPE), SoundEvents.AXE_SCRAPE.value());
		}
		if (!result.consumesAction()) {
			result = transform(ctx, getToolModifiedState(ctx, ToolAction.AXE_WAX_OFF), SoundEvents.AXE_WAX_OFF.value());
		}
		return result;
	}

	private InteractionResult tryShovelFlatten(UseOnContext ctx) {
		if (ctx.getClickedFace() != Direction.UP || !ctx.getLevel().getBlockState(ctx.getClickedPos().above()).isAir()) {
			return InteractionResult.PASS;
		}
		return transform(ctx, getToolModifiedState(ctx, ToolAction.SHOVEL_FLATTEN), SoundEvents.SHOVEL_FLATTEN.value());
	}

	/**
	 * Turns the clicked block into {@code newState} with the tool action's sound, charging
	 * the tool one durability; PASS when the action does not apply ({@code newState} null).
	 */
	private static InteractionResult transform(UseOnContext ctx, @Nullable BlockState newState, SoundEvent sound) {
		if (newState == null) {
			return InteractionResult.PASS;
		}
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Player player = ctx.getPlayer();
		level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
		if (!level.isClientSide()) {
			level.setBlock(pos, newState, 11);
			level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
			TraitDispatch.spendDurability(ctx.getItemInHand(), 1, player, EquipmentSlot.MAINHAND);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Main-hand clicks whose right-click-anywhere trait is waiting on the off-hand item,
	 * by player, with the game time of the click. Server thread only; weak keys let
	 * logged-out players go.
	 */
	private static final Map<ServerPlayer, Long> DEFERRED_USES = new WeakHashMap<>();

	@Override
	public @NotNull InteractionResult use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
		ItemStack toolItem = player.getItemInHand(hand);

		// A right-click-anywhere trait in the main hand gives the off-hand item the click
		// first. The client always predicts PASS for these traits and goes on to use the
		// off hand as well, so firing here would do both: raise the shield AND throw the
		// fireball. Whether the off-hand item acts can't be known without using it, so
		// wait for its real result: afterUseItem fires the trait if that use passes.
		if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer
				&& !player.getOffhandItem().isEmpty() && TraitDispatch.usesAnywhere(toolItem)) {
			DEFERRED_USES.put(serverPlayer, level.getGameTime());
			return InteractionResult.PASS;
		}

		return useAnywhereTraits(level, player, hand, toolItem) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	/**
	 * Called by each loader after the server has finished an item use, with its result.
	 * If the player's main-hand tool deferred a right-click-anywhere trait on this click
	 * and the off-hand use that followed did nothing, the trait fires now.
	 *
	 * <p>The client sends the off-hand use right behind the main-hand one, so the two
	 * normally land in the same tick; one tick of slack covers a tick boundary between
	 * them. A click that ends on the off-hand block use instead is settled by
	 * {@link #afterUseItemOn}; a deferral nothing answers at all simply goes stale.
	 */
	public static void afterUseItem(ServerPlayer player, InteractionHand hand, InteractionResult result) {
		if (hand != InteractionHand.OFF_HAND) {
			return;
		}

		Long deferredAt = DEFERRED_USES.remove(player);
		if (deferredAt == null || result.consumesAction() || player.level().getGameTime() - deferredAt > 1) {
			return;
		}

		ItemStack tool = player.getMainHandItem();
		if (tool.getItem() instanceof LootItem item) {
			item.useAnywhereTraits(player.level(), player, InteractionHand.MAIN_HAND, tool);
		}
	}

	/**
	 * Called by each loader after the server has finished using an item on a block. An
	 * off-hand block use that succeeds or fails ends the click - the client sends no
	 * off-hand item use after it - so a trait deferred on this click is dropped here
	 * rather than left for some later, unrelated off-hand use to set off.
	 */
	public static void afterUseItemOn(ServerPlayer player, InteractionHand hand, InteractionResult result) {
		if (hand == InteractionHand.OFF_HAND
				&& (result instanceof InteractionResult.Success || result instanceof InteractionResult.Fail)) {
			DEFERRED_USES.remove(player);
		}
	}

	/** Runs the tool's right-click-anywhere traits; true when one of them acted. */
	private boolean useAnywhereTraits(Level level, Player player, InteractionHand hand, ItemStack toolItem) {
		boolean used = TraitDispatch.use(toolItem, level, player, hand);

		// Award the tool's ITEM_USED stat exactly once, server-side. awardStat also
		// updates scoreboard objectives, unlike a bare getStats().increment().
		if (used && player instanceof ServerPlayer sPlayer) {
			sPlayer.awardStat(Stats.ITEM_USED.get(this));
		}

		return used;
	}

	@Override
	protected void appendStatLines(ItemStack item, ToolType tt, Consumer<Component> tips) {
		statLine(tips, "tooltip.randomloot.speed", "Speed: %s", getDigSpeed(item, tt));
		statLine(tips, "tooltip.randomloot.damage", "Damage: %s", getAttackDamage(item, tt));
	}

	/** Hold-style traits run while the tool is actually held. */
	@Override
	protected EquipmentSlot holdSlot(ItemStack stack) {
		return EquipmentSlot.MAINHAND;
	}

	// Datapack-editable enchantment groups; see data/randomloot/tags/enchantment/.
	private static final TagKey<Enchantment> ALL_TOOL_ENCHANTS = enchantTag("all_tools");
	private static final TagKey<Enchantment> MINING_ENCHANTS = enchantTag("mining_tools");
	private static final TagKey<Enchantment> WEAPON_ENCHANTS = enchantTag("weapons");
	private static final TagKey<Enchantment> SWORD_ENCHANTS = enchantTag("swords");

	/**
	 * The per-tool-type filter has to live in supportsEnchantment, not isPrimaryItemFor:
	 * the enchanting table consults isPrimaryItemFor, but the anvil/book path checks
	 * supportsEnchantment, whose default just reads the enchantment's supported-items tag.
	 * Since the single tool item sits in every minecraft:enchantable/* tag (it covers all
	 * four tool types), that default let an efficiency book land on a sword. The default
	 * isPrimaryItemFor delegates here, so the table stays filtered too.
	 */
	@Override
	public Boolean supportsEnchantmentCommon(ItemStack stack, Holder<Enchantment> enchantment) {
		ToolType type = LootUtils.getToolType(stack);

		if (enchantment.is(ALL_TOOL_ENCHANTS)) {
			return true;
		}

		if (enchantment.is(MINING_ENCHANTS)) {
			return type == ToolType.PICKAXE || type == ToolType.AXE || type == ToolType.SHOVEL;
		}

		if (enchantment.is(WEAPON_ENCHANTS)) {
			return type == ToolType.SWORD || type == ToolType.AXE;
		}

		if (enchantment.is(SWORD_ENCHANTS)) {
			return type == ToolType.SWORD;
		}

		// Enchantments in none of the randomloot tags (e.g. modded ones) follow their own
		// supported-items definition; null = defer to the loader's default check.
		return null;
	}

	/**
	 * The state a block turns into under a tool action (axe strip/scrape/wax-off,
	 * shovel flatten), or null when the action does not apply. 26.3 moved strip and
	 * flatten into the datapack {@code BLOCK_TRANSFORMER} registry (both loaders read
	 * it, and datapacks can extend it); scrape and wax-off are still standalone vanilla
	 * lookups. This is pure vanilla API, so it lives in common rather than the platform
	 * seam.
	 */
	private static BlockState getToolModifiedState(UseOnContext ctx, ToolAction action) {
		BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());
		Block block = state.getBlock();
		return switch (action) {
			case AXE_STRIP -> transformedState(ctx, BlockTransformers.AXE, SoundEvents.AXE_STRIP);
			case AXE_SCRAPE -> WeatheringCopper.getPrevious(state).orElse(null);
			case AXE_WAX_OFF -> {
				Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(block);
				yield unwaxed == null ? null : unwaxed.withPropertiesOf(state);
			}
			case SHOVEL_FLATTEN -> transformedState(ctx, BlockTransformers.SHOVEL, SoundEvents.SHOVEL_FLATTEN);
		};
	}

	/**
	 * The block state the given vanilla block transformer produces at the clicked
	 * position, or null when none applies. The AXE transformer bundles strip + scrape +
	 * wax-off, so match only the transform tagged with the action's own sound to keep
	 * AXE_STRIP strip-only.
	 */
	private static BlockState transformedState(UseOnContext ctx, ResourceKey<BlockTransformer> key,
			Holder<SoundEvent> sound) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Direction face = ctx.getClickedFace();
		BlockTransformer transformer = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).getValueOrThrow(key);
		for (BlockTransformer.BlockTransformData data : transformer.transforms()) {
			if (!data.sound().equals(sound) || data.disallowedFaces().contains(face)) {
				continue;
			}
			BlockState newState = data.blockStateProvider().value().getOptionalState(level, level.getRandom(), pos);
			if (newState != null) {
				return data.updateFromNeighbors() ? Block.updateFromNeighbourShapes(newState, level, pos) : newState;
			}
		}
		return null;
	}

}
