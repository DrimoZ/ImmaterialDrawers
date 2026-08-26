# Review: `feat/energy-drawer-spike` → `main`

**Reviewed**: 23 August 2026
**Scope**: 22 commits, 26 Java files, 3062 lines added
**Decision**: **APPROVE** — all findings fixed in `feat/energy-drawer-spike`, 15/15 tests green

> Resolved 23 August 2026. The CRITICAL is fixed and pinned by `pushingEnergyNeverCreatesIt`,
> which was checked against the defective code first: 1 FE seeded, 1000 FE present after 40 ticks.
> Findings 2 to 6 are fixed as described. Finding 7 was a comment, and got one.

The working tree is clean, so the command's local mode had nothing to look at. The reviewable unit
is the branch, which is what this covers.

## Summary

The architecture is sound and unusually well evidenced: each risky third-party reading is pinned by
a game test, and the two upstream traps found along the way (the 1.5.7 chunk-load deadlock, the
linking-does-not-build-the-network timing) are documented where they will be found again. The build
is green and all 14 game tests pass.

One defect is serious enough to block: the push-to-neighbours path can create energy from nothing.
It is covered by no test, which is precisely why it survived.

## Findings

### CRITICAL

**1. `EnergyDrawerTile.pushToNeighbours` duplicates energy** —
`src/main/java/dev/drimoz/immaterialdrawers/block/tile/energy/EnergyDrawerTile.java`

```java
int budget = EnergyScaling.transferPerOperation(energyStorage.getCapacityRaw());
...
int accepted = other.receiveEnergy(budget, false);   // offers the full budget
if (accepted > 0) {
    energyStorage.extractEnergy(accepted, false);    // gives up only what it has
```

`budget` is derived from **capacity**, never clamped to what the drawer actually **holds**. The
offer to the neighbour is committed (`simulate = false`), but `extractEnergy` returns
`min(stored, accepted)` — so the difference is conjured.

*Reproduction:* a base drawer (capacity 500,000 → budget 2,500) holding 1 FE, beside anything that
accepts energy. The neighbour receives 2,500; the drawer loses 1. **2,499 FE created, on every push,
so every 4 ticks.** A nearly-empty drawer next to a machine is an infinite generator, and the tick
guard (`getStoredRaw() <= 0`) does not prevent it — it only requires 1 FE to be present.

*Fix:* offer only what is available, taking the creative case into account.

```java
long available = isCreative() ? budget : Math.min(budget, energyStorage.getStoredRaw());
if (available <= 0) return;
int accepted = other.receiveEnergy((int) available, false);
```

*Needs a regression test.* Its absence is the reason this shipped — every other architectural claim
on this branch is pinned by a game test, and this path is not.

### HIGH

None.

### MEDIUM

**2. A fresh `ControllerEnergyStorage` per capability query** — `ImmaterialDrawers.java`,
`registerCapabilities`. The provider returns `new ControllerEnergyStorage(tile)` on every lookup.
NeoForge's `BlockCapabilityCache` is built to hold a stable object; handing out a new one each time
defeats that and makes identity-based invalidation meaningless. The drawer providers correctly
return the tile's own storage — the controller should hold one instance per tile too.

**3. `ControllerEnergyStorage` walks the network on every accessor.** `getStoredLong()` and
`getCapacityLong()` each call `drawers()`, so a single Jade or TOP query costs two full walks —
50 `getBlockEntity` calls apiece on a 50-drawer wall, at HUD refresh rate. The comment justifying no
caching is right (a stale reference voids energy); the cheap fix is to walk once per call site and
pass the list, not to cache across ticks.

**4. Neighbour scan cost.** `pushToNeighbours` does up to six `getBlockEntity` plus six
`getCapability` per drawer per 4 ticks. On the 50-drawer wall the tests exercise, that is roughly 75
lookups per tick. `BlockCapabilityCache` is the idiomatic fix, and CLAUDE.md §7 is explicit that
this mod's whole design exists to avoid exactly this kind of per-tick cost.

**5. `receiveEnergy` can return a negative.** `BigEnergyStorage`: `Math.min(capacity - energy,
toReceive)` goes negative when `capacity < energy`, and that value is returned even though the
guarded store does not run. `setCapacity` clamps, so the window is narrow — but `receiveEnergy`
returning a negative violates the `IEnergyStorage` contract, and `Math.max(0, …)` closes it.

### LOW

**6. `(level.getGameTime() + pos.asLong()) % interval`** — `pos.asLong()` is frequently negative, so
this is a negative modulus. It still fires and the spread across positions still works, but
`Math.floorMod` says what is meant.

**7. Void drawers make the controller over-report.** A void drawer in a network returns the full
amount from `receiveEnergy` while storing less, so `ControllerEnergyStorage.receiveEnergy` reports
acceptance of energy that was deliberately destroyed. That is the void contract and probably right;
worth a comment so the next reader does not file it as finding 1 again.

## Validation

| Check | Result |
|---|---|
| Compile (`./gradlew build`) | Pass, no warnings |
| Tests (`runGameTestServer`) | Pass — 14/14 |
| Lint | N/A — no linter configured |
| Type check | N/A — covered by compilation |

## What is good, and worth keeping

- Every load-bearing reading of Functional Storage and Titanium is pinned by a game test whose
  failure message explains what the failure *means*, not merely what differed.
- Third-party integrations are thin adapters over one source of truth
  (`BigEnergyStorage` / `ControllerEnergyStorage` / `EnergyFormat`) rather than reimplementations —
  the rule CLAUDE.md §11bis sets, and it held for both Jade and TOP.
- Reversed decisions are marked as reversed, with the evidence that reversed them, instead of being
  quietly edited away.
