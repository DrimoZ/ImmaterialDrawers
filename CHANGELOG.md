# Changelog

Newest first. Versions are `{mod}+{minecraft}`.

## Unreleased (1.20.1)

### Notes
- The three drawer screens share one layout, which reads Functional Storage's slot and hover
  rectangles instead of carrying copies of them. Comparator and Redstone Upgrade handling, the walk
  over a controller's network, and the number and indicator on chemical and Source drawer fronts
  are each written once instead of per drawer - the same cleanup as 1.21.1. No behaviour change.

## 0.2.0+1.20.1 — 2026-09-30

The same drawers as 0.2.0 for 1.21.1, on Forge 1.20.1 with Functional Storage 1.2.14. What
differs comes from the older mods it runs beside.

### Added
- Energy Drawer and its framed twin, with storage upgrades, the Storage Controller reaching every
  drawer on its network, pushing into neighbours, Functional Storage's Redstone Upgrade, the
  Wireless Charger, and Jade / The One Probe bars.
- Chemical Drawers, when Mekanism 10.4 is installed: 1x1, 1x2 and 2x2, framed or not. One drawer
  holds any of Mekanism's four chemical types - gases, infuse types, pigments, slurries; a tank takes
  the type of whatever enters it first. Pipes of every type connect, and so does a Storage Controller.
- Source Drawer, when Ars Nouveau 4.12 is installed, framed or not. The enchanting apparatus,
  imbuement and sourcelinks find it like a Source Jar.

### Notes
- **Ars relays do not connect to the Source Drawer on 1.20.1.** In Ars 4.12 a relay only moves
  Source between Ars's own machines, which a Functional Storage drawer cannot be. On 1.21.1 they do.
- The framed drawers are framed with the usual crafting grid, through a recipe of this mod's:
  Functional Storage 1.20.1's own framing recipe only accepts its own blocks.
- The Iron downgrade does not go into these drawers, as it does not go into Functional Storage's
  fluid drawer on 1.20.1.
- Registry names and saved data match 1.21.1, so drawers placed on 1.20.1 are still there after the
  world moves to 1.21.1.
- A framed drawer dressed in leaves (or any block whose texture is mostly holes) shows through its
  sides - Functional Storage's own framed drawers do the same on 1.20.1.

### Fixed
- The amount on the drawer screens sat off-centre, to the left of the drawer front: its width was
  counted at full scale for text drawn at half scale. Energy, Source, and the 1x1 and 1x2 chemical
  drawers; the 2x2 was already placed right.

## 0.2.0+1.21.1 — 2026-09-29

Two new kinds of drawer, each arriving with the mod it belongs to, and two fixes every 0.1.0 player
should have.

### Added
- Chemical Drawers, when Mekanism is installed: gases, infuse types, pigments and slurries, in the
  three layouts Functional Storage gives its fluid drawers (1x1, 1x2, 2x2), each with a framed twin.
  Same capacities as the fluid drawer beside them, scaled by the same storage upgrades; same fill
  order, locking, void and creative. Pressurized tubes connect from any side, and a Storage
  Controller or extension exposes every chemical tank on its network. Right-click a slot with a
  chemical tank to empty it in, left-click to fill it back.
- They look like the fluid drawer, in the graphite family of the energy drawer, with a teal window
  where energy has copper. Gases glow and their surface slowly breathes; heavier chemicals sit still
  like a fluid.
- Without Mekanism nothing changes: the blocks are not registered, no Mekanism class is loaded, and
  the recipes are conditioned on it.
- Source Drawer, when Ars Nouveau is installed: a drawer of Source, framed or not. Relays connect
  to it through Ars's Source capability, and everything that draws Source from nearby - the
  enchanting apparatus, imbuement, sourcelinks filling it - finds it the way it finds a Source Jar.
  32,000 Source unupgraded, over two billion with four Netherite upgrades. A Storage Controller
  exposes the Source of its whole network to a relay. Violet-rimmed, with Ars's own Source glowing
  inside.
- A switch per feature in the config - Energy Drawer, Chemical Drawers, Source Drawer, Wireless Charger. Off means
  no recipe and no creative-tab entry; drawers already placed keep working, and worlds and
  multiplayer are unaffected.

### Changed
- A new logo showing the whole family - the energy drawer's cube, a chemical drawer, a source
  drawer and a 2x2 chemical drawer - instead of four energy drawers.
- The Wireless Charger has a new face: Functional Storage's own utility-upgrade plate and port,
  with copper wave arcs and a lit port, instead of a flat dark tile that belonged to no mod.

### Fixed
- Right-clicking or left-clicking the front of an Energy Drawer with anything in hand threw on the
  server (`Slot 0 not in valid range - [0,0)`). Functional Storage offers the held stack to the
  drawer's item handler at the slot that was clicked, and the empty handler validated that index.
  It is now NeoForge's `EmptyItemHandler`, which has no slots and does not mind being asked.
- Breaking an Energy Drawer dropped nothing - not the block, not the energy in it, not its upgrades.
  The drawers need the correct tool to drop, and were in no `mineable` tag, so no tool was correct.
  Every block of the mod is now generated into `minecraft:mineable/pickaxe`, like Functional
  Storage's own drawers.

### Notes
- Mekanism and Ars Nouveau are optional dependencies, declared as such. Tested with both, with one,
  and with neither: 52 game tests with both installed, 21 with neither, and no Mekanism or Ars class
  is ever loaded without its mod.
- Tested through the other mods' own blocks, not only through our code: a Mekanism pressurized tube
  moving chemicals between two drawers, a Mekanism universal cable moving energy, an Ars Nouveau
  relay moving Source - each checked to create nothing on the way.
- Botania mana is planned, and waits for a Botania release on 1.21.1.

## 0.1.0+1.21.1 — 2026-09-28

The first release: the Energy Drawer, its framed variant, and the Wireless Charger.

### Added
- A logo, shown in the in-game mod list: four drawers glowing in four colours, because the mod is a
  family of drawers and energy is only the first.
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
- Comparator output is redefined here rather than inherited: Functional Storage dispatches on its
  own tile types and would report an energy drawer as permanently empty.
