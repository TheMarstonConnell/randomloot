package dev.marston.randomloot.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Isolated so Minecraft (a client-only class) is never classloaded on a dedicated server. */
final class FabricClientLevelGetter {

    private FabricClientLevelGetter() {
    }

    @Nullable
    static Level get() {
        return Minecraft.getInstance().level;
    }
}
