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

What is left is the art, and looking at it. The client bakes the models; nobody has yet seen the
result.

Then the gauge itself: energy has no fluid texture to borrow, so it is designed from nothing. The
current textures are flat generated placeholders and are meant to be thrown away.

**7 — Augments, started.** These are the product; the block is the support - a drawer that only
holds FE duplicates a Powah energy cell. The **Wireless Charger** is in: put it in a utility slot and
the wall keeps the tools, armour and offhand of everyone in range charged. It was built instead of
the throwaway behaviour the plan called for, because it proves the same thing - that Functional
Storage really calls `work()` on an upgrade of ours - and leaves something behind.

## Then

**7 — The rest of the augments.**

- Auto-output to adjacent blocks — `MoveFluidsBehavior` is the model.
- Fuel-burning generator — `GenerateFluidBehavior` is the model.
- Redstone on a charge threshold — already in the interface.

## Still open

- The CurseForge project is created as `immaterial-drawers` and waits for approval; the same slug is
  still to be reserved on Modrinth.

## Settled

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
  would justify X_2 / X_4 variants. Mana, Source and other bespoke APIs are out — they are not
  "other kinds of energy", they are one integration each, broken on every upstream release.
