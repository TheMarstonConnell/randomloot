package dev.marston.randomloot.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.marston.randomloot.RandomLoot;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


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
        this.tags = Collections.unmodifiableMap(new HashMap<>(enforceLimit(tagIn)));
    }

    /**
     * Caps the modifier map at {@link #MAX_MODIFIERS}. Oversized maps used to be replaced
     * with an empty one, silently stripping every trait off the tool; instead keep the
     * first MAX_MODIFIERS entries (sorted by name, so the cut is deterministic) and warn.
     */
    private static Map<String, CompoundTag> enforceLimit(Map<String, CompoundTag> tagIn) {
        if (tagIn.size() <= MAX_MODIFIERS) {
            return tagIn;
        }

        RandomLoot.LOGGER.warn("Tool has {} modifier entries (max {}); keeping the first {} sorted by name and dropping the rest.",
                tagIn.size(), MAX_MODIFIERS, MAX_MODIFIERS);

        Map<String, CompoundTag> trimmed = new HashMap<>();
        new TreeMap<>(tagIn).entrySet().stream().limit(MAX_MODIFIERS)
                .forEach(e -> trimmed.put(e.getKey(), e.getValue()));
        return trimmed;
    }

    public static final Codec<ToolModifier> CODEC = RecordCodecBuilder.create(
            builder -> builder.group(
                            Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC)
                                    .xmap(ToolModifier::enforceLimit, m -> m)
                                    .fieldOf("tags")
                                    .forGetter(ToolModifier::getTags)
                    )
                    .apply(builder, ToolModifier::new)
    );





}
