package elytracompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.enderitemc.enderitemod.misc.EnderiteElytraSpecialRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;

/**
 * The compat mod routes all "enderite chestplate + elytra" combining through
 * Armored Elytra's anvil, so the Enderite mod's own crafting-grid recipe (which
 * produces the merged {@code enderite_elytra} item) is disabled by making it
 * never match. It stays registered but inert, and - being a special recipe -
 * is not shown in the recipe book. This also removes the recipe's ability to
 * destroy an armored elytra's stored chestplate.
 */
@Mixin(value = EnderiteElytraSpecialRecipe.class, remap = false)
public abstract class EnderiteElytraSpecialRecipeMixin {

	@Inject(method = "matches", at = @At("HEAD"), cancellable = true)
	private void elytracompat$disableRecipe(CraftingInput input, Level world,
			CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
