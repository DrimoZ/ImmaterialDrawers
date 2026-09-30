# Porting: back to 1.20.1, forward to 26.1

Written before any port code was touched. It says what each port actually is, so starting one (or not)
is decided with its size known rather than discovered halfway.

Researched 29 September 2026. Sources: the Modrinth API for every dependency, the Functional Storage
and Titanium branches for both targets (cloned into `.reference/`), the published jars of Mekanism 10.4,
Ars Nouveau 4.12 and Botania 456, the NeoForge 26.1.2.112 sources, and `BeaconPack/PORTING.md`, the
author's own 1.21.1 → 26.1 port, finished in August. Anything marked **[verified]** was compiled, read
in a jar, or read in source. Everything else is my reading of this repository against those sources,
and is an estimate until the compiler disagrees.

---

## 1. Where the ecosystem is

**[verified]** Modrinth, 29 September 2026. Newest build per game version.

| | 1.20.1 | 1.21.1 (today) | 26.1.2 |
|---|---|---|---|
| Loader | **Forge** 47.4.23 | NeoForge 21.1 | NeoForge 26.1.2.112 |
| Java | 17 | 21 | 25 |
| Functional Storage | `1.20.1-1.2.14`, Forge only | `1.21-1.5.7` | `26.1-1.6.1` |
| Titanium | `1.20.1-3.8.35` | `1.21-4.0.34` (what FS compiles against) | `26.1.2-4.0.8` |
| Mekanism | 10.4.16 | 10.7.19 | **none** |
| Ars Nouveau | 4.12.7 | 5.13.2 | **none** |
| Botania | **1.20.1-456** | **none** (the port is an unreleased branch) | **none** |
| Jade / TOP | 11.13.3 / 10.0.4 | 15.10.6 / 12.0.8 | 26.1.11 / 14.0.0 |

Three things follow from this table, and together they are the whole plan:

- **1.20.1 is the only version where every drawer can exist, and the only one with Botania.** The
  Mana Drawer that was asked for and deferred (ROADMAP, *Still open*) can only be built there.
- **26.1 is an Energy Drawer port.** Without Mekanism or Ars, the chemical and Source drawers have
  nothing to hold. The optional-dependency guard (CLAUDE.md §16) was written for exactly this: the
  mod loads and works without them.
- **This mod cannot be newer than Functional Storage.** FS stops at 26.1.2, so 26.2 is not a target
  yet, however current it is.

## 2. Order: 1.20.1 first

1.20.1 is still where a large share of modpacks live, it is the version with the most to gain (all
three drawers, plus mana), and it is the harder port. 26.1 is a smaller port with a smaller payoff.
Doing the harder one first also teaches the most about which of our abstractions actually hold.

Both branches exist and build as far as `compileJava` (§5). Neither port has started. **26.1 is on hold** (29 September 2026): the branch stays as it is until 1.20.1 ships.

---

## 3. The 1.20.1 backport

### Toolchain [verified]

ModDevGradle's `legacyforge` plugin (2.0.148), Forge `1.20.1-47.4.23`, Parchment `2023.09.03`, Java 17
toolchain. That is the same build DSL as the 1.21.1 branch, not the ForgeGradle 5 that FS uses, so the
two build files stay comparable. Mod jars go through `modImplementation` / `modCompileOnly`: on legacy
Forge every mod jar is SRG-named and has to be remapped, and a plain `implementation` compiles against
the wrong names.

`./gradlew compileJava`: Minecraft sets up, Titanium, FS, Jade, TOP, Mekanism, Ars and Botania all
resolve and remap, and javac reaches our code. **683 errors in 53 of 59 files.** This is a first pass:
most errors are missing packages, and javac does not report attribution errors behind those. The real
number moves once the imports are fixed.

### What changes, layer by layer

**The loader (mechanical, all files).** `net.neoforged.*` becomes `net.minecraftforge.*`: event bus,
`@Mod`, `Dist`, config, the gametest annotations (`@GameTestHolder`, `@PrefixGameTestTemplate` exist in
Forge). Titanium absorbs part of it: `ModuleController`, `@ConfigFile`, `@Save` / `NBTManager` all exist
in 3.8 **[verified]**.

