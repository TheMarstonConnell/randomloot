package dev.marston.randomloot.component;

import dev.marston.randomloot.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import java.util.function.Supplier;


public class ModDataComponents {

    // ignoreSwapAnimation: this component changes on nearly every use (XP per block and
    // per hit, trait state), and the client replays the hand re-equip animation whenever
    // a component on the held stack changes unless its type opts out, as vanilla's
    // "damage" does. Swapping between two different pieces of gear still animates: their
    // name, attributes and max damage differ.
    public static final Supplier<DataComponentType<ToolModifier>> TOOL_MODIFIER =
            Services.REG.register(Registries.DATA_COMPONENT_TYPE, "tool_mod",
                    () -> DataComponentType.<ToolModifier>builder().persistent(ToolModifier.CODEC)
                            .ignoreSwapAnimation().build());

    /** Classloads the class so the component registration above runs. */
    public static void init() {
    }
}
