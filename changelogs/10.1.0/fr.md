## Rechercher ! 🎉

**La version 10.1.0 introduit Rechercher !**

### Trouver les composants plus rapidement

Utilisez la nouvelle icône de recherche dans la barre supérieure pour trouver des composants dans la hiérarchie du groupe actuel.

La recherche est approximative, les correspondances proches fonctionnent donc, et les descriptions sont également incluses.

Importer et exporter partagent désormais une boîte de dialogue compacte avec des onglets, et chaque groupe mémorise le dernier onglet utilisé.

### Rechercher dans l'historique

Les écrans d'historique des traqueurs et des fonctions disposent aussi de la recherche. Les points de données sont filtrés par valeur, description ou notes correspondantes, tout en conservant l'ordre chronologique inverse.

### Rechercher dans les notes

L'écran des notes prend désormais en charge la recherche, ainsi que des filtres pour les notes globales et les notes de points de données.

---

### Corrections de bugs et améliorations

- Corrigé : graphiques désactivés trop agressivement au démarrage avec Lua désactivé
- Corrigé : la boîte de dialogue du catalogue de fonctions Lua pouvait rester bloquée indéfiniment en chargement sur les réseaux instables
- Corrigé : la barre supérieure de l'écran d'historique ne s'animait pas à l'ouverture
- Corrigé : le graphique en barres empilait incorrectement les points de données dans la dernière barre
- Corrigé : action IME ignorée à la fin de la saisie de durée
- Corrigé : les fonctions Lua personnalisées n'affichaient pas les options enum utilisant des noms de recherche par clé