**Capabilities: simpler than on 1.21.1.** Forge has no `RegisterCapabilitiesEvent`: a block entity
answers `getCapability(Capability, Direction)` with a `LazyOptional`, and our tile is ours. So the
§8 problem (Titanium registering the energy capability for `PoweredTile` only) does not exist: the tile
overrides `getCapability`. The controller is theirs, but `StorageControllerTile.getCapability` falls back
to `super`, and `StorageControllerExtensionTile` forwards to its controller's `getCapability`
**[verified]**. An `AttachCapabilitiesEvent<BlockEntity>` provider on their controller therefore reaches
both, which does what registering on their `BlockEntityType` does today. `IEnergyStorage` exists in
Forge (`net.minecraftforge.energy`) with the same int contract, so `BigEnergyStorage` changes imports,
not maths.

**The controller workaround (§7) holds.** The `instanceof ItemControllableDrawerTile` filter and the
`getConnectedDrawers().size() != items + fluids + extensions` invariant are both in FS 1.2.14
**[verified]** (`ConnectedDrawers.java:68`, `StorageControllerTile.java:91`). The zero-slot item handler
trick carries over unchanged. The game test that proves it is the first one to port.

**Storage upgrades: one override instead of a component.** 1.20.1 has no data components.
`ControllableDrawerTile.getStorageDiv()` is public and `FluidDrawerTile` overrides it with
`FLUID_DIVISOR` **[verified]**; our tile returns `ENERGY_DIVISOR`. `IDComponents`, the
`ModifyDefaultComponentsEvent` attachment and `SizeProvider` all go away. Item stack data (drawer
contents on the dropped item, the Wireless Charger state) moves from components to NBT.

**Augments: no API, and we don't need one.** `FunctionalUpgradeBehavior` does not exist on 1.20.1.
Upgrades are hard-coded `item.equals(FunctionalStorage.X)` checks. But the utility slot accepts any
`UpgradeItem` of `Type.UTILITY` **[verified]** (`ControllableDrawerTile.java:66`), and the tile that has
to act on the Wireless Charger is ours. So the charger is an `UpgradeItem` subclass, and our own
`serverTick` scans the utility slots for it. The Redstone Upgrade works the same way it does today:
their `serverTick` updates neighbours for it, their `DrawerBlock.getSignal` reads the empty item handler,
and our block overrides `getSignal`.

**Framed: less is shared.** FS 1.20.1 has no framed fluid drawer, no `FramedBlock` interface and no
`FramedTile`. Its `FramedDrawerRecipe.matches` tests `instanceof FramedDrawerBlock` **[verified]**, so our
framed drawers are not framable by their recipe. We need our own recipe serializer (their class is
~60 lines to adapt) and our own model-data plumbing. The `framedblock` model loader exists and is
registered by FS **[verified]**; whether it takes our JSON unchanged is to be checked in client.

**Models: inherit from a different parent.** 1.20.1 has no `side_machine` / `fluid_front_1` /
`fluid_inner_1`: the fluid drawer is one monolithic `fluid_1.json`, with textures `#3` side, `#4` top,
`#6` front, `#8` inner **[verified]**. Our energy drawer can be a child of it that only swaps textures,
which is simpler than today's composite. The framed variants have no FS model to inherit from.

**The 1.5.7 deadlock.** 1.2.14 has the same `isLoaded` + `getBlockEntity(controllerPos)` in
`invalidateCaps` **[verified]**. On Forge 1.20.1, `invalidateCaps` runs on removal and unload rather
than inside the chunk post-load, so I expect no deadlock. **Corrected at step 1:** the override cannot be kept. On 1.21.1 it replaces
theirs and redoes NeoForge's one-line invalidation by hand; on Forge, skipping their `invalidateCaps`
means skipping `BlockEntity.invalidateCaps` too, which Java cannot call past them. It is dropped until a
world actually hangs.

**Rendering, GUI, datagen.** `GuiGraphics` exists since 1.20, `BlockEntityRenderer` has the same shape,
and the Forge data providers mirror NeoForge's (they were forked from them). Our `feature_enabled` recipe
condition becomes a Forge `ICondition` + serializer. I expect this layer to be mostly renames.

### The optional mods on 1.20.1

**Mekanism 10.4: four handlers, not one.** **Decided 29 September 2026: one drawer for all four types**, same registry names as 1.21.1. The unified Chemical API is 10.7. In 10.4, gases, infusion
types, pigments and slurries each have their own handler and capability (`IGasHandler`,
`IInfusionHandler`, `IPigmentHandler`, `ISlurryHandler`) **[verified in the jar]**. Mekanism's own tanks
bridge the four with `mekanism.api.chemical.merged.MergedChemicalTank` / `BoxedChemical`, which are
in the API **[verified]**. That is the model: a drawer slot holds one chemical of any of the four
types, and exposes four capabilities that each see only their own type. This is the largest single
piece of the backport: `BigChemicalHandler` (285 lines) and its controller aggregate are rewritten,
not migrated.

