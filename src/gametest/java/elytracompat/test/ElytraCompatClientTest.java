package elytracompat.test;

import dorkix.armored.elytra.ArmoredElytra;
import net.enderitemc.enderitemod.EnderiteMod;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;

/**
 * Builds real armored elytras through Armored Elytra's own createArmoredElytra
 * (server side, exactly as an anvil would), places them in the player's
 * inventory, and screenshots the inventory so the icons can be inspected.
 */
public class ElytraCompatClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(20);

			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ContainerLevelAccess access = ContainerLevelAccess.create(level, BlockPos.ZERO);
				var inv = player.getInventory();

				Holder<net.minecraft.world.item.equipment.trim.TrimMaterial> gold =
						level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.GOLD);
				Holder<net.minecraft.world.item.equipment.trim.TrimPattern> sentry =
						level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY);
				ArmorTrim goldTrim = new ArmorTrim(gold, sentry);

				// controls: AE's own combos (diamond, netherite chestplate)
				inv.setItem(0, new ItemStack(Items.ELYTRA));
				inv.setItem(1, armored(new ItemStack(Items.ELYTRA), new ItemStack(Items.DIAMOND_CHESTPLATE), access));
				inv.setItem(2, armored(new ItemStack(Items.ELYTRA), new ItemStack(Items.NETHERITE_CHESTPLATE), access));
				// enderite chestplate on a vanilla elytra (plain + gold-trimmed)
				inv.setItem(3, armored(new ItemStack(Items.ELYTRA),
						new ItemStack(EnderiteMod.ENDERITE_CHESTPLATE.get()), access));
				ItemStack trimmedEnderiteChest = new ItemStack(EnderiteMod.ENDERITE_CHESTPLATE.get());
				trimmedEnderiteChest.set(DataComponents.TRIM, goldTrim);
				inv.setItem(4, armored(new ItemStack(Items.ELYTRA), trimmedEnderiteChest, access));
				// vanilla chestplate carrying an enderite trim
				ItemStack ironEnderiteTrim = new ItemStack(Items.IRON_CHESTPLATE);
				Holder<net.minecraft.world.item.equipment.trim.TrimMaterial> enderiteMat =
						level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL)
								.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.TRIM_MATERIAL,
										net.minecraft.resources.Identifier.fromNamespaceAndPath("enderitemod", "enderite")));
				ironEnderiteTrim.set(DataComponents.TRIM, new ArmorTrim(enderiteMat, sentry));
				inv.setItem(5, armored(new ItemStack(Items.ELYTRA), ironEnderiteTrim, access));
				// enderite (seperated) elytra as the base + chestplates
				inv.setItem(6, armored(new ItemStack(EnderiteMod.ENDERITE_ELYTRA_SEPERATED.get()),
						new ItemStack(Items.NETHERITE_CHESTPLATE), access));
				inv.setItem(7, armored(new ItemStack(EnderiteMod.ENDERITE_ELYTRA_SEPERATED.get()),
						trimmedEnderiteChest.copy(), access));
				// reference: plain enderite elytra
				inv.setItem(8, new ItemStack(EnderiteMod.ENDERITE_ELYTRA_SEPERATED.get()));

				player.inventoryMenu.broadcastChanges();
			});

			context.waitTicks(15);
			context.runOnClient(client ->
					client.setScreenAndShow(new InventoryScreen(client.player)));
			context.waitTicks(10);
			context.takeScreenshot("elytracompat-icons");
		}
	}

	private static ItemStack armored(ItemStack elytra, ItemStack chestplate, ContainerLevelAccess access) {
		return ArmoredElytra.createArmoredElytra(elytra, chestplate, access, null);
	}
}
