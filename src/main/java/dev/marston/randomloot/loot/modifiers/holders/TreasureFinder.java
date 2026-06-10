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

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = RandomLoot.MODID)
public class TreasureFinder extends AbstractModifier implements HoldModifier {

	private float power;
	private final static String POWER = "power";

	static int maxTime = 10;
	static int time = 0;
	static int maxShulkerLife = 10;

	static boolean locked = false;

	private static ArrayList<Shulker> shulkers = new ArrayList<Shulker>();
	private static ArrayList<Integer> timings = new ArrayList<Integer>();

	public TreasureFinder(String name, float power) {
		this.name = name;
		this.power = power;
	}

	public TreasureFinder() {
		this.name = "Tomb Raider";
		this.power = 4.0f;
	}

	public Modifier clone() {
		return new TreasureFinder();
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
		return new TreasureFinder(tag.getStringOr(NAME, "Tomb Raider"), tag.getFloatOr(POWER, 4.0f));
	}

	@Override
	public String tagName() {
		return "spawner";
	}

	@Override
	public String color() {
		return ChatFormatting.DARK_AQUA.getName();
	}

	@Override
	public String description() {
		return "While held or worn, spawners around you will glow.";
	}

	@Override
	public boolean forTool(ToolType type) {
		return true;
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
		locked = true;

		time++;
		time = time % maxTime;

		if (time == 0) {
			int off = 0;
			for (int i = 0; i < shulkers.size(); i++) {
				int iOff = i - off;
				int tick = timings.get(iOff) + 1;
				timings.set(iOff, tick);
				Shulker sh = shulkers.get(iOff);

				if (tick > maxShulkerLife
						|| sh.level().getBlockState(sh.blockPosition()).getBlock().equals(Blocks.AIR)) {
					shulkers.get(iOff).setPos(0, -64, 0);
					shulkers.get(iOff).setHealth(0);
					shulkers.remove(iOff);
					timings.remove(iOff);
					off++;
				}
			}
		}

		locked = false;
	}

	@Override
	public void hold(ItemStack stack, Level level, Entity holder) {
		if (locked) {
			return;
		}

		int size = 10;

		for (int i = -size; i < size; i++) {
			for (int j = -size; j < size; j++) {
				for (int k = -size; k < size; k++) {
					BlockPos p = new BlockPos((int) (holder.getX() + i), (int) (holder.getY() + j),
							(int) (holder.getZ() + k));
					Block b = level.getBlockState(p).getBlock();

					if (b == Blocks.SPAWNER) {

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