**Ars Nouveau 4.12: easier than 5.13.** 4.12 has `ISourceTile`, `SourceManager` and `SourceUtil`, and no
`ISourceCap` capability **[verified in the jar]**; that capability arrived in 5.x. Our tile can implement
`ISourceTile` directly, which removes the `BigSourceStorage` / `TileView` split that exists only because
the two 5.x interfaces clash (CLAUDE.md §18). The creative-drawer and simulated-removal traps from §18
need to be rechecked against the 4.12 bytecode, not assumed.

**Botania 456: the Mana Drawer, new on this branch.** What a mana block has to be **[verified in the
jar]**: `ManaReceiver` (`getCurrentMana`, `isFull`, `receiveMana(int)`, `canReceiveManaFromBursts`) and
`ManaPool` on top of it (`getMaxMana`, `isOutputtingPower`, a colour), exposed through
`BotaniaForgeCapabilities.MANA_RECEIVER`. A pool is registered with the mana network by posting a
`ManaNetworkEvent` (`ManaBlockType.POOL`, add/remove). That event is how spreaders and nearby
consumers find it: the same pattern as Ars's `SourceManager`, and no mixin. Sparks need `SPARK_ATTACHABLE`
too. Mana is an int. A Mana Pool holds 1,000,000, so the fluid curve (§18) fits under the int ceiling,
as it does for Source. What a mana *drawer* offers over a pool (upgrades, the controller network, the
wall) is a design question for `DESIGN.md`, not a technical one.

### World compatibility

Registry names do not change between branches, so a drawer placed in 1.20.1 is still a
`immaterialdrawers:energy_drawer` in 1.21.1. Its block entity data survives the upgrade if every `@Save`
field keeps its name. **Keep the NBT keys identical to 1.21.1.** Item stacks carrying data (a picked-up
full drawer) go through vanilla's 1.20.5 item-component upgrade. What happens to Titanium's NBT there is
not checked.

---

## 4. The 26.1 port

### Toolchain [verified]

Gradle 9.1, Java 25, ModDevGradle 2.0.148, NeoForge 26.1.2.112, Titanium 26.1.2-4.0.8, FS 26.1-1.6.1.
No Parchment: Mojang stopped obfuscating in 26.1. The chemical and Source drawers are excluded from the
source set and left out of `neoforge.mods.toml`, not deleted. They come back in one commit the day
Mekanism or Ars ships for 26.x. `BeaconPack` did the same for EMI.

`./gradlew compileJava`: **206 errors in 24 of the 37 files left.** First pass, same caveat as above.
The dependency-free core (`EnergyScaling`, `EnergyFormat`, the configs) compiles.

### What changes

The rename layer is the one BeaconPack already measured: `ResourceLocation` → `Identifier`,
`ResourceKey#location` → `#identifier`, `Item.Properties#setId`, input events as objects. What is
specific to this mod:

**Energy: the int ceiling goes away at the capability.** `Capabilities.Energy.BLOCK` is now typed
`EnergyHandler`, and `EnergyHandler.getAmountAsLong()` / `getCapacityAsLong()` return **long**
**[verified, NeoForge sources]**. `IEnergyStorage` still exists, `@Deprecated(forRemoval = true)`, with
an `IEnergyStorage.of(EnergyHandler)` bridge. Insert and extract stay int per operation. For §11bis this
means cables and meters that use the new API see the real total, and the "standard capability: clamped
at 2.1B" row becomes history on 26.1. The clamp stays for per-operation transfer only.

**Transactions: the real rewrite.** NeoForge's transfer API is transactional: `insert` / `extract` take
a `TransactionContext`, and FS's own `BigFluidHandler` became a `SnapshotJournal` **[verified]**
(273 changed lines). `BigEnergyStorage` has to do the same: snapshot `stored`, restore it on abort. It is
the one class where a mistake creates or deletes energy, and the conservation tests are what catch it.
**Migrate, don't bridge**: the bridge is deprecated for removal, and BeaconPack came to the same
conclusion for items.

