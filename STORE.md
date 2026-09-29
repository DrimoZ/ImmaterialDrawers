# Store copy

Paste-ready text for the CurseForge (and later Modrinth) project page. Not documentation: this file
sells the mod, the wiki explains it. Every claim here is something the mod does today — check it
against CHANGELOG.md before changing a number.

Images are referenced by the URL CurseForge gave them on upload, through the description editor's image upload, which refuses anything wider than 850 px. The sources are generated, not
committed: `run/store-screenshots/` for the in-game shots, the banner and headers from the showcase
run (CLAUDE.md §4).

**`UPLOAD:<file>` marks an image not uploaded yet.** Upload `<file>` (850 px wide, from the scratchpad `store850/`) through the description editor, then replace the marker with the URL CurseForge gives it.

## Summary

> One line, 256 characters at most, shown under the name in every search result.

Functional Storage drawers for what isn't an item or a fluid: Energy (2 billion FE), Mekanism chemicals and Ars Nouveau Source. Same upgrades, same Storage Controller, framed variants for all.

## Categories

Main: **Addons** (it is one, of Functional Storage). Additional: Storage, Energy, Utility & QoL.

## Description

<!-- Everything below this line is pasted into CurseForge's Markdown editor as-is. -->

![Immaterial Drawers](UPLOAD:banner.jpg)

### Functional Storage stores items and fluids. Immaterial Drawers stores everything else.

A family of drawers for the things you can't pick up: power, Mekanism chemicals, Ars Nouveau
Source, and whatever else your mods pipe around. Every one of them sits in your wall of drawers, takes the upgrades you
already craft, and joins the Storage Controller like any other drawer. No new storage system to
learn: if you know Functional Storage, you already know how to use this.

![The family is growing](UPLOAD:header_more.jpg)

The foundations are built once, for every kind of drawer: the Storage Controller link, the
upgrades, the framed variants, the probe support, the numbers past two billion. Each kind of
drawer is built on top of them, and three are here:

