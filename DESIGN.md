# Immaterial Drawers — Design

**Ce que fait ce fichier :** la réflexion produit, écrite avant le code. Pourquoi le mod existe,
face à quoi il se place, ce qui a été arrêté et ce qui a été écarté.
C'est le seul document en français avec `CLAUDE.md`, et pour la même raison : il est interne.

**Ce que ce fichier ne fait pas :** l'opérationnel. Les contraintes techniques vérifiées,
l'architecture et l'ordre de travail sont dans `CLAUDE.md`. Ce qui a été livré et dans quel ordre
est dans `ROADMAP.md`.

> Rédigé le 22 août 2026, extrait du brief unique d'origine. Les sections gardent leur numérotation
> de l'époque pour que les renvois croisés depuis `CLAUDE.md` restent lisibles.

---

## 1. Concept

Functional Storage stocke des items et des fluides. Rien d'autre. Ce mod ajoute des tiroirs pour
le reste — l'énergie, les chemicals, et ce qui viendra — en s'intégrant visuellement et
fonctionnellement aux murs de drawers existants.

Le fil conducteur : **ce qu'on ne peut pas tenir dans la main.** D'où le nom.

**Le premier contenu livré est l'Energy Drawer (FE)**, qui sert de test de viabilité technique
et commercial. L'architecture, le nommage et le découpage en packages doivent anticiper les
suivants dès le départ : rien de spécifique à l'énergie ne doit remonter dans le code commun.

Pour l'Energy Drawer en particulier : le bloc est le support, les **augments custom** sont le
produit — un tiroir FE seul ne fait que dupliquer une Energy Cell de Powah.

---


## 3. Positionnement (concurrence)

### Stockage d'énergie

| Mod | Résumé | Pourquoi ça ne couvre pas le créneau |
|---|---|---|
| Powah | Energy Cells par tier, réacteurs, thermoélectrique | Blocs isolés, aucun rapport avec un mur de tiroirs |
| Mekanism | Induction Matrix, Energy Cubes | Multiblock ou cube autonome, esthétique propre |
| Flux Networks | Réseau d'énergie sans fil | Résout le transport, pas le stockage de proximité |
| Energized Power | Cellules par tier | Idem Powah |

**Aucun n'a d'apparence ni de comportement de tiroir.** Un joueur qui a construit un mur de
drawers doit y coller un bloc étranger.

### Addons Functional Storage

| Addon | Ce qu'il ajoute | Ce qu'il n'ajoute pas |
|---|---|---|
| More Functional Storage (matyrobbrt, 1.1M DL) | Upgrades : breaker, placer, refill, stonecutter | Aucun type de tiroir |

**C'est le seul addon significatif, et il n'ajoute que des upgrades.** Ce n'est pas un choix
esthétique de son auteur : c'est la seule porte que FS ouvre (voir §7).

### Concurrents de FS lui-même

Storage Drawers, Extended Drawers, Storage Drawers Unlimited. Alternatives à FS, pas des
extensions — hors sujet, mais à connaître pour le nommage : « Extra Drawers » entrait en
collision frontale avec Extended Drawers, d'où son rejet.

**Créneau libre** : aucun addon FS n'ajoute de type de tiroir, et aucun mod d'énergie ne
s'intègre à un mur de drawers.

### Le contre-exemple qui explique tout : AE2

AE2 a conçu un point d'extension pour ça. `AEKeyType` est un registre : un addon enregistre un
nouveau type de clé, et **tout** AE2 le comprend d'un coup — drives, import/export buses, storage
buses, terminaux, P2P, autocrafting.

Preuve que ça marche : Applied Mekanistics (54,5M DL) ajoute le support des chemicals Mekanism
aux devices AE2 existants — storage bus sur un waste barrel, cellules 1k/4k/16k/64k pour les gaz
dans les ME Drives. Un seul addon, un type de ressource entièrement nouveau dans tout l'écosystème.

**Le mérite est au design d'AE2, pas à l'addon.** L'addon a rempli un formulaire.
Functional Storage ne fournit pas ce formulaire — c'est toute la difficulté de ce projet.

---


## 5. Décisions produit (arrêtées)

