package dev.marston.randomloot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marston.randomloot.gametest.GameTestBodies;
import dev.marston.randomloot.gametest.GameTestTable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import org.junit.jupiter.api.Test;

/**
 * The gametest table is the only place a shared gametest is listed, so a body that
 * never got a row would simply not run on either loader. These checks need no world.
 */
class GameTestTableTest {

	@Test
	void everyBodyHasARow() {
		// A body is a public static void method of GameTestBodies taking only the helper.
		// A row cannot be traced back to its method, so this compares counts.
		List<String> bodies = Arrays.stream(GameTestBodies.class.getDeclaredMethods())
				.filter(GameTestTableTest::isBody)
				.map(Method::getName)
				.sorted()
				.toList();

		assertEquals(bodies.size(), GameTestTable.rows().size(),
				"GameTestBodies has " + bodies.size() + " test bodies but the gametest table has "
						+ GameTestTable.rows().size() + " rows. Add a row to GameTestTable for the new body,"
						+ " or make it private if it is a helper. Bodies: " + bodies);
	}

	@Test
	void rowNamesAreUniqueIdentifierPaths() {
		Set<String> seen = new HashSet<>();
		for (GameTestTable.Row row : GameTestTable.rows()) {
			assertTrue(row.name().matches("[a-z0-9_]+"), "not a snake_case test name: " + row.name());
			assertTrue(seen.add(row.name()), "two rows share the name " + row.name());
			assertTrue(row.maxTicks() > 0, row.name() + " needs a positive tick limit");
		}
	}

	private static boolean isBody(Method method) {
		return Modifier.isPublic(method.getModifiers())
				&& Modifier.isStatic(method.getModifiers())
				&& method.getReturnType() == void.class
				&& Arrays.equals(method.getParameterTypes(), new Class<?>[] { GameTestHelper.class });
	}
}
