package dev.marston.randomloot;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Boots vanilla's static registries for unit tests that touch a Minecraft class with
 * registry-backed statics (Attributes, BuiltInRegistries). The newer branches get this
 * from ModDevGradle's NeoForge unit-test harness, which has no legacy-Forge equivalent,
 * so tests that need it call {@link #init()} from a {@code @BeforeAll}.
 */
final class McBootstrap {

	private McBootstrap() {
	}

	static synchronized void init() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}
}
