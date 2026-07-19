package elytracompat;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import dorkix.armored.elytra.ArmoredElytra;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Moves Elytra Trims decorations (trim, dye, banner patterns, effect flags)
 * between an armored elytra's outer stack and its stored source-item
 * snapshots, so decorations render on the wings and survive a grindstone
 * split.
 */
public final class CompatDecorations {
	/** Elytra Trims stores its effect flags as custom-data keys with this prefix. */
	private static final String ET_FLAG_PREFIX = "elytratrims:";

	private CompatDecorations() {}

	public static ItemStack parseStack(CompoundTag nbt, HolderLookup.Provider registries) {
		return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), nbt)
				.resultOrPartial().orElse(ItemStack.EMPTY);
	}

	public static Optional<CompoundTag> customDataCompound(ItemStack stack, String key) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(key);
	}

	public static ItemStack storedChestplate(ItemStack armoredElytra, HolderLookup.Provider registries) {
		return customDataCompound(armoredElytra, ArmoredElytra.CHESTPLATE_DATA.toString())
				.map(tag -> parseStack(tag, registries)).orElse(ItemStack.EMPTY);
	}

	/**
	 * Copies decorations from {@code outer} (the armored elytra as it is now)
	 * onto {@code target} (the rendered or restored source elytra).
	 *
	 * {@code chestplate} is the stored chestplate snapshot; decorations that
	 * came from the chestplate itself (its trim, its leather dye) are only
	 * copied when {@code includeChestplateDerived} is set - the renderer wants
	 * them on the wings, but a grindstone split must leave them on the
	 * chestplate instead of duplicating them onto the plain elytra.
	 *
	 * @return whether {@code target} was modified
	 */
	public static boolean applyOuterDecorations(ItemStack outer, ItemStack target, ItemStack chestplate,
			boolean includeChestplateDerived) {
		boolean changed = false;

		// Armor trim
		var outerTrim = outer.get(DataComponents.TRIM);
		var chestTrim = chestplate.get(DataComponents.TRIM);
		boolean chestDerivedTrim = outerTrim != null && outerTrim.equals(chestTrim);
		if (outerTrim != null && (includeChestplateDerived || !chestDerivedTrim)
				&& !outerTrim.equals(target.get(DataComponents.TRIM))) {
			target.set(DataComponents.TRIM, outerTrim);
			changed = true;
		}

		// Banner patterns (chestplates never carry these; always sync, including removal)
		var outerPatterns = outer.get(DataComponents.BANNER_PATTERNS);
		if (!Objects.equals(outerPatterns, target.get(DataComponents.BANNER_PATTERNS))) {
			setOrRemove(target, DataComponents.BANNER_PATTERNS, outerPatterns);
			changed = true;
		}

		// Dyed color: skip when it is exactly the color inherited from the chestplate
		var outerColor = outer.get(DataComponents.DYED_COLOR);
		var chestColor = chestplate.get(DataComponents.DYED_COLOR);
		boolean chestDerivedColor = outerColor != null && outerColor.equals(chestColor);
		if ((includeChestplateDerived || !chestDerivedColor)
				&& !Objects.equals(outerColor, target.get(DataComponents.DYED_COLOR))) {
			// never strip target's own color just because outer has none and chest has one
			if (outerColor != null || chestColor == null) {
				setOrRemove(target, DataComponents.DYED_COLOR, outerColor);
				changed = true;
			}
		}

		// Elytra Trims effect flags (glow, banner, gateway, animation) live in custom data
		CompoundTag outerData = outer.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		CompoundTag targetData = target.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		boolean dataChanged = false;
		Set<String> keys = new HashSet<>();
		for (String key : outerData.keySet())
			if (key.startsWith(ET_FLAG_PREFIX)) keys.add(key);
		for (String key : targetData.keySet())
			if (key.startsWith(ET_FLAG_PREFIX)) keys.add(key);
		for (String key : keys) {
			var outerValue = outerData.get(key);
			if (outerValue == null) {
				targetData.remove(key);
				dataChanged = true;
			} else if (!outerValue.equals(targetData.get(key))) {
				targetData.put(key, outerValue.copy());
				dataChanged = true;
			}
		}
		if (dataChanged) {
			if (targetData.isEmpty()) target.remove(DataComponents.CUSTOM_DATA);
			else target.set(DataComponents.CUSTOM_DATA, CustomData.of(targetData));
			changed = true;
		}

		return changed;
	}

	private static <T> void setOrRemove(ItemStack stack, DataComponentType<T> type, T value) {
		if (value == null) stack.remove(type);
		else stack.set(type, value);
	}
}
