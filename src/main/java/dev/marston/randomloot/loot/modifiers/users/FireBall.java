package dev.marston.randomloot.loot.modifiers.users;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.modifiers.Modifier;
import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import dev.marston.randomloot.loot.modifiers.UseModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

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
	public NBTTagCompound toNBT() {

		NBTTagCompound tag = new NBTTagCompound();

		tag.setString(NAME, name);
		tag.setInteger(DAMAGE, damage);

		return tag;
	}

	@Override
	public Modifier fromNBT(NBTTagCompound tag) {
		return new FireBall(
			tag.hasKey(NAME) ? tag.getString(NAME) : "Flame Thrower",
			tag.hasKey(DAMAGE) ? tag.getInteger(DAMAGE) : 20);
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
		return TextFormatting.DARK_RED.getFriendlyName();
	}

	@Override
	public EnumActionResult use(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		return EnumActionResult.PASS;
	}

	@Override
	public String description() {
		return "Right clicking throws a fire ball.";
	}

	@Override
	public void writeToLore(List<String> list, boolean shift) {
		String comp = Modifier.formatText(this.name(), this.color());
		list.add(comp);
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
	public boolean use(World world, EntityPlayer player, EnumHand hand) {

		double d1 = 2.5D;
		Vec3d vec3 = player.getLookVec();

		EntityLargeFireball largefireball = new EntityLargeFireball(world, player, vec3.x, vec3.y, vec3.z);
		largefireball.explosionPower = 1;

		largefireball.setPosition(player.posX + vec3.x * d1, player.posY + player.getEyeHeight() + 0.5D, player.posZ + vec3.z * d1);

		world.spawnEntity(largefireball);

		ItemStack stack = player.getHeldItem(hand);
		stack.damageItem(this.damage, player);

		return true;
	}

	@Override
	public boolean useAnywhere() {
		return true;
	}
}
