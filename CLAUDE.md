# Immaterial Drawers — Addon Functional Storage

**Mod ID : `immaterialdrawers`** — irréversible, voir §2.

**Ce que fait ce fichier :** l'opérationnel. Conventions de nommage figées, cible technique,
contraintes vérifiées dans le code de Functional Storage et de Titanium, architecture, ordre de
travail. C'est le fichier à ouvrir avant d'écrire une ligne.

**Ce que ce fichier ne fait pas :** la réflexion produit — concept, concurrence, décisions arrêtées,
hors scope. Elle est dans `DESIGN.md`, qui garde la numérotation §1, §3, §5, §6 d'origine pour que
les renvois d'ici restent lisibles.

> Recherche menée le 22 août 2026 par lecture des sources de Functional Storage (branche `1.21`,
> `mod_version` 1.5.8) et de Titanium (`InnovativeOnlineIndustries/Titanium`, branche `1.21`),
> clonées en local. **Mis à jour le 22 août 2026 après le spike bloquant** (§12 tâche 1) : ce qui a
> été compilé et exécuté est marqué **[vérifié]**. Le reste est toujours une lecture. Voir §15, et
> `SPIKE.md` pour le compte rendu du spike.

---

## 1. Concept

→ `DESIGN.md` §1.

En une ligne : Functional Storage stocke des items et des fluides ; ce mod ajoute des tiroirs pour
le reste, l'énergie d'abord. Le nom est un parapluie, pas une description.

---

## 2. Identité et conventions de nommage — À RESPECTER STRICTEMENT

Le `modid` préfixe chaque nom de registre et est écrit dans chaque sauvegarde de monde.
**Le changer après une release publique casse les mondes des joueurs.** Il est figé.

| Élément | Valeur |
|---|---|
| Mod ID | `immaterialdrawers` |
| Nom affiché | `Immaterial Drawers` |
| Dépôt | `https://github.com/DrimoZ/ImmaterialDrawers` |
| Licence | **MIT** (code) — voir §4 pour les assets |
| groupId | `dev.drimoz.immaterialdrawers` |
| Package racine | `dev.drimoz.immaterialdrawers` |
| Classe `@Mod` | `ImmaterialDrawers` (extends `ModuleController`) |
| Auteur | `DrimoZ` |

