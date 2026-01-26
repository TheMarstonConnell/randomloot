package dev.marston.randomloot.loot.modifiers.holders;

import dev.marston.randomloot.RandomLoot;
import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = RandomLoot.MODID)
public class TreasureFinder implements HoldModifier {

	private String name;
	private float power;
	private final static String POWER = "power";

	static int maxTime = 10;
	static int time = 0;
	static int maxShulkerLife = 10;

	static boolean locked = false;

	private static ArrayList<EntityShulker> shulkers = new ArrayList<EntityShulker>();
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
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setFloat(POWER, power);

		tag.setString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new TreasureFinder(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Tomb Raider",
			tag.hasKey(POWER) ? tag.getFloat(POWER) : 4.0f);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "spawner";
	}

	@Override
	public String color() {
		return TextFormatting.DARK_AQUA.getFriendlyName();
	}

	@Override
	public String description() {
		return "While holding the spawners around you will glow.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return true;
	}

	@SubscribeEvent
	public static void serverStop(WorldEvent.Unload event) {
		for (EntityShulker shulker : shulkers) {
			shulker.setPosition(0, -256, 0);
			shulker.setHealth(0);
		}
		shulkers.clear();
		timings.clear();
	}

	@SubscribeEvent
	public static void tickEvent(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		locked = true;

		time++;
		time = time % maxTime;

		if (time == 0) {
			int off = 0;
			for (int i = 0; i < shulkers.size(); i++) {
				int iOff = i - off;
				if (iOff >= timings.size() || iOff >= shulkers.size()) break;

				int tick = timings.get(iOff) + 1;
				timings.set(iOff, tick);
				EntityShulker sh = shulkers.get(iOff);

				if (tick > maxShulkerLife
						|| sh.world.getBlockState(sh.getPosition()).getBlock() == Blocks.AIR) {
					shulkers.get(iOff).setPosition(0, -64, 0);
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
	public void hold(ItemStack stack, World world, Entity holder) {
		if (locked) {
			return;
		}

		int size = 10;

		for (int i = -size; i < size; i++) {
			for (int j = -size; j < size; j++) {
				for (int k = -size; k < size; k++) {
					BlockPos p = new BlockPos((int) (holder.posX + i), (int) (holder.posY + j),
							(int) (holder.posZ + k));
					Block b = world.getBlockState(p).getBlock();

					if (b == Blocks.MOB_SPAWNER) {

						List<Entity> entitiesInBlock = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(p));
						if (!entitiesInBlock.isEmpty()) {
							boolean isShulker = false;
							for (Entity entity : entitiesInBlock) {
								if (entity instanceof EntityShulker) {
									isShulker = true;
									break;
								}
							}
							if (isShulker) {
								continue;
							}
						}

						EntityShulker se = new EntityShulker(world);
						se.setGlowing(true);

						se.setEntityInvulnerable(true);
						se.setInvisible(true);
						se.setPosition(p.getX(), p.getY(), p.getZ());
						se.setNoAI(true);

						world.spawnEntity(se);
						se.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 1200, 0, false, false));

						shulkers.add(se);
						timings.add(-1);

					}
				}
			}
		}

	}
}
