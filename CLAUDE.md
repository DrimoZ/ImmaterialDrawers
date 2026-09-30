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

### Distribution

**Slug : `immaterial-drawers`, identique partout.**

| Plateforme | État |
|---|---|
| **CurseForge** | Projet approuvé et public (0.2.0+1.21.1 publiée le 29 sept. 2026 ; 30 téléchargements au 30 sept.). |
| **Modrinth** | Libre (404 sur l'API), à réserver avec le même slug. |

<https://www.curseforge.com/minecraft/mc-mods/immaterial-drawers>

Le projet a d'abord été créé sous `immarterial-drawers` — un `r` de trop — et corrigé avant
approbation. Noté parce que c'est la fenêtre où c'était encore gratuit : un slug CurseForge se
change mal une fois le projet approuvé, et il apparaît dans chaque lien jamais partagé.

Le slug n'a aucun effet sur le `modid`, qui reste `immaterialdrawers` et reste figé (voir plus
haut).

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

### Mods de test dans le run de dev

Jamais expédiés, jamais requis pour jouer. Ils répondent à la seule question que les game tests ne
peuvent pas poser : est-ce qu'un vrai câble, d'un vrai mod, fait vraiment entrer et sortir du FE de
ce bloc ?

Powah est `runtimeOnly` seul. **Jade et TOP sont aussi `compileOnly`**, parce qu'on écrit un plugin
pour chacun (§11bis) — ça reste une dépendance douce : la classe n'est chargée que par le scan de
Jade, ou par le `@FeaturePlugin` de Titanium pour TOP.

| Mod | Pourquoi |
|---|---|
| **Powah** | Petit, énergie en FE pur sans conversion, et il a les trois choses nécessaires : un générateur, des câbles, et des cellules à comparer. Mekanism stocke des Joules et convertit à la frontière — un échec y serait ambigu. |
| **Jade** | Lit la capability `EnergyStorage` de n'importe quel bloc et l'affiche au réticule. Le plus rapide pour voir si la capability répond du tout. |
| **The One Probe** | L'autre mod de sonde. Deux APIs différentes pour le même besoin — c'est ce qui a fait écrire le §11bis. |
| GuideME, Cloth Config | Dépendances dures de Powah. |

**Une coordonnée maven `runtimeOnly` amène le jar, pas les mods dont ce jar a besoin**, et FML fait
échouer le lancement entier plutôt que de sauter le mod. D'où les deux dernières lignes.

### Run vitrine — screenshots de la page du mod

`./gradlew runClient -Pshowcase "-PquickPlay=Drawer Preview"`. Deux propriétés, toutes deux absentes
par défaut, donc le client de tous les jours et le serveur de game tests n'en paient rien :

