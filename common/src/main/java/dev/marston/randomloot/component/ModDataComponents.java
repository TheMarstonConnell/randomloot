package dev.marston.randomloot.component;

import dev.marston.randomloot.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import java.util.function.Supplier;


public class ModDataComponents {

    @SuppressWarnings("unchecked")
    public static final Supplier<DataComponentType<ToolModifier>> TOOL_MODIFIER =
            (Supplier<DataComponentType<ToolModifier>>) (Supplier<?>) Services.REG.register(Registries.DATA_COMPONENT_TYPE,
                    "tool_mod", () -> DataComponentType.<ToolModifier>builder().persistent(ToolModifier.CODEC).build());

    /** Classloads the class so the component registration above runs. */
    public static void init() {
    }
}
