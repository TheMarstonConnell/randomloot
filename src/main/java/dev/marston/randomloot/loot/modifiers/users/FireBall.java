package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FireBall implements UseModifier {
	private String name;
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

	public Modifier clone() {
		return new FireBall();
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
		return new FireBall(tag.getStringOr(NAME, "Flame Thrower"), tag.getIntOr(DAMAGE, 20));
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "flame_thrower";
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_RED.getName();
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
	public void writeToLore(List<Component> list, boolean shift) {

		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);

	}

	@Override
	public Component writeDetailsToLore(Level level) {

		return null;
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

		double d1 = 2.5D;
		Vec3 vec3 = player.getLookAngle();

		LargeFireball largefireball = new LargeFireball(level, player, vec3, 1);

		largefireball.setPos(player.getX() + vec3.x * d1, player.getY(0.5D) + 0.5D, largefireball.getZ() + vec3.z * d1);

		level.addFreshEntity(largefireball);

		player.getItemInHand(hand).hurtAndBreak(this.damage, player, EquipmentSlot.MAINHAND);

		return true;
	}

	@Override
	public boolean useAnywhere() {
		return true;
	}

	public boolean canLevel() {
		return false;
	}

	public void levelUp() {
		return;
	}

}
