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
  2,048,000,000 FE with four Netherite upgrades, calibrated so all four slots do something.
- An upgrade cannot be pulled out of a drawer too full to do without it.
- Energy is stored in a long and clamped only at the standard capability, so a drawer can hold far
  more than an int expresses. Jade's own energy bar shows the real figure, fed through its
  registerEnergyStorage hook rather than a second line of our own; the controller sums its whole
  network the same way. The One Probe gets the same treatment through its own API.
- The drawer hands energy to its neighbours on its own, because nothing in the Forge Energy
  ecosystem pulls - a cable set to "extract" is describing its own output side, not draining what is
  behind it.
- Every number is in a config file, including the ones Functional Storage keeps fixed in code.
- The drawer looks like Functional Storage's fluid drawer, empty, in graphite and copper - its models
  are inherited from theirs, not copied. Inside, an energy cube turns: an open frame, and a faceted
  core that is absent when the drawer is empty and grows more solid, and spins faster, as it fills.
  The core is drawn at full brightness, so a charged drawer reads in the dark. The amount is written
  where a fluid drawer writes its own, on the bottom rail of the front.
- Void and creative behave as they do on a fluid drawer. Locking does not, deliberately: energy has
  one content type, so there is nothing to lock a drawer to.
- Framed Energy Drawer. Framable with Functional Storage's own recipe — its `FramedBlock` check is
  an interface test, so the recipe accepts a block from another mod unchanged.
- Data generation: blockstates, item models, loot tables, recipes and en_us, generated and committed.
  Block models and textures are hand-authored.
- The framed drawer registers its own tint handler, reusing Functional Storage's implementation.
  Theirs is generic; only their registration is not, since it is driven by a scan of their own
  block registry.
- Wireless Charger augment: drop it in a drawer's utility slot and the wall keeps whatever the
  players in range are carrying topped up - main inventory, armour and offhand. It goes through
  `FunctionalUpgradeBehavior`, Functional Storage's one supported extension point, so there is no
  mixin behind it. Range, throughput and sweep interval are config. Two game tests cover it: that
  the behaviour really is in Functional Storage's registry and on the item, and that the drawer
  loses exactly what the battery gains.
- Functional Storage's own Redstone Upgrade works on an energy drawer, and there is no augment of
  ours for it. Theirs already connects and ticks for us; only its signal read the zero-slot item
  handler and gave up, so the drawer answers that half itself - with the same number the comparator
  reports, because two ways of asking how full a drawer is should not disagree.

### Notes on this release
- The client loads every asset without an error or a warning of ours, framed model loader included,
  and the result has been looked at in game: six drawers from empty to full, and a framed one.
- Recipes: planks around a redstone block, and iron nuggets around a redstone block for the framed
  variant, mirroring the shapes Functional Storage uses for its fluid drawers.
- Project scaffolding: NeoForge 21.1.248 / Minecraft 1.21.1, ModDevGradle 2.0.75, Titanium and
  Functional Storage as hard dependencies.

### Notes
- Nothing is released yet and nothing is published. There is no texture, model, recipe or controller
  integration; the block exists and works, and that is all.
- Comparator output is redefined here rather than inherited: Functional Storage dispatches on its
  own tile types and would report an energy drawer as permanently empty.
