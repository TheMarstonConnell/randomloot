package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.Stored;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import dev.marston.randomloot.loot.modifiers.TraitDispatch;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FireBall extends AbstractModifier implements UseModifier {
	private static final String DAMAGE = "DAMAGE";

	/** Durability a fire ball costs. Stored, so gear keeps the cost it was made with. */
	private final Stored<Integer> damage = stored(DAMAGE, 20);

	@Override
	public InteractionResult use(UseOnContext ctx) {
		return InteractionResult.PASS;
	}

	@Override
	public String description() {
		return "Right clicking throws a fire ball.";
	}

	@Override
	public boolean use(Level level, Player player, InteractionHand hand) {

		double d1 = 2.5D;
		Vec3 vec3 = player.getLookAngle();

		LargeFireball largefireball = new LargeFireball(level, player, vec3.x, vec3.y, vec3.z, 1);

		largefireball.setPos(player.getX() + vec3.x * d1, player.getY(0.5D) + 0.5D, player.getZ() + vec3.z * d1);

		level.addFreshEntity(largefireball);

		TraitDispatch.spendDurability(player.getItemInHand(hand), this.damage.get(), player, EquipmentSlot.MAINHAND);

		return true;
	}

	@Override
	public boolean useAnywhere() {
		return true;
	}
}
