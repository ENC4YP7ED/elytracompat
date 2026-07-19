package elytracompat.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Regenerates the armored-elytra icon-backing textures from whatever textures
 * are currently active, so resource packs (and the correct per-mod textures)
 * show through on the combined item icon.
 *
 * Layout (mirrors how a chestplate would sit over an elytra):
 *  - elytra layer is shifted DOWN so its lowest pixels rest on the bottom edge
 *    of the item square;
 *  - chestplate + leather overlay + trim layers are shifted UP by one pixel from
 *    the normal item position, so the chestplate sits just above the elytra.
 *
 * Everything is read live from the {@link ResourceManager}, so it is fully
 * resource-pack aware - including the vanilla trim item template and palettes
 * used to recolour the trim overlays.
 */
public final class CompatTextureGen {
	private CompatTextureGen() {}

	private enum Mode { ELYTRA_BOTTOM, CHESTPLATE_UP }

	private record BaseRecipe(Identifier source, Mode mode) {}

	private record TrimRecipe(Identifier palette) {}

	/**
	 * Trim overlay template + palette key. Armored Elytra ships a chestplate
	 * trim template with a proper alpha shape (already shifted up by h/8); we
	 * reposition it to the +1px chestplate offset and recolour it through the
	 * vanilla palettes.
	 */
	private static final Identifier TRIM_TEMPLATE =
			Identifier.fromNamespaceAndPath("armored_elytra", "textures/item/chestplate_trim.png");
	private static final Identifier TRIM_KEY =
			Identifier.fromNamespaceAndPath("minecraft", "textures/trims/color_palettes/trim_palette.png");

	private static final Map<Identifier, BaseRecipe> BASES = new HashMap<>();
	private static final Map<Identifier, TrimRecipe> TRIMS = new HashMap<>();

	private static Identifier item(String ns, String path) {
		return Identifier.fromNamespaceAndPath(ns, "textures/item/" + path + ".png");
	}

	private static Identifier palette(String ns, String name) {
		return Identifier.fromNamespaceAndPath(ns, "textures/trims/color_palettes/" + name + ".png");
	}

	static {
		// --- elytra bases: shifted DOWN to rest on the bottom edge -------
		BASES.put(item("armored_elytra", "elytra"), new BaseRecipe(item("minecraft", "elytra"), Mode.ELYTRA_BOTTOM));
		BASES.put(item("elytracompat", "enderite_elytra_base"),
				new BaseRecipe(item("enderitemod", "enderite_elytra_seperated"), Mode.ELYTRA_BOTTOM));
		BASES.put(item("elytracompat", "enderite_elytra_broken_base"),
				new BaseRecipe(item("enderitemod", "enderite_elytra_seperated_broken"), Mode.ELYTRA_BOTTOM));

		// --- chestplates (+ leather overlay): shifted UP by one pixel -----
		for (String c : new String[] { "chainmail_chestplate", "copper_chestplate", "diamond_chestplate",
				"golden_chestplate", "iron_chestplate", "leather_chestplate", "netherite_chestplate" }) {
			BASES.put(item("armored_elytra", c), new BaseRecipe(item("minecraft", c), Mode.CHESTPLATE_UP));
		}
		BASES.put(item("armored_elytra", "leather_chestplate_overlay"),
				new BaseRecipe(item("minecraft", "leather_chestplate_overlay"), Mode.CHESTPLATE_UP));
		BASES.put(item("elytracompat", "enderite_chestplate"),
				new BaseRecipe(item("enderitemod", "enderite_chestplate"), Mode.CHESTPLATE_UP));

		// --- trim overlays: recoloured vanilla template, shifted UP -------
		for (String pal : new String[] { "amethyst", "copper", "copper_darker", "diamond", "diamond_darker",
				"emerald", "gold", "gold_darker", "iron", "iron_darker", "lapis", "netherite", "netherite_darker",
				"quartz", "redstone", "resin" }) {
			TRIMS.put(item("armored_elytra", "chestplate_trim_" + pal), new TrimRecipe(palette("minecraft", pal)));
		}
		TRIMS.put(item("elytracompat", "chestplate_trim_enderite"),
				new TrimRecipe(palette("enderitemod", "enderite")));
		TRIMS.put(item("elytracompat", "chestplate_trim_enderite_darker"),
				new TrimRecipe(palette("enderitemod", "enderite_darker")));
	}

