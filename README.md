# BountyCraft — Paper 26.2 (Java 25, Maven)

Économie (`/bank`, `/pay`…), PNJ **Hunter** (primes), évènement **/hunt**, PNJ **Forgeron**
(boutique pilotée par `forge.yml`), PNJ **Alchimiste** (8 élixirs), PNJ **Guide**,
resource pack (pioche, hache, potions). Les PNJ sont des entités **Mannequin** natives
(skin de joueur, aucune dépendance : ni Citizens, ni ProtocolLib).

## Compiler
Prérequis : **JDK 25** + Maven.

```
mvn clean package
```
→ `target/BountyCraft-1.0.0.jar` à placer dans `plugins/` d'un serveur **Paper 26.2**.
La dépendance est `io.papermc.paper:paper-api:[26.2.build,26.3)` (dernière build 26.2) ;
pour figer une build : remplacez par exemple `26.2.build.92-stable` dans `pom.xml`.

## Resource pack (obligatoire pour les textures)
1. Hébergez `BountyCraft-ResourcePack.zip` en **lien direct** (GitHub Release, votre site, etc.).
2. `config.yml` → `resource-pack.url: "https://…/BountyCraft-ResourcePack.zip"`
   (le `sha1` est déjà rempli pour le zip fourni ; si vous modifiez le zip, recalculez-le avec `sha1sum`).
3. Le pack est envoyé à la connexion. `/bountycraft pack` le renvoie à un joueur.
Sans pack, les objets custom s'affichent avec un modèle manquant (violet/noir).

## Mise en place des PNJ (admin)
```
/npc create hunter | blacksmith | alchemist | guide     (apparaît là où vous êtes)
/npc skin                 -> VOTRE skin sur le PNJ regardé
/npc skin <joueur>        -> le skin d'un autre joueur
/npc skinall [joueur]     -> même chose pour TOUS les PNJ
/npc remove               -> supprime le PNJ regardé
/npc list
```
Clic droit sur un PNJ = ouvre son menu. Les PNJ sont invulnérables et immobiles.

## Commandes
| Commande | Rôle | Permission |
|---|---|---|
| `/bank [joueur]` (`/bal`, `/money`) | solde | tous (autres joueurs : `bountycraft.bank.others`) |
| `/pay <joueur> <montant>` | envoyer de l'argent | tous |
| `/baltop` | top 10 | tous |
| `/contract [cancel]` | voir/annuler son contrat Hunter | tous |
| `/eco give\|take\|set\|reset <joueur> [montant]` | admin éco | `bountycraft.admin.eco` |
| `/hunt <joueur> [prime]` · `/hunt stop` · `/hunt info` | chasse à l'homme | `bountycraft.admin.hunt` |
| `/npc …` | PNJ | `bountycraft.admin.npc` |
| `/bountycraft reload\|give <joueur> <id>\|pack\|ids` | admin | `bountycraft.admin` |

## Mécaniques
**Hunter.** Le joueur choisit une cible dans le menu et doit la tuer (60 min max). La prime vaut
`max(min, base × decay^morts)` où *morts* = nombre de fois où la cible est morte **de la main d'un joueur**
(valeurs dans `config.yml › bounty`). Une cible tuée est protégée 10 min. Action bar : distance, flèche, temps.

**/hunt.** 30 min, boss bar pour tout le monde (temps, prime, coordonnées de la cible ; la cible voit sa propre barre).
Le joueur qui tue la cible empoche la prime (`hunt-event`, ou valeur donnée à `/hunt`).

**Forgeron.** Tout est dans `forge.yml` (voir plus bas). Accroupi = capacités désactivées.

**Élixirs** (une seule potion active à la fois, perdue à la mort) — compétence = touche **F**
(échange de main) ; l'End a aussi **Shift+F**. Les durées/cooldowns/prix sont dans `config.yml › elixirs`.

| Élixir | Passif | Compétence |
|---|---|---|
| Invisibilité | invisibilité **permanente**, les monstres ne vous attaquent jamais | aura 5 min : *Obscurité* aux joueurs à ≤ 10 blocs |
| Warden | +10 cœurs | rayon sonique (dégâts + recul) |
| Soin | Régénération II | 30 s : soigne l'être regardé |
| Émeraude | Héros du village + Célérité II | — |
| Vent | zéro dégât de chute | propulsion dans les airs |
| End | — | F : téléportation (sans cooldown) · Shift+F : souffle du dragon |
| Feu | Résistance au feu | 3 min : blocs/mobs tués cuits, tout ce que vous frappez brûle |
| Eau | Grâce du dauphin + Puissance de l'aqueduc | pluie 15 min + trident *Impulsion* prêté 15 min |

## Ajouter un outil / une arme sans toucher au plugin
Éditez `plugins/BountyCraft/forge.yml` puis `/bountycraft reload` :

```yaml
items:
  ma_pelle:
    price: 18000
    material: NETHERITE_SHOVEL
    name: "<gradient:#ff9a1a:#ff3d00><bold>Pelle des Braises"
    lore: ["<gray>Mine en 5x5x5"]
    item-model: "bountycraft:ma_pelle"     # optionnel : texture ajoutée au resource pack
    unbreakable: true
    enchantments: { efficiency: 8, unbreaking: 5 }
    abilities:
      area-mine: { radius: 2 }             # area-mine | tree-feller | ignite | lifesteal
```
Pour une texture perso : ajoutez `assets/bountycraft/textures/item/ma_pelle.png`,
`models/item/ma_pelle.json` et `items/ma_pelle.json` (copiez ceux de la pioche).

## Fichiers générés
`config.yml`, `forge.yml`, `guide.yml` (Discord, site, règles, fonctionnement — **à personnaliser**),
`data.yml` (comptes, stats), `npcs.yml` (registre des PNJ).

## Notes
* Les contrats Hunter sont en mémoire (perdus au redémarrage) ; l'argent et les stats sont sauvegardés.
* L'API `Mannequin`/`ResolvableProfile` est marquée *expérimentale* par Paper : à surveiller lors des mises à jour.
