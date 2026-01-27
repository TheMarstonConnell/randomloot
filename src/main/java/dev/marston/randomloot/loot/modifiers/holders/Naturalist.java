package dev.marston.randomloot.loot.modifiers.holders;

import java.util.ArrayList;
import java.util.List;

import dev.marston.randomloot.loot.LootItem.ToolType;
import dev.marston.randomloot.loot.LootUtils;
import dev.marston.randomloot.loot.modifiers.HoldModifier;
import dev.marston.randomloot.loot.modifiers.Modifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;

public class Naturalist implements HoldModifier {

    private static final String LAST_UPDATE = "lastUpdate";
    private static final String LEVEL = "level";
    private static final int MAX_LEVEL = 3;
    private static final int SEARCH_RADIUS = 5;

    private String name;
    private int level;
    private long lastUpdateTick;
    private RandomSource random = RandomSource.create();

    public Naturalist() {
        this.name = "Naturalist";
        this.level = 1;
        this.lastUpdateTick = 0;
    }

    public Naturalist(String name, int level, long lastUpdateTick) {
        this.name = name;
        this.level = level;
        this.lastUpdateTick = lastUpdateTick;
    }

    private long getUpdateInterval() {
        // Level 1: 200 ticks (10s), Level 2: 140 ticks (7s), Level 3: 100 ticks (5s)
        return 260 - (level * 60);
    }

    @Override
    public String tagName() {
        return "naturalist";
    }

    @Override
    public String name() {
        if (level == 1) {
            return this.name;
        }
        return this.name + " " + LootUtils.roman(level);
    }

    @Override
    public boolean canLevel() {
        return level < MAX_LEVEL;
    }

    @Override
    public void levelUp() {
        this.level++;
    }

    @Override
    public String color() {
        return ChatFormatting.GREEN.getName();
    }

    @Override
    public String description() {
        float seconds = getUpdateInterval() / 20.0f;
        return "Bone meals nearby crops and saplings every " + String.format("%.0f", seconds) + " seconds";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        tag.putInt(LEVEL, level);
        tag.putLong(LAST_UPDATE, lastUpdateTick);
        return tag;
    }

    @Override
    public Modifier fromNBT(CompoundTag tag) {
        String name = tag.getStringOr(NAME, "Naturalist");
        int level = tag.getIntOr(LEVEL, 1);
        long lastUpdate = tag.getLongOr(LAST_UPDATE, 0L);
        return new Naturalist(name, level, lastUpdate);
    }

    @Override
    public Modifier clone() {
        return new Naturalist();
    }

    @Override
    public boolean forTool(ToolType type) {
        return true; // Works with all tool types
    }

    @Override
    public void writeToLore(List<Component> list, boolean shift) {
        list.add(Modifier.makeComp(this.name(), this.color()));
    }

    @Override
    public void hold(ItemStack stack, Level level, Entity holder) {
        if (level.isClientSide()) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long currentTick = level.getGameTime();
        if (currentTick - lastUpdateTick < getUpdateInterval()) {
            return;
        }

        // Find all bonemealable blocks nearby
        BlockPos holderPos = holder.blockPosition();
        List<BlockPos> bonemealableBlocks = new ArrayList<>();

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos checkPos = holderPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    Block block = state.getBlock();

                    if (block instanceof BonemealableBlock bonemealable) {
                        if (bonemealable.isValidBonemealTarget(level, checkPos, state)) {
                            bonemealableBlocks.add(checkPos);
                        }
                    }
                }
            }
        }

        // Bone meal a random one
        if (!bonemealableBlocks.isEmpty()) {
            BlockPos targetPos = bonemealableBlocks.get(random.nextInt(bonemealableBlocks.size()));
            BlockState state = level.getBlockState(targetPos);
            Block block = state.getBlock();

            if (block instanceof BonemealableBlock bonemealable) {
                if (bonemealable.isBonemealSuccess(level, random, targetPos, state)) {
                    bonemealable.performBonemeal(serverLevel, random, targetPos, state);

                    // Spawn particles
                    serverLevel.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                        10, 0.5, 0.5, 0.5, 0.0
                    );
                }
            }
        }

        // Update timestamp
        lastUpdateTick = currentTick;
        LootUtils.updateModifier(stack, this);
    }
}
