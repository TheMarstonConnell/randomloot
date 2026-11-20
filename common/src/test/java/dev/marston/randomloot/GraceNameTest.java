package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import dev.marston.randomloot.loot.modifiers.ModifierRegistry;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The registry's own Grace is named plain "Grace". It used to be named after a forger
 * rolled at launch, and whatever shows the registry's instance changed with it from one
 * launch to the next - the comment above its toggle in {@code randomloot-common.toml}
 * among them. Gear is another matter: a grant that knows its world still names that
 * world's forger ({@code TraitGrantTest}, {@code trait_grant_stamps_world_forger}).
 */
class GraceNameTest {

	@BeforeAll
	static void bootstrap() {
		McBootstrap.init();
	}

	@Test
	void theCommentAboveGracesConfigToggleNamesNoForger() {
		assertEquals("Grace", ModifierRegistry.DIRT_PLACE.name());

		ForgeConfigSpec.ValueSpec toggle = Config.SPEC.getSpec().get(List.of("Modifiers Enabled", "dirt_place_enabled"));
		assertEquals("should the Grace trait be enabled", toggle.getComment());
	}
}
