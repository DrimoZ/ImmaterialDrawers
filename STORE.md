# Store copy

Paste-ready text for the CurseForge (and later Modrinth) project page. Not documentation: this file
sells the mod, the wiki explains it. Every claim here is something the mod does today — check it
against CHANGELOG.md before changing a number.

Images are referenced by the URL CurseForge gave them on upload, through the description editor's image upload, which refuses anything wider than 850 px. The sources are generated, not
committed: `run/store-screenshots/` for the in-game shots, the banner and headers from the showcase
run (CLAUDE.md §4).

## Summary

> One line, 256 characters at most, shown under the name in every search result.

Functional Storage drawers for everything that isn't an item or a fluid. Energy Drawers are here, up to 2 billion FE each and linkable to the Storage Controller. Chemical drawers and more are on the way.

## Categories

Main: **Addons** (it is one, of Functional Storage). Additional: Storage, Energy, Utility & QoL.

## Description

<!-- Everything below this line is pasted into CurseForge's Markdown editor as-is. -->

![Immaterial Drawers](https://media.forgecdn.net/attachments/description/1663641/description_ceb6cd3f-d240-45ad-a7ca-fb45c91f9bbd.jpg)

### Functional Storage stores items and fluids. Immaterial Drawers stores everything else.

A family of drawers for the things you can't pick up: power, chemicals, and whatever else your
tech mods pipe around. Every one of them sits in your wall of drawers, takes the upgrades you
already craft, and joins the Storage Controller like any other drawer. No new storage system to
learn: if you know Functional Storage, you already know how to use this.

![The family is growing](https://media.forgecdn.net/attachments/description/1663641/description_651ab81d-4ddf-4f66-b209-85a3b53069d6.jpg)

The foundations are built once, for every kind of drawer: the Storage Controller link, the
upgrades, the framed variants, the probe support, the numbers past two billion. **Energy is the
first drawer on top of them, and it works.** Each new kind is a new drawer on the same base, so the
next ones come fast:

- ✅ **Energy Drawer**, Forge Energy. Available now.
- 🔜 **Chemical Drawer**, for Mekanism chemicals. Next up.
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

![Framed](https://media.forgecdn.net/attachments/description/1663641/description_57ae690a-b9df-480c-9fca-23970a13b99d.jpg)

The **Framed Energy Drawer** wears any block you give it through Functional Storage's framing
recipe, the same one you use for your framed item drawers. Cherry cabinets, quartz walls, copper
panels: the cube still glows through the window.

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

### FAQ

**Do I need Mekanism, Powah or another tech mod?** No. Any mod that produces or uses Forge Energy
works with it. The screenshots use Mekanism and Powah for scenery.

**Can I put it in my modpack?** Yes, as long as you ship the published jar unmodified.

**Fabric?** No. Functional Storage is NeoForge on 1.21.1, and so is this.

**Is every number configurable?** Yes: base capacity, how upgrades scale it, whether and how fast
a drawer pushes power, and everything about the Wireless Charger.

### Credits

Built on [Functional Storage](https://www.curseforge.com/minecraft/mc-mods/functional-storage) by
Buuz135 and Rid, and on Titanium. Source code on
[GitHub](https://github.com/DrimoZ/ImmaterialDrawers). Found a bug or have an idea? Open an
[issue](https://github.com/DrimoZ/ImmaterialDrawers/issues).
