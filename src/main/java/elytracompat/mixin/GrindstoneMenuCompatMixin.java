package elytracompat.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dorkix.armored.elytra.ArmoredElytra;
import elytracompat.CompatDecorations;
import elytracompat.ElytraCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Two jobs, both around Armored Elytra's grindstone splitting:
 *
 * 1. Before AE reads the stored source-item snapshots ({@code createResult}
 * HEAD), merge any decorations the player applied to the combined item
 * afterwards (Elytra Trims trims/dyes/banners/effects) into the stored elytra
 * snapshot, so splitting doesn't silently destroy them. Chestplate-derived
 * trim/color stay with the chestplate.
 *
 * 2. AE's own result-slot preview is gated on {@code is(Items.ELYTRA)}; mirror
 * it for tagged modded gliders (enderite elytra) so those armored elytras can
 * be split too (AE's generic take-handler does the rest).
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuCompatMixin extends AbstractContainerMenu {

	protected GrindstoneMenuCompatMixin(MenuType<?> type, int syncId) {
		super(type, syncId);
	}

	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Shadow
	@Final
	private Container resultSlots;

	@Shadow
	@Final
	Container repairSlots;

	@Inject(method = "createResult", at = @At("HEAD"))
	private void elytracompat$syncSnapshots(CallbackInfo ci) {
		for (int slot = 0; slot < 2; slot++) {
			elytracompat$syncSnapshot(repairSlots.getItem(slot));
		}
	}

	@Inject(method = "createResult", at = @At("RETURN"))
	private void elytracompat$showModdedGliderSplit(CallbackInfo ci) {
		for (int slot = 0; slot < 2; slot++) {
			ItemStack stack = repairSlots.getItem(slot);
			if (stack.is(ElytraCompat.ARMORABLE_GLIDERS) && ArmoredElytra.isArmoredElytra(stack)) {
				int resultSlot = slot;
				this.access.execute((world, blockPos) -> {
					ItemStack chestplate = CompatDecorations.storedChestplate(stack, world.registryAccess());
					if (!chestplate.isEmpty()) {
						this.resultSlots.setItem(resultSlot, chestplate);
					}
				});
				broadcastChanges();
				return;
			}
		}
	}

	private void elytracompat$syncSnapshot(ItemStack stack) {
		if (stack.isEmpty() || !ArmoredElytra.isArmoredElytra(stack)) {
			return;
		}
		this.access.execute((world, blockPos) -> {
			CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			var elytraNbt = customData.getCompound(ArmoredElytra.ELYTRA_DATA.toString());
			var chestNbt = customData.getCompound(ArmoredElytra.CHESTPLATE_DATA.toString());
			if (elytraNbt.isEmpty() || chestNbt.isEmpty()) {
				return;
			}

			ItemStack elytra = CompatDecorations.parseStack(elytraNbt.get(), world.registryAccess());
			ItemStack chestplate = CompatDecorations.parseStack(chestNbt.get(), world.registryAccess());
			if (elytra.isEmpty() || chestplate.isEmpty()) {
				return;
			}

			if (!CompatDecorations.applyOuterDecorations(stack, elytra, chestplate, false)) {
				return;
			}

			customData.put(ArmoredElytra.ELYTRA_DATA.toString(),
					ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, world.registryAccess()), elytra)
							.getOrThrow());
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
		});
	}
}
