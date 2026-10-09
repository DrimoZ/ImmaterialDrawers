# Store copy

Paste-ready text for the CurseForge and Modrinth project pages. Not documentation: this file sells
the mod, the wiki explains it. Every claim here is something the mod does today: check it against
`CHANGELOG.md` before changing a number.

Images live in `docs/store-art/` and the description links them from GitHub
(`raw.githubusercontent.com`, `main`), never from the description editor: an image uploaded there
joins the CurseForge gallery. Change an image, push, and both pages show it. The full-size captures
behind them are in `run/store-screenshots/` (showcase run, CLAUDE.md §4), and only the gallery is
uploaded by hand.


## Summary

> One line, 256 characters at most, shown under the name in every search result.

Functional Storage drawers for what isn't an item or a fluid: Energy (2 billion FE), Mekanism chemicals and Ars Nouveau Source. Same upgrades, same Storage Controller, framed variants for all.

## Description

<!-- Everything below this line is pasted into the site's Markdown editor as-is. -->

![Immaterial Drawers](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/banner.jpg)

**Functional Storage stores items and fluids. Immaterial Drawers stores everything else.**

Drawers for the things you can't pick up: power, Mekanism chemicals, Ars Nouveau Source. They sit
in your wall of drawers, take the upgrades you already craft, and join the Storage Controller like
any other drawer. If you know Functional Storage, you already know how to use this.

![The family is growing](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/header_family.jpg)

