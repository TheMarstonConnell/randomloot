package dev.marston.randomloot.loot.modifiers;

import dev.marston.randomloot.loot.LootItem.ToolType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

public interface Modifier {

	public static MutableComponent makeComp(String text, ChatFormatting color) {
		MutableComponent comp = Component.empty();
		comp.append(text);
		comp = comp.withStyle(color);

		return comp;
	}

	public static MutableComponent makeComp(String text, String color) {
		MutableComponent comp = Component.empty();
		comp.append(text);
		comp = comp.withStyle(ChatFormatting.getByName(color));

		return comp;
	}

	public static MutableComponent makeComp(Component compIn) {
		MutableComponent comp = Component.empty();
		comp.append(compIn);
		return comp;
	}

	public static void TrackEntityParticle(Level level, Entity e, ParticleOptions particleType) {
		if (!level.isClientSide()) {
			Random r = new Random();

			ServerLevel sl = ((ServerLevel) level);

			for (int i = 0; i < 32; ++i) {
				double d0 = (double) (r.nextFloat() * 2.0F - 1.0F);
				double d1 = (double) (r.nextFloat() * 2.0F - 1.0F);
				double d2 = (double) (r.nextFloat() * 2.0F - 1.0F);
				if (!(d0 * d0 + d1 * d1 + d2 * d2 > 1.0D)) {
					double d3 = e.getX(d0 / 4.0D);
					double d4 = e.getY(0.5D + d1 / 4.0D);
					double d5 = e.getZ(d2 / 4.0D);
					sl.sendParticles(particleType, d3, d4, d5, 1, d0, d1 + 0.2D, d2, 0.0D);
				}
			}

		}
	}

	public static final String MODTAG = "modifiers";

	static final String NAME = "name";

	public String tagName();

	public void writeToLore(List<Component> list, boolean shift);

	public Component writeDetailsToLore(Level level);

	public String description();

	public String name();

	public String color();

	public Modifier clone();

	public CompoundTag toNBT();

	public Modifier fromNBT(CompoundTag tag);

	public boolean compatible(Modifier mod);

	public boolean forTool(ToolType type);

	public boolean canLevel();

	public void levelUp();
}