**Convention reprise de PortableBeacons** (mod le plus récent de l'auteur) : `dev.drimoz.<modid>`,
groupId et package racine strictement identiques. Ne **pas** reproduire PunchThemAll
(`com.drimoz.pta` avec un package `com/drimoz/punchthemall`, et un modid `pta` qui ne correspond
à aucun des deux) — c'est l'ancienne façon de faire.

**Interdit dans le `modid` et dans les packages communs :** `energy`, `fe`, `power`.
Le mod n'est pas un mod d'énergie, l'énergie est son premier module.

### Nommage des registres

```
immaterialdrawers:energy_drawer
immaterialdrawers:framed_energy_drawer
immaterialdrawers:wireless_charger        (augment)
immaterialdrawers:energy_storage_modifier (DataComponent)
```

### Découpage en packages

```
dev.drimoz.immaterialdrawers/
  ImmaterialDrawers.java        ← @Mod, extends ModuleController
  IDConfig.java                 ← config (cf. BPConfig dans PortableBeacons)
  registry/                     ← DeferredRegister / enregistrements
  block/                        ← commun à tous les types de tiroir
  block/energy/                 ← EnergyDrawerBlock, FramedEnergyDrawerBlock
  block/tile/energy/            ← EnergyDrawerTile, FramedEnergyDrawerTile
  storage/                      ← BigEnergyStorage et futurs handlers
  augment/                      ← implémentations FunctionalUpgradeBehavior
  client/
  compat/                       ← jade/, top/
  datagen/
  gametest/                     ← IDGameTests (cf. BPGameTests)
```

Toute abstraction partagée (tile de base, logique du hack contrôleur, enregistrement des
capabilities) vit dans `block/` et `storage/`, **jamais** dans un sous-package `energy/`.
Le futur Chemical Drawer doit pouvoir réutiliser la base sans refactor.

### Avant la première release

Vérifier que le slug `immaterial-drawers` est libre sur **CurseForge** et **Modrinth**.
Non confirmé au moment de la rédaction.

---

## 3. Positionnement (concurrence)

→ `DESIGN.md` §3.

---

## 4. Cible technique

| Élément | Valeur | Source |
|---|---|---|
| Loader | NeoForge uniquement | décision |
| Minecraft | 1.21.1 | aligné sur FS |
| NeoForge | `21.1.248` | PortableBeacons (plus récent que FS) |
| Java | 21 | — |
| Build | ModDevGradle `2.0.75` | FS `build.gradle` |
| Parchment | `1.21.1` / `2024.11.17` | PunchThemAll (plus récent que FS) |
| Titanium | `com.hrznstudio:titanium:1.21-4.0.34` | **obligatoire** |
| Functional Storage | `maven.modrinth:functional-storage:1.21-1.5.7` | **[vérifié]** compile |
| Gradle | `8.8` | **[vérifié]** wrapper, build vert |

**[vérifié]** Toute cette ligne compile et lance un serveur de game tests. Deux corrections :

- **`1.5.8` n'est pas une release.** C'est le `mod_version` de la branche `1.21` — la tête lue pour
  ce brief. La dernière build 1.21.1 **publiée** est **1.5.7**. On compile contre 1.5.7 en lisant du
  code 1.5.8 : les références `Fichier.java:NN` restent justes, elles décrivent une demi-version
  d'avance sur le jar. Ça a déjà coûté une erreur de compilation, voir §11.
- **`1.21-4.0.34` est le bon Titanium** — ce n'est pas le plus récent (4.0.45 existe), c'est celui
  contre lequel FS lui-même compile. C'est cette version-là qui compte.

### `gradle.properties` — reprendre le format de PortableBeacons

Setup piloté par propriétés, avec substitution de template
(`src/main/templates/META-INF/neoforge.mods.toml`, **pas** un fichier statique sous `resources/`).

```properties
mod_id=immaterialdrawers
mod_name=Immaterial Drawers
mod_license=MIT (code) / All Rights Reserved (assets)
mod_version=0.1.0
mod_group_id=dev.drimoz.immaterialdrawers
mod_authors=DrimoZ
mod_description=Drawers for what you can't hold. Energy first, more to come.

minecraft_version=1.21.1
minecraft_version_range=[1.21.1]
neo_version=21.1.248
neo_version_range=[21.1,21.2)
loader_version_range=[4,)
parchment_minecraft_version=1.21.1
parchment_mappings_version=2024.11.17
```

**Bornes de version : reprendre celles de PortableBeacons, pas celles de PunchThemAll.**
`[1.21.1,1.22)` acceptait 1.21.2 à 1.21.11 (dix primers de breaking changes), et `[21.1,)`
acceptait NeoForge 26.x. Leçon déjà tirée sur un mod précédent — ne pas la reperdre.

**Titanium est non négociable.** `ControllableDrawerTile` hérite de `ActiveTile` (Titanium),
la persistance passe par `@Save` / `NBTManager`, l'enregistrement par `ModuleController` +
`DeferredRegistryHelper`. On adopte le framework entier.

### Dépendances / maven

Functional Storage **ne publie sur aucun maven public** — son bloc `publishing` pointe sur
`file('repo')`. Titanium est sur le maven BlameJared (`https://maven.blamejared.com/`), déjà
déclaré dans le `build.gradle` de FS.

**[vérifié] — CurseMaven n'est pas nécessaire pour FS.** Le maven de Modrinth sert le jar publié par
numéro de version, sans `fileId` opaque à retrouver à la main à chaque fois que la dépendance bouge :

```gradle
maven { url = 'https://api.modrinth.com/maven'; content { includeGroup 'maven.modrinth' } }
implementation "maven.modrinth:functional-storage:1.21-1.5.7"
```

Les deux sont en `implementation` (compile + runtime) et **ni l'un ni l'autre n'est bundlé** —
c'est ce que fait FS lui-même pour Titanium. CurseMaven reste déclaré, pour Jade et TOP (§11).

### Licences

- **Ce mod : MIT pour le code.** Suivre la convention de PortableBeacons : un fichier
  `LICENSE-ASSETS` séparé (textures, modèles) en All Rights Reserved, et
  `mod_license=MIT (code) / All Rights Reserved (assets)` — cette chaîne est affichée telle
  quelle dans la liste des mods en jeu, donc elle doit énoncer les deux moitiés.
- Functional Storage : **MIT** — copier/adapter du code est explicitement autorisé.
  **Obligation :** la licence MIT impose de préserver la notice de copyright. Maintenir un
  fichier `NOTICE` (ou une section README) créditant `Copyright (c) 2021 Buuz135, Rid`, et
  ajouter un en-tête d'attribution en tête de chaque fichier directement dérivé des leurs
  (`BigEnergyStorage` ← `BigFluidHandler`, les renderers, les blocs/tiles).
- Titanium : **LGPLv3** — le texte de la licence précise que définir une sous-classe est
  « un mode d'usage de l'interface » et non une œuvre dérivée. Notre MIT tient.
  **Ne jamais bundler Titanium dans le jar.**

---

## 5. Décisions produit (arrêtées)

→ `DESIGN.md` §5.

---

## 6. Hors scope (v1)

→ `DESIGN.md` §6.

---

## 7. Contrainte centrale : FS n'a pas d'API pour les types de tiroir

`FunctionalUpgradeBehavior` (voir §9) est la **seule** extension officielle, et elle ne couvre
que les comportements d'upgrade. Aucun registre pour un nouveau type de contenu.
`FluidDrawerTile` est référencé **54 fois dans 15 fichiers**, toujours par `instanceof` en dur.

### Le filtre bloquant

```java
// ConnectedDrawers.java:78-80 — cœur du Storage Controller
if (entity instanceof ItemControllableDrawerTile<?>
        || entity instanceof FluidDrawerTile
        || entity instanceof StorageControllerExtensionTile) {
    validDrawers.add(connectedDrawer);
}
```

Un tile qui ne matche pas est **retiré du réseau à chaque rebuild**.

### La contrainte de performance

```java
// StorageControllerTile.java:76
if (connectedDrawers.getConnectedDrawers().size()
    != (getItemHandlers().size() + getFluidHandlers().size() + getExtensions())) {
    // rebuild complet du réseau
}
```

Si notre tiroir est dans la liste mais ne contribue à aucun compteur, **l'invariant est faux en
permanence et le contrôleur reconstruit tout son réseau à chaque tick**. Sur un mur de 200
tiroirs, c'est un serveur mort.

### La solution retenue (sans mixin)

**`EnergyDrawerTile extends ItemControllableDrawerTile<EnergyDrawerTile>`, avec
`getStorage()` retournant un `IItemHandler` à 0 slot.**

- passe le filtre `instanceof` ✅
- compté dans `itemHandlers`, donc l'invariant du tick tient ✅
- inoffensif côté agrégation, vérifié ligne par ligne :

```java
// ControllerInventoryHandler.invalidateSlots()
int handlerSlots = handler.getSlots();          // = 0
for (int i = 0; i < handlerSlots; ++i) { ... }  // ne s'exécute pas
this.slots += handlerSlots;                     // += 0
```

`ConnectedDrawers.getConnectedDrawers()` (`:159`) et
`StorageControllerTile.getConnectedDrawers()` (`:194`) sont **publics** — on peut lire la liste
des positions connectées pour agréger le FE nous-mêmes, sans toucher au code de FS.

Un mixin dans `ConnectedDrawers` reste possible en dernier recours, mais c'est un mixin dans un
mod tiers à 56M de téléchargements : rejeté tant qu'une solution dans le contrat public existe.

### [vérifié] Tâche 2 — 50 tiroirs sur un contrôleur, aucun rebuild

`aWallOfDrawersDoesNotRebuildTheControllerEveryTick` : 50 Energy Drawers liés à un Storage
Controller par `addConnectedDrawers(ADD, …)` — l'appel exact du Linking Tool. Les 50 sont comptés
dans `itemHandlers`, et la liste n'est pas remplacée pendant 60 ticks d'inactivité. L'invariant
tient. **Le contournement du §7 fonctionne en réseau, pas seulement à l'unité.**

**Piège trouvé en route, à ne pas reperdre :** *lier ne construit pas le réseau.*
`ConnectedDrawers` est construit dans le constructeur du tile, où `getLevel()` est encore `null`,
et son `rebuild()` ne fait rien sans level. Le `rebuild()` déclenché par `addConnectedDrawers`
laisse donc les listes vides. C'est le `serverTick` du contrôleur qui appelle `setLevel()` puis
reconstruit pour de bon. **L'invariant est faux pendant un ou deux ticks après un linking, par
construction** — tout test ou diagnostic qui mesure immédiatement après le linking mesure le
mauvais moment.

---

## 8. ✅ Le risque n°1 — RÉSOLU

> **[vérifié] 22 août 2026 — `./gradlew runGameTestServer` : 4 tests requis, 4 passés.**
> L'architecture tient telle qu'elle est décrite ci-dessous. Compte rendu complet dans `SPIKE.md`.
>
> La raison est dans le code de NeoForge, ce qui vaut mieux qu'un test qui passe :
> `BlockCapability.getCapability` garde une **liste** de providers par bloc et la parcourt jusqu'à
> ce que l'un retourne non-null (`net/neoforged/neoforge/capabilities/BlockCapability.java`,
> NeoForge 21.1.248). Celui de Titanium retourne `null` pour un non-`PoweredTile`, le nôtre répond.
> L'ordre d'enregistrement est indifférent : avec l'un des deux toujours nul, les deux ordres
> résolvent pareil.
>
> Le test reste comme non-régression. Voir `gametest/IDGameTests.java`.

