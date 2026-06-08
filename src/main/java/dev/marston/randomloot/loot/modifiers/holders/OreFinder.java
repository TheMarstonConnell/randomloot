package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.AbstractModifier;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber(modid = RandomLoot.MODID)
public class OreFinder extends AbstractModifier implements HoldModifier {

	private float power;
	private final static String POWER = "power";

	static int maxTime = 10;
	static int time = 0;
	static int maxShulkerLife = 10;

	// Thread-safe lists for concurrent access between tick events and hold()
	private static final List<Shulker> shulkers = new CopyOnWriteArrayList<>();
	private static final List<Integer> timings = new CopyOnWriteArrayList<>();

	public OreFinder(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public OreFinder() {
		this.name = "Detecting";
		this.power = 4.0f;
	}

	public Modifier clone() {
		return new OreFinder();
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
		return new OreFinder(tag.getStringOr(NAME, "Detecting"), tag.getFloatOr(POWER, 4.0f));
	}

	@Override
	public String tagName() {
		return "detecting";
	}

	@Override
	public String color() {
		return ChatFormatting.WHITE.getName();
	}

	@Override
	public String description() {
		return "While holding the tool, ores around you will glow.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return isMiningTool(type);
	}

	@SubscribeEvent
	public static void serverStop(ServerStoppingEvent event) {
		for (Shulker shulker : shulkers) {
			shulker.setPos(0, -256, 0);
			shulker.setHealth(0);
		}
	}

	@SubscribeEvent
	public static void tickEvent(ServerTickEvent.Post event) {
		time++;
		time = time % maxTime;

		if (time == 0) {
			// Iterate over copy to avoid ConcurrentModificationException
			for (int i = shulkers.size() - 1; i >= 0; i--) {
				if (i >= shulkers.size() || i >= timings.size()) {
					continue; // List may have been modified
				}

				int tick = timings.get(i) + 1;
				timings.set(i, tick);
				Shulker sh = shulkers.get(i);

				if (tick > maxShulkerLife
						|| sh.level().getBlockState(sh.blockPosition()).getBlock().equals(Blocks.AIR)) {
					sh.setPos(0, -256, 0);
					sh.setHealth(0);
					shulkers.remove(i);
					timings.remove(i);
				}
			}
		}
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		int size = 10;
		for (int i = -size; i < size; i++) {
			for (int j = -size; j < size; j++) {
				for (int k = -size; k < size; k++) {
					BlockPos p = new BlockPos((int) (holder.getX() + i), (int) (holder.getY() + j),
							(int) (holder.getZ() + k));
					Block b = level.getBlockState(p).getBlock();
					name = b.getName().getString();

					if (name.toLowerCase().contains("ore")) {

						List<Entity> entitiesInBlock = level.getEntities(null, new AABB(p));
						if (!entitiesInBlock.isEmpty()) {
							boolean isShulker = false;
							for (Entity entity : entitiesInBlock) {
								if (entity.getType() == EntityType.SHULKER) {
									isShulker = true;
									break;
								}
							}
							if (isShulker) {
								continue;
							}
						}

						Shulker se = new Shulker(EntityType.SHULKER, level);
						se.setGlowingTag(true);
						se.setInvulnerable(true);
						se.setInvisible(true);
						se.setPos(p.getX(), p.getY(), p.getZ());
						se.setNoAi(true);

						level.addFreshEntity(se);
						se.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0, false, false));

						shulkers.add(se);
						timings.add(-1);

					}
				}
			}
		}

	}
}
