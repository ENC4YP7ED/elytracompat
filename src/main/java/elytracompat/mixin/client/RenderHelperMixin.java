package elytracompat.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dorkix.armored.elytra.RenderHelper;
import elytracompat.CompatDecorations;
import elytracompat.ElytraCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client half of the bridge:
 *
 * 1. Widen the {@code is(Items.ELYTRA)} gates so armored elytras built on
 * tagged modded gliders get their chestplate / source-wings render swap.
 *
 * 2. Armored Elytra renders the wings from the stored *snapshot* elytra, so
 * anything Elytra Trims put on the combined item (trims, dyes, banners,
 * effects) would never show. Copy the outer stack's decorations — including
 * the chestplate-derived trim — onto the render stack.
 */
@Mixin(value = RenderHelper.class, remap = false)
public abstract class RenderHelperMixin {

	// See ArmoredElytraMixin: is(Item) erases to is(Object) in 26.2 bytecode.
	// Both RenderHelper methods gate on ItemStack.is(Items.ELYTRA); the bundle
	// checks use ItemStackTemplate.is(...), a different owner, so they are safe.
	@WrapOperation(method = { "modifyStackWithArmor", "modifyStackWithElytra" }, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z", remap = false))
	private static boolean elytracompat$widenElytraCheck(ItemStack stack, Object arg, Operation<Boolean> original) {
		return original.call(stack, arg)
				|| (arg == Items.ELYTRA && stack.is(ElytraCompat.ARMORABLE_GLIDERS));
	}

	@Inject(method = "modifyStackWithElytra", at = @At("RETURN"), cancellable = true)
	private static void elytracompat$decorateRenderStack(ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack render = cir.getReturnValue();
		if (render == stack || render == null || render.isEmpty()) {
			return; // no armored-elytra swap happened
		}
		var player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}

		ItemStack chestplate = CompatDecorations.storedChestplate(stack, player.registryAccess());
		CompatDecorations.applyOuterDecorations(stack, render, chestplate, true);
		cir.setReturnValue(render);
	}
}
