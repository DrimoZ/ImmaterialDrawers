# Changelog

Newest first. Versions are `{mod}+{minecraft}`.

## Unreleased

### Added
- Energy Drawer: a Functional Storage drawer that holds FE and exposes
  `Capabilities.EnergyStorage.BLOCK`.
- Game tests covering the two third-party readings the architecture rests on — the second
  capability provider, and the zero-slot item handler that keeps the Storage Controller's per-tick
  invariant true.
- A network-scale game test: 50 energy drawers on one Storage Controller, all counted as item
  handlers, no network rebuild across 60 idle ticks.
- Storage upgrades scale the drawer, through `immaterialdrawers:energy_storage_modifier` — our own
  size component, attached to Functional Storage's upgrade items at load. 500,000 FE unupgraded,
  2,048,000,000 FE with four Netherite upgrades, calibrated so all four slots do something and the
  fourth still fits in the int that `IEnergyStorage` is built on.
- An upgrade cannot be pulled out of a drawer too full to do without it.
- Void and creative behave as they do on a fluid drawer. Locking does not, deliberately: energy has
  one content type, so there is nothing to lock a drawer to.
- Framed Energy Drawer. Framable with Functional Storage's own recipe — its `FramedBlock` check is
  an interface test, so the recipe accepts a block from another mod unchanged.
- Project scaffolding: NeoForge 21.1.248 / Minecraft 1.21.1, ModDevGradle 2.0.75, Titanium and
  Functional Storage as hard dependencies.

### Notes
- Nothing is released yet and nothing is published. There is no texture, model, recipe or controller
  integration; the block exists and works, and that is all.
- Comparator output is redefined here rather than inherited: Functional Storage dispatches on its
  own tile types and would report an energy drawer as permanently empty.
