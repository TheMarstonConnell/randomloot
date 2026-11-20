package dev.marston.randomloot.loot.modifiers;

import net.minecraft.nbt.CompoundTag;

/**
 * One field a trait keeps on gear, declared where the field is: the key it is stored
 * under and the value a fresh trait starts with.
 *
 * <pre>
 * private final Stored&lt;Integer&gt; points = stored("points", 10);
 * </pre>
 *
 * <p>That line is the whole of the field's persistence. {@link AbstractModifier} writes
 * every declared field when the trait is saved and reads it back when the trait is
 * loaded; a key missing from the gear leaves the starting value in place. So a value
 * that is on old gear stays what it is, whatever the starting value becomes later.
 *
 * @param <T> the field's type: an int, a long or a float through
 *            {@link AbstractModifier#stored}, anything else with a reader and writer
 */
public final class Stored<T> {

	/** Reads a field out of a trait's tag, or returns {@code fallback} when it is not there. */
	@FunctionalInterface
	public interface Reader<T> {
		T read(CompoundTag tag, String key, T fallback);
	}

	/** Writes a field into a trait's tag. */
	@FunctionalInterface
	public interface Writer<T> {
		void write(CompoundTag tag, String key, T value);
	}

	private final String key;
	private final Reader<T> reader;
	private final Writer<T> writer;
	private T value;

	Stored(String key, T initial, Reader<T> reader, Writer<T> writer) {
		this.key = key;
		this.value = initial;
		this.reader = reader;
		this.writer = writer;
	}

	public T get() {
		return this.value;
	}

	public void set(T value) {
		this.value = value;
	}

	void save(CompoundTag tag) {
		this.writer.write(tag, this.key, this.value);
	}

	void load(CompoundTag tag) {
		this.value = this.reader.read(tag, this.key, this.value);
	}
}
