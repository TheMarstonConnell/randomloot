package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;


public class FirePlace extends AbstractModifier implements UseModifier {
	private int damage;
	private static final String DAMAGE = "DAMAGE";

	public FirePlace(String name, int damage) {
		this.name = name;
		this.damage = damage;
	}

	public FirePlace() {
		this.name = "Fire Starter";
		this.damage = 2;
	}

	public Modifier clone() {
		return new FirePlace();
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putString(NAME, name);
		tag.putInt(DAMAGE, damage);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new FirePlace(tag.getStringOr(NAME, "Fire Starter"), tag.getIntOr(DAMAGE, 2));
	}

	@Override
	public String tagName() {
		return "fire_place";
	}

	@Override
	public String color() {
		return ChatFormatting.RED.getName();
	}

	private InteractionResult flintNSteel(UseOnContext ctx) {
		Player player = ctx.getPlayer();
		Level level = ctx.getLevel();
		BlockPos blockpos = ctx.getClickedPos();
		BlockState blockstate = level.getBlockState(blockpos);
		if (!CampfireBlock.canLight(blockstate) && !CandleBlock.canLight(blockstate)
				&& !CandleCakeBlock.canLight(blockstate)) {
			BlockPos blockpos1 = blockpos.relative(ctx.getClickedFace());
			if (BaseFireBlock.canBePlacedAt(level, blockpos1, ctx.getHorizontalDirection())) {
				level.playSound(player, blockpos1, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
						level.getRandom().nextFloat() * 0.4F + 0.8F);
				BlockState blockstate1 = BaseFireBlock.getState(level, blockpos1);
				level.setBlock(blockpos1, blockstate1, 11);
				level.gameEvent(player, GameEvent.BLOCK_PLACE, blockpos);
				ItemStack itemstack = ctx.getItemInHand();
				if (player instanceof ServerPlayer) {
					CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, blockpos1, itemstack);
					ctx.getItemInHand().hurtAndBreak(this.damage, ctx.getPlayer(), EquipmentSlot.MAINHAND);
				}

				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.FAIL;
			}
		} else {
			level.playSound(player, blockpos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
					level.getRandom().nextFloat() * 0.4F + 0.8F);
			level.setBlock(blockpos, blockstate.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)), 11);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, blockpos);
			if (player != null) {
				ctx.getItemInHand().hurtAndBreak(this.damage, player, EquipmentSlot.MAINHAND);
			}

			return InteractionResult.SUCCESS;
		}
	}

	@Override
	public InteractionResult use(UseOnContext ctx) {

		if (!ctx.getPlayer().isCrouching()) {
			return InteractionResult.PASS;  // Allow axe stripping when not crouching
		}

		return flintNSteel(ctx);

	}

	@Override
	public String description() {
		return "Right clicking on the top of a block while crouching with the tool in hand will start a fire and use "
				+ this.damage + " durability points.";
	}

	@Override
	public boolean compatible(Modifier mod) {
		return !ModifierRegistry.USERS.contains(mod);
	}

	@Override
	public boolean forTool(ToolType type) {
		return true;
	}

	@Override
	public boolean use(Level level, Player player, InteractionHand hand) {
		return true;
	}

	@Override
	public boolean useAnywhere() {
		return false;
	}
}
