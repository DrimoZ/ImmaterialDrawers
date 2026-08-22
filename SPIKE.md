# Capability spike — what actually happened

**What this file is:** the record of the blocking task in CLAUDE.md §12 — the one that was allowed
to invalidate the whole architecture. It follows the habit of `PORTING.md` on Portable Beacons: an
analysis written before the code, then completed afterwards with what really happened and where the
estimate was wrong. The gap between the two is the most useful thing in it.

**What this file is not:** documentation. Nothing here describes how to use the mod, and nothing
here is maintained. It is dated and it stays dated.

- **Written:** 22 August 2026
- **Verdict:** the design in CLAUDE.md §7 and §8 holds. Four game tests pass. No architecture change.

---

## The question

CLAUDE.md §8 called this risk n°1 and said, correctly, that it had not been verified:

> Titanium registers `Capabilities.EnergyStorage.BLOCK` for every block entity type it creates,
> through a provider that only answers for a `PoweredTile`. We cannot be a `PoweredTile` — Java has
> single inheritance and we must be a `ControllableDrawerTile`. Will NeoForge accept a *second*
> provider for the same capability on the same block entity type, and fall through to it when
> Titanium's returns null?

If the answer were no, the mod would have had to bypass Titanium's registration entirely or reach
for a mixin.

## The answer

**Yes, and the reason is in the source rather than in the behaviour**, which is worth more than a
passing test on its own. `BlockCapability.getCapability` keeps a *list* of providers per block and
walks it:

```java
for (var provider : providers.getOrDefault(state.getBlock(), List.of())) {
    var ret = provider.getCapability(level, pos, state, blockEntity, context);
    if (ret != null)
        return ret;
}
return null;
```
<sub>`net/neoforged/neoforge/capabilities/BlockCapability.java`, NeoForge 21.1.248</sub>

Titanium's provider returns null for anything that is not a `PoweredTile`. Ours is next in the list
and answers. Registration order does not matter here — with one of the two always returning null,
either order resolves the same way.

`./gradlew runGameTestServer`: **4 required tests, 4 passed, 2.1 s.**

## What the tests pin down

| Test | What breaks if it goes red |
|---|---|
| `energyCapabilityIsPresent` | NeoForge stopped falling through providers, or Titanium's provider stopped returning null. The design in §8 is gone. |
| `energyCapabilityIsPresentFromEverySide` | The capability became side-dependent. A drawer in a wall is cabled from whichever side has room. |
| `energyCapabilityStoresWhatItIsGiven` | The provider hands out a detached or per-query storage object. Silent FE loss — the worst possible failure mode here. |
| `drawerCountsAsAnItemDrawerButHoldsNoItems` | Either half of the §7 hack. Lose the `ItemControllableDrawerTile` half and every controller drops us from its network; lose the zero-slot half and we advertise phantom item slots. |

The fourth one is the one to keep an eye on. Its failure mode in the wild is not a visible bug: it
is `StorageControllerTile.serverTick` finding its invariant false forever and rebuilding the entire
network every tick, on every controller in the world.

## Where the brief was wrong

Small things, all of them found by the compiler or by a lookup, none of them structural.

1. **Functional Storage 1.5.8 is not a release.** `1.5.8` is `mod_version` on the `1.21` branch —
   the head the brief was read from. The newest *published* 1.21.1 build is **1.5.7**
   (1 June 2026), and that is what this mod compiles against. The brief's file references are all
   still accurate; they just describe code half a version ahead of the jar.

2. **That gap already cost one compile error.**
   `ControllableDrawerTile.updateComparatorOutput()` is public on the branch and absent from 1.5.7.
   `EnergyDrawerTile` calls `Level.updateNeighbourForOutputSignal` instead — vanilla, stable, and
   not worth a hard floor on the Functional Storage version. This is the "permanent tax" of
   CLAUDE.md §11 arriving on day one, in the mildest possible form.

3. **CurseMaven is not needed for Functional Storage.** The brief assumed it, because Functional
   Storage publishes to no public maven. Modrinth's maven serves the released jar by version
   number — `maven.modrinth:functional-storage:1.21-1.5.7` — with no opaque `fileId` to look up by
   hand every time the dependency moves. CurseMaven stays declared for Jade and The One Probe.

4. **`getSelf()` was missed.** Titanium's `ActiveTile` requires it. Two lines, but it is the kind of
   thing a reading of the *supertypes we planned to use* would have caught and a reading of the
   *code we planned to copy* did not.

5. **`Titanium 1.21-4.0.34` is exactly right** — not the newest (4.0.45 is), but the one Functional
   Storage itself compiles against, which is the version that matters.

## What the brief got right, and had no business getting right without running anything

- `ConnectedDrawers.java:78-80` — the `instanceof` filter, verbatim.
- `StorageControllerTile` — the `itemHandlers + fluidHandlers + extensions` invariant, verbatim.
- `Titanium DeferredRegistryHelper:117-125` — the `EnergyStorage.BLOCK` auto-registration, verbatim.
- `Drawer.getAnalogOutputSignal` — dispatches on `FluidDrawerTile` then `ItemControllableDrawerTile`,
  so an energy drawer does fall through to the item branch and would report zero forever.
  Overridden in `EnergyDrawerBlock`.
- The zero-slot `IItemHandler` is inert in the controller's aggregation, exactly as the line-by-line
  reading of `ControllerInventoryHandler` predicted.

## Still unverified

The spike proves the block exists, holds energy, and is reachable. It does not prove the drawer
behaves in a *network*:

- **No controller was ever linked.** CLAUDE.md §12 task 2 — link a wall of drawers with the Linking
  Tool and confirm at the profiler that no per-tick rebuild happens with 50+ drawers. The invariant
  is proven arithmetically and by a unit-scale test; it is not proven at scale.
- **No upgrades.** `getStorageMultiplier()` is not wired to the storage capacity yet, so
  `ENERGY_DIVISOR` and `BASE_CAPACITY` are both still guesses (task 4).
- **Nothing renders.** No blockstate, model, texture or lang entry exists. The game test server does
  not care; a client will.