**The zero-slot handler.** `ItemControllableDrawerTile.getStorage()` returns
`ResourceHandler<ItemResource>`, and `ConnectedDrawers` holds a `List<ResourceHandler<ItemResource>>`
**[verified]**. Our empty handler becomes a `ResourceHandler` with `size() == 0`. The filter and the
invariant are unchanged in 1.6.1 **[verified]** (`ConnectedDrawers.java:79`,
`StorageControllerTile.java:79`), so the §7 workaround survives.

**FS's own API moved little.** `FunctionalUpgradeBehavior`: the `Identifier` rename, nothing else
**[verified]**. `SizeProvider.calculateAsFactor` takes a `ResourceHandler<ItemResource>`.
`FramedDrawerRecipe` still matches on the `FramedBlock` interface. `FramedModel` changed by 691 lines,
so our framed JSON has to be checked against its new format in client.

**Rendering and datagen: rewrites.** Block entity renderers now extract a render state first and draw it
afterwards (`BlockEntityRenderState`; FS's `FluidDrawerRenderer` changed by 191 lines **[verified]**).
`EnergyDrawerRenderer` reads the clock and the charge for the cube, and that becomes state extraction.
BeaconPack found that the screen side of the same split migrated by renaming; renderers are less
certain. The model generators are gone (1.21.4 client items), which means `IDBlockStateProvider` and
`IDItemModelProvider` are rewritten. Datagen splits into client and server runs.

**Game tests: the biggest unknown.** `net.minecraft.gametest.framework.GameTest` no longer exists
**[verified by the compiler]**. BeaconPack found the framework rearchitected, not renamed. We have 52
game tests, and they are the proof the rest of the port works, so porting them comes first, not last.

---

## 5. Branches

One branch per game version. Each keeps getting files; none replaces another.

| branch | what it is |
|---|---|
| `main` | documentation and planning, and 1.21.1 until a port ships. |
| `1.20.1` | the backport. Branched from `main`. Toolchain in place, compiles to 683 errors. |
| `26.1` | the port. Branched from `main`. Toolchain in place, compiles to 206 errors. |
| `1.21.1` | to create when `main` moves on, so 1.21.1 hotfixes have somewhere to live. |

A fix to shared logic (scaling, formatting, conservation) lands on the version where it was found, then
is cherry-picked. Nothing is merged across versions wholesale.

Version numbers keep the `{mod}+{minecraft}` form: `0.2.0+1.20.1`, `0.2.0+26.1.2`. Same number, same
features, **except** where §1 says otherwise. The CHANGELOG says which drawers each file has.

## 6. Work order for 1.20.1

Same logic as CLAUDE.md §12: the first task decides whether the rest is worth doing.

1. **Spike, as a game test.** The energy capability through our `getCapability`, the zero-slot tile
   counted in `itemHandlers`, 50 drawers on a controller without a rebuild, and FE through their
   controller via `AttachCapabilitiesEvent`. If any of these fails, stop and rethink.
2. **Storage and upgrades.** `BigEnergyStorage` imports, `getStorageDiv`, and the scaling tests.
3. **Framed, models, renderer, GUI, datagen.** The framed recipe is ours now.
4. **Wireless Charger** as an `UpgradeItem`, and the Redstone Upgrade signal.
5. **Jade, TOP.**
6. **Ars 4.12**, the smaller optional port.
7. **Mekanism 10.4**, the merged-tank rewrite.
8. **Botania: the Mana Drawer.** Design first (`DESIGN.md`), then build it the way the Source Drawer
   was built.

## 7. Still open

- **The Mana Drawer's design**: capacity, whether it is a pool for spreaders or only a buffer, sparks.
- **Is the Mana Drawer 1.20.1-only for good?** Only if Botania never ships 1.21.1. If it does, the
  drawer is ported forward like the others.
- **Modrinth.** Both ports make reserving the slug more pressing: CurseForge files per version are
  fine, but Forge 1.20.1 players look on Modrinth too.

## 8. What actually happened

*Written as each step lands: the real error counts, where the estimate above was wrong, and what no
compiler could have predicted. BeaconPack's version of this section is the reason this one exists.*

### 1.20.1, step 1: the spike (29 September 2026)

**All 6 spike tests pass** on Forge 47.4.23 with Functional Storage 1.2.14: the energy capability from
the tile and from every side, storing what it is given, the zero-slot tile counted as an item drawer,
**50 drawers on a controller with no rebuild over 60 ticks**, and FE in and out through their controller
via `AttachCapabilitiesEvent`. The architecture holds on 1.20.1, as read.

How: the energy core only (13 files) was ported, and `build.gradle` lists the ported files explicitly;
the rest of the tree stays out of the build until its step. The list is the progress.

What the reading did not predict:

- **Forge 1.20.1 ignores a mod's resources without `pack.mcmeta`.** No error, no warning: the game
  test structures were simply not there. NeoForge dropped the requirement, so the 1.21.1 tree never
  had one.
- **Test structures live in `data/<ns>/structures/`** (plural) on 1.20.1, `structure/` on 1.21.1.
  The 1.21.1 `.nbt` files (data version 3955) load as they are on 1.20.1.
- **`GameTestHelper` has no `assertValueEqual`** on 1.20.1; the tests carry a four-line equivalent.
- **The deadlock override had to go** - see §3.
- Registration hands back a `Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>>`, not a
  `BlockWithTile`, and FS 1.20.1's blocks are `RotatableBlock` + a one-method `Drawer` interface, so
  `ImmaterialDrawerBlock` carries the interaction, drops and unlinking that FS 1.21 keeps in a base
  class. More code than on 1.21.1, all of it copied from their fluid drawer.

### 1.20.1, step 2: storage upgrades (29 September 2026)

**12/12.** The five scaling tests carry over unchanged in substance: base capacity, all four Netherite
slots strictly increasing, Max Storage positive and past the int ceiling, the removal guard, the
bottomless creative drawer. Plus conservation on push, against Powah's starter cell.

No tile code changed for it: `getStorageDiv()` returning `ENERGY_DIVISOR` and the removal guard were
written at step 1. The estimate held - one override replaces the whole component of 1.21.1.

Worth knowing:

- **Functional Storage folds the multiplier into an int** (`mult *= calculated`). Java's compound
  assignment saturates rather than wraps, so Max Storage lands at `Integer.MAX_VALUE / 4` and our long
  capacity goes on from there. The guard reuses exactly that arithmetic, truncation included, so the
  two can never disagree about what fits.
- **The Iron downgrade is refused**, where 1.21.1 treats it as a reset to base size. On 1.20.1 it only
  flags an item drawer as downgraded; Functional Storage's own fluid drawer refuses it too.
- **Powah 5.0.11 needs Architectury and Cloth Config** in the dev run, as `modRuntimeOnly`.

### 1.20.1, step 3: framed, client, data (30 September 2026)

**18/18**, and the client loads with **0 errors and 0 missing textures**. Not yet looked at in game: the
first visual check is still to do.

Where the estimate was right and where it was not:

- **Framing cost more than on 1.21.1, as predicted.** No `FramedBlock` / `FramedTile` interfaces, so the
  style handling (place, drop, pick-block), the tint handler and **our own framing recipe**
  (`FramedEnergyDrawerRecipe`, serializer `immaterialdrawers:framed_recipe`) are ours. Their static
  `FramedDrawerBlock.fill` / `getDrawerModelData` and the `Style` tag are reused, and their
  `framedblock` loader takes our model unchanged - it reads the `ModelData`, not the tile class.
- **The models did not "just swap parents".** Our textures are recolours of FS 1.21's *machine*
  casing, drawn for 1.21 geometry; FS 1.20.1's `fluid_1.json` is a different casing. Four FS 1.21
  geometries (`side_machine`, `side`, `fluid_front_1`, `fluid_inner_1`) are copied into
  `models/block/fs/` (NOTICE updated), so the block looks the same on both branches. Their default
  textures pointed at FS 1.21-only files (`machine_*`, `placeholder`) - the client log said so, and they
  now point at ours and at `framed_side`.
- **Datagen ported cleanly**: Forge's providers mirror NeoForge's. Blockstates are simpler (four
  horizontal facings). The config condition became an `ICondition` + JSON serializer, and Titanium's
  conditional recipe builder takes it as an extra condition.
- **The 1.21.1 `src/generated` output was deleted and regenerated** on this branch: NeoForge conditions
  and blocks this branch does not have yet.
- **`run/` is shared across branches.** The 1.20.1 client and server now use `run-1.20.1/`: a 1.21.1 save
  opened in 1.20.1 is a downgrade.
- The not-yet-ported chemical and Source models are excluded from the jar until their steps: Forge logs
  NeoForge's composite loader as a failed model load.
