# Duper

**Téléphone perdu ? Envoyez-lui un « duper ».**

Retrouvez votre téléphone Android à distance par SMS. Aucun Internet nécessaire sur l'appareil cible.
[![F-Droid](https://img.shields.io/f-droid/v/fr.sudotiz.duper?label=F-Droid)](https://f-droid.org/packages/fr.sudotiz.duper)

> **Pourquoi « Duper » ?** Verlan français de *perdu* : **du-per**. L'app qui retrouve ce qui est perdu.

|  |  |
| :---: | :---: |
| ![screenshot1](assets/screenshot1.png) | ![screenshot2](assets/screenshot2.png) |
| | |

## Fonctionnalités

- **Sonnerie** – Alarme + flash au volume max, fonctionne en mode silencieux
- **Localisation** – Coordonnées GPS par SMS, avec lien OpenStreetMap
- **Hors ligne** – SMS purs, pas d'Internet, pas de compte, pas de cloud
- **Configurable** – Préfixe, mot de passe Sonnerie optionnel, secret Localisation obligatoire

## Démarrage rapide

Version actuelle : **2.1.2** (22 septembre 2026)

1. Téléchargez la dernière APK
    - [Releases GitHub](https://github.com/sudo-Tiz/Duper/releases) — **v2.1.2**
    - [F-Droid](https://f-droid.org/packages/fr.sudotiz.duper) — **2.1.2**
2. Activez Sonnerie ou Localisation dans l'app et n'accordez que les permissions demandées
3. Depuis un autre téléphone, envoyez une commande configurée

## Commandes

| Commande | Action |
|----------|--------|
| `<préfixe>` | Sonnerie (si aucun mot de passe défini) |
| `<préfixe> ring` | Idem |
| `<préfixe> <mot de passe>` | Sonnerie (si mot de passe défini) |
| `<préfixe> locate <secret>` | Envoie les coordonnées GPS |

- Préfixe : un mot. Mot de passe/secret : sensible à la casse.
- Sonnerie et Localisation sont **désactivées par défaut**.
- Le secret de Localisation est **obligatoire**. Le mot de passe de Sonnerie est optionnel ; confirmation SMS optionnelle.
- Les commandes ne s'exécutent que lorsque le téléphone est **verrouillé**. Déverrouillé → refusé, aucune réponse.

## Permissions

- **SMS** – Demandée à l'activation de Sonnerie ou Localisation pour recevoir les commandes et envoyer les réponses
- **Appareil photo** – Demandée à l'activation de Sonnerie pour piloter la lampe du téléphone ; Duper ne capture ni photo ni vidéo
- **Position** – Demandée à l'activation de Localisation pour le suivi GPS
- **Position en arrière-plan** – Demandée à l'activation de Localisation pour suivre quand l'app est fermée
- **Notifications** – Optionnelles ; affiche les tentatives de commande acceptées ou refusées sur le téléphone cible

## Confidentialité

Pas de compte, pas de serveur, pas de traçage, pas de permission Internet. Secrets, réglages, historique et dernière position restent sur l'appareil et sont exclus des sauvegardes et transferts Android.

La Sonnerie utilise le flux audio d'alarme. Le mode Ne pas déranger peut limiter les alertes (support DND prévu).

## Dépannage

### Android 15+ : SMS grisés

Android 15+ bloque les SMS par défaut. Correction unique :

1. **Paramètres → Applications → Duper → ⋮ → Autoriser les paramètres restreints**
2. Retour dans l'app, accorder **SMS**

## Activation automatique du GPS (Optionnel)

Si les services de position sont désactivés, la localisation échoue. Une autorisation ADB unique permet à Duper de les réactiver :

```sh
adb shell pm grant fr.sudotiz.duper android.permission.WRITE_SECURE_SETTINGS
```

Nécessite le débogage USB. Optionnel — inutile si la position est déjà active.

## Feuille de route

- [ ] Blocage d'expéditeurs : liste noire locale avec ajout, édition, recherche, collage
- [ ] Support optionnel du mode Ne pas déranger
- [ ] Import/export de la liste noire en UTF-8 via sélecteur de documents

## Licence

GPL-3.0 – Voir [LICENSE](LICENSE)
