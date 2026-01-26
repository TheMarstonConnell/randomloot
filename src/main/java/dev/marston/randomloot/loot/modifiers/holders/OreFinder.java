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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = RandomLoot.MODID)
public class OreFinder implements HoldModifier {

	private String name;
	private float power;
	private final static String POWER = "power";

	static int maxTime = 10;
	static int time = 0;
	static int maxShulkerLife = 10;

	// Thread-safe lists for concurrent access between tick events and hold()
	private static final List<EntityShulker> shulkers = new CopyOnWriteArrayList<>();
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
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setFloat(POWER, power);

		tag.setString(NAME, name);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new OreFinder(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Detecting",
			tag.hasKey(POWER) ? tag.getFloat(POWER) : 4.0f);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String tagName() {
		return "detecting";
	}

	@Override
	public String color() {
		return TextFormatting.WHITE.getFriendlyName();
	}

	@Override
	public String description() {
		return "While holding the tool, ores around you will glow.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
	}

	@Override
	public boolean forTool(ToolType type) {
		return type.equals(ToolType.PICKAXE) || type.equals(ToolType.AXE) || type.equals(ToolType.SHOVEL);
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
				EntityShulker sh = shulkers.get(i);

				if (tick > maxShulkerLife
						|| sh.world.getBlockState(sh.getPosition()).getBlock() == Blocks.AIR) {
					sh.setPosition(0, -256, 0);
					sh.setHealth(0);
					shulkers.remove(i);
					timings.remove(i);
				}
			}
		}
	}

	@Override
	public void hold(ItemStack stack, World world, Entity holder) {
		int size = 10;
		for (int i = -size; i < size; i++) {
			for (int j = -size; j < size; j++) {
				for (int k = -size; k < size; k++) {
					BlockPos p = new BlockPos((int) (holder.posX + i), (int) (holder.posY + j),
							(int) (holder.posZ + k));
					Block b = world.getBlockState(p).getBlock();
					String blockName = b.getLocalizedName();

					if (blockName.toLowerCase().contains("ore")) {

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
