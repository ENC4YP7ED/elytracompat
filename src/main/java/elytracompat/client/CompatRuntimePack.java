package elytracompat.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * A synthetic client resource pack, injected on top of the real pack stack,
 * that serves runtime-regenerated (pack-aware) versions of Armored Elytra's and
 * the compat mod's shifted icon textures. See {@link CompatTextureGen}.
 */
public class CompatRuntimePack implements PackResources {
	private static final Logger LOGGER = LoggerFactory.getLogger("ElytraCompatRuntimePack");
	private static final byte[] MISSING = new byte[0];

	public static final PackLocationInfo INFO = new PackLocationInfo(
			"elytracompat-generated", Component.literal("Elytra Compat Generated"),
			PackSource.BUILT_IN, Optional.empty());

	/**
	 * Item-model definitions we must override even though another mod's data pack
	 * would normally win by alphabetical order (Fabric sorts mod packs by id, and
	 * {@code enderitemod} sorts after {@code elytracompat}). Served from our own
	 * jar via the classloader at this pack's top priority. Value = classpath path.
	 */
	private static final Map<Identifier, String> ITEM_DEFS = itemDefs();

	private static Map<Identifier, String> itemDefs() {
		Map<Identifier, String> m = new LinkedHashMap<>();
		m.put(Identifier.fromNamespaceAndPath("minecraft", "items/elytra.json"),
				"/assets/minecraft/items/elytra.json");
		m.put(Identifier.fromNamespaceAndPath("enderitemod", "items/enderite_elytra_seperated.json"),
				"/assets/enderitemod/items/enderite_elytra_seperated.json");
		return m;
	}

	private final ResourceManager source;
	private final ConcurrentHashMap<Identifier, byte[]> cache = new ConcurrentHashMap<>();

	public CompatRuntimePack(ResourceManager source) {
		this.source = source;
		if (elytracompat.ElytraCompat.DEBUG_ICONS) {
			int ok = 0;
			for (Identifier id : CompatTextureGen.targets()) {
				if (bytesFor(id).length > 0) {
					ok++;
				}
			}
			int defs = 0;
			for (Identifier id : ITEM_DEFS.keySet()) {
				if (bytesFor(id).length > 0) {
					defs++;
				}
			}
			LOGGER.info("[icon-debug] runtime pack injected; regenerated {}/{} textures, {}/{} item-model overrides",
					ok, CompatTextureGen.targets().size(), defs, ITEM_DEFS.size());
		}
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) {
			exportForDebug();
		}
	}

	/** Dev-only: write every regenerated texture to run/.elytracompat so it can be inspected. */
	private void exportForDebug() {
		java.nio.file.Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve(".elytracompat");
		int ok = 0;
		for (Identifier id : CompatTextureGen.targets()) {
			byte[] bytes = bytesFor(id);
			if (bytes.length == 0) {
				continue;
			}
			try {
				java.nio.file.Path out = dir.resolve(id.getNamespace() + "/" + id.getPath());
				java.nio.file.Files.createDirectories(out.getParent());
				java.nio.file.Files.write(out, bytes);
				ok++;
			} catch (Exception e) {
				LOGGER.error("export failed {}", id, e);
			}
		}
		LOGGER.info("Regenerated {}/{} icon-backing textures", ok, CompatTextureGen.targets().size());
	}

	private byte[] bytesFor(Identifier target) {
		return cache.computeIfAbsent(target, id -> {
			try {
				String classpath = ITEM_DEFS.get(id);
				if (classpath != null) {
					try (InputStream in = CompatRuntimePack.class.getResourceAsStream(classpath)) {
						return in == null ? MISSING : in.readAllBytes();
					}
				}
				byte[] generated = CompatTextureGen.generate(source, id);
				return generated == null ? MISSING : generated;
			} catch (Exception e) {
				LOGGER.error("Failed to produce {}", id, e);
				return MISSING;
			}
		});
	}

	private boolean handles(Identifier id) {
		return ITEM_DEFS.containsKey(id) || CompatTextureGen.handles(id);
	}

	private static IoSupplier<InputStream> supplier(byte[] bytes) {
		return () -> new ByteArrayInputStream(bytes);
	}

	@Override
	public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
		if (type != PackType.CLIENT_RESOURCES || !handles(id)) {
			return null;
		}
		byte[] bytes = bytesFor(id);
		return bytes.length == 0 ? null : supplier(bytes);
	}

	@Override
	public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
		if (type != PackType.CLIENT_RESOURCES) {
			return;
		}
		for (Identifier id : CompatTextureGen.targets()) {
			emitIfMatches(id, namespace, path, output);
		}
		for (Identifier id : ITEM_DEFS.keySet()) {
			emitIfMatches(id, namespace, path, output);
		}
	}

	private void emitIfMatches(Identifier id, String namespace, String path, ResourceOutput output) {
		if (!id.getNamespace().equals(namespace) || !id.getPath().startsWith(path)) {
			return;
		}
		byte[] bytes = bytesFor(id);
		if (bytes.length > 0) {
			output.accept(id, supplier(bytes));
		}
	}

	@Override
	public Set<String> getNamespaces(PackType type) {
		return type == PackType.CLIENT_RESOURCES
				? Set.of("armored_elytra", "elytracompat", "minecraft", "enderitemod")
				: Set.of();
	}

	@Override
	public IoSupplier<InputStream> getRootResource(String... path) {
		return null;
	}

	@Override
	public <T> T getMetadataSection(MetadataSectionType<T> type) {
		return null;
	}

	@Override
	public PackLocationInfo location() {
		return INFO;
	}

	@Override
	public void close() {
	}
}
