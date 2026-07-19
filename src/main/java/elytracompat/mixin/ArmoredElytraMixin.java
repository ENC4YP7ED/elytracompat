package elytracompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dorkix.armored.elytra.ArmoredElytra;
import elytracompat.ElytraCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * Widens Armored Elytra's hardcoded {@code stack.is(Items.ELYTRA)} checks to
 * accept tagged modded gliders (enderite elytra), and bridges the chestplate's
 * armor trim onto the combined item as a real TRIM component so Elytra Trims
 * renders it on the wings.
 */
@Mixin(value = ArmoredElytra.class, remap = false)
public abstract class ArmoredElytraMixin {

	// In MC 26.2 ItemStack.is(Item) erases to is(Object) in bytecode; the
	// tag/predicate overloads keep their own descriptors, so targeting the
	// Object descriptor hits exactly the `stack.is(Items.ELYTRA)` gates.
	@WrapOperation(method = { "createArmoredElytra", "isArmoredElytra" }, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z", remap = false))
	private static boolean elytracompat$widenElytraCheck(ItemStack stack, Object arg, Operation<Boolean> original) {
		return original.call(stack, arg)
				|| (arg == Items.ELYTRA && stack.is(ElytraCompat.ARMORABLE_GLIDERS));
	}

	@Inject(method = "createArmoredElytra", at = @At("RETURN"), cancellable = true)
	private static void elytracompat$bridgeTrim(ItemStack elytra, ItemStack armor, ContainerLevelAccess context,
			String newItemName, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack result = cir.getReturnValue();
		if (result == armor) return; // AE's early return for invalid inputs

		if (elytracompat.ElytraCompat.DEBUG_ICONS) {
			elytracompat.ElytraCompat.LOGGER.info("[icon-debug] combined item={} custom_model_data={}",
					net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()),
					result.get(DataComponents.CUSTOM_MODEL_DATA));
		}

		var trim = armor.get(DataComponents.TRIM);
		if (trim == null) return;

		// Real trim component: Elytra Trims renders it on the wings and vanilla
		// provides the proper tooltip lines.
		result.set(DataComponents.TRIM, trim);

		// Drop AE's faked trim tooltip (blank line + upgrade + pattern + material)
		// which would now show up twice.
		ItemLore lore = result.get(DataComponents.LORE);
		if (lore != null && lore.lines().size() >= 4) {
			result.set(DataComponents.LORE, new ItemLore(lore.lines().subList(4, lore.lines().size())));
		}
		cir.setReturnValue(result);
	}
}