Titanium enregistre **déjà** `Capabilities.EnergyStorage.BLOCK` pour tout bloc passé par
`registerBlockWithTile(Item)` :

```java
// Titanium DeferredRegistryHelper.java:117-125
public void registerCapabilities(Holder<BlockEntityType<?>> type) {
    bus.addListener((final RegisterCapabilitiesEvent event) -> {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type.value(), (object, context) -> {
            if (object instanceof PoweredTile<?> powered) { return powered.getEnergyStorage(); }
            return null;
        });
        // + FluidHandler et ItemHandler, gated sur ActiveTile
    });
}
```

**Conflit d'héritage simple :** `PoweredTile extends ActiveTile` et
`ControllableDrawerTile extends ActiveTile`. On ne peut pas être les deux, et on *doit* être un
`ControllableDrawerTile`. Donc pas d'enregistrement automatique de la capability énergie.

**Contournement :**
1. Enregistrer `Capabilities.EnergyStorage.BLOCK` nous-mêmes sur notre `BlockEntityType`.
2. Utiliser `EnergyStorageComponent` de Titanium **par composition** (pas par héritage) —
   bonus : il fournit `getScreenAddons()`, donc la barre d'énergie dans la GUI est gratuite.

Le point 1 est fait (`ImmaterialDrawers.registerCapabilities`). Le point 2 ne l'est pas :
`EnergyStorageComponent` de Titanium n'est pas encore utilisé, et le sera à la tâche 6 pour la
barre d'énergie dans la GUI — le stockage actuel est notre `BigEnergyStorage`.

