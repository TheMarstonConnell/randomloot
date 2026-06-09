package dev.marston.randomloot.loot;

import dev.marston.randomloot.Config;
import dev.marston.randomloot.loot.modifiers.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class LootItem extends Item  {

	public enum ToolType {
		PICKAXE, SHOVEL, AXE, SWORD, NULL;

		@Override
		public String toString() {
            return switch (this) {
                case PICKAXE -> "Pickaxe";
                case SHOVEL -> "Shovel";
                case AXE -> "Axe";
                case SWORD -> "Sword";
                default -> "Null";
            };
		}
	}


	public LootItem(Properties p) {
		super(p.stacksTo(1).durability(100));
	}

	public static float getDigSpeed(ItemStack stack, ToolType type) {

		float statMod = 1.0f;

		List<Modifier> mods = LootUtils.getModifiers(stack);

		for (Modifier mod : mods) {
			if (mod instanceof StatsModifier ehm) {
				if (!Config.traitEnabled(mod.tagName())) {
					continue;
				}

                statMod *= ehm.getStats(stack);
			}
		}

		if (type.equals(ToolType.SWORD)) {
			return 1.0f;
		}

		float speed = (LootUtils.getStats(stack) / 2.0f) + 6.0f;
		return speed * statMod;
	}

	public static float getAttackSpeed(ItemStack stack, ToolType type) {

		float speed = 0.0f;

		switch (type) {
		case PICKAXE:
			speed = -2.8F;
			break;
		case AXE:
			speed = -3.0F;
			break;
		case SHOVEL:
			speed = -3.0F;
			break;
		case SWORD:
			speed = -2.4F;
			break;
		default:
			break;
		}

		return speed;
	}

	public static float getAttackDamage(ItemStack stack, ToolType type) {

		float damage = (LootUtils.getStats(stack)) + 1.0f;

		switch (type) {
		case PICKAXE:
			damage = damage * 0.5f;
			break;
		case AXE:
			damage = damage * 1.2f;
			break;
		case SHOVEL:
			damage = damage * 0.6f;
			break;
		default:
			break;
		}

		return damage;
	}

	@Override
	public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState block) {

		ToolType type = LootUtils.getToolType(stack);

		TagKey<Block> blocks = null;

		if (type == ToolType.PICKAXE) {
			blocks = BlockTags.MINEABLE_WITH_PICKAXE;
		} else if (type == ToolType.AXE) {
			blocks = BlockTags.MINEABLE_WITH_AXE;
		} else if (type == ToolType.SHOVEL) {
			blocks = BlockTags.MINEABLE_WITH_SHOVEL;
		} else if (type == ToolType.SWORD) {
			Block blockType = block.getBlock();

			// Cobwebs - instant break (vanilla sword behavior)
			if (blockType == Blocks.COBWEB) {
				return 15.0f;
			}

			// Bamboo - instant break (vanilla sword behavior)
			if (blockType == Blocks.BAMBOO || blockType == Blocks.BAMBOO_SAPLING) {
				return 100.0f;
			}

			// Leaves - faster than fists (vanilla sword behavior)
			if (block.is(BlockTags.LEAVES)) {
				return 1.5f;
			}

			return 1.0f;
		} else {
			return 1.0f;
		}

		return block.is(blocks) ? getDigSpeed(stack, type) : 1.0F;
	}


	@Override
	public boolean isCombineRepairable(ItemStack stack) {
		return false;
	}



	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {

		ToolType type = LootUtils.getToolType(stack);

		TagKey<Block> blocks = null;

		if (type == ToolType.PICKAXE) {
			blocks = BlockTags.MINEABLE_WITH_PICKAXE;
		} else if (type == ToolType.SHOVEL) {
			blocks = BlockTags.MINEABLE_WITH_SHOVEL;
		} else if (type == ToolType.AXE) {
			blocks = BlockTags.MINEABLE_WITH_AXE;
		} else {
			return false;
		}

		return state.is(blocks);
	}


	@Override
	public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {

		ToolType tt = LootUtils.getToolType(stack);

		float attack = getAttackDamage(stack, tt);
		float speed = getAttackSpeed(stack, tt);

		return ItemAttributeModifiers.builder().add(
				Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attack, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND).build();
	}

	@Override
	public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
		if (!(stack instanceof ItemStack itemStack)) {
			return false;
		}
		ToolType type = LootUtils.getToolType(itemStack);

		if (type == ToolType.AXE) {
			return itemAbility == ItemAbilities.AXE_STRIP ||
				   itemAbility == ItemAbilities.AXE_SCRAPE ||
				   itemAbility == ItemAbilities.AXE_WAX_OFF;
		}
		if (type == ToolType.SHOVEL) {
			return itemAbility == ItemAbilities.SHOVEL_FLATTEN;
		}
		return false;
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {

		ToolType type = LootUtils.getToolType(itemstack);

		if (type == ToolType.AXE || type == ToolType.SWORD) {
			LootUtils.addXp(itemstack, hurter, 1);
		}

		List<Modifier> mods = LootUtils.getModifiers(itemstack);

		boolean shouldSkipBreak = false;
		for (Modifier mod : mods) {
			if (mod instanceof EntityHurtModifier ehm) {
				if (!Config.traitEnabled(mod.tagName())) {
					continue;
				}

				if (ehm.hurtEnemy(itemstack, hurtee, hurter)) {
					shouldSkipBreak = true;
				}
			}

			if (mod instanceof Unbreaking unbreaking) {
				if (!Config.traitEnabled(mod.tagName())) {
					continue;
				}

				if (unbreaking.test(hurtee.level())) {
					shouldSkipBreak = true;
				}
			}
		}
		if (!shouldSkipBreak) {
			itemstack.hurtAndBreak(1, hurter, EquipmentSlot.MAINHAND);
		}
	}