- ✅ **Energy Drawer**, Forge Energy. Works with every FE mod.
- ✅ **Chemical Drawers**, for Mekanism gases, slurries, pigments and infuse types. *With Mekanism.*
- ✅ **Source Drawer**, for Ars Nouveau Source. *With Ars Nouveau.*
- 🔜 **Mana**, for Botania: planned.
- 💡 Want something else stored? Say so in the [issues](https://github.com/DrimoZ/ImmaterialDrawers/issues).

![Drawer #1: Energy](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/header_energy.jpg)

- **500,000 FE out of the box**, and Functional Storage's own Storage Upgrades take it to
  **2,048,000,000 FE** with four Netherite. No new upgrade items.
- **An energy cube turns inside every drawer**: no core when empty, faint when low, blazing when
  full. Read a whole wall from across the room.
- **Numbers that don't lie.** Counted in 64 bits: the drawer front, Jade and The One Probe show the
  real total, even past the 2.1 billion where most FE blocks stop counting.
- **Pushes power into its neighbours on its own** (configurable). Powah, Mekanism, Thermal:
  anything with an FE cable connects.

![A charged wall, up close](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/energy_wall_close.jpg)

![Jade shows the real total, past two billion](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/energy_jade.jpg)

![One wall, one battery](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/header_controller.jpg)

Link the drawers to a **Storage Controller** with the Linking Tool and put one cable on the
controller: the whole wall charges and discharges as a single battery, and shows what it really
holds.

![A Mekanism power room built around one Storage Controller](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/energy_power_room.jpg)

![Drawer #2: Chemicals](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/header_chemicals.jpg)

With **Mekanism**, Functional Storage's fluid drawers for chemicals: the same 1x1, 1x2 and 2x2
layouts, capacities and upgrades.

- **Every Mekanism chemical:** hydrogen, chlorine, ore slurries, pigments, infuse types.
- **Pressurized tubes connect from any side**, and a tube on a Storage Controller reaches every tank.
- **Fill and empty by hand** with a chemical tank: right-click to pour in, left-click to fill back.
- **Gases glow** and their surface breathes. **Lock, Void and Creative** work as on a fluid drawer.

![A Mekanism chemical plant feeding a wall of Chemical Drawers](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/chemical_plant.jpg)

![Gases, slurries and pigments, up close](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/chemical_close.jpg)

![Drawer #3: Source](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/header_source.jpg)

With **Ars Nouveau**, a drawer Ars treats like a Source Jar: sourcelinks fill it, the Enchanting
Apparatus and Imbuement Chamber draw from it, and on 1.21.1 relays link to it with the Dominion
Wand. 32,000 Source unupgraded, over two billion with four Netherite.

![An Ars Nouveau sanctum around a wall of Source Drawers](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/source_sanctum.jpg)

![Source Drawers, up close](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/source_close.jpg)

![The whole family in one wall](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/family_wall.jpg)

![Framed](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/header_framed.jpg)

Every drawer has a **framed** version that wears any block through Functional Storage's framing
recipe. What the drawer holds still glows through the window.

![Framed energy drawers hiding in a cherry cabinet](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/framed_cabinet.jpg)

![Upgrades that already work](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/header_upgrades.jpg)

| Upgrade | On an Energy Drawer |
|---|---|
| Storage Upgrades (Copper to Netherite) | Multiply the capacity, all four slots count |
| Redstone Upgrade, comparator | A signal from 0 to 15 that follows the charge |
| Void Upgrade | Accepts power even when full, and discards the excess |
| Creative Vending Upgrade | An endless source of power |
| Configuration Tool | Hide the number, hide the cube, show a fill bar |

**Wireless Charger**: in a drawer's utility slot, it keeps the tools, armour and offhand item of
every player within 8 blocks charged (range, rate and interval in the config).

![A solar shed at sunset](https://raw.githubusercontent.com/DrimoZ/ImmaterialDrawers/main/docs/store-art/solar_shed.jpg)

### Requirements

| | 1.21.1 | 1.20.1 |
|---|---|---|
| Loader | NeoForge 21.1+ | Forge 47.2+ |
| Functional Storage | 1.5.7+ | 1.2.14+ |
| Titanium | required | required |
| Mekanism *(optional)* | 10.7+ | 10.4 |
| Ars Nouveau *(optional)* | 5.13+ | 4.12 |

### FAQ

**Do I need Mekanism or Ars Nouveau?** No. Without them their drawers are simply not there; the
Energy Drawer works with any Forge Energy mod.

**Can I put it in my modpack?** Yes, as long as you ship the published jar unmodified.

**Fabric?** No: Functional Storage is NeoForge on 1.21.1 and Forge on 1.20.1, and so is this. On
1.20.1, Ars Nouveau relays do not connect to the Source Drawer (Ars 4.12 only feeds its own machines).

**Is it configurable?** Capacities, upgrade scaling, pushing, the Wireless Charger, and a switch to
turn off any drawer your pack does not want.

### Credits

Built on Functional Storage by Buuz135 and Rid, and on Titanium.
[Source](https://github.com/DrimoZ/ImmaterialDrawers) ·
[Wiki](https://github.com/DrimoZ/ImmaterialDrawers/wiki) ·
[Report a bug](https://github.com/DrimoZ/ImmaterialDrawers/issues)

<!-- End of the pasted description. -->


## CurseForge

Main category **Addons** (of Functional Storage). Additional: Storage, Energy, Utility & QoL.

<https://www.curseforge.com/minecraft/mc-mods/immaterial-drawers>

## Modrinth — creating the project

Slug `immaterial-drawers` was still free on 9 Oct. 2026. Create at <https://modrinth.com/dashboard/projects>.

| Field | Value |
|---|---|
| Name / slug | `Immaterial Drawers` / `immaterial-drawers` |
| Summary | the one above |
| Icon | `src/main/resources/logo.png` |
| Categories | **storage**, **technology**, utility (`magic` once the Source Drawer is worth a tag) |
| Environment | Client: required · Server: required |
| License | Custom: `MIT (code) / All Rights Reserved (assets)`, URL `https://github.com/DrimoZ/ImmaterialDrawers/blob/main/LICENSE` |
| Links | Source `https://github.com/DrimoZ/ImmaterialDrawers` · Issues `…/issues` · Wiki `…/wiki` |
| Description | the block above, as-is |
| Gallery | from `run/store-screenshots/` (full PNG, no 850 px limit): `02_out_hero_day`, `10_power_hero`, `05_jade_power`, `15_chem_wide`, `17_ars_wide`, `19_family`, `13_lib_hero`; first one featured |

Versions: one file per Minecraft version, same jars as CurseForge, version number `{mod}+{minecraft}`,
changelog from `CHANGELOG.md`, release type as on CurseForge.

| Loader / game | Required | Optional |
|---|---|---|
| NeoForge / 1.21.1 | `functional-storage`, `titanium` | `mekanism`, `ars-nouveau`, `jade`, `the-one-probe` |
| Forge / 1.20.1 | `functional-storage`, `titanium` | `mekanism`, `ars-nouveau`, `jade`, `the-one-probe` |

Modrinth reviews a project before it goes public: submit it with its first version attached.
