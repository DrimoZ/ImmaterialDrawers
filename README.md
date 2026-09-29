# Immaterial Drawers

Drawers for what you can't hold.

An addon for [Functional Storage](https://github.com/Buuz135/FunctionalStorage). Functional Storage
stores items and fluids; this one adds drawers for everything else. **Energy first** — a drawer that
holds FE, scales with the storage upgrades you already use, and sits in a wall of drawers instead of
next to one.

The umbrella name is deliberate. Energy is the first module, not the mod.

**Chemicals second**, with Mekanism installed: Chemical Drawers for gases, infuse types, pigments and
slurries, in the 1x1, 1x2 and 2x2 layouts of Functional Storage's fluid drawers. Mekanism is optional —
without it those blocks simply do not exist.

> **Status: pre-release.** The drawer works, is drawn, joins a Storage Controller's network and
> answers the energy capability, proven by game tests and looked at in game. Nothing is published
> anywhere yet. See [ROADMAP.md](ROADMAP.md).

## Requires

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1+ (no Fabric, no multiloader) |
| Functional Storage | 1.5.7+ |
| Titanium | 4.0.34+ (Functional Storage needs it too) |
| Mekanism *(optional)* | 10.7+, for the Chemical Drawers |

## Building

```
./gradlew build                # jar in build/libs/
./gradlew runClient
./gradlew runGameTestServer    # the tests that hold the architecture up
./gradlew runGameTestServer -PnoMekanism   # the same, without Mekanism in the run
```

Java 21. Gradle provisions its own toolchain if the system JDK differs.

## Documentation

The **GitHub wiki is the reference**; this file is the summary.

- [ROADMAP.md](ROADMAP.md) — what is being built, in order
- [CHANGELOG.md](CHANGELOG.md)
- [SPIKE.md](SPIKE.md) — why the mod is built the way it is, and what was checked before it was
- [DESIGN.md](DESIGN.md) — the reasoning behind the product decisions (French)
- [CLAUDE.md](CLAUDE.md) — operational constraints and architecture (French)

## Licence

Code is [MIT](LICENSE). Assets are [All Rights Reserved](LICENSE-ASSETS). Modpacks may include the
published jar unmodified.

Functional Storage is MIT — parts of this mod are adapted from it, and [NOTICE](NOTICE) says which.
Titanium is LGPLv3 and is never bundled.
