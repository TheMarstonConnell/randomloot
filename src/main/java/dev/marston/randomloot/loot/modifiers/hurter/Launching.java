package dev.marston.randomloot.loot.modifiers.hurter;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.EntityHurtModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class Launching implements EntityHurtModifier {

	private String name;
	private final static String LEVEL = "trait_level";
	private int level = 0;

	public Launching(String name, int level) {
		this.name = name;
		this.level = level;
	}

	public Launching() {
		this.name = "Launching";
		this.level = 0;
	}

	public Modifier clone() {
		return new Launching();
	}

	@Override
	public CompoundTag toNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putString(NAME, name);
		tag.putInt(LEVEL, level);
		return tag;
	}

	@Override
	public Modifier fromNBT(CompoundTag tag) {
		return new Launching(tag.getStringOr(NAME, "Launching"), tag.getIntOr(LEVEL, 0));
	}

	@Override
	public String name() {
		if (this.level == 0) {
			return this.name;
		}
		return this.name + " " + LootUtils.roman(this.level + 1);
	}

	@Override
	public String tagName() {
		return "launching";
	}

	@Override
	public String color() {
		return ChatFormatting.WHITE.getName();
	}

	@Override
	public String description() {
		return "Send enemies flying upward when hit.";
	}

	@Override
	public void writeToLore(List<Component> list, boolean shift) {
		MutableComponent comp = Modifier.makeComp(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.SWORD) || type.equals(ToolType.AXE);
	}

	@Override
	public boolean hurtEnemy(ItemStack itemstack, LivingEntity hurtee, LivingEntity hurter) {
		if (hurtee.level().isClientSide()) {
			return false;
		}

		// Launch power scales with level: 0.6, 0.8, 1.0, 1.2
		double launchPower = 0.6 + (level * 0.2);

		// Get current velocity and add upward momentum
		Vec3 currentVelocity = hurtee.getDeltaMovement();
		hurtee.setDeltaMovement(currentVelocity.x * 0.5, launchPower, currentVelocity.z * 0.5);

		// Mark velocity as changed so it syncs to client
		hurtee.hurtMarked = true;

		// Spawn cloud particles for visual feedback
		Modifier.TrackEntityParticle(hurtee.level(), hurtee, ParticleTypes.CLOUD);

		return false;
	}

	public boolean canLevel() {
		return level < 3;
	}

	public void levelUp() {
		this.level++;
	}
}
