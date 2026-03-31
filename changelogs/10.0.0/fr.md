## Liens symboliques ! 🎉

**La version 10.0.0** introduit les Liens symboliques. Les Liens symboliques vous permettent d'avoir le même traqueur, graphique, fonction ou même groupe dans plusieurs groupes. Ce n'est pas un doublon ni une copie, c'est une référence vers le même composant. Toute modification apportée à l'un sera répercutée sur tous les autres. Pour commencer avec les Liens symboliques, appuyez simplement sur le bouton + en haut à droite de n'importe quel groupe et sélectionnez « Lien symbolique ».

## Nouvelles actions sur les points de données ! 📝

Vous pouvez désormais ajouter des points de données à un traqueur directement depuis l'écran d'historique (celui qui s'ouvre lorsque vous appuyez sur la carte du traqueur). Vous trouverez le nouveau bouton d'action flottant en bas à droite de l'écran.

![Actions sur les points de données](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/changelogs/10.0.0/data_point_actions.jpg)

Il existe également un nouveau mode de sélection multiple dans l'écran d'historique qui vous permet de déplacer, copier ou supprimer plusieurs points de données à la fois. Vous pouvez même copier des points de données d'une fonction vers un traqueur ! Activez le mode de sélection multiple en appuyant longuement sur un point de données, sélectionnez ceux que vous souhaitez et cherchez les nouveaux boutons d'action en bas à droite de l'écran.

## Suivi verrouillé ! 🔒

Une nouvelle fonctionnalité dans la boîte de dialogue d'ajout de point de données vous permet d'ajouter plusieurs points de données pour un traqueur à la suite. Cherchez la nouvelle icône de cadenas à la fin des champs de saisie :

![Suivi verrouillé](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/changelogs/10.0.0/locked_tracking.jpg)

Lorsqu'un cadenas est activé, la boîte de dialogue restera ouverte après l'ajout d'un point de données, et les champs verrouillés seront pré-remplis avec la même valeur que le point de données précédent.

## Corrections de bugs et améliorations

- Corrigé : graphiques bloqués en chargement infini (désolé pour ça)
- Corrigé : modifier un rappel après l'avoir mis à jour ne permettait pas d'ouvrir la boîte de dialogue
- Corrigé : traductions manquantes pour les rappels
- Corrigé : requêtes work manager uniques par rappel pour éviter les rappels en double
- Les rappels copiés sont désormais programmés immédiatement
- Corrigé : rappels copiés apparaissant au mauvais endroit
- Corrigé : notes non affichées sous les graphiques
- Corrigé : l'écart-type renvoie NaN en cas d'erreurs de précision en virgule flottante (dans les nœuds de fonction)
- Corrigé : indices d'affichage ne se mettant pas à jour correctement en cas de conflit d'IDs
- Lien du bouton d'information des scripts Lua vers le guide développeur dans la boîte de dialogue de sélection de nœuds
- Amélioration de la fiabilité des widgets de suivi après les mises à jour de l'application
- Mise à jour des dépendances pour améliorer les performances et la stabilité
- Ciblage d'Android API niveau 36
            