package elytracompat.mixin.client;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import elytracompat.client.CompatRuntimePack;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

/**
 * Injects {@link CompatRuntimePack} on top of the client pack stack so it can
 * override Armored Elytra's icon textures with pack-aware regenerated versions.
 * The pack reads its sources from an inner MultiPackResourceManager built from
 * the real packs; a thread-local guard stops that inner construction from
 * re-injecting (which would recurse forever).
 */
@Mixin(value = MultiPackResourceManager.class, priority = 900)
public class MultiPackResourceManagerMixin {
	@Unique
	private static final ThreadLocal<Boolean> elytracompat$building = ThreadLocal.withInitial(() -> false);

	@Unique
	private static final boolean elytracompat$enabled =
			!"false".equalsIgnoreCase(System.getProperty("elytracompat.runtimePack", "true"));

	@ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
	private static List<PackResources> elytracompat$injectRuntimePack(List<PackResources> packs,
			@Local(argsOnly = true) PackType type) {
		if (!elytracompat$enabled || type != PackType.CLIENT_RESOURCES || elytracompat$building.get()) {
			return packs;
		}
		if (packs.stream().anyMatch(p -> p instanceof CompatRuntimePack)) {
			return packs;
		}
		// only inject once our mod's own resources are on the stack
		if (packs.stream().noneMatch(p -> p.getNamespaces(PackType.CLIENT_RESOURCES).contains("elytracompat"))) {
			return packs;
		}

		elytracompat$building.set(true);
		try {
			CompatRuntimePack runtime = new CompatRuntimePack(new MultiPackResourceManager(type, packs));
			List<PackResources> copy = new ArrayList<>(packs);
			copy.add(runtime);
			return copy;
		} finally {
			elytracompat$building.set(false);
		}
	}
}
