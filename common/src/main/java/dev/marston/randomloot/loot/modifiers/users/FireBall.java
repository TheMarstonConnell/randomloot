package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.NbtCompat;

import dev.marston.randomloot.loot.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;


public class FireBall extends AbstractModifier implements UseModifier {
	private int damage;
	private static final String DAMAGE = "DAMAGE";

	public FireBall(String name, int damage) {
		this.name = name;
		this.damage = damage;
	}

	public FireBall() {
		this.name = "Flame Thrower";
		this.damage = 20;
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = super.toNBT();
		tag.putInt(DAMAGE, damage);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new FireBall(NbtCompat.getStringOr(tag, NAME, "Flame Thrower"), NbtCompat.getIntOr(tag, DAMAGE, 20));
	}

	@Override
	public String tagName() {
		return "flame_thrower";
	}

	@Override
	public ChatFormatting color() {
		return ChatFormatting.DARK_RED;
	}

	@Override
	public InteractionResult use(UseOnContext ctx) {
		return InteractionResult.PASS;
	}

	@Override
	public String description() {
		return "Right clicking throws a fire ball.";
	}

	@Override
	public boolean compatible(Modifier mod) {
		return !ModifierRegistry.USERS.contains(mod);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type == ToolType.SWORD;
	}

	@Override
	public boolean use(Level level, Player player, InteractionHand hand) {

		if (level.isClientSide()) {
			return false;
		}

		double d1 = 2.5D;
		Vec3 vec3 = player.getLookAngle();

		LargeFireball largefireball = new LargeFireball(level, player, vec3, 1);

		largefireball.setPos(player.getX() + vec3.x * d1, player.getY(0.5D) + 0.5D, player.getZ() + vec3.z * d1);

		level.addFreshEntity(largefireball);

		player.getItemInHand(hand).hurtAndBreak(this.damage, player, EquipmentSlot.MAINHAND);

		return true;
	}

	@Override
	public boolean useAnywhere() {
		return true;
	}
}