- **`-Pshowcase`** ajoute Mekanism + Mekanism Generators (machines, câbles universels, panneaux
  solaires : un décor qu'un joueur tech reconnaît) et Sodium + Iris pour un pack de shaders. Iris
  1.8.12 exige Sodium 0.6.13 exactement — on les bouge ensemble ou pas du tout.
- **`-PquickPlay=<monde>`** ouvre directement une sauvegarde, sans passer par les menus.

Le pack de shaders n'est pas une dépendance Gradle : `run/shaderpacks/ComplementaryReimagined_r5.9.3.zip`
(Modrinth), activé par `run/config/iris.properties`. Tout ça vit dans `run/`, gitignoré.

Les scènes (atelier, centrale Mekanism, bibliothèque framed, abri solaire) sont un datapack dans la
sauvegarde `run/saves/Drawer Preview` : une fonction `preview:scene_*` construit, une fonction
`preview:shot_*` place la caméra en spectateur. Les captures sélectionnées sont dans
`run/store-screenshots/`.

**Piloter le jeu au clavier depuis un script est dangereux** : `SetForegroundWindow` ne garantit pas
le focus, et des commandes destinées au chat du jeu ont atterri dans une autre application. Toute
frappe doit être précédée d'une vérification `GetForegroundWindow() == fenêtre Minecraft`, et
abandonner sinon.

À noter : **JEI est déjà dans le run sans être déclaré nulle part** — il arrive par le pom de
Titanium. C'est aussi la preuve que `runtimeOnly` suffit pour qu'un mod soit découvert par FML dans
un run MDG, sans le détour par `run/mods` qu'utilise PortableBeacons.

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

### [vérifié] Le contrôleur agrège le FE — et c'est nous qui le faisons

Le contrôleur de FS agrège les item handlers et les fluid handlers de son réseau et les expose
comme un inventaire et un tank. **Il ne peut pas faire pareil pour l'énergie** : il ramasse notre
item handler vide et n'a pas de troisième type de contenu à chercher.

Ce qu'il donne, c'est le réseau lui-même. `getConnectedDrawers()` est public des deux côtés, donc
`storage/ControllerEnergyStorage` lit la liste des positions et fait la somme. Le provider est
enregistré **sur leur `BlockEntityType`** — NeoForge ne demande jamais à qui appartient le type.
Câble sur le contrôleur = tout le mur. Idem sur les extensions, qui redirigent vers leur contrôleur.
Couvert par `theControllerMovesEnergyForItsWholeNetwork` (insert **et** extract : l'insert seul
passerait avec un agrégat qui avale ce qu'on lui donne).

**Ce que ça ne règle pas :** l'écran du contrôleur montre toujours items et fluides, pas l'énergie.
Ce panneau est construit depuis les screen addons de *leur* tile, et il n'y a pas de hook pour un
quatrième. L'ajouter voudrait dire un mixin, que le §7 refuse. Jade lit la capability et affiche le
total ; c'est un panneau qui manque dans leur GUI, pas de l'énergie.

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

Le point 1 est fait (`ImmaterialDrawers.registerCapabilities`).

**Le point 2 s'est révélé inutile.** Il n'y a pas besoin de tenir un `EnergyStorageComponent` par
composition juste pour récupérer son `getScreenAddons()` : `EnergyBarScreenAddon` de Titanium prend
un `IEnergyStorage` nu, donc notre propre `BigEnergyStorage` lui est passé directement
(`EnergyDrawerTile.initClient`). Un objet énergie de moins à garder synchronisé avec l'autre.

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

**[vérifié] Le contrat tient, et il est le seul qui n'ait rien coûté.** `immaterialdrawers:charge_nearby`
est enregistré via `registerGeneric(FunctionalUpgradeBehavior.REGISTRY_KEY, …)`, l'item est un
`UpgradeItem` construit depuis le behaviour — une ligne, comme le Redstone Upgrade de FS — et le
`serverTick` du tiroir appelle bien notre `work()`. Aucun `instanceof` à contourner, aucun mixin :
c'est le seul point d'extension de FS qui se comporte comme une API. Couvert par
`augmentIsRegisteredWithFunctionalStorage` et `wirelessChargerFillsGearWithoutInventingEnergy`.

**Le behaviour ne fait rien hors d'un tiroir d'énergie**, et c'est voulu : les slots utilitaires
appartiennent à FS et acceptent n'importe quel upgrade utilitaire. Un Wireless Charger dans un
tiroir de cobble ne charge rien, ce qui est la réponse honnête pour un augment qui a besoin de FE.

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

### ⚠️ RENVERSÉ — le stockage est en `long`

> Ce paragraphe disait : *« Ne pas stocker en `long` avec clamp à l'exposition en v1 —
> `getEnergyStored()` mentirait aux câbles et à Jade. »* **C'était faux, et le clamp est la seule
> façon de dépasser int.**

`IEnergyStorage` est un int de bout en bout : 2 147 483 647 FE maximum. Tout mod qui stocke plus
garde un nombre plus large en interne et clampe à la frontière. **Vérifié dans le jar de Powah :**
`CableTile.receiveEnergy(long, boolean, Direction)`, et l'accès à la capability standard passe par
un `AbstractEnergyStorage$ExternalAdapter`. C'est comme ça qu'une Nitro Ender Cell affiche 18B FE.

`BigEnergyStorage` stocke donc en `long` (9,22 × 10¹⁸ FE, quatre milliards de fois le plafond int).
`BigInteger` serait illimité, mais c'est une allocation par opération pour une marge déjà absurde.

**Le clamp ment, et il faut savoir où — voir §11bis.** `getStoredLong()` / `getCapacityLong()` sont
la vérité, et **tous** nos affichages les utilisent : face du bloc, écran, tooltip d'item,
comparateur, provider Jade.

Le contrôleur additionne aussi en `long` et sature **une seule fois à la fin** — sommer les vues
clampées plafonnerait chaque tiroir à 2,1B *avant* l'addition, et un mur de tiroirs pleins
s'afficherait comme une poignée.

**`ENERGY_DIVISOR` reste à 4.** Le plafond int n'était que la moitié de sa raison d'être ; l'autre
moitié est l'équilibrage, et c'est la courbe avec laquelle le mod est joué. Qui veut la marge que le
`long` achète baisse le diviseur **en config** (à 2 : ~32,8B FE à fond).

### §11bis. Qui voit quoi — les trois niveaux de lecture

Question à se reposer à chaque nouvelle intégration : *ce lecteur passe-t-il par la capability
standard, ou peut-on lui donner le vrai chiffre ?*

| Niveau | Qui | Ce qu'il voit | Action possible |
|---|---|---|---|
| **Capability standard** | Câbles, machines, la plupart des compteurs | Clampé à 2,1B. `IEnergyStorage` est int, il n'existe pas de contrat FE en `long`. | **Aucune.** C'est le contrat de l'écosystème, pas notre bug. |
| **Mods de sonde** | Jade ✅, TOP ✅, WTHIT… | Ce qu'on leur envoie. Chacun a son API de plugin. | **Un provider par mod**, ~40 lignes, qui appelle `getStoredLong()` et `EnergyFormat.format`. |
| **API propriétaires** | Mekanism (Joules), Powah (`long`) | Rien, sauf adaptateur dédié. | Seulement si une intégration le justifie. Hors scope v1. |

**Le point d'architecture :** la vérité et le formatage vivent à un seul endroit
(`BigEnergyStorage` / `ControllerEnergyStorage` / `util/EnergyFormat`). Toute couche de compat
future est un adaptateur mince par-dessus — jamais une réimplémentation du calcul.

Le test qui compte est `everyStorageUpgradeSlotChangesTheCapacity` : chaque slot doit **strictement**
augmenter la capacité et rester positif. Un diviseur trop petit sature avant le 4ᵉ slot, un trop
grand gâche le haut de la courbe — et les deux échouent en silence, sans crash ni log.

### À redéfinir manuellement

- **Comparateur** — **[fait]** `Drawer.getAnalogOutputSignal` dispatche sur `FluidDrawerTile` /
  `ItemControllableDrawerTile` ; notre tile tombe sur la branche item et retournerait 0
  (handler vide). Override dans `EnergyDrawerBlock`.
- **Jade** — **[fait]** `compat/jade/IDJadePlugin`. Leur barre d'énergie intégrée lit la capability,
  donc elle affiche 2,14G dès que le total dépasse int — deux tiroirs 4× netherite suffisent.

  **On alimente *leur* barre, on n'ajoute pas de ligne.** Une première version enregistrait un
  `IBlockComponentProvider` et écrivait son propre texte : deux affichages de la même chose dans
  deux styles différents. Le bon hook est `registerEnergyStorage`, et `EnergyView.of(long, long)`
  prend déjà des `long` — Jade n'a jamais été la pièce incapable de compter au-delà d'un int.

  **Pourquoi ça remplace leur lecture au lieu de s'y ajouter :** `getServerExtensionData` renvoie
  **une seule** `Map.Entry` issue d'un lookup hiérarchique, donc Jade retient le provider le plus
  spécifique. S'enregistrer sur nos classes de tuile l'emporte sur son provider universel.

  Le provider doit implémenter les deux moitiés (`IServerExtensionProvider` +
  `IClientExtensionProvider`) avec **le même `getUid()`** : le client retrouve son provider par
  l'UID que le serveur a estampillé sur les données.

  Enregistré aussi sur **leur** bloc contrôleur : son total est la somme d'un réseau que le client
  n'a pas forcément chargé, donc il doit être calculé côté serveur.
- **TOP** — **[fait]** `compat/top/IDTopPlugin` + `EnergyProbeProvider`. Même réponse que Jade, autre
  API : `IProbeInfo.progress` prend des `long`, donc on remplit **sa** barre au lieu d'ajouter une
  ligne.

  **Le `@FeaturePlugin` de Titanium est ce qui rend le `compileOnly` sûr** : le plugin manager
  n'instancie la classe que si `theoneprobe` est chargé, donc rien n'est classloadé dans un pack
  sans TOP. L'entrée se fait par `InterModComms`, la porte publiée de TOP, pas par un appel direct.
- **Rendu** — **[vérifié en client, 28 septembre 2026]** le bloc **est** le Fluid Drawer de FS vide,
  et il contient un **cube d'énergie** qui tourne, dans la tradition du cube de Mekanism.

  **La carcasse n'est pas copiée, elle est héritée.** `models/block/energy_drawer.json` est un
  `neoforge:composite` dont les trois enfants ont pour parent les modèles de FS —
  `side_machine`, `fluid_front_1`, `fluid_inner_1` — avec nos textures. Même géométrie au pixel que
  leur Fluid Drawer, zéro coordonnée recopiée. Le framed fait pareil avec `side` / `fluid_front_1` /
  `fluid_inner_1` et leurs textures framed : c'est `framed_fluid_1.json` enfant pour enfant.
  Les trois modèles existent dans le jar 1.5.7 (vérifié). S'ils sont renommés un jour, le symptôme est
  un tiroir en damier rose — rien ne le teste, c'est le prix de ne pas les dupliquer.

  Nos textures sont les leurs recolorées : même dessin, pierre → graphite, verre de la fenêtre →
  cuivre. Un tiroir d'énergie se reconnaît d'un tiroir de fluide au premier coup d'œil, et se pose dans
  le même mur sans détonner. **La carcasse graphite est commune à tous nos tiroirs**
  (`drawer_side/top/inner`, `drawer_divider`) ; seule la façade porte l'identité du contenu, par la
  couleur du liseré vitré (`energy_drawer_front` cuivre, `chemical_drawer_front*` sarcelle).

  **Pourquoi un objet et pas un niveau.** Un fluide a une surface, donc le Fluid Drawer montre son
  remplissage par la hauteur du fluide. L'énergie n'a ni texture ni surface, et une barre sur une
  façade de tiroir lit comme une jauge de machine. Le tiroir contient donc un objet, et la charge est
  dans son noyau : absent à vide, translucide et lent à faible charge, opaque et rapide plein. Le
  chiffre exact est le nombre sur la façade ; le cube, c'est le coup d'œil.

  **Deux modèles autonomes**, `models/block/energy_cube_frame.json` (8 coins, 12 arêtes, ouvert) et
  `energy_cube_core.json` (trois cubes imbriqués, dont deux tournés à 45° — c'est ce qui lui donne
  des facettes plutôt qu'une boîte). Enregistrés par `ModelEvent.RegisterAdditional` dans
  `IDClientSetup` : **sans ça, `getModel` ne plante pas, il rend le modèle manquant** — un cube rose
  et noir qui tourne dans chaque tiroir. Rendus par `EnergyDrawerRenderer` à l'échelle 9/16 :
  le cadre tourne sur Y, donc c'est sa diagonale (9 × √2 ≈ 12,7 px) qui doit tenir dans les 13 px du
  réservoir. Le cadre prend la lumière du bloc, le noyau est `FULL_BRIGHT` et translucide. Chaque
  tiroir est déphasé d'après sa position, pour qu'un mur ne tourne pas au pas comme une seule machine.
  Éditables dans Blockbench, comme n'importe quel modèle de bloc.

  **Le nombre suit le Fluid Drawer, pas l'Item Drawer.** Le placement de `DrawerRenderer.renderStack`
  met le texte un pixel trop bas pour cette carcasse : derrière le rebord inférieur de `side_machine`,
  invisible. `EnergyDrawerRenderer` reprend les 0,84 / 0,453 et l'échelle 0,007 de
  `FluidDrawerRenderer`, convertis dans le repère de `BaseDrawerRenderer`.

  **Le modèle framed passe par le loader de FS.** `models/block/framed_energy_drawer.json` déclare
  `"loader": "functionalstorage:framedblock"`. `FramedModel` est générique : il indexe ses
  `children` par nom et remplace les textures de ceux qui sont dans le design (`side`, `front`) depuis
  `FramedDrawerModelData` lu sur la `ModelData` du tile — le nôtre la fournit. `tank` n'est pas une
  clé du design, donc il garde nos parois et le cube se voit derrière n'importe quelle façade.

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

### [vérifié] Le deadlock de chargement de FS 1.5.7

**Symptôme :** monde bloqué à 100%, définitivement. Thread dump : le Server thread est parké dans
`ServerChunkCache.getChunk` → `managedBlock`, appelé depuis
`ControllableDrawerTile.invalidateCapabilities` (1.5.7:369), lui-même appelé par `clearRemoved`
pendant `LevelChunk.setBlockEntity` du post-load de chunk.

**Cause :** pour reconstruire le réseau du contrôleur, 1.5.7 fait `Level.getBlockEntity(controllerPos)`
— un fetch de chunk **bloquant**, émis depuis le post-load, sur le thread qui doit exécuter ce
post-load. Si le chunk du contrôleur est dans le même lot, le thread serveur attend une tâche que
lui seul peut exécuter. Leur garde `isLoaded` ne protège pas : elle répond sur l'existence du chunk
holder, pas sur la disponibilité du chunk pour ce thread à cet instant.

**Déjà corrigé en amont, pas publié :** la branche `1.21` utilise `getChunkSource().getChunkNow(...)`,
qui rend `null` au lieu d'attendre. `EnergyDrawerTile.invalidateCapabilities` reprend ce correctif
**pour nos tuiles uniquement** — les tiroirs de FS gardent le bug jusqu'à 1.5.8, et le corriger chez
eux demanderait un mixin. **Override à supprimer quand le plancher passera à 1.5.8.**

Troisième instance de la dérive 1.5.7 / branche, et la première qui casse un monde plutôt qu'une
compilation.

### L'art — ce qui n'est pas un choix esthétique

Les textures sont modifiables librement, les deux modèles du cube aussi (Blockbench). **Trois choses
ne sont pas des choix esthétiques**, et les casser donne un bloc qui compile, se pose, et n'affiche
rien :

1. **La façade reste celle du Fluid Drawer** (`fluid_front_1`, encastrée à z=0,5). `BaseDrawerRenderer`
   dessine à 0,5 px dans le bloc : sur une façade affleurante, le nombre et l'indicateur tombent
   *dans* la géométrie. Changer de parent, c'est revérifier `TEXT_Y` / `INDICATOR_Y` en jeu.
2. **`energy_drawer_front.png` garde sa fenêtre transparente** (texels 3..13). Le cube se voit à
   travers, et la GUI dessine les parois puis le remplissage *puis* la façade par-dessus — l'ordre de
   `FluidDrawerInfoGuiAddon`.
3. **Le cube tient dans le réservoir.** `CUBE_SCALE` × √2 ≤ 13 px, parce que le cadre tourne sur Y.
   Un modèle de cadre plus large que 16 unités, ou une échelle plus grande, traverse les parois.

`energy_core.png` est animée (12 frames, `.mcmeta`) et seule la zone 5..11 de chaque frame est
utilisée par les faces du noyau.

Les chemins de modèles des blocs restent `immaterialdrawers:block/<nom de registre>`, dérivés par
`IDBlockStateProvider`.

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

**État : tâches 1 à 6 faites, tâche 7 en cours (26 août 2026).** Client lancé et validé. Datagen,
teinte, affichages (face, écran, tooltip), config complète, stockage en `long`, agrégation
contrôleur, push vers les voisins, Jade et TOP : faits. Le premier augment — le **Wireless Charger**
— est en place, ce qui valide `FunctionalUpgradeBehavior` de bout en bout (§9). 18 game tests au
vert. La tâche 7 est close : les trois augments qui suivaient sont réglés plutôt que construits
(voir §12). L'art est fait (28 septembre 2026) : la carcasse du Fluid Drawer et un cube d'énergie
qui tourne dedans, voir §11. Reste le texte de la page CurseForge.
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

6. ✅ **[FAIT] Rendu + datagen + GUI.** Datagen, teinte, affichages (face, écran, tooltip) et GUI
   faits, et le client charge le tout sans une plainte — `models/block/framed_energy_drawer.json` et
   son loader `functionalstorage:framedblock` compris, qui était le suspect nº1.

   **L'art est fait** (28 septembre 2026) : la carcasse du Fluid Drawer de FS, héritée plutôt que
   recopiée, et un cube d'énergie à la Mekanism qui tourne dans le réservoir. Vérifié en client, sur
   un mur de six tiroirs de 0 à 100 %. Ce que l'art ne doit pas casser est en §11.

7. ✅ **[FAIT] Augments.** Le **Wireless Charger** est fait, et il a servi de validation du registre :
   pas de behaviour jetable, le premier vrai augment prouve la même chose. `augment/ChargeNearbyBehavior`,
   enregistré dans `FunctionalUpgradeBehavior.REGISTRY` sous `immaterialdrawers:charge_nearby` et porté
   par `IDContent.WIRELESS_CHARGER`, un `UpgradeItem` construit depuis le behaviour — exactement comme
   le Redstone Upgrade de FS.

   **Le piège du test, à ne pas reperdre :** `GameTestHelper.makeMockPlayer` rend un `Player` que le
   level ne connaît pas, donc `getEntitiesOfClass` ne le voit jamais et le test échoue pour une raison
   qui ne concerne pas l'augment. `makeMockServerPlayerInLevel` l'ajoute vraiment, mais passe par le
   vrai chemin de join sur un `EmbeddedChannel` : Jade explose en essayant d'envoyer son server ping
   dedans. La combinaison qui marche est `makeMockPlayer` + `setPos` + `addFreshEntity`.

   **L'auto-output est déjà là** : le tiroir pousse vers ses voisins de lui-même (`pushToNeighbours`),
   parce que rien dans l'écosystème FE ne tire. Ce n'est pas un augment, c'est le comportement de
   base, réglable en config.

   **Pas de générateur.** Le mod stocke, il ne produit pas.

   **Le seuil de redstone passe par la Redstone Upgrade de FS**, pas par un augment à nous. Leur
   `EmitRedstoneBehavior` se connecte et tick déjà pour nous — il teste `ItemControllableDrawerTile`,
   ce que nous sommes. Seul son signal lisait `getStorage()`, donc le handler à 0 slot, et rendait
   -1 ; `Drawer.getSignal` traduit -1 par « rien à dire » et retourne 0. L'upgrade s'insérait, se
   connectait, et restait morte. `EnergyDrawerBlock.getSignal` répond ce qui manquait, avec le même
   nombre que le comparateur. Couvert par `functionalStorageRedstoneUpgradeReadsTheCharge`.

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
| `STORE.md` | en | Copie paste-ready CurseForge. Pas de la documentation. | ✅ publiée sur la page CurseForge (0.2.0, 29 sept. 2026) |
| `PORTING.md` | en | Backport 1.20.1 et port 26.1 : analyse avant le code, puis ce qui s'est passé | ✅ analyse (29 sept. 2026) |
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
- ✅ **Le registre `FunctionalUpgradeBehavior` (tâche 7)** — un augment à nous y est enregistré, porté
  par un `UpgradeItem`, et appelé par le `serverTick` du tiroir. Voir §9.
- ✅ **La Redstone Upgrade de FS sur un tiroir d'énergie (tâche 7)** — leur upgrade se connecte, tick
  et pilote un signal tiré de la charge, sans augment de notre côté. Voir §12 tâche 7.

### Toujours non vérifié

- ✅ **Le slug sur CurseForge** — le projet existe sous `immaterial-drawers` (23 août 2026), en
  preview jusqu'à approbation. Créé au départ avec une coquille, corrigée avant approbation. Même
  slug visé sur Modrinth. Voir §2.
- ✅ **Le chargement client (tâche 6, partie code)** — `./gradlew runClient` : blockstates, modèles,
  textures et lang chargent sans erreur ni warning nous concernant, loader `functionalstorage:framedblock`
  compris. Voir §11.
- ✅ **À quoi ça ressemble (tâche 6, partie art)** — regardé en client le 28 septembre 2026 : six
  tiroirs de 0 à 100 % et un framed, cube et nombre visibles sur chacun. Voir §11.

### Toujours non vérifié

- **Les APIs NeoForge sensibles à la version** — vérifier sur `https://docs.neoforged.net/`
  avant d'écrire du code de registre ou de capability.

---

## 16. Chemical Drawers — Mekanism, dépendance optionnelle

**Ajouté le 29 septembre 2026.** Six blocs, présents **seulement si Mekanism est installé** :
`chemical_drawer_1`, `_2`, `_4` et leurs `framed_`. Noms de registre permanents, comme le modid.
Calqués sur les Fluid Drawers de FS : mêmes trois dispositions (1x1, 1x2, 2x2), même taille de base
(`type.getSlotAmount()` × `CHEMICAL_MB_PER_UNIT`, 1000 par défaut → 32 000 mB en 1x1), **même composant
d'upgrade** (`FSAttachments.FLUID_STORAGE_MODIFIER` : l'option 1 du §10, qui convient ici parce que
l'API chemical est en `long` et qu'il n'y a pas de plafond int à calibrer), même ordre de remplissage,
même verrou, void, creative, comparateur et Redstone Upgrade.

### La règle de garde — à ne jamais casser

Le JVM résout une classe quand une méthode qui l'utilise est liée. Une classe Mekanism absente à ce
moment, c'est un `NoClassDefFoundError` qui fait tomber le jeu **dans un pack qui n'a rien demandé**.

- `compat/Mods` est **le seul** endroit qui demande si Mekanism est là, et il n'importe rien d'optionnel.
- Tout ce qui touche Mekanism vit dans des classes marquées « Mekanism only » : `block/chemical`,
  `block/tile/chemical`, `storage/chemical`, `client/ChemicalClient`, `client/ChemicalDrawerRenderer`,
  `client/gui/ChemicalDrawerInfoGuiAddon`, `gametest/IDChemicalGameTests`, `registry/IDChemicalContent`.
- Le code toujours chargé n'appelle ces classes que derrière `if (Mods.mekanism())`, et via des
  signatures sans type Mekanism (`ImmaterialDrawers`, `IDClientSetup`, `IDColors`, `IDLangProvider`).
- **Pas de `@GameTestHolder`** sur les tests chimiques : NeoForge fait `Class.forName(…, true, …)` sur
  chaque holder en dev, Mekanism ou pas. Ils passent par `RegisterGameTestsEvent`, avec
  `templateNamespace` sur chaque `@GameTest`.
- Les données générées doivent survivre à l'absence de Mekanism : recettes sous `mod_loaded`
  (+ `item_exists`, ajouté par Titanium), **entrées du tag `mineable/pickaxe` en `required: false`** —
  une seule entrée requise inconnue fait échouer le tag entier, donc la pioche sur tous les blocs du jeu.

`./gradlew runGameTestServer -PnoMekanism` retire Mekanism du run : c'est le test du chemin « absent ».
**[vérifié]** 20/20 sans Mekanism, 35/35 avec. Déclaré `optional`, `[10.7,)`, `AFTER` dans
`neoforge.mods.toml` ; `compileOnly` + `runtimeOnly` en dev, jamais bundlé.

### Capability : celle de Mekanism, par son nom

La capability vit dans `mekanism.common.capabilities.Capabilities` — l'implémentation, pas l'API.
`ChemicalCapabilities` la recrée par son nom (`mekanism:chemical_handler`, typée `IChemicalHandler`) :
NeoForge interne les capabilities par nom et rend **la même instance**. **[vérifié]**
`weSpeakMekanismsOwnCapability`. Enregistrée sur les six tiroirs, et sur les contrôleurs et
extensions de FS (`ControllerChemicalHandler`), comme l'énergie.

`ControllerChemicalHandler` met sa liste de réservoirs en cache **pour le tick en cours**, invalidé par
l'identité de `getItemHandlers()` (qu'un rebuild remplace) : un tube interroge réservoir par réservoir,
et un parcours du mur par appel ferait ~10 000 lookups par tick et par tube sur 50 tiroirs.

### Pièges trouvés en route

- **Les items-réservoirs de Mekanism sont limités en débit** (1 000 mB par opération pour le basic).
  Un clic ne transférait qu'une tranche ; l'interaction à la main boucle jusqu'à ce que rien ne passe,
  bornée par `MAX_ROUNDS`.
- **`ItemStackHandler(0)` lève une exception sur l'index 0** — bug de la 0.1.0, voir CHANGELOG. La
  base commune utilise `EmptyItemHandler`.
- **Aucun tag `mineable`** — autre bug de la 0.1.0 : aucun outil n'était « correct », casser un tiroir ne
  rendait rien. `aPickaxeIsTheRightToolForEveryDrawer` vérifie tous nos blocs.

### Design

La carcasse et les façades sont celles du Fluid Drawer de FS (modèles hérités, 1/2/4, diviseurs
compris), dans la famille graphite de l'énergie ; le liseré vitré est **sarcelle** là où l'énergie est
cuivre. Le rendu reprend les coordonnées de `FluidDrawerRenderer` (repère décalé de `1 − 1/32` en z
depuis `BaseDrawerRenderer`). Ce qui vient de l'énergie : **les gaz luisent** (pleine luminosité,
translucides, comme le noyau du cube) et **leur surface respire** (¼ px, déphasée par tiroir). Les
chimiques lourds (slurries, pigments, infusions) restent opaques et éclairés comme un fluide.

### Base commune

`block/ImmaterialDrawerBlock` et `block/tile/ImmaterialDrawerTile` portent ce qui ne dépend pas du
contenu : handler vide, slots d'upgrade et leurs gardes, correctif du deadlock 1.5.7, géométrie par
`DrawerType`, cadre du tooltip. Un troisième type de tiroir n'implémente que `storageModifier`,
`onStorageMultiplierChanged`, `canChangeMultiplier` et `hasContents`.

**[vérifié en client, 29 septembre 2026]** les six blocs chargent sans avertissement de modèle ni de
texture ; façades, niveaux, textures et teintes des chimiques, nombres, cadenas et framed s'affichent,
à côté d'Energy Drawers et d'un Fluid Drawer de FS. L'écran (GUI) n'a pas été ouvert en jeu.

### Non fait

Pas de provider Jade/TOP dédié (l'API chemical est déjà en `long`, ce que Jade affiche via ses propres
intégrations Mekanism n'a pas été vérifié) ; item en main rendu comme le bloc, sans contenu (même dette
que l'énergie) ; pas de gestion de la radioactivité à la casse.

---

## 17. Activer / désactiver un contenu — la config, et ce qu'« off » veut dire

**Ajouté le 29 septembre 2026.** Trois interrupteurs dans `immaterialdrawers-common.toml` :
`ENERGY_DRAWER_ENABLED`, `CHEMICAL_DRAWERS_ENABLED`, `WIRELESS_CHARGER_ENABLED`.

**« Off » = inobtenable, pas désenregistré.** Plus de recette (condition
`immaterialdrawers:feature_enabled`, évaluée au chargement des datapacks, donc effective au prochain
chargement de monde ou `/reload`), retiré de l'onglet créatif (`BuildCreativeModeTabContentsEvent`, en
`LOWEST` pour passer après Titanium qui remplit l'onglet). Les blocs restent enregistrés et **ceux déjà
posés continuent de marcher**.

Pourquoi pas désenregistrer, pour mémoire :
- **Titanium enregistre avant de charger la config.** `ModuleController` appelle `initModules()`
  depuis son constructeur, et n'ajoute les `@ConfigFile` que dans `onPostInit()`, après. Au moment de
  l'enregistrement, `IDConfig` vaut ses défauts. Un interrupteur ne peut pas y toucher.
- **Désenregistrer supprime** chaque tiroir posé, avec son contenu, dans les mondes existants.
- **Les registres doivent être identiques** entre serveur et client : une config différente des deux
  côtés refuserait toute connexion.

Le seul interrupteur qui empêche vraiment un bloc d'exister, c'est l'absence de Mekanism (§16) — et
celui-là est le même des deux côtés par construction.

Couvert par `aDisabledFeatureLosesItsRecipe` (la condition suit la config ; les recettes sont bien
chargées par défaut, ce qui prouve aussi que le codec de condition est enregistré). L'onglet créatif
n'est **pas** testé automatiquement : il faut un client.

---

## 18. Source Drawer — Ars Nouveau, dépendance optionnelle

**Ajouté le 29 septembre 2026.** Deux blocs, présents **seulement si Ars Nouveau est installé** :
`source_drawer` et `framed_source_drawer` (noms permanents). Un seul format, 1x1, comme l'énergie : la
Source n'a qu'une sorte de contenu. Même règle de garde que le §16, avec `Mods.arsNouveau()` ; classes
« Ars Nouveau only » dans `block/source`, `block/tile/source`, `storage/source`, `client/Source*`,
`client/gui/SourceDrawerInfoGuiAddon`, `gametest/IDSourceGameTests`, `registry/IDSourceContent`.
Ars, Curios et GeckoLib en `runtimeOnly` de dev (épinglés par **id de version Modrinth** : GeckoLib a le
même numéro pour plusieurs loaders), `-PnoArs` pour le chemin « absent ». Déclaré `optional`, `[5.13,)`.

**[vérifié]** 52 game tests avec Mekanism + Ars, 21 sans aucun des deux. Vu en client
(façade violette, texture de Source d'Ars, niveaux, framed) le 29 septembre 2026 ; l'écran n'a pas été
ouvert.

### Comment Ars trouve la Source — lu dans le jar 5.13.2 et la branche `main`

| Qui | Chemin | Pour nous |
|---|---|---|
| Relais, splitters, tourelles | capability `ars_nouveau:source` (`ISourceCap`) | enregistrée sur nos types, et sur contrôleurs/extensions de FS (`ControllerSourceStorage`) |
| Apparatus, imbuement, sourcelinks… | `SourceUtil` : `instanceof SourceJarTile` **ou** registre `SourceManager` | chaque tiroir s'inscrit dans `SourceManager` à `onLoad` (`SourceDrawerProvider`) |

`SourceManager.addInterface` est public et nettoyé par Ars lui-même (toutes les 60 ticks, les
providers dont `isValid()` est faux). Aucun mixin. Le contrôleur, lui, **n'est pas** inscrit dans
`SourceManager` : ses tiroirs le sont déjà, un consommateur proche des deux compterait la Source deux fois.

Capability recréée par son nom comme pour Mekanism (`CapabilityRegistry` est du setup, pas de l'API) ;
**[vérifié]** `weSpeakArsOwnCapability`.

### Pièges trouvés en route

- **`ISourceCap` et `ISourceTile` ne peuvent pas être une seule classe** : les deux déclarent
  `setSource(int)`, l'une en `void`, l'autre en `int`. `BigSourceStorage` implémente la capability,
  `asTile()` est une vue sur les mêmes nombres.
- **`canReceive()` → `canAcceptSource(1)` → `receiveSource` → `canReceive()`** : récursion si on
  n'override pas `canReceive`/`canExtract`. Ars casse la boucle pareil dans `SourceStorage`.
- **Creative** : `SourceUtil.takeSourceMultiple` compte `avant − après` comme pris. Un creative qui
  reste « plein » ne fournirait **rien** (Ars ne dispense que son propre `CreativeSourceJarTile`, par
  `instanceof`). La vue répond à `removeSource(n)` par `MAX − n` sans rien vider.
  **[vérifié]** `aCreativeDrawerSuppliesArs`.
- **Le jar publié diffère de `main`** : son `ISourceTile` a des défauts `addSource(int, boolean)` /
  `removeSource(int, boolean)` qui **ignorent `simulate`** et exécutent. Surchargés pour renvoyer la
  quantité déplacée en respectant la simulation, comme les machines d'Ars.
  **[vérifié]** `aSimulatedRemovalLeavesTheSource`. Leçon : lire le bytecode du jar, pas seulement la branche.
- **`onLoad()` arrive au tick suivant la pose**, pas pendant `setBlock` : un test qui interroge
  `SourceManager` juste après avoir posé le tiroir le trouve absent. Les tests attendent 2 ticks.

### Capacité

Courbe des fluides (`FLUID_STORAGE_MODIFIER`, base = 32 unités × `SOURCE_PER_UNIT` = 32 000 Source,
3,2 jarres). 4 Netherite = 2 097 152 000, **sous le plafond int de l'API Source** : les 4 slots servent,
sans composant à nous ; le Max Storage sature à `Integer.MAX_VALUE`.
**[vérifié]** `capacityFollowsTheFluidCurveAndFitsAnInt`.

### Design

Carcasse graphite, liseré **violet**. Dans le réservoir, la texture animée de la Source d'Ars
(`ars_nouveau:block/mana_still`, celle des Source Jars), non teintée, pleine luminosité, surface qui
respire — `client/TankVolume`, partagé avec le tiroir chimique. Recette : planches autour d'une Source
Jar (pépites de fer pour le framed), sous `mod_loaded` + `feature_enabled`. Interrupteur
`SOURCE_DRAWER_ENABLED` (§17).

### Non fait

Pas de provider Jade/TOP (Jade n'affiche pas la quantité de Source) ; item en main sans contenu.

---

## 19. Tests par les vrais blocs des mods, et release 0.2.0

**Ajouté le 29 septembre 2026.** Les tests des §16 et §18 appelaient nos handlers et les utilitaires
des mods. Trois tests font maintenant passer le contenu **par les blocs des autres mods**, entre deux
de nos tiroirs, avec contrôle de conservation (rien créé en route) :

- `aPressurizedTubeMovesChemicalsBetweenDrawers` — tube Mekanism en *pull* (`setConnectionTypeRaw`) ;
- `aUniversalCableCarriesEnergyBetweenDrawers` — câble universel Mekanism, Joules convertis à la frontière ;
- `anArsRelayMovesSourceBetweenDrawers` — relais Ars lié par `setTakeFrom` / `setSendTo`, comme la Dominion Wand.

**Écartés, parce que ce n'est pas ce que fait un joueur :** insérer dans un tube par sa capability
(Mekanism rend tout — un tube se remplit en tirant ou par l'éjection d'une machine) ; remplir un
`basic_chemical_tank` Mekanism fraîchement posé (aucune face n'expose la capability tant que la config de
côtés n'est pas faite). GeckoLib est `compileOnly` parce que `RelayTile` en étend un type — tests seulement.

**Release 0.2.0** (`mod_version=0.2.0`) : Chemical Drawers, Source Drawer, interrupteurs de config,
Wireless Charger et logo refaits, et les deux correctifs de la 0.1.0 (clic qui lève une exception, casse
sans drop). Le jar ne contient aucune classe Mekanism, Ars ou GeckoLib.

**Botania : reporté.** Aucune release 1.21.1 (dernière : 1.20.1-456, le portage est une branche). Voir
ROADMAP. **Images de la page CurseForge :** régénérées et téléversées ; description, résumé, logo et galerie à jour (29 sept.).
La galerie CurseForge refuse les fichiers de plus de 2 Mo : JPG pleine résolution. **Wiki :** poussé.

---

## 20. Ports — 1.20.1 d'abord, 26.1 ensuite

**Ajouté le 29 septembre 2026.** Analyse complète dans `PORTING.md` ; ici, ce qu'il faut savoir avant
de toucher une des deux branches.

| Branche | Cible | État |
|---|---|---|
| `1.20.1` | **Forge** 47.4.23, Java 17, MDG `legacyforge` 2.0.148, FS 1.20.1-1.2.14, Titanium 3.8.35 | toolchain vert, `compileJava` : 683 erreurs / 53 fichiers |
| `26.1` | NeoForge 26.1.2.112, Java 25, Gradle 9.1, FS 26.1-1.6.1, Titanium 4.0.8 | toolchain vert, `compileJava` : 206 erreurs / 24 fichiers |

**1.20.1 est prioritaire** : seule version où les trois tiroirs existent (Mekanism 10.4, Ars 4.12), et
**seule version avec Botania** — le Mana Drawer ne peut être construit que là. **26.1 est un port
Energy Drawer seul** : ni Mekanism ni Ars n'y existent ; leurs classes sont exclues du source set (pas
supprimées) et absentes du `neoforge.mods.toml`.

Ce qui change la donne, à ne pas redécouvrir :
- **1.20.1 est Forge, pas NeoForge** — FS 1.20.1 n'est publié que pour Forge. Capabilities par
  `getCapability` / `LazyOptional` sur notre tile (le §8 disparaît), et `AttachCapabilitiesEvent` sur
  leur contrôleur, qui retombe sur `super.getCapability`. Pas de `FunctionalUpgradeBehavior` : le
  Wireless Charger est un `UpgradeItem` `UTILITY` que **notre** `serverTick` lit. Pas de composants :
  `getStorageDiv()` remplace `energy_storage_modifier`. La recette framed de FS teste
  `instanceof FramedDrawerBlock` : il nous faut la nôtre.
- **Mekanism 10.4 a quatre handlers** (gaz, infusion, pigment, slurry) ; le modèle est leur
  `MergedChemicalTank`. **Ars 4.12 n'a pas `ISourceCap`** : le tile implémente `ISourceTile` directement.
- **Sur 26.1, `EnergyHandler` expose `getAmountAsLong()`** : le plafond int du §11bis disparaît côté
  capability standard (insert/extract restent int). `BigEnergyStorage` devient transactionnel
  (`SnapshotJournal`, comme `BigFluidHandler` chez FS) — c'est la seule classe où une erreur crée ou
  détruit de l'énergie.
- **Garder les clés NBT `@Save` identiques** entre branches : c'est ce qui fait survivre un tiroir posé
  à la montée de version d'un monde.