- ✅ **Energy Drawer**, Forge Energy. Works with every FE mod.
- ✅ **Chemical Drawers**, for Mekanism gases, slurries, pigments and infuse types. *With Mekanism.*
- ✅ **Source Drawer**, for Ars Nouveau Source. *With Ars Nouveau.*
- 🔜 **Mana**, for Botania, as soon as Botania is released for 1.21.1.
- 💡 More after that. Tell us what you want to store in the [issues](https://github.com/DrimoZ/ImmaterialDrawers/issues).

![Drawer #1: Energy](https://media.forgecdn.net/attachments/description/1663641/description_3e24b98f-0bd2-4f37-bc1d-fc16cee86356.jpg)

The **Energy Drawer** holds Forge Energy the way a drawer holds cobblestone: a lot of it, in one
block, right where you can see it.

- **500,000 FE out of the box**, before a single upgrade.
- **Grows with Functional Storage's own Storage Upgrades.** Four Netherite upgrades take one drawer
  to **2,048,000,000 FE**. No new upgrade items, no new recipes to learn.
- **An energy cube turns inside every drawer.** Its core is gone when the drawer is empty, faint
  when it is low, and blazing when it is full, so you can read a whole wall from across the room.
- **Numbers that don't lie.** Energy is counted in 64 bits, so the drawer front, Jade and The One
  Probe show the real total, even past the 2.1 billion where most FE blocks quietly stop counting.
- **Pushes power into its neighbours on its own.** Cables and machines placed next to it just
  work, no extraction setup needed. (Configurable.)
- **Standard Forge Energy.** Powah, Mekanism, Thermal: anything with an FE cable connects.

![A charged wall, up close](https://media.forgecdn.net/attachments/description/1663641/description_acd4fc41-cd49-415e-848f-3c4c7fdf088b.jpg)

![Jade shows the real total, past two billion](https://media.forgecdn.net/attachments/description/1663641/description_e6d63940-c101-4f7f-8c38-5d2f5f4aea6e.jpg)

![One wall, one battery](https://media.forgecdn.net/attachments/description/1663641/description_8b9f2641-d47c-46b9-b911-64b7cbdc6ac6.jpg)

Link your Energy Drawers to a **Storage Controller** with Functional Storage's Linking Tool, then
put one cable on the controller: the whole wall charges and discharges as a single battery. The
controller adds up the entire network, in 64 bits, so a wall of full drawers is shown as what it
really holds.

![A Mekanism power room built around one Storage Controller](https://media.forgecdn.net/attachments/description/1663641/description_b43dcca6-0a37-4d1f-81c7-aa4153a5a2f8.jpg)

![Drawer #2: Chemicals](UPLOAD:header_chemical.jpg)

With **Mekanism** installed, **Chemical Drawers** join the family. They are Functional Storage's fluid
drawers for chemicals: the same 1x1, 1x2 and 2x2 layouts, the same capacities, the same upgrades.

- **Every Mekanism chemical:** hydrogen, oxygen, chlorine, ore slurries, pigments, infuse types.
- **Pressurized tubes connect from any side**, and a tube on a Storage Controller reaches every
  tank on the wall.
- **Fill and empty them by hand** with a Mekanism chemical tank: right-click to pour in, left-click
  to fill back.
- **Gases glow**, and their surface slowly breathes. Slurries and pigments sit still like a liquid.
- **Lock, Void and Creative** behave as they do on a fluid drawer.

![Drawer #3: Source](UPLOAD:header_source.jpg)

With **Ars Nouveau** installed, the **Source Drawer** appears, and Ars treats it like a Source Jar:
sourcelinks fill it, the Enchanting Apparatus and Imbuement Chamber draw from it, and relays link
to it with the Dominion Wand. 32,000 Source unupgraded, over two billion with four Netherite
upgrades, glowing with Ars's own Source behind a violet window.

![Chemical and Source drawers next to an Energy Drawer](UPLOAD:family_shot.jpg)

**Without Mekanism or Ars Nouveau, their drawers simply do not exist** and nothing else changes:
both are optional.

![Framed](https://media.forgecdn.net/attachments/description/1663641/description_57ae690a-b9df-480c-9fca-23970a13b99d.jpg)

Every drawer has a **framed** version that wears any block you give it through Functional Storage's framing
recipe, the same one you use for your framed item drawers. Cherry cabinets, quartz walls, copper
panels: what the drawer holds still glows through the window.

![Framed energy drawers hiding in a cherry cabinet](https://media.forgecdn.net/attachments/description/1663641/description_b758d909-d3d0-45be-820a-386bfbffde45.jpg)

![Upgrades that already work](https://media.forgecdn.net/attachments/description/1663641/description_f3f6f568-499b-4cd0-b07d-02502a001469.jpg)

Everything Functional Storage puts in a drawer's upgrade slots does what you would expect:

| Upgrade | On an Energy Drawer |
|---|---|
| Storage Upgrades (Copper to Netherite) | Multiply the capacity, all four slots count |
| Redstone Upgrade, comparator | A signal from 0 to 15 that follows the charge |
| Void Upgrade | Accepts power even when full, and discards the excess |
| Creative Vending Upgrade | An endless source of power |
| Configuration Tool | Hide the number, hide the cube, show a fill bar |

**New: the Wireless Charger.** Slot it into a drawer's utility slots and the wall keeps the tools,
armour and offhand item of every player within 8 blocks charged. Range, rate and interval are all
in the config.

![A solar shed at sunset](https://media.forgecdn.net/attachments/description/1663641/description_6afb557c-0b03-4996-8557-3d27e03a002a.jpg)

### Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1+ |
| [Functional Storage](https://www.curseforge.com/minecraft/mc-mods/functional-storage) | 1.5.7+ |
| [Titanium](https://www.curseforge.com/minecraft/mc-mods/titanium) | required by Functional Storage too |
| [Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism) *(optional)* | 10.7+, for the Chemical Drawers |
| [Ars Nouveau](https://www.curseforge.com/minecraft/mc-mods/ars-nouveau) *(optional)* | 5.13+, for the Source Drawer |

### FAQ

**Do I need Mekanism, Ars Nouveau or another mod?** No. The Energy Drawer works with any mod that
produces or uses Forge Energy. Mekanism adds the Chemical Drawers and Ars Nouveau the Source
Drawer; without them, those drawers are simply not there.

**Can I put it in my modpack?** Yes, as long as you ship the published jar unmodified.

**Fabric?** No. Functional Storage is NeoForge on 1.21.1, and so is this.

**Is every number configurable?** Yes: capacities, how upgrades scale them, whether and how fast
a drawer pushes power, everything about the Wireless Charger, and a switch to turn off any drawer
your pack does not want.

### Credits

Built on [Functional Storage](https://www.curseforge.com/minecraft/mc-mods/functional-storage) by
Buuz135 and Rid, and on Titanium. Source code on
[GitHub](https://github.com/DrimoZ/ImmaterialDrawers). Found a bug or have an idea? Open an
[issue](https://github.com/DrimoZ/ImmaterialDrawers/issues).
