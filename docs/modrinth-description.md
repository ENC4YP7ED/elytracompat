# Elytra Compat

**Makes [Armored Elytra](https://modrinth.com/mod/elytra-armor), [Elytra Trims](https://modrinth.com/mod/elytra-trims) and the [Enderite Mod](https://modrinth.com/mod/enderite-mod) work together** — with every feature of each still working, on vanilla items *and* on the modded/combined ones.

Install all four mods (plus Fabric API and Fabric Language Kotlin) and they finally cooperate instead of stepping on each other.

## What it fixes

- **Armor your enderite elytra.** The Enderite Mod's standalone elytra can now have a chestplate combined onto it in the anvil, just like a vanilla elytra — and split back apart in a grindstone.
- **Real trims on the wings.** A trimmed chestplate's armor trim now renders on the combined elytra through Elytra Trims (Armored Elytra used to fake it with tooltip text).
- **Decorations are safe.** Trims, dyes, banner patterns and Elytra Trims effects you apply after combining now render on the wings *and* survive a grindstone split instead of vanishing.
- **No item loss.** The Enderite crafting recipe used to eat an armored elytra and destroy the chestplate inside it — that combination now goes through Armored Elytra's anvil only.
- **Enderite elytras are decoratable** by Elytra Trims.

## Resource-pack aware chestplate-over-elytra icons

Combined items get a proper inventory icon: the elytra sits at the bottom of the slot with the chestplate layered just above it, complete with trims — for **both vanilla and enderite** elytras and every chestplate (including the enderite chestplate and enderite trim material).

Best of all, the icon is **regenerated live from your active textures**, so if a resource pack re-textures your elytra or a chestplate, that shows through on the armored elytra icon too.

## Requires

- [Armored Elytra](https://modrinth.com/mod/elytra-armor) (by DorkixAzIgazi)
- [Elytra Trims](https://modrinth.com/mod/elytra-trims)
- [Enderite Mod](https://modrinth.com/mod/enderite-mod)
- [Fabric API](https://modrinth.com/mod/fabric-api) + [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)

## Open source

Source, issues and full technical write-up: **https://github.com/ENC4YP7ED/elytracompat** (MIT).

Huge thanks to the authors of the three mods this bridges.