---

## 9. API officielle : `FunctionalUpgradeBehavior`

Seule porte supportée, et celle qu'utilise *More Functional Storage*.

```java
// FunctionalUpgradeBehavior.java
ResourceKey<Registry<MapCodec<? extends FunctionalUpgradeBehavior>>> REGISTRY_KEY =
    ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("functionalstorage", "functional_upgrade_behavior"));
Registry<...> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY).sync(true).create();

default void work(Level level, BlockPos pos, ControllableDrawerTile<?> drawer, ItemStack upgradeStack, int upgradeSlot) {}
default int getRedstoneSignal(Level, BlockPos, BlockState, ControllableDrawerTile<?>, Direction, ItemStack, int) { return -1; }
default boolean canConnectRedstone(...) { return false; }
default List<Component> getTooltip() { return new ArrayList<>(); }
MapCodec<? extends FunctionalUpgradeBehavior> codec();
```

Le comportement s'attache à un item via le DataComponent `FSAttachments.FUNCTIONAL_BEHAVIOR`.
Registre synchronisé client/serveur, dispatché par codec. Contrat stable, pas de mixin.

---

## 10. Ce qui est réutilisable gratuitement

### Géométrie — identique au pixel près

```java
// DrawerBlock.java:39 — public static
public static final HashMap<DrawerType, Multimap<Direction, VoxelShape>> CACHED_SHAPES = ...
public static Collection<VoxelShape> getDefaultHitShapes(DrawerType type, BlockState state)
```

D'où, comme dans `FluidDrawerBlock` :

```java
@Override
public Collection<VoxelShape> getHitShapes(BlockState state) {
    return DrawerBlock.getDefaultHitShapes(this.type, state);
}
```

`FunctionalStorage.DrawerType` est un enum public (`getSlots()`, `getSlotPosition()`).

### Variante Framed — ~100 lignes

`FramedFluidDrawerBlock` = **57 lignes** (dont 40 de recettes),
`FramedFluidDrawerTile` = **46 lignes**.

