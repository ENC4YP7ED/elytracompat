package elytracompat.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import elytracompat.client.CompatRuntimePack;
import elytracompat.client.CompatTextureGen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Programmatic (non-screenshot) checks that the runtime resource pack is active
 * and produces pack-aware icon-backing textures. The gametest resources ship a
 * blue-overridden {@code minecraft:item/elytra}, so the regenerated
 * {@code armored_elytra:item/elytra} must be a blue, shifted copy of it.
 */
public class ElytraCompatRuntimePackTest implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		context.waitTick();
		context.runOnClient(client -> {
			ResourceManager rm = client.getResourceManager();

			check(rm.listPacks().anyMatch(p -> p instanceof CompatRuntimePack),
					"CompatRuntimePack was not injected into the client resource manager");

			for (Identifier target : CompatTextureGen.targets()) {
				byte[] bytes = read(rm, target);
				check(bytes != null && bytes.length > 0, "Backing texture missing / empty: " + target);
			}

			// Our item-model overrides must win over the mods that ship the same
			// files (armored-elytra / enderitemod), regardless of pack ordering.
			for (String[] def : new String[][] {
					{ "minecraft", "items/elytra.json" },
					{ "enderitemod", "items/enderite_elytra_seperated.json" } }) {
				byte[] bytes = read(rm, Identifier.fromNamespaceAndPath(def[0], def[1]));
				String json = bytes == null ? "" : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
				check(json.contains("minecraft:custom_model_data"),
						def[0] + ":" + def[1] + " is not our chestplate-aware override (another mod's model won)");
			}

			Identifier elytraTarget = Identifier.fromNamespaceAndPath("armored_elytra", "textures/item/elytra.png");
			Identifier elytraSource = Identifier.fromNamespaceAndPath("minecraft", "textures/item/elytra.png");
			BufferedImage served = decode(read(rm, elytraTarget));
			BufferedImage source = decode(read(rm, elytraSource));
			check(served != null && source != null, "Could not decode elytra textures");

			BufferedImage expected = CompatTextureGen.shiftToBottom(source);
			check(pixelsEqual(served, expected),
					"armored_elytra:item/elytra is not a shift-down of the active minecraft:item/elytra");
			check(isBlueDominant(served),
					"The blue elytra resource override did not propagate to the armored elytra backing texture");
		});
	}

	private static byte[] read(ResourceManager rm, Identifier id) {
		return rm.getResource(id).map(ElytraCompatRuntimePackTest::readAll).orElse(null);
	}

	private static byte[] readAll(Resource resource) {
		try (InputStream in = resource.open()) {
			return in.readAllBytes();
		} catch (Exception e) {
			return null;
		}
	}

	private static BufferedImage decode(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		try {
			return ImageIO.read(new ByteArrayInputStream(bytes));
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean pixelsEqual(BufferedImage a, BufferedImage b) {
		if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
			return false;
		}
		for (int y = 0; y < a.getHeight(); y++) {
			for (int x = 0; x < a.getWidth(); x++) {
				if (a.getRGB(x, y) != b.getRGB(x, y)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isBlueDominant(BufferedImage img) {
		long r = 0, g = 0, b = 0, n = 0;
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int argb = img.getRGB(x, y);
				if ((argb >>> 24) == 0) {
					continue;
				}
				r += (argb >> 16) & 0xFF;
				g += (argb >> 8) & 0xFF;
				b += argb & 0xFF;
				n++;
			}
		}
		return n > 0 && b > r && b > g;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
