package dev.marston.randomloot.loot.modifiers.breakers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.BlockBreakModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;


public class Explode extends AbstractModifier implements BlockBreakModifier {

	private float power;
	private final static String POWER = "power";

	public Explode(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public Explode() {
		this.name = "Explosive";
		this.power = 4.0f;
	}

	@Override
	public boolean startBreak(ItemStack itemstack, BlockPos pos, LivingEntity player) {

		Level l = player.level();
		if (l.isClientSide()) {
			return false;
		}

		l.explode(player, Explosion.getDefaultDamageSource(l, player), null, pos.getX(), pos.getY() + 0.5, pos.getZ(), power, false, ExplosionInteraction.TNT);

		return false;
	}

	@Override
	public CompoundTag toNBT() {

		CompoundTag tag = new CompoundTag();

		tag.putFloat(POWER, power);

		tag.putString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Explode(tag.getStringOr(NAME, "Explosive"), tag.getFloatOr(POWER, 4.0f));
	}

	@Override
	public String tagName() {
		return "explode";
	}

	@Override
	public String color() {
		return "red";
	}

	@Override
	public String description() {
		return "Upon breaking a block (allowed by tool type), the current block position will explode causing damage to surrounding blocks.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type);
	}
}
