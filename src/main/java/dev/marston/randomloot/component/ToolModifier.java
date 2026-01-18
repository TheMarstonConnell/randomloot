package dev.marston.randomloot.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class ToolModifier {

    private static final int MAX_MODIFIERS = 256;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != ToolModifier.class) {
            return false;
        }
        ToolModifier t = (ToolModifier) obj;

        return t.tags.equals(this.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tags);
    }

    private final Map<String, CompoundTag> tags;

    public Map<String, CompoundTag> getTags() {
        return new HashMap<>(this.tags);
    }

    public ToolModifier(Map<String, CompoundTag> tagIn) {
        // Enforce size limit and make immutable
        if (tagIn.size() > MAX_MODIFIERS) {
            this.tags = Collections.unmodifiableMap(new HashMap<>());
        } else {
            this.tags = Collections.unmodifiableMap(new HashMap<>(tagIn));
        }
    }

    public static final Codec<ToolModifier> CODEC = RecordCodecBuilder.create(
            builder -> builder.group(
                            Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC)
                                    .xmap(
                                            m -> m.size() <= MAX_MODIFIERS ? m : new HashMap<String, CompoundTag>(),
                                            m -> m
                                    )
                                    .fieldOf("tags")
                                    .forGetter(ToolModifier::getTags)
                    )
                    .apply(builder, ToolModifier::new)
    );





}
