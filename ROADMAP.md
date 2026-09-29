# Roadmap

Ordered, not scheduled. The order is the point: each step is what makes the next one safe to write.

## Now

**Energy is stored in a long, not an int.** `IEnergyStorage` is an int API, so anything reading the
standard capability - cables, machines, most meters - sees at most 2,147,483,647 FE. Every mod that
holds more does the same clamp; Powah's cable is literally `receiveEnergy(long, boolean, Direction)`
behind an adapter. The long values are the truth and every display in this mod uses them.

**Everything is configurable.** Base size, FE per unit, the upgrade divisor, whether the drawer
pushes, its throughput and its tick interval - all in `immaterialdrawers-common.toml`, deliberately
wider than what Functional Storage exposes for its own drawers.

**6 — Rendering, datagen, GUI.** Datagen and the framed tint are done: blockstates, item models,
loot tables, recipes and en_us are generated and committed, block models and textures are
hand-authored, and the framed drawer registers the tint handler Functional Storage cannot register
for it.

The client loads all of it cleanly — `./gradlew runClient`, no errors, no missing textures, and
nothing in the log about this mod beyond it being loaded. That includes the framed block model going
through Functional Storage's `framedblock` loader, which was the part most likely to break.

The art is in, and it has been looked at in game. The drawer is Functional Storage's fluid drawer,
empty - their casing, their recessed front, their tank, inherited from their models rather than
copied, in a graphite-and-copper recolour of their textures. Inside the tank an energy cube turns, in
the Mekanism tradition: an open frame, and a faceted core that is absent when the drawer is empty
and grows more solid and faster as it fills. The number sits where a fluid drawer puts its own.

**7 — Augments.** These are the product; the block is the support - a drawer that only
holds FE duplicates a Powah energy cell. The **Wireless Charger** is in: put it in a utility slot and
the wall keeps the tools, armour and offhand of everyone in range charged. It was built instead of
the throwaway behaviour the plan called for, because it proves the same thing - that Functional
Storage really calls `work()` on an upgrade of ours - and leaves something behind.

The three that were listed after it are settled rather than built - see **Then**. What is left before
a release is a published file: the CurseForge page (logo, gallery, description) is filled in from STORE.md.

**Chemical Drawers (Mekanism, optional).** Six blocks mirroring Functional Storage's fluid drawers,
registered only when Mekanism is installed. Tested with Mekanism (35 game tests) and without it
(20). Still to look at: a probe provider for Jade/TOP, and the in-hand item showing its contents.

**Source Drawer (Ars Nouveau, optional).** Found by relays through the Source capability and by
every nearby consumer through Ars's `SourceManager`. 49 game tests with both optional mods, 36 without
Ars, 21 without either.

## Then

**7 — Augments.** The Wireless Charger is in. The other three the plan listed are not coming, and
each for its own reason:

- **Auto-output to adjacent blocks** is already the drawer's base behaviour, not an augment. Nothing
  in the Forge Energy ecosystem pulls, so a drawer that waits to be asked never gives anything up.
  It is on by default and configurable.
- **A fuel-burning generator** is not this mod. It stores energy; it does not make it.
- **Redstone on a charge threshold** uses Functional Storage's own Redstone Upgrade. An upgrade the
  player already owns should work, and theirs almost did — it connects and ticks for an energy drawer
  unchanged, and only its signal read the zero-slot item handler and gave up. That half is answered
  in `EnergyDrawerBlock.getSignal`, with the same number the comparator reports.

## Still open

- **Mana (Botania).** Asked for, and deferred: Botania has no release for 1.21.1 (the latest is
  1.20.1-456; the port is an unreleased branch). Build it when Botania ships, the way the Source
  Drawer was built: capability or public registry, never a mixin.
- **Probes for chemicals and Source.** Jade and The One Probe show energy only.

- The CurseForge project is created as `immaterial-drawers`, its page is filled in (logo, 14 screenshots,
  the STORE.md description), and it waits for approval with its first file. The same slug is still to
  be reserved on Modrinth.

## Settled

- **The mod stores energy and does not generate it.** A fuel-burning augment was on the list and is
  off it: every tech mod already has generators, and none of them has a drawer.
- **Every number is config, including the ones Functional Storage keeps in code.** The worry was
  that a pack author moving `ENERGY_DIVISOR` breaks the top of the curve. `EnergyScaling.capacityFor`
  clamps instead of overflowing, so the failure mode is a flat top, not a negative capacity.
- **Probe mods get a provider each, and it feeds their bar rather than adding a line.** Jade and The
  One Probe both read the standard capability, so both stop at 2.1B without one. Neither API was the
  limitation: `EnergyView.of` and `IProbeInfo.progress` have taken longs all along.
- **NeoForge 1.21.1 only.** No Fabric, no multiloader. 1.21.1 is not a choice — it is where
  Functional Storage is.
- **Umbrella name, not "Energy Drawers".** The mod id is written into every save that contains one
  of these blocks and can never change. Energy is the first module.
- **X_1 only for energy.** FE has one kind of content; the 2- and 4-slot geometries would have
  nothing to put in the other slots.
- **Energy scales on its own component, not Functional Storage's fluid one.** `energy_storage_modifier`
  is ours, attached to their upgrades through `ModifyDefaultComponentsEvent`. Not a tidiness call:
  reusing their fluid component welds energy to `FLUID_DIVISOR` and caps the base at ~32,000 FE,
  which is less than the cheapest energy cell in any tech mod.
- **The framed variant carries its own block entity type**, and therefore its own capability
  registration and its own NBT scan. Sharing the unframed type would have been simpler and wrong.
- **A drawer never locks.** Locking pins a drawer to the kind of thing it holds; FE has one kind,
  so there is nothing to pin and nothing to preserve.
- **No mixin into Functional Storage.** The zero-slot item handler solves the same problem inside
  the public contract, and it is a mixin into a mod with 56M downloads. Proven at network scale:
  50 drawers on one controller, no per-tick rebuild.
- **Titanium by composition, not inheritance.** `PoweredTile` and `ControllableDrawerTile` are
  sibling subclasses of `ActiveTile` and the drawer half is not negotiable.
- **Chemical Drawer (Mekanism) is v2 of this mod, not a separate one.** It is the first content that
  would justify X_2 / X_4 variants. Mana and other bespoke APIs are out — they are not "other kinds of energy", they are one
  integration each, broken on every upstream release. **Source was reversed** (2026-09-29): Ars
  Nouveau publishes an extension point (`SourceManager`) as well as its capability, and game tests on
  its real code paths catch the breakage this note feared. See CLAUDE.md §18.