	/** Every texture id this generator can produce. */
	public static Set<Identifier> targets() {
		Set<Identifier> all = new HashSet<>(BASES.keySet());
		all.addAll(TRIMS.keySet());
		return all;
	}

	public static boolean handles(Identifier id) {
		return BASES.containsKey(id) || TRIMS.containsKey(id);
	}

	/** @return PNG bytes for {@code target}, or null if it isn't ours / a source is missing. */
	public static byte[] generate(ResourceManager manager, Identifier target) throws IOException {
		BufferedImage out;
		BaseRecipe base = BASES.get(target);
		if (base != null) {
			BufferedImage src = read(manager, base.source());
			if (src == null) {
				return null;
			}
			out = base.mode() == Mode.ELYTRA_BOTTOM ? shiftToBottom(src) : shift(src, -onePixel(src));
		} else {
			TrimRecipe trim = TRIMS.get(target);
			if (trim == null) {
				return null;
			}
			BufferedImage template = read(manager, TRIM_TEMPLATE);
			BufferedImage key = read(manager, TRIM_KEY);
			BufferedImage palette = read(manager, trim.palette());
			if (template == null || key == null || palette == null) {
				return null;
			}
			// AE's template is shifted up by h/8; bring it down to the +1px offset.
			out = applyPalette(shift(template, onePixel(template)), key, palette);
		}
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(out, "PNG", bytes);
		return bytes.toByteArray();
	}

	// -- image helpers -----------------------------------------------------

	private static BufferedImage read(ResourceManager manager, Identifier id) throws IOException {
		var resource = manager.getResource(id);
		if (resource.isEmpty()) {
			return null;
		}
		try (InputStream in = resource.get().open()) {
			BufferedImage img = ImageIO.read(in);
			return img == null ? null : toArgb(img);
		}
	}

	private static BufferedImage toArgb(BufferedImage src) {
		if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
			return src;
		}
		BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		argb.getGraphics().drawImage(src, 0, 0, null);
		return argb;
	}

	private static int onePixel(BufferedImage img) {
		return Math.max(1, Math.round(img.getHeight() / 16.0f));
	}

	/** Shift down so the lowest opaque pixel lands on the bottom row. */
	public static BufferedImage shiftToBottom(BufferedImage src) {
		int w = src.getWidth();
		int h = src.getHeight();
		int lowest = -1;
		for (int y = h - 1; y >= 0 && lowest < 0; y--) {
			for (int x = 0; x < w; x++) {
				if ((src.getRGB(x, y) >>> 24) != 0) {
					lowest = y;
					break;
				}
			}
		}
		return shift(src, lowest < 0 ? 0 : (h - 1) - lowest);
	}

	/** Positive delta shifts content DOWN, negative UP. */
	public static BufferedImage shift(BufferedImage src, int delta) {
		int w = src.getWidth();
		int h = src.getHeight();
		BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			int srcY = y - delta;
			if (srcY < 0 || srcY >= h) {
				continue;
			}
			for (int x = 0; x < w; x++) {
				dst.setRGB(x, y, src.getRGB(x, srcY));
			}
		}
		return dst;
	}

	/** Recolour a grayscale trim template through the palette key (port of AE's applyPalette). */
	private static BufferedImage applyPalette(BufferedImage template, BufferedImage key, BufferedImage palette) {
		int keyN = Math.min(key.getWidth(), palette.getWidth());
		Map<Integer, Integer> grayToColor = new HashMap<>();
		for (int i = 0; i < keyN; i++) {
			grayToColor.put(key.getRGB(i, 0) & 0xFF, palette.getRGB(i, 0));
		}
		int w = template.getWidth();
		int h = template.getHeight();
		BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int argb = template.getRGB(x, y);
				int alpha = (argb >>> 24) & 0xFF;
				if (alpha == 0) {
					continue;
				}
				Integer mapped = grayToColor.get(argb & 0xFF);
				if (mapped != null) {
					int outAlpha = ((mapped >>> 24) & 0xFF) > 0 ? alpha : 0;
					dst.setRGB(x, y, (mapped & 0x00FFFFFF) | (outAlpha << 24));
				}
			}
		}
		return dst;
	}
}
