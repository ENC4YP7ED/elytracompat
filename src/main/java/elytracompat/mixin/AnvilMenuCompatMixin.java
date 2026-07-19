package elytracompat.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dorkix.armored.elytra.ArmoredElytra;
import elytracompat.ElytraCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Runs after Armored Elytra's own anvil hook (priority 2000 → applied later →
 * our RETURN callback executes after theirs).
 *
 * Adds: combining chestplates onto tagged modded gliders (enderite elytra).
 * Removes: Armored Elytra treating a glider chest-armor item (the merged
 * enderite elytra) as a chestplate to fuse onto another elytra.
 */
@Mixin(value = AnvilMenu.class, priority = 2000)
public abstract class AnvilMenuCompatMixin extends ItemCombinerMenu {

	public AnvilMenuCompatMixin(@Nullable MenuType<?> type, int syncId, Inventory playerInventory,
			ContainerLevelAccess context, ItemCombinerMenuSlotDefinition forgingSlotsManager) {
		super(type, syncId, playerInventory, context, forgingSlotsManager);
	}

	@Shadow
	@Final
	private DataSlot cost;

	@Shadow
	private String itemName;

	@Inject(method = "createResult", at = @At("RETURN"))
	private void elytracompat$handleModdedGliders(CallbackInfo ci) {
		var first = inputSlots.getItem(0);
		var second = inputSlots.getItem(1);

		if (elytracompat$blockGliderAsChestplate(first, second)
				|| elytracompat$blockGliderAsChestplate(second, first)) {
			return;
		}

		if (!elytracompat$tryCombine(first, second)) {
			elytracompat$tryCombine(second, first);
		}
	}

	/**
	 * AE combines elytra + anything in #minecraft:chest_armor - including the
	 * enderite elytra, which is a full glider of its own. Un-set that result;
	 * merging wings into wings is the enderite crafting recipe's job.
	 */
	private boolean elytracompat$blockGliderAsChestplate(ItemStack elytra, ItemStack armor) {
		if (!ElytraCompat.isAnyElytra(elytra) || !ElytraCompat.isGliderChestArmor(armor)) {
			return false;
		}
		if (!resultSlots.getItem(0).isEmpty()) {
			resultSlots.setItem(0, ItemStack.EMPTY);
			cost.set(0);
			broadcastChanges();
		}
		return true;
	}

	private boolean elytracompat$tryCombine(ItemStack glider, ItemStack armor) {
		if (!glider.is(ElytraCompat.ARMORABLE_GLIDERS)
				|| !armor.is(ItemTags.CHEST_ARMOR)
				|| armor.has(DataComponents.GLIDER)) {
			return false;
		}
		// no infinite nesting
		if (ArmoredElytra.isArmoredElytra(glider)) {
			return false;
		}

		resultSlots.setItem(0, ArmoredElytra.createArmoredElytra(glider, armor, this.access, itemName));
		cost.set(1);
		broadcastChanges();
		return true;
	}
}