| Décision | Raison |
|---|---|
| **Nom-parapluie**, pas « Energy Drawers » | L'énergie est le premier module, pas le mod. Le `modid` est irréversible. |
| **Energy Drawer X_1 uniquement** | La géométrie d'un drawer FS, ce sont des zones de clic multiples (`DrawerBlock.CACHED_SHAPES`). Le FE n'a qu'un seul type — les slots 2/3/4 n'auraient rien à contenir. |
| **Variante Framed obligatoire** | ~100 lignes, et le joueur applique n'importe quelle texture. C'est le vrai mécanisme d'intégration aux murs existants — plus efficace que de deviner quelle essence de bois assortir. |
| **Scaling via les Storage Upgrades de FS** | `SizeProvider` est une interface publique avec des helpers statiques. Aucun système parallèle à inventer. |
| **Nerf plus agressif que les fluides** | `IEnergyStorage` est basé sur des `int` (voir §11). Constante `ENERGY_DIVISOR` calquée sur `FLUID_DIVISOR`, valeur à caler plus tard. |
| **Les augments sont le produit** | Un tiroir FE seul duplique une Energy Cell de Powah. Le bloc est le support. |
| **Wireless Charger en tête d'affiche** | « Mon mur de tiroirs recharge mes outils » — personne ne l'a, et ça passe par l'API officielle. |

### Augments prévus

| Augment | Faisabilité | Modèle à copier dans FS |
|---|---|---|
| **Wireless Charger** — recharge joueurs/items dans un rayon | ✅ | `work()`, tick |
| Auto-output — pousse le FE vers les blocs adjacents | ✅ | `MoveFluidsBehavior` |
| Générateur — brûle du combustible → FE | ✅ | `GenerateFluidBehavior` |
| Redstone sur seuil de charge | ✅ | `getRedstoneSignal()`, dans l'interface |

---

## 6. Hors scope (v1)

**Chemical Drawer (Mekanism)** — reporté en **v2 du même mod**, pas écarté (c'est précisément ce
que le nom-parapluie anticipe). Meilleur candidat après l'énergie : Mekanism 1.21.1 a unifié ses
types en un seul `IChemicalHandler`, ajusté pour être implémentable à côté de `IFluidHandler`,
et c'est le seul contenu qui justifierait des variantes X_2 / X_4 — plusieurs chemicals dans un
même tiroir, la sémantique d'origine du drawer retrouvée. Dépendance optionnelle gardée par
`ModList.get().isLoaded()` — Titanium fournit déjà un `PluginManager` pour ça.

**Mana Botania, Source Ars Nouveau** — écartés de la v1. Ce ne sont pas « d'autres types
d'énergie » mais des APIs propriétaires, une intégration bespoke chacune, cassée à chaque release
du mod source. Si repris un jour : jar séparé.

> **⚠️ RENVERSÉ pour la Source, le 29 septembre 2026** — à la demande de l'auteur, et dans le même
> jar. Ce qui a changé depuis cette note : Ars expose un point d'extension public (`SourceManager`)
> en plus de sa capability, donc l'intégration n'a demandé ni mixin ni classe interne ; la garde
> `Mods` du Mekanism a prouvé qu'une dépendance optionnelle dans le même jar ne coûte rien sans le
> mod ; et le risque « cassé à chaque release » est couvert par des game tests qui exercent les
> vrais chemins d'Ars (`SourceUtil`), épinglés sur une version. Le risque n'a pas disparu — il est
> maintenant détecté. Voir `CLAUDE.md` §18. Le Mana de Botania reste écarté.

**« Spark » façon Botania** — un Spark est une *entité* greffée sur un Mana Pool avec son propre
réseau. Ce n'est pas un comportement d'upgrade ; rien à voir avec les autres augments.

**Vis Thaumcraft** — n'existe pas en 1.21.1. Le seul « Thaumcraft » 1.21.1 est *Thaumon*
(décoration uniquement, ni aura ni aspects). Le port AuramMods cible 1.20.1 Forge et n'a pas fini
de remplacer ses implémentations placeholder. Rien à intégrer.

**Multiloader / Fabric** — non.

---

