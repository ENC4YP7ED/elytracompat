package elytracompat.test;

import java.util.List;

import dorkix.armored.elytra.ArmoredElytra;
import elytracompat.CompatDecorations;
import elytracompat.ElytraCompat;
import net.enderitemc.enderitemod.EnderiteMod;
import net.enderitemc.enderitemod.misc.EnderiteElytraSpecialRecipe;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.GameType;

public class ElytraCompatGameTests {

	private static ArmorTrim trim(GameTestHelper ctx, ResourceKey<TrimMaterial> material,
			ResourceKey<TrimPattern> pattern) {
		Holder<TrimMaterial> mat = ctx.getLevel().registryAccess()
				.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(material);
		Holder<TrimPattern> pat = ctx.getLevel().registryAccess()
				.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(pattern);
		return new ArmorTrim(mat, pat);
	}

	private static AnvilMenu anvil(GameTestHelper ctx, Player player) {
		return new AnvilMenu(1, player.getInventory(),
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))));
	}

	private static GrindstoneMenu grindstone(GameTestHelper ctx, Player player) {
		return new GrindstoneMenu(1, player.getInventory(),
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))));
	}

	private static void assertTrue(GameTestHelper ctx, boolean condition, String message) {
		ctx.assertTrue(condition, Component.nullToEmpty(message));
	}

	// --- anvil combining -------------------------------------------------

	@GameTest
	public void combineElytraWithTrimmedEnderiteChestplate(GameTestHelper ctx) {
		Player player = ctx.makeMockPlayer(GameType.SURVIVAL);
		AnvilMenu menu = anvil(ctx, player);

		ItemStack chest = new ItemStack(EnderiteMod.ENDERITE_CHESTPLATE.get());
		ArmorTrim chestTrim = trim(ctx, TrimMaterials.QUARTZ, TrimPatterns.SENTRY);
		chest.set(DataComponents.TRIM, chestTrim);

		menu.getSlot(0).set(new ItemStack(Items.ELYTRA));
		menu.getSlot(1).set(chest);

		ItemStack result = menu.getSlot(2).getItem();
		assertTrue(ctx, !result.isEmpty(), "No anvil result for elytra + enderite chestplate");
		assertTrue(ctx, ArmoredElytra.isArmoredElytra(result), "Result is not an armored elytra");
		assertTrue(ctx, result.is(Items.ELYTRA), "Result should still be a vanilla elytra item");
		assertTrue(ctx, chestTrim.equals(result.get(DataComponents.TRIM)),
				"Chestplate trim was not bridged onto the combined elytra (Elytra Trims would not render it)");

		ItemStack storedChest = CompatDecorations.storedChestplate(result, ctx.getLevel().registryAccess());
		assertTrue(ctx, storedChest.is(EnderiteMod.ENDERITE_CHESTPLATE.get()),
				"Stored chestplate snapshot is missing or wrong");
		ctx.succeed();
	}

	@GameTest
	public void combineEnderiteElytraWithNetheriteChestplate(GameTestHelper ctx) {
		Player player = ctx.makeMockPlayer(GameType.SURVIVAL);
		AnvilMenu menu = anvil(ctx, player);

		ItemStack glider = new ItemStack(EnderiteMod.ENDERITE_ELYTRA_SEPERATED.get());
		assertTrue(ctx, glider.is(ElytraCompat.ARMORABLE_GLIDERS),
				"enderite_elytra_seperated is not in elytracompat:armorable_gliders (tag not loaded?)");

		menu.getSlot(0).set(glider);
		menu.getSlot(1).set(new ItemStack(Items.NETHERITE_CHESTPLATE));

		ItemStack result = menu.getSlot(2).getItem();
		assertTrue(ctx, !result.isEmpty(), "No anvil result for enderite elytra + netherite chestplate");
		assertTrue(ctx, result.is(EnderiteMod.ENDERITE_ELYTRA_SEPERATED.get()),
				"Combined item should still be the enderite elytra");
		assertTrue(ctx, ArmoredElytra.isArmoredElytra(result),
				"Combined enderite elytra is not recognized as an armored elytra");
		assertTrue(ctx, result.getMaxDamage() >= new ItemStack(Items.NETHERITE_CHESTPLATE).getMaxDamage(),
				"Combined durability was not upgraded");

		ItemStack storedChest = CompatDecorations.storedChestplate(result, ctx.getLevel().registryAccess());
		assertTrue(ctx, storedChest.is(Items.NETHERITE_CHESTPLATE), "Stored chestplate snapshot is wrong");
		ctx.succeed();
	}

	@GameTest
	public void refuseCombiningGliderAsChestplate(GameTestHelper ctx) {
		Player player = ctx.makeMockPlayer(GameType.SURVIVAL);
		AnvilMenu menu = anvil(ctx, player);

		// enderite_elytra (the merged chestplate+wings item) is chest armor AND a
		// glider - fusing it onto another elytra must not produce a result
		menu.getSlot(0).set(new ItemStack(Items.ELYTRA));
		menu.getSlot(1).set(new ItemStack(EnderiteMod.ENDERITE_ELYTRA.get()));

		assertTrue(ctx, menu.getSlot(2).getItem().isEmpty(),
				"Anvil combined a full enderite elytra onto a vanilla elytra (should be blocked)");
		ctx.succeed();
	}

	@GameTest
	public void refuseDoubleArmoring(GameTestHelper ctx) {
		Player player = ctx.makeMockPlayer(GameType.SURVIVAL);
		AnvilMenu menu = anvil(ctx, player);

		ItemStack armored = ArmoredElytra.createArmoredElytra(
				new ItemStack(EnderiteMod.ENDERITE_ELYTRA_SEPERATED.get()),
				new ItemStack(Items.IRON_CHESTPLATE),
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))), null);
		assertTrue(ctx, ArmoredElytra.isArmoredElytra(armored), "Setup: combine failed");

		menu.getSlot(0).set(armored);
		menu.getSlot(1).set(new ItemStack(Items.DIAMOND_CHESTPLATE));

		ItemStack result = menu.getSlot(2).getItem();
		assertTrue(ctx, result.isEmpty() || !CompatDecorations
				.storedChestplate(result, ctx.getLevel().registryAccess()).is(Items.DIAMOND_CHESTPLATE),
				"An already-armored enderite elytra was armored again");
		ctx.succeed();
	}

	// --- enderite crafting recipe disabled -------------------------------

	@GameTest
	public void enderiteElytraRecipeIsDisabled(GameTestHelper ctx) {
		// Combining enderite chestplate + elytra must only work via Armored
		// Elytra's anvil now, so the Enderite crafting-grid recipe never matches.
		EnderiteElytraSpecialRecipe recipe = new EnderiteElytraSpecialRecipe();

		CraftingInput plainInput = CraftingInput.of(1, 2,
				List.of(new ItemStack(Items.ELYTRA), new ItemStack(EnderiteMod.ENDERITE_CHESTPLATE.get())));
		assertTrue(ctx, !recipe.matches(plainInput, ctx.getLevel()),
				"Enderite elytra crafting recipe still matches (should be disabled - combine via anvil only)");

		ItemStack armored = ArmoredElytra.createArmoredElytra(
				new ItemStack(Items.ELYTRA), new ItemStack(Items.NETHERITE_CHESTPLATE),
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))), null);
		CraftingInput armoredInput = CraftingInput.of(1, 2,
				List.of(armored, new ItemStack(EnderiteMod.ENDERITE_CHESTPLATE.get())));
		assertTrue(ctx, !recipe.matches(armoredInput, ctx.getLevel()),
				"Enderite elytra crafting recipe still matches an armored elytra");
		ctx.succeed();
	}

	// --- grindstone splitting --------------------------------------------

	@GameTest
	public void splitPreservesPlayerDecorations(GameTestHelper ctx) {
		Player player = ctx.makeMockPlayer(GameType.SURVIVAL);

		ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
		ArmorTrim chestTrim = trim(ctx, TrimMaterials.QUARTZ, TrimPatterns.SENTRY);
		chest.set(DataComponents.TRIM, chestTrim);

		ItemStack armored = ArmoredElytra.createArmoredElytra(
				new ItemStack(Items.ELYTRA), chest,
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))), null);
		assertTrue(ctx, ArmoredElytra.isArmoredElytra(armored), "Setup: combine failed");

		// simulate what Elytra Trims recipes do to the combined item afterwards:
		ArmorTrim playerTrim = trim(ctx, TrimMaterials.AMETHYST, TrimPatterns.SILENCE);
		armored.set(DataComponents.TRIM, playerTrim);
		armored.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFF00FF));
		var data = armored.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		data.putBoolean("elytratrims:glow", true);
		armored.set(DataComponents.CUSTOM_DATA, CustomData.of(data));

		GrindstoneMenu menu = grindstone(ctx, player);
		menu.getSlot(0).set(armored);

		ItemStack preview = menu.getSlot(2).getItem();
		assertTrue(ctx, preview.is(Items.IRON_CHESTPLATE), "Grindstone result preview is not the chestplate");

		menu.getSlot(2).onTake(player, preview);

		ItemStack restored = menu.getSlot(0).getItem();
		assertTrue(ctx, restored.is(Items.ELYTRA) && !ArmoredElytra.isArmoredElytra(restored),
				"Input slot does not hold the restored plain elytra");
		assertTrue(ctx, playerTrim.equals(restored.get(DataComponents.TRIM)),
				"Player-applied trim was destroyed by the split");
		assertTrue(ctx, new DyedItemColor(0xFF00FF).equals(restored.get(DataComponents.DYED_COLOR)),
				"Player-applied dye was destroyed by the split");
		var restoredData = restored.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		assertTrue(ctx, restoredData.getBooleanOr("elytratrims:glow", false),
				"Elytra Trims glow effect was destroyed by the split");
		assertTrue(ctx, chestTrim.equals(preview.get(DataComponents.TRIM)),
				"Chestplate lost its own trim in the split");
		ctx.succeed();
	}

	@GameTest
	public void splitDoesNotDuplicateChestplateTrim(GameTestHelper ctx) {
		Player player = ctx.makeMockPlayer(GameType.SURVIVAL);

		ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
		ArmorTrim chestTrim = trim(ctx, TrimMaterials.REDSTONE, TrimPatterns.VEX);
		chest.set(DataComponents.TRIM, chestTrim);

		ItemStack armored = ArmoredElytra.createArmoredElytra(
				new ItemStack(Items.ELYTRA), chest,
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))), null);
		assertTrue(ctx, chestTrim.equals(armored.get(DataComponents.TRIM)), "Setup: trim was not bridged");

		GrindstoneMenu menu = grindstone(ctx, player);
		menu.getSlot(0).set(armored);

		ItemStack preview = menu.getSlot(2).getItem();
		assertTrue(ctx, preview.is(Items.NETHERITE_CHESTPLATE), "Grindstone result preview is not the chestplate");
		menu.getSlot(2).onTake(player, preview);

		ItemStack restored = menu.getSlot(0).getItem();
		assertTrue(ctx, restored.is(Items.ELYTRA), "Input slot does not hold the restored elytra");
		assertTrue(ctx, restored.get(DataComponents.TRIM) == null,
				"Chestplate-derived trim leaked onto the split-off elytra");
		assertTrue(ctx, chestTrim.equals(preview.get(DataComponents.TRIM)), "Chestplate lost its trim");
		ctx.succeed();
	}

	@GameTest
	public void splitEnderiteBasedArmoredElytra(GameTestHelper ctx) {
		Player player = ctx.makeMockPlayer(GameType.SURVIVAL);

		ItemStack armored = ArmoredElytra.createArmoredElytra(
				new ItemStack(EnderiteMod.ENDERITE_ELYTRA_SEPERATED.get()),
				new ItemStack(Items.DIAMOND_CHESTPLATE),
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))), null);
		assertTrue(ctx, ArmoredElytra.isArmoredElytra(armored), "Setup: combine failed");

		GrindstoneMenu menu = grindstone(ctx, player);
		menu.getSlot(0).set(armored);

		ItemStack preview = menu.getSlot(2).getItem();
		assertTrue(ctx, preview.is(Items.DIAMOND_CHESTPLATE),
				"Grindstone shows no chestplate for an enderite-based armored elytra");

		menu.getSlot(2).onTake(player, preview);

		ItemStack restored = menu.getSlot(0).getItem();
		assertTrue(ctx, restored.is(EnderiteMod.ENDERITE_ELYTRA_SEPERATED.get()),
				"Splitting did not restore the enderite elytra");
		assertTrue(ctx, !ArmoredElytra.isArmoredElytra(restored), "Restored enderite elytra still has AE data");
		ctx.succeed();
	}

	// --- elytra trims tag bridging ----------------------------------------

	@GameTest
	public void enderiteElytrasAreDecoratable(GameTestHelper ctx) {
		var decoratable = net.minecraft.tags.TagKey.create(Registries.ITEM,
				net.minecraft.resources.Identifier.fromNamespaceAndPath("elytratrims", "decoratable"));
		assertTrue(ctx, new ItemStack(EnderiteMod.ENDERITE_ELYTRA_SEPERATED.get()).is(decoratable),
				"enderite_elytra_seperated is not decoratable by Elytra Trims");
		assertTrue(ctx, new ItemStack(EnderiteMod.ENDERITE_ELYTRA.get()).is(decoratable),
				"enderite_elytra is not decoratable by Elytra Trims");
		assertTrue(ctx, new ItemStack(Items.ELYTRA).is(decoratable),
				"vanilla elytra lost its decoratable tag (compat data broke it)");
		ctx.succeed();
	}

	// --- client render decoration path ------------------------------------

	/**
	 * The client render mixin swaps in the stored source elytra, then calls
	 * {@link CompatDecorations#applyOuterDecorations} with
	 * includeChestplateDerived=true so the chestplate's own trim shows on the
	 * wings. Exercise that exact call on the server (no GL needed).
	 */
	@GameTest
	public void renderPathCopiesChestplateTrimOntoWings(GameTestHelper ctx) {
		ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
		ArmorTrim chestTrim = trim(ctx, TrimMaterials.GOLD, TrimPatterns.EYE);
		chest.set(DataComponents.TRIM, chestTrim);

		ItemStack armored = ArmoredElytra.createArmoredElytra(
				new ItemStack(Items.ELYTRA), chest,
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))), null);

		// what RenderHelper.modifyStackWithElytra returns before our @Inject runs:
		ItemStack renderStack = CompatDecorations.customDataCompound(armored,
				dorkix.armored.elytra.ArmoredElytra.ELYTRA_DATA.toString())
				.map(tag -> CompatDecorations.parseStack(tag, ctx.getLevel().registryAccess()))
				.orElse(ItemStack.EMPTY);
		assertTrue(ctx, renderStack.is(Items.ELYTRA) && renderStack.get(DataComponents.TRIM) == null,
				"Setup: stored elytra snapshot is wrong");

		ItemStack storedChest = CompatDecorations.storedChestplate(armored, ctx.getLevel().registryAccess());
		boolean changed = CompatDecorations.applyOuterDecorations(armored, renderStack, storedChest, true);

		assertTrue(ctx, changed, "Render path made no change to the wing render stack");
		assertTrue(ctx, chestTrim.equals(renderStack.get(DataComponents.TRIM)),
				"Chestplate trim was not copied onto the rendered wings");
		ctx.succeed();
	}

	@GameTest
	public void renderPathCopiesPlayerBannerOntoWings(GameTestHelper ctx) {
		ItemStack armored = ArmoredElytra.createArmoredElytra(
				new ItemStack(Items.ELYTRA), new ItemStack(Items.IRON_CHESTPLATE),
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))), null);

		// player applies an Elytra Trims glow flag to the combined item
		var data = armored.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		data.putBoolean("elytratrims:gateway", true);
		armored.set(DataComponents.CUSTOM_DATA, CustomData.of(data));

		ItemStack renderStack = new ItemStack(Items.ELYTRA);
		ItemStack storedChest = CompatDecorations.storedChestplate(armored, ctx.getLevel().registryAccess());
		CompatDecorations.applyOuterDecorations(armored, renderStack, storedChest, true);

		var renderData = renderStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		assertTrue(ctx, renderData.getBooleanOr("elytratrims:gateway", false),
				"Elytra Trims effect flag was not carried onto the rendered wings");
		ctx.succeed();
	}

	// --- Armored Elytra vanilla path still intact -------------------------

	@GameTest
	public void combineVanillaElytraWithVanillaChestplate(GameTestHelper ctx) {
		Player player = ctx.makeMockPlayer(GameType.SURVIVAL);
		AnvilMenu menu = anvil(ctx, player);
		menu.getSlot(0).set(new ItemStack(Items.ELYTRA));
		menu.getSlot(1).set(new ItemStack(Items.IRON_CHESTPLATE));

		ItemStack result = menu.getSlot(2).getItem();
		assertTrue(ctx, ArmoredElytra.isArmoredElytra(result),
				"Compat mixin broke Armored Elytra's own vanilla elytra + chestplate combine");
		assertTrue(ctx, result.is(Items.ELYTRA), "Vanilla armored elytra is no longer an elytra item");
		assertTrue(ctx, CompatDecorations.storedChestplate(result, ctx.getLevel().registryAccess())
				.is(Items.IRON_CHESTPLATE), "Stored chestplate is not the iron chestplate");
		ctx.succeed();
	}

	@GameTest
	public void combineBridgesTrimAndStripsFakedLore(GameTestHelper ctx) {
		ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
		ArmorTrim chestTrim = trim(ctx, TrimMaterials.EMERALD, TrimPatterns.WILD);
		chest.set(DataComponents.TRIM, chestTrim);

		ItemStack armored = ArmoredElytra.createArmoredElytra(new ItemStack(Items.ELYTRA), chest,
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))), null);

		assertTrue(ctx, chestTrim.equals(armored.get(DataComponents.TRIM)),
				"Chestplate trim was not bridged onto the combined elytra as a real TRIM component");
		var lore = armored.get(DataComponents.LORE);
		// AE without a faked-trim block leaves 3 lore lines (blank, "With chestplate:", name);
		// the faked trim block would add 4 more. Our mixin must strip those 4.
		assertTrue(ctx, lore != null && lore.lines().size() == 3,
				"Faked trim lore was not stripped (expected 3 lore lines, got "
						+ (lore == null ? "null" : lore.lines().size()) + ")");
		ctx.succeed();
	}

	// --- recipe disabled through the real recipe manager ------------------

	@GameTest
	public void noCraftingRecipeCombinesEnderiteChestplateAndElytra(GameTestHelper ctx) {
		CraftingInput input = CraftingInput.of(1, 2,
				List.of(new ItemStack(Items.ELYTRA), new ItemStack(EnderiteMod.ENDERITE_CHESTPLATE.get())));
		boolean anyMatch = ctx.getLevel().getServer().getRecipeManager().getRecipes().stream()
				.map(holder -> holder.value())
				.filter(recipe -> recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe)
				.anyMatch(recipe -> ((net.minecraft.world.item.crafting.CraftingRecipe) recipe)
						.matches(input, ctx.getLevel()));
		assertTrue(ctx, !anyMatch,
				"A crafting recipe still turns enderite chestplate + elytra into a result "
						+ "(the enderite_elytra recipe was not disabled)");
		ctx.succeed();
	}

	// --- helpers ----------------------------------------------------------

	@GameTest
	public void gliderHelpersClassifyItems(GameTestHelper ctx) {
		assertTrue(ctx, ElytraCompat.isAnyElytra(new ItemStack(Items.ELYTRA)),
				"isAnyElytra(vanilla elytra) should be true");
		assertTrue(ctx, ElytraCompat.isAnyElytra(new ItemStack(EnderiteMod.ENDERITE_ELYTRA_SEPERATED.get())),
				"isAnyElytra(enderite seperated elytra) should be true");
		assertTrue(ctx, !ElytraCompat.isAnyElytra(new ItemStack(Items.IRON_CHESTPLATE)),
				"isAnyElytra(iron chestplate) should be false");

		assertTrue(ctx, ElytraCompat.isGliderChestArmor(new ItemStack(EnderiteMod.ENDERITE_ELYTRA.get())),
				"isGliderChestArmor(merged enderite elytra) should be true");
		assertTrue(ctx, !ElytraCompat.isGliderChestArmor(new ItemStack(Items.IRON_CHESTPLATE)),
				"isGliderChestArmor(iron chestplate) should be false");
		assertTrue(ctx, !ElytraCompat.isGliderChestArmor(new ItemStack(Items.ELYTRA)),
				"isGliderChestArmor(elytra) should be false");
		ctx.succeed();
	}

	// --- CompatDecorations: chestplate-derived vs player-applied ----------

	@GameTest
	public void splitKeepsChestplateDyeOnChestplate(GameTestHelper ctx) {
		// A dyed leather chestplate's colour is chestplate-derived: it must NOT be
		// copied to the split-off elytra (includeChestplateDerived = false).
		ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
		chest.set(DataComponents.DYED_COLOR, new DyedItemColor(0x00FF00));

		ItemStack armored = ArmoredElytra.createArmoredElytra(new ItemStack(Items.ELYTRA), chest,
				ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(0, 1, 0))), null);
		ItemStack storedChest = CompatDecorations.storedChestplate(armored, ctx.getLevel().registryAccess());
		ItemStack splitElytra = new ItemStack(Items.ELYTRA);

		CompatDecorations.applyOuterDecorations(armored, splitElytra, storedChest, false);
		assertTrue(ctx, splitElytra.get(DataComponents.DYED_COLOR) == null,
				"Chestplate-derived dye leaked onto the split-off elytra");

		// but the render path (includeChestplateDerived = true) DOES show it
		ItemStack renderElytra = new ItemStack(Items.ELYTRA);
		CompatDecorations.applyOuterDecorations(armored, renderElytra, storedChest, true);
		assertTrue(ctx, new DyedItemColor(0x00FF00).equals(renderElytra.get(DataComponents.DYED_COLOR)),
				"Chestplate-derived dye did not show on the rendered wings");
		ctx.succeed();
	}

	@GameTest
	public void applyOuterDecorationsRemovesClearedFlags(GameTestHelper ctx) {
		// target has a glow flag the outer item no longer has -> it must be removed.
		ItemStack outer = new ItemStack(Items.ELYTRA);
		ItemStack target = new ItemStack(Items.ELYTRA);
		var data = target.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		data.putBoolean("elytratrims:glow", true);
		target.set(DataComponents.CUSTOM_DATA, CustomData.of(data));

		boolean changed = CompatDecorations.applyOuterDecorations(outer, target,
				new ItemStack(Items.IRON_CHESTPLATE), true);
		var out = target.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		assertTrue(ctx, changed && !out.getBooleanOr("elytratrims:glow", false),
				"A cleared Elytra Trims flag was not removed from the target");
		ctx.succeed();
	}

	// --- runtime-pack texture math (pure, no GL) --------------------------

	@GameTest
	public void textureShiftMovesContent(GameTestHelper ctx) {
		java.awt.image.BufferedImage src = new java.awt.image.BufferedImage(16, 16,
				java.awt.image.BufferedImage.TYPE_INT_ARGB);
		int marker = 0xFFFF0000;
		for (int x = 0; x < 16; x++) {
			src.setRGB(x, 8, marker); // opaque row at y=8
		}

		// chestplate shift: up one pixel (h/16 == 1 for 16px)
		var up = elytracompat.client.CompatTextureGen.shift(src, -1);
		assertTrue(ctx, (up.getRGB(0, 7) & 0xFFFFFF) == (marker & 0xFFFFFF),
				"Shift-up did not move the marker row up by one pixel");
		assertTrue(ctx, (up.getRGB(0, 15) >>> 24) == 0, "Shift-up did not clear the bottom row");

		// elytra shift: bottom-align (lowest opaque row -> row 15)
		var bottom = elytracompat.client.CompatTextureGen.shiftToBottom(src);
		assertTrue(ctx, (bottom.getRGB(0, 15) & 0xFFFFFF) == (marker & 0xFFFFFF),
				"Bottom-align did not move the marker row to the bottom edge");
		assertTrue(ctx, (bottom.getRGB(0, 0) >>> 24) == 0, "Bottom-align did not clear the top");
		ctx.succeed();
	}

	@GameTest
	public void textureGenCoversBasesAndTrims(GameTestHelper ctx) {
		var targets = elytracompat.client.CompatTextureGen.targets();
		// 12 base textures (3 elytra + 7 chestplates + leather overlay + enderite
		// chestplate) + 18 trim overlays (16 vanilla palettes + 2 enderite) = 30.
		assertTrue(ctx, targets.size() == 30,
				"Expected 30 runtime-generated backing textures, got " + targets.size());
		boolean hasElytra = targets.stream()
				.anyMatch(id -> id.getNamespace().equals("armored_elytra") && id.getPath().endsWith("/elytra.png"));
		boolean hasEnderiteChest = targets.stream().anyMatch(
				id -> id.getNamespace().equals("elytracompat") && id.getPath().endsWith("/enderite_chestplate.png"));
		boolean hasEnderiteTrim = targets.stream().anyMatch(
				id -> id.getPath().endsWith("/chestplate_trim_enderite.png"));
		assertTrue(ctx, hasElytra, "Runtime pack does not regenerate armored_elytra:item/elytra");
		assertTrue(ctx, hasEnderiteChest, "Runtime pack does not regenerate the enderite chestplate backing");
		assertTrue(ctx, hasEnderiteTrim, "Runtime pack does not regenerate the enderite trim overlay");
		ctx.succeed();
	}
}
