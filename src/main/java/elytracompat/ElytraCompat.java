package elytracompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ElytraCompat implements ModInitializer {
	public static final String MOD_ID = "elytracompat";
	public static final Logger LOGGER = LoggerFactory.getLogger("ElytraCompat");

	/** Diagnostic icon logging; toggle on with -Delytracompat.debugIcons=true. */
	public static final boolean DEBUG_ICONS =
			"true".equalsIgnoreCase(System.getProperty("elytracompat.debugIcons", "false"));

	/**
	 * Non-vanilla gliders that Armored Elytra should treat exactly like an
	 * elytra (currently: enderitemod:enderite_elytra_seperated).
	 */
	public static final TagKey<Item> ARMORABLE_GLIDERS = TagKey.create(Registries.ITEM,
			Identifier.fromNamespaceAndPath(MOD_ID, "armorable_gliders"));

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Vanilla elytra or a tagged modded glider. */
	public static boolean isAnyElytra(ItemStack stack) {
		return stack.is(Items.ELYTRA) || stack.is(ARMORABLE_GLIDERS);
	}

	/** Chest armor that is itself a glider (e.g. enderitemod:enderite_elytra). */
	public static boolean isGliderChestArmor(ItemStack stack) {
		return stack.is(ItemTags.CHEST_ARMOR) && stack.has(DataComponents.GLIDER);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Bridging Armored Elytra + Elytra Trims + Enderite Mod");
	}
}
