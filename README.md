# Elytra Compat — Armored Elytra × Elytra Trims × Enderite Mod

A Fabric compatibility bridge for **Minecraft 26.2** that makes these three mods
work together, with every feature of each still working on vanilla items *and*
on the modded/combined ones:

- [Armored Elytra](https://github.com/DorkixAzIgazi/armored-elytra) — anvil a chestplate onto an elytra
- [Elytra Trims](https://codeberg.org/KikuGie/elytra-trims) — trims, dyes, banners and effects rendered on wings
- [Enderite Mod](https://github.com/Nic4Las/Minecraft-Enderite-Mod) — enderite chestplate + two elytra items

Install all three mods, this compat mod, Fabric API and Fabric Language Kotlin.

## What was broken, and what this fixes

The three mods never knew about each other. Armored Elytra hardcodes
`stack.is(Items.ELYTRA)` everywhere and fakes armor trims with lore because
vanilla can't render a `TRIM` on wings. Elytra Trims only decorates items in its
`elytratrims:decoratable` tag (just `minecraft:elytra`). The Enderite Mod adds
its own chestplate and two elytra items and a crafting recipe that eats *any*
glider. Put together, that produces these bugs — each one is fixed here:

| # | Bug without this mod | Fix |
|---|----------------------|-----|
| 1 | Enderite's standalone elytra (`enderite_elytra_seperated`) can't be armored — every Armored Elytra path is gated on the vanilla elytra item. | New tag `elytracompat:armorable_gliders`; a `@WrapOperation` widens Armored Elytra's `is(elytra)` checks (server + client). |
| 2 | The enderite-elytra crafting recipe accepts an armored elytra and **destroys the stored chestplate**. | Mixin on `EnderiteElytraSpecialRecipe.matches` refuses armored elytras (split it first). |
| 3 | The merged `enderite_elytra` is chest armor *and* a glider, so Armored Elytra let you fuse it onto another elytra (double wings). | Anvil mixin blocks combining a glider-type chest item as the "chestplate". |
| 4 | Combining an already-armored enderite elytra again. | Anvil mixin refuses re-armoring. |
| 5 | Chestplate armor trims never showed on the wings (Armored Elytra faked them as lore). | On combine, the chestplate's real `TRIM` is copied onto the result (Elytra Trims renders it) and the duplicate faked lore is stripped. |
| 6 | Trims/dyes/banners/effects applied to an armored elytra with Elytra Trims didn't render — the wings draw from the stored *snapshot*. | Client render mixin copies the outer item's decorations onto the render stack. |
| 7 | Grindstone-splitting an armored elytra silently deleted any decoration the player added after combining. | Grindstone mixin merges player-applied decorations into the stored elytra snapshot before the split (chestplate-derived trim/dye stay with the chestplate). |
| 8 | Enderite elytras weren't decoratable by Elytra Trims. | Datapack adds `#enderitemod:enderite_elytras` to `elytratrims:decoratable`. |

## How it's built

- `ElytraCompat` — mod init, the `armorable_gliders` tag, and the shared
  `isAnyElytra` / `isGliderChestArmor` helpers.
- `CompatDecorations` — moves Elytra Trims decorations (trim, dye, banner
  patterns, effect flags) between the outer item and the stored snapshots, with
  a flag controlling whether chestplate-derived trim/dye come along (yes for
  rendering, no for a grindstone split so they aren't duplicated).
- `mixin/ArmoredElytraMixin` — widens the elytra checks and bridges the trim.
- `mixin/AnvilMenuCompatMixin` — combines modded gliders, blocks glider-as-chestplate and re-armoring.
- `mixin/GrindstoneMenuCompatMixin` — preserves decorations on split, shows the chestplate result for modded gliders.
- `mixin/EnderiteElytraSpecialRecipeMixin` — protects against item loss.
- `mixin/client/RenderHelperMixin` — widens the render swap and decorates the rendered wings.

### Note on MC 26.2

Snapshot 26.2 ships **unobfuscated**, so Loom runs in a non-obfuscated
environment: the dependency mod jars are already in real names (no remapping),
and `ItemStack.is(Item)` compiles to `is(Ljava/lang/Object;)Z` in bytecode — the
mixins target that descriptor with `remap = false`.

## Testing

`src/gametest` contains 11 game tests covering every fix above (anvil combining
for both vanilla and enderite gliders, glider/​re-armor blocking, recipe
protection, grindstone decoration preservation and chestplate-derived-trim
non-duplication, enderite-based splitting, the decoratable tag, and the client
render decoration path). Run them with:

```
./gradlew runGameTest
```

All pass, and the client boots cleanly to the title screen with all three mods
installed.

## Inventory icons

The enderite items are hooked all the way into Armored Elytra's inventory-icon
system, so combined items get proper chestplate-over-elytra icons (with trims),
exactly like the vanilla chestplates:

- **Vanilla elytra + enderite chestplate** — every trim material, including the
  enderite trim material, plus a plain (untrimmed) icon.
- **Vanilla chestplate + enderite trim material** — the enderite trim now shows
  on every vanilla armored elytra too.
- **Enderite (seperated) elytra + any chestplate** — the enderite elytra works
  as an armor base as well; its item model is wrapped so an *un-armored* enderite
  elytra still renders exactly the icon the Enderite Mod ships (the override only
  kicks in once a chestplate is embedded).

`tools/gen_icons.py` regenerates every delta asset (shifted enderite chestplate /
elytra textures, palette-mapped enderite trim overlays, ~125 models, and the two
item-definition overrides), mirroring `ArmoredElytraDataGenerator` exactly and
reusing Armored Elytra's own vanilla trim overlays instead of duplicating them.
The `docs/` screenshots show the rendered icons (verified via a client game
test): the gold trim renders on the gold-trimmed enderite chestplate and the
enderite elytra base renders teal, with trims layered on top.

The in-world wings also render fully correctly (chestplate armor look + trims +
Elytra Trims decorations) via the render mixin.

### Resource-pack aware icons

Every icon layer — the elytra, every chestplate, the leather overlay, and the
trim overlays, for vanilla **and** enderite items — is regenerated at runtime
from whatever textures are currently active, so a resource pack that re-textures
the elytra or a chestplate shows through on the armored elytra icon (e.g. a pack
with a blue elytra gives blue armored elytras). `CompatRuntimePack` (a synthetic
client pack injected via `MultiPackResourceManagerMixin`) reads the live source
textures, repositions them, and overrides `armored_elytra:item/*` and the compat
textures — which makes Armored Elytra's own vanilla icons pack-aware too.

The layout is built to read as "a chestplate worn with an elytra":

- the **elytra** is shifted **down** so its lowest pixels rest on the bottom
  edge of the item square (content-aware, so it bottom-aligns for any texture);
- the **chestplate**, leather overlay and **trim** overlays are shifted **up by
  one pixel** from the normal item position, so the chestplate sits just above
  the elytra.

Trim overlays are recoloured at runtime from the vanilla trim palettes (so trim
colours are pack-aware too) and repositioned to match the chestplate. In a dev
environment the runtime pack dumps everything it generates to
`run/.elytracompat/` for inspection.

### Combining routes through the anvil only

The Enderite mod's own "enderite chestplate + elytra → `enderite_elytra`"
crafting-grid recipe is disabled (`EnderiteElytraSpecialRecipeMixin` makes it
never match; it stays registered but inert and, being a special recipe, is not
shown in the recipe book), so that combination now happens only through Armored
Elytra's anvil — producing a splittable armored elytra with the chestplate
stored inside. Combining an enderite chestplate onto the enderite (seperated)
elytra shows both enderite textures, not a vanilla one.

## Building

Populate `libs/` with the dependency jars (see `libs/README.md`), then:

```
./gradlew build
```

The mod jar lands in `build/libs/`. Requires JDK 25+.

## Credits

Bridges and builds on three mods — huge thanks to their authors:

- **Armored Elytra** by DorkixAzIgazi — https://github.com/DorkixAzIgazi/armored-elytra (MIT)
- **Elytra Trims** by KikuGie — https://codeberg.org/KikuGie/elytra-trims (LGPL-3.0)
- **Enderite Mod** by Nic4Las — https://github.com/Nic4Las/Minecraft-Enderite-Mod (MIT)

## License

MIT — see [LICENSE](LICENSE). Derived icon assets from Armored Elytra and the
Enderite Mod are used under their MIT licenses; Elytra Trims is an optional
runtime dependency only (no code redistributed).