Pattern à suivre, celui du Fluid Drawer : **une variante matérielle distincte**
(`Blocks.STONE_BRICKS` pour les fluides — choisir autre chose pour l'énergie) **+ une variante
Framed**. Pas de déclinaison par essence de bois.

### Storage Upgrades

`SizeProvider` est une interface publique avec des helpers `static` :

```java
static float calculateAsFactor(IItemHandler upgrades, Supplier<DataComponentType<SizeProvider>> component,
                               float baseIn, ItemStack[] replacements)
```

Deux options :
1. **Simple** — réutiliser `FSAttachments.FLUID_STORAGE_MODIFIER` tel quel. Zéro composant à créer.
2. **Propre** — enregistrer notre propre `ENERGY_STORAGE_MODIFIER` et l'attacher aux upgrades de
   FS via `ModifyDefaultComponentsEvent` (confirmé disponible en NeoForge 21.1 : FS l'importe
   lui-même). Nécessaire pour un scaling énergie indépendant du scaling fluide.

**[vérifié] Option 2 retenue, et ce n'était pas optionnel.** `immaterialdrawers:energy_storage_modifier`
est enregistré dans `registry/IDComponents.java` et attaché aux upgrades de FS via
`ModifyDefaultComponentsEvent` — confirmé disponible et suffisant en NeoForge 21.1, sans mixin.

L'option 1 aurait été une ligne au lieu d'un fichier, mais elle soude l'énergie à la courbe des
fluides : même `FLUID_DIVISOR` de 2, donc base plafonnée à ~32 000 FE (§11). Le point de bascule
n'est pas la propreté, c'est que le diviseur *doit* différer.

Les multiplicateurs sont **lus** dans `FunctionalStorageConfig`, pas recopiés : ce sont des valeurs
de config, et un pack qui double le Netherite doit déplacer notre courbe avec, pas laisser
l'énergie sur les valeurs d'origine.

### Fichiers FS à lire comme modèles

| Besoin | Fichier |
|---|---|
| Bloc | `block/FluidDrawerBlock.java` (173 l.) |
| Tile | `block/tile/FluidDrawerTile.java` (250 l.) |
| Framed | `block/FramedFluidDrawerBlock.java`, `block/tile/FramedFluidDrawerTile.java` |
| Handler | `fluid/BigFluidHandler.java` → modèle pour `BigEnergyStorage` |
| Renderer | `client/BaseDrawerRenderer.java`, `client/FluidDrawerRenderer.java` |
| GUI | `client/gui/FluidDrawerInfoGuiAddon.java` |
| Augments | `item/component/MoveFluidsBehavior.java`, `GenerateFluidBehavior.java`, `EmitRedstoneBehavior.java` |
| Enregistrement | `FunctionalStorage.java` — `initModules()` et le listener `RegisterCapabilitiesEvent` (~l.246) |

---

## 11. Contraintes connues (pas des tâches immédiates)

### Plafond `int` sur `IEnergyStorage`

Valeurs réelles de FS :

```java
COPPER_MULTIPLIER = 8;  GOLD = 16;  DIAMOND = 24;  NETHERITE = 32;
FLUID_DIVISOR = 2;   // les fluides divisent tous les multiplicateurs
```

4 slots d'upgrade, facteurs **multiplicatifs**. En mode fluide, 4× netherite = ×65 536.
`IEnergyStorage` plafonne à `Integer.MAX_VALUE` = 2 147 483 647 FE.

Avec une base de 1 000 000 FE : le 3ᵉ slot d'upgrade sature, le 4ᵉ ne fait **rien**.

**[vérifié] Calibrage retenu — `storage/EnergyScaling.java`, tâches 3 et 4 :**

```
base    = BASE_UNITS × FE_PER_UNIT  = 500 × 1 000  =       500 000 FE
facteur = (NETHERITE / DIVISOR)^4   = (32 / 4)^4   =         4 096
max     = base × facteur                           = 2 048 000 000 FE
plafond = Integer.MAX_VALUE                        = 2 147 483 647 FE
```

`ENERGY_DIVISOR = 4`. Les 4 slots d'upgrade servent tous, et le 4ᵉ passe encore avec ~5 % de marge.
La marge est volontaire : `NETHERITE_MULTIPLIER` est une valeur de config qu'un moddeur de pack
peut monter, et `EnergyScaling.capacityFor` **clampe** au lieu de déborder quand il le fait.

Pourquoi pas 2 comme les fluides : ça imposait une base de ~32 000 FE, moins qu'une Basic Energy
Cell de Powah — le bloc serait inutile avant d'être upgradé. Pourquoi pas 8 : le haut de la courbe
resterait aux deux tiers vide. 4 est la valeur qui utilise tout l'int.

**Le Max Storage upgrade renvoie un multiplicateur de `Integer.MAX_VALUE`**
(`FunctionalStorageConfig.getLevelMult(-1)`). Sans clamp, le cast donne une capacité **négative** et
un tiroir qui refuse tout FE. Couvert par `maxStorageUpgradeSaturatesWithoutOverflowing`.

*Ne pas* stocker en `long` avec clamp à l'exposition en v1 — `getEnergyStored()` mentirait aux
câbles et à Jade.

Le test qui compte est `everyStorageUpgradeSlotChangesTheCapacity` : chaque slot doit **strictement**
augmenter la capacité et rester positif. Un diviseur trop petit sature avant le 4ᵉ slot, un trop
grand gâche le haut de la courbe — et les deux échouent en silence, sans crash ni log.

### À redéfinir manuellement

- **Comparateur** — **[fait]** `Drawer.getAnalogOutputSignal` dispatche sur `FluidDrawerTile` /
  `ItemControllableDrawerTile` ; notre tile tombe sur la branche item et retournerait 0
  (handler vide). Override dans `EnergyDrawerBlock`.
- **Jade / TOP** — `instanceof` en dur dans `compat/jade/DrawerComponentProvider.java` et
  `compat/top/FunctionalDrawerProvider.java`. Intégration à écrire.
- **Rendu** — l'énergie n'a pas de texture de fluide. Jauge émissive custom à concevoir.
  Principal poste de travail artistique. **Textures actuelles = placeholders générés**
  (`scratchpad/GenTextures.java`), volontairement plates, à remplacer entièrement.

  **Le modèle framed passe par le loader de FS.** `models/block/framed_energy_drawer.json` déclare
  `"loader": "functionalstorage:framedblock"`. `FramedModel` est générique : il indexe ses
  `children` par nom et remplace leurs textures depuis `FramedDrawerModelData` lu sur la
  `ModelData` du tile — le nôtre la fournit. Les enfants s'appellent `side` et `front` parce que
  ce sont les clés du design.

  **[vérifié] Le client charge tout ça sans une seule plainte.** `./gradlew runClient`, 22 août 2026 :
  0 erreur, 0 texture manquante, 0 ligne mentionnant `immaterialdrawers` autrement que pour dire
  qu'il est chargé. Les 50 warnings du log sont les blocs de test de Titanium
  (`titanium:block_test` et compagnie, sans modèle) et un son de corne de chèvre vanilla.
  Le modèle framed passe donc bien par le loader de FS : c'était le risque, il n'en était pas un.
- **Teinte du framed** — **[fait, non vérifié en client]** `client/FramedColors` enregistre ses
  handlers depuis `FunctionalStorage.FRAMED_BLOCKS`, une liste construite en scannant **le registre
  de blocs de FS uniquement** (`FunctionalStorage.java:404`). Notre bloc framed n'y sera jamais.
  Les deux méthodes `getColor` sont pourtant génériques (`instanceof FramedTile` / `FramedBlock`) :
  `client/IDColors` **réutilise leur instance** et fait l'enregistrement de notre côté. Le handler
  est le leur, l'enregistrement est le nôtre — deux implémentations qui doivent s'accorder sur la
  couleur d'un bloc framed, autant n'en avoir qu'une.
- **Datagen** — **[fait]** `datagen/IDDataGenerators` : blockstates, modèles d'item, loot tables,
  recettes, lang. `./gradlew runData`, sortie committée (`src/generated/resources`).

  Le provider de blockstates de FS **n'est pas réutilisable tel quel** : son constructeur code en
  dur leur modid comme namespace de sortie, donc il écrirait nos blockstates dans
  `assets/functionalstorage/`. `IDBlockStateProvider` en est une adaptation.

  **Multipart, pas variants :** l'orientation d'un tiroir est deux propriétés (`facing` × `subfacing`)
  plus `locked`, soit 6 × 6 × 2 variants qui doivent tous exister sous peine d'erreur de variant
  manquant. Le multipart permet de poser le cadenas en part séparée.

  Loot tables via `TitaniumLootTableProvider`, qui lit `BasicBlock.getLootTable` — `Drawer` renvoie
  `droppingNothing()`, parce qu'un tiroir ne passe pas par la loot table : `Drawer.getDrops`
  fabrique la stack lui-même pour que contenu et upgrades voyagent avec.

  **Ce qui n'est pas généré : les modèles de bloc et les textures.** Hand-authorés sous
  `src/main/resources`, comme chez FS. Un modèle de tiroir est de la géométrie ; l'exprimer via un
  model builder revient à écrire un moins bon Blockbench.

### Dette permanente

54 références à `FluidDrawerTile` dans 15 fichiers. Chaque release de FS peut ajouter un
`instanceof` où nous ne sommes pas. Ce n'est pas un risque ponctuel, c'est une taxe continue —
d'où le choix de couvrir le contournement par un GameTest dès la première tâche.

**[vérifié] La taxe est arrivée le premier jour**, sous sa forme la plus douce :
`ControllableDrawerTile.updateComparatorOutput()` est public sur la branche `1.21` et **absent de
1.5.7**, la version publiée. `EnergyDrawerTile` appelle `Level.updateNeighbourForOutputSignal` à la
place — vanilla, stable, et ça évite de poser un plancher dur sur la version de FS pour un
rafraîchissement de comparateur. Règle générale à en tirer : quand une méthode de FS a un
équivalent vanilla, prendre le vanilla.

### Risque produit assumé

FS a des Fluid Drawers depuis des années et pas d'Energy Drawer. Buuz135 peut en ajouter en un
week-end. Ce mod existe parce qu'il n'en a pas eu envie — pari raisonnable, pas certitude, et
une raison de plus de viser un périmètre livrable en un mois.

---

## 12. Ordre de travail

**Ne pas dévier de l'ordre. La tâche 1 conditionne l'architecture entière.**

**État : tâches 1 à 5 faites, tâche 6 entamée — datagen et teinte faits (22 août 2026,
`SPIKE.md`). Reste la jauge d'énergie, la GUI, et surtout : lancer un client. Rien de ce qui a
été écrit sous `assets/` n'a jamais été chargé par quoi que ce soit.**
La liste vivante de ce qui vient est dans `ROADMAP.md` ; celle-ci reste comme ordre de référence.

1. ✅ **[BLOQUANT — FAIT] Spike capability — sous forme de GameTest.**
   `build.gradle` + `gradle.properties` (voir §4), la classe
   `ImmaterialDrawers extends ModuleController`, un bloc minimal
   `extends ItemControllableDrawerTile` avec un `IItemHandler` 0 slot,
   `Capabilities.EnergyStorage.BLOCK` enregistrée manuellement dessus.

   **Écrire le test dans `gametest/IDGameTests`** (modèle : `BPGameTests` de PortableBeacons),
   pas une vérification manuelle en jeu. Critère de réussite :
   `level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side)` retourne notre storage,
   et aucune exception au chargement.

   Ce test reste ensuite comme non-régression contre les mises à jour de FS et Titanium.

   *Si le test échoue, s'arrêter et rediscuter de l'architecture.* — il est passé, 4 tests sur 4.

2. ✅ **[FAIT] Réseau contrôleur.** Lier le bloc au Storage Controller via le Linking Tool.
   Vérifier qu'aucun rebuild ne se déclenche à chaque tick sur 50+ tiroirs.

   Fait en GameTest plutôt qu'au profiler : `addConnectedDrawers` est public, donc le linking est
   scriptable, et l'invariant qui déclenche le rebuild est une expression qu'on peut asserter
   directement. Un profiler aurait montré le symptôme ; le test montre la cause et reste. Voir §7.

3. ✅ **[FAIT] `BigEnergyStorage`** calqué sur `BigFluidHandler` (void / creative / locked hérités).

   Void et creative repris à l'identique de `CustomFluidTank`. **Locked volontairement absent :**
   verrouiller un tiroir le fixe sur le *type* de contenu qu'il détient pour qu'un tiroir vidé
   garde son assignation. Le FE n'a qu'un type — il n'y a rien à fixer, et un tiroir d'énergie
   n'est jamais dans l'état que le verrou empêche.

4. ✅ **[FAIT] Storage Upgrades** — `getStorageUpgradesConstructor()` sur le modèle de
   `FluidDrawerTile`, avec notre composant (§10) et le calibrage du plafond `int` (§11).

   Fait avec la tâche 3, parce que c'est le même travail : un handler dont la capacité ne dépend
   pas encore de `getStorageMultiplier()` ne peut pas être calibré, et le calibrage *est* l'enjeu.

5. ✅ **[FAIT] Framed variant** — copier `FramedFluidDrawerBlock` / `FramedFluidDrawerTile`.

   Moins cher que prévu : `FramedBlock` est une **interface marqueur vide**, et tout ce que FS fait
   de spécifique au framing teste cette interface, jamais ses propres classes — recette,
   tooltip, pick-block, copie du style sur l'item lâché. `FramedDrawerRecipe.matches` accepte
   n'importe quel `BlockItem` dont le bloc implémente `FramedBlock` : **notre tiroir est framable
   par la recette de FS sans une ligne de notre côté.** Couvert par
   `framedDrawerIsFramableByFunctionalStorage`, parce que c'est le seul point d'extension de FS
   qui généralise, et par accident d'écriture plutôt que par intention.

   **Piège :** la variante framed a son **propre `BlockEntityType`**, donc sa propre
   `registerBlockEntity` pour la capability, et son propre `scanTileClassForAnnotations`. Rater
   la première donne un tiroir qui se pose, s'affiche, rejoint un réseau — et que tous les câbles
   du jeu ignorent. Rater la seconde et il oublie ses textures au reload.

6. **← ICI. Rendu + datagen + GUI.** Datagen et teinte faits ; il reste la jauge et la GUI.

   **Aucun client n'a jamais été lancé.** Le serveur de game tests ne charge ni modèle ni texture,
   donc tout `assets/` est écrit et jamais exécuté. `./gradlew runClient` est la prochaine chose
   à faire, et le premier suspect est `models/block/framed_energy_drawer.json`, qui utilise le
   loader `functionalstorage:framedblock`.

7. **Augments** — enregistrer un premier `FunctionalUpgradeBehavior` trivial pour valider le
   registre de bout en bout, puis le **Wireless Charger**, puis les autres.

---

## 13. Commandes

```bash
./gradlew build                # jar dans build/libs/
./gradlew runClient
./gradlew runServer
./gradlew runData              # datagen
./gradlew runGameTestServer    # GameTests — critère de la tâche 1
./gradlew clean
```

Vérifier la plateforme : `grep -r "net.neoforged" gradle.properties build.gradle`

---

## 14. Documentation — en place

Convention, reprise de PortableBeacons : **chaque fichier a un seul métier et l'annonce en
ouverture, y compris ce qu'il n'est pas.**

| Fichier | Langue | Métier | État |
|---|---|---|---|
| `CLAUDE.md` | fr | Opérationnel : contraintes vérifiées, architecture | ✅ (§2, §4, §7 → §15) |
| `DESIGN.md` | **fr** | Réflexion interne, écrite avant le code | ✅ (§1, §3, §5, §6, extraits verbatim) |
| `README.md` | en | Version courte, pointe vers le wiki | ✅ |
| `ROADMAP.md` | en | `## Now`, `## Then`, `## Still open`, `## Settled` | ✅ |
| `CHANGELOG.md` | en | Newest first, `Added`/`Changed`/`Fixed`/`Notes`, versions `{mod}+{minecraft}` | ✅ |
| `SPIKE.md` | en | Le spike bloquant : analyse, puis ce qui s'est réellement passé | ✅ |
| `NOTICE` | en | Attribution MIT que FS impose de préserver | ✅ |
| `STORE.md` | en | Copie paste-ready CurseForge. Pas de la documentation. | à écrire avant la release |
| `PORTING.md` | en | Seulement quand un port est envisagé | — |
| `LICENSE` / `LICENSE-ASSETS` | — | Code MIT / assets ARR | ✅ |

**Frontière de langue : interne vs public**, pas doc vs code. `DESIGN.md` est le seul en français.
**Le wiki GitHub est la référence**, le README en est le résumé.

**Habitude à reprendre de `PORTING.md` :** un document d'analyse écrit avant le code est daté,
distingue les faits sourcés des estimations, puis est **complété après coup** par « ce qui s'est
réellement passé » et « où l'estimation était fausse ». À appliquer au spike bloquant — l'écart
entre l'analyse statique de ce brief et le comportement réel est l'information la plus précieuse
du projet.

**README, ROADMAP et CHANGELOG : écrits après le spike**, pour cette raison-là. Tant que la tâche 1
n'avait pas tourné, un README qui promet une fonctionnalité non validée est la première chose à
réécrire.

### Sources de référence en local

`.reference/` (gitignoré) contient les clones lus pour ce brief. Ce ne sont pas nos sources, elles
ne sont pas vendorées, et elles se re-clonent en une commande :

```bash
git clone --depth 1 -b 1.21 https://github.com/Buuz135/FunctionalStorage.git .reference/FunctionalStorage
git clone --depth 1 -b 1.21 https://github.com/InnovativeOnlineIndustries/Titanium.git .reference/Titanium
```

Attention : la branche est en 1.5.8, le jar contre lequel on compile est en 1.5.7 (§4).

---

## 15. Limites de la vérification

**Mis à jour le 22 août 2026, après le spike.** Le brief d'origine était de l'analyse statique pure,
des sources de Functional Storage (branche `1.21`, `mod_version` 1.5.8) et de Titanium
(branche `1.21`). Depuis, le projet compile et un serveur de game tests tourne.

### Vérifié depuis

- ✅ **Le double enregistrement de `Capabilities.EnergyStorage.BLOCK` (§8)** — passe, et la raison
  est dans le code de NeoForge, pas seulement dans un test vert. Voir `SPIKE.md`.
- ✅ **Le handler 0 slot en runtime** — le tile est bien un `ItemControllableDrawerTile`, son
  `getStorage().getSlots()` vaut 0, et rien n'explose au chargement. Couvert par
  `drawerCountsAsAnItemDrawerButHoldsNoItems`.
- ✅ **Les coordonnées maven** — Modrinth remplace CurseMaven pour FS (§4). Titanium `1.21-4.0.34`
  existe bien sur BlameJared et est la version que FS utilise.
- ✅ **Le slug `immaterial-drawers` est libre sur Modrinth** (404 sur l'API).
- ✅ **Le scaling par upgrades (tâches 3 et 4)** — capacité dérivée de `getStorageMultiplier()`, les
  4 slots servent tous, le Max Storage sature sans déborder, et un upgrade ne sort pas d'un tiroir
  trop plein pour s'en passer. Voir §11.
- ✅ **Le comportement en réseau réel (tâche 2)** — 50 tiroirs liés à un Storage Controller, tous
  comptés dans `itemHandlers`, aucun rebuild pendant 60 ticks. Voir §7 et `SPIKE.md`.

### Toujours non vérifié

- **Le slug sur CurseForge.** Leur site répond 403 à une vérification automatisée : ni libre ni
  pris, juste inconnu. À confirmer à la main avant la release.
- ✅ **Le chargement client (tâche 6, partie code)** — `./gradlew runClient` : blockstates, modèles,
  textures et lang chargent sans erreur ni warning nous concernant, loader `functionalstorage:framedblock`
  compris. Voir §11.

### Toujours non vérifié

- **À quoi ça ressemble.** Le client charge les modèles ; personne n'a encore regardé le résultat.
  Les textures sont des placeholders générés et la jauge d'énergie n'existe pas.
- **Les APIs NeoForge sensibles à la version** — vérifier sur `https://docs.neoforged.net/`
  avant d'écrire du code de registre ou de capability.
