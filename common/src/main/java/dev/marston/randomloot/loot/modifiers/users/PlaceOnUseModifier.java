package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Base for right-click traits that place or ignite a block while crouching and charge
 * durability on success (DirtPlace, TorchPlace, FirePlace). Owns the crouch gate, the
 * durability cost and its {@code DAMAGE} NBT field; subclasses implement
 * {@link #place(UseOnContext)}.
 */
public abstract class PlaceOnUseModifier extends AbstractModifier implements UseModifier {

	protected static final String DAMAGE = "DAMAGE";

	protected int damage;

	/** Attempts the placement; durability is only charged on SUCCESS. */
	protected abstract InteractionResult place(UseOnContext ctx);

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = super.toNBT();
		tag.putInt(DAMAGE, damage);
		return tag;
	}

	@Override
	public InteractionResult use(UseOnContext ctx) {
		if (!ctx.getPlayer().isCrouching()) {
			return InteractionResult.PASS; // Allow normal tool behaviors when not crouching
		}

		InteractionResult result = place(ctx);

		if (result == InteractionResult.SUCCESS) {
			ctx.getItemInHand().hurtAndBreak(this.damage, ctx.getPlayer(), EquipmentSlot.MAINHAND);
		}

		return result;
	}

	@Override
	public boolean use(Level level, Player player, InteractionHand hand) {
		return true;
	}

	@Override
	public boolean useAnywhere() {
		return false;
	}

	@Override
	public boolean compatible(Modifier mod) {
		return !ModifierRegistry.USERS.contains(mod);
	}

	@Override
	public boolean forTool(ToolType type) {
		// Right-click traits never fire from worn armor.
		return !type.isArmor();
	}
}
