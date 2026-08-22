# Roadmap

Ordered, not scheduled. The order is the point: each step is what makes the next one safe to write.

## Now

**5 — Framed variant.** ~100 lines, and the real answer to blending into an existing wall — better
than guessing which wood the player used.

## Then

**6 — Rendering, datagen, GUI.** The largest art item: energy has no fluid texture to borrow, so the
gauge is designed from nothing. Blockstates, models, loot tables, recipes and lang are all still
missing.

**7 — Augments.** These are the product; the block is the support. A drawer that only holds FE
duplicates a Powah energy cell. Register one trivial `FunctionalUpgradeBehavior` first to prove the
registry end to end, then:

- **Wireless Charger** — the wall of drawers charges tools and players in range. Nobody has this,
  and it goes through Functional Storage's one official extension point.
- Auto-output to adjacent blocks — `MoveFluidsBehavior` is the model.
- Fuel-burning generator — `GenerateFluidBehavior` is the model.
- Redstone on a charge threshold — already in the interface.

## Still open

- Which slug the mod gets. `immaterial-drawers` is free on Modrinth and appears free on CurseForge
  (403 to an automated check, so confirm by hand before release).
- Whether `BASE_UNITS` and `ENERGY_DIVISOR` should be config values rather than constants. They are
  calibrated against the int ceiling, so a pack author who moves them can break the top of the
  curve — which argues for leaving them alone, or for validating them at load.
- Jade and The One Probe integration. Both dispatch on Functional Storage's tile types by
  `instanceof`, so both need writing rather than inheriting.

## Settled

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