//	@Override
//	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
//
//		if (enchantment.category.equals(EnchantmentCategory.BREAKABLE)) { // all these items are breakable so we can
//																			// enchant them first!
//			return true;
//		}
//
//		ToolType type = LootUtils.getToolType(stack);
//		if (enchantment.category.equals(EnchantmentCategory.DIGGER)) {
//			if (type == ToolType.AXE || type == ToolType.SHOVEL || type == ToolType.PICKAXE) {
//				return true;
//			}
//		}
//
//		if (enchantment.category.equals(EnchantmentCategory.WEAPON)) {
//			if (type == ToolType.AXE || type == ToolType.SWORD) {
//				return true;
//			}
//		}
//
//		return enchantment.category.canEnchant(stack.getItem());
//	}

	@Override
	public boolean mineBlock(@NotNull ItemStack stack, Level level, @NotNull BlockState blockState, @NotNull BlockPos pos, @NotNull LivingEntity player) {
		if (!level.isClientSide() && blockState.getDestroySpeed(level, pos) != 0.0F) {

			List<Modifier> mods = LootUtils.getModifiers(stack);

			boolean shouldSkipBreak = false;
			for (Modifier mod : mods) {

				if (mod instanceof BlockBreakModifier bbm) {

                    if (bbm.startBreak(stack, pos, player)) {
						shouldSkipBreak = true;
					}
				}

				if (mod instanceof Unbreaking unbreaking) {
					if (!Config.traitEnabled(mod.tagName())) {
						continue;
					}

                    if (unbreaking.test(level)) {
						shouldSkipBreak = true;
					}

				}
			}

			if (!shouldSkipBreak) {
				stack.hurtAndBreak(1, player,EquipmentSlot.MAINHAND);
			}

			LootUtils.addXp(stack, player, 1);

		}

		return true;
	}



	@Override
	public int getMaxDamage(@NotNull ItemStack stack) {
		float stats = (LootUtils.getStats(stack) + 10.0f) * 80.0f;

		return (int) stats;
	}

	@Override
	public @NotNull InteractionResult useOn(UseOnContext ctx) {

		ItemStack itemstack = ctx.getItemInHand();
		ToolType type = LootUtils.getToolType(itemstack);
		List<Modifier> mods = LootUtils.getModifiers(itemstack);

		// First, try UseModifier abilities
		for (Modifier mod : mods) {

			if (mod instanceof UseModifier um) {
				if (!Config.traitEnabled(mod.tagName())) {
					continue;
				}

				InteractionResult result = um.use(ctx);
				if (result.consumesAction()) {
					return result;  // Modifier consumed the action
				}
			}

		}

		// No modifier consumed - try vanilla tool behaviors
		if (type == ToolType.AXE) {
			InteractionResult result = tryAxeActions(ctx);
			if (result.consumesAction()) return result;
		}

		if (type == ToolType.SHOVEL) {
			InteractionResult result = tryShovelFlatten(ctx);
			if (result.consumesAction()) return result;
		}

		return InteractionResult.PASS;
	}

	private InteractionResult tryAxeActions(UseOnContext ctx) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Player player = ctx.getPlayer();
		ItemStack stack = ctx.getItemInHand();

		// Try stripping logs
		BlockState stripped = state.getToolModifiedState(ctx, ItemAbilities.AXE_STRIP, false);
		if (stripped != null) {
			level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
			if (!level.isClientSide()) {
				level.setBlock(pos, stripped, 11);
				level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, stripped));
				stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
			}
			return InteractionResult.SUCCESS;
		}

		// Try scraping oxidation
		BlockState scraped = state.getToolModifiedState(ctx, ItemAbilities.AXE_SCRAPE, false);
		if (scraped != null) {
			level.playSound(player, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
			if (!level.isClientSide()) {
				level.setBlock(pos, scraped, 11);
				level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, scraped));
				stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
			}
			return InteractionResult.SUCCESS;
		}

		// Try removing wax
		BlockState unwaxed = state.getToolModifiedState(ctx, ItemAbilities.AXE_WAX_OFF, false);
		if (unwaxed != null) {
			level.playSound(player, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
			if (!level.isClientSide()) {
				level.setBlock(pos, unwaxed, 11);
				level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, unwaxed));
				stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private InteractionResult tryShovelFlatten(UseOnContext ctx) {
		if (ctx.getClickedFace() != Direction.UP) return InteractionResult.PASS;

		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();

		if (!level.getBlockState(pos.above()).isAir()) return InteractionResult.PASS;

		BlockState flattened = level.getBlockState(pos).getToolModifiedState(ctx, ItemAbilities.SHOVEL_FLATTEN, false);
		if (flattened != null) {
			Player player = ctx.getPlayer();
			level.playSound(player, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
			if (!level.isClientSide()) {
				level.setBlock(pos, flattened, 11);
				level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, flattened));
				ctx.getItemInHand().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public @NotNull InteractionResult use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
		ItemStack toolItem = player.getItemInHand(hand);

		boolean used = false;

		List<Modifier> mods = LootUtils.getModifiers(toolItem);

		for (Modifier mod : mods) {

			if (mod instanceof UseModifier um) {
				if (!Config.traitEnabled(mod.tagName())) {
					continue;
				}

                if (um.useAnywhere()) {
					used = used || um.use(level, player, hand);
				}
			}

		}

		if (used) {
			// Award the tool's ITEM_USED stat exactly once, server-side. awardStat also
			// updates scoreboard objectives, unlike a bare getStats().increment().
			if (player instanceof ServerPlayer sPlayer) {
				sPlayer.awardStat(Stats.ITEM_USED.get(this));
			}

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private MutableComponent makeComp(String text, ChatFormatting color) {
		MutableComponent comp = Component.empty();
		comp.append(text);
		comp = comp.withStyle(color);

		return comp;
	}

	private void newLine(Consumer<Component> tipList) {
		tipList.accept(makeComp("", ChatFormatting.GRAY));
	}

	@Override
	public void appendHoverText(ItemStack item, TooltipContext pContext, TooltipDisplay display, Consumer<Component> tipList, TooltipFlag pTooltipFlag) {
		Level level = pContext.level();

		// Check if shift/ctrl are held using InputConstants
		long window = net.minecraft.client.Minecraft.getInstance().getWindow().handle();
		boolean show = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
				|| org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

		boolean showDescription = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
				|| org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

		ToolType tt = LootUtils.getToolType(item);

		if (show) {
			tipList.accept(makeComp(tt.toString(), ChatFormatting.BLUE));
		}

		MutableComponent desc = makeComp(LootUtils.getItemLore(item), ChatFormatting.GRAY);
		tipList.accept(desc);

		if (show) {
			newLine(tipList);
			int itemLevel = LootUtils.getLevel(item);
			tipList.accept(makeComp("Level: " + itemLevel, ChatFormatting.GRAY));
			tipList.accept(makeComp("XP: " + LootUtils.getXP(item) + " / " + LootUtils.getMaxXP(itemLevel),
					ChatFormatting.GRAY));
		}

		newLine(tipList);

		List<Modifier> mods = LootUtils.getModifiers(item);
		mods.sort(new Comparator<Modifier>() {
            @Override
            public int compare(final Modifier object1, final Modifier object2) {
                return object1.tagName().compareTo(object2.tagName());
            }
        });

		for (Modifier modifier : mods) {
			if (!Config.traitEnabled(modifier.tagName())) {
				continue;
			}
			// Wrapper to bridge List<Component> interface to Consumer<Component>
			List<Component> tempList = new ArrayList<>();
			modifier.writeToLore(tempList, show);
			tempList.forEach(tipList);
			if (show) {
				Component details = modifier.writeDetailsToLore(level);

				if (details != null) {
					MutableComponent detailComp = makeComp(" - ", ChatFormatting.GRAY);
					detailComp.append(details);
					tipList.accept(detailComp);
				}
			}
			if (showDescription) {
				MutableComponent detailComp = makeComp("", ChatFormatting.GRAY);
				detailComp.append(modifier.description());
				tipList.accept(detailComp);
			}
		}

		if (show) {
			newLine(tipList);

			float digSpeed = LootItem.getDigSpeed(item, tt);
			tipList.accept(makeComp(String.format("Speed: %.2f", digSpeed), ChatFormatting.GRAY));

			float attackDamage = LootItem.getAttackDamage(item, tt);
			tipList.accept(makeComp(String.format("Damage: %.2f", attackDamage), ChatFormatting.GRAY));

		}

		if (!show && !showDescription) {
			newLine(tipList);
			MutableComponent comp = Component.empty();
			comp.append("[Shift for more]");
			comp = comp.withStyle(ChatFormatting.GRAY);
			tipList.accept(comp);
			MutableComponent descComp = Component.empty();
			descComp.append("[Ctrl for trait info]");
			descComp = descComp.withStyle(ChatFormatting.GRAY);
			tipList.accept(descComp);

		}
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity holder, EquipmentSlot slot) {

		// Only trigger for mainhand slot (replacing the old 'holding' boolean check)
		if (slot == EquipmentSlot.MAINHAND) {

			List<Modifier> mods = LootUtils.getModifiers(stack);

			for (Modifier mod : mods) {

				if (mod instanceof HoldModifier hodlMod) {

                    hodlMod.hold(stack, level, holder);
				}

			}
		}

	}

	@Override
	public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
		ToolType type = LootUtils.getToolType(stack);
		String enchantPath = enchantment.getRegisteredName();

		// Universal enchantments - all tools (Unbreaking, Mending)
		if (enchantPath.contains("unbreaking") || enchantPath.contains("mending")) {
			return true;
		}

		// Mining enchantments - pickaxe, axe, shovel only (Efficiency, Fortune, Silk Touch)
		if (enchantPath.contains("efficiency") || enchantPath.contains("fortune") || enchantPath.contains("silk_touch")) {
			return type == ToolType.PICKAXE || type == ToolType.AXE || type == ToolType.SHOVEL;
		}

		// Weapon enchantments - sword and axe (Sharpness, Smite, Bane, Knockback, Fire Aspect, Looting)
		if (enchantPath.contains("sharpness") || enchantPath.contains("smite") || enchantPath.contains("bane_of_arthropods") ||
			enchantPath.contains("knockback") || enchantPath.contains("fire_aspect") || enchantPath.contains("looting")) {
			return type == ToolType.SWORD || type == ToolType.AXE;
		}

		// Sword-only enchantments (Sweeping Edge)
		if (enchantPath.contains("sweeping")) {
			return type == ToolType.SWORD;
		}

		return false;
	}

}
