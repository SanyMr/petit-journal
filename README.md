# Mon petit journal 日記

Journal intime kawaii pour Android : pages du jour, humeurs, coin déco (photos + stickers),
tampons, gachapon, mascottes et accessoires, to-do list, livre à feuilleter, bilan, rappel quotidien.

## Récupérer l'APK

À chaque modification envoyée sur la branche `main`, GitHub compile l'application automatiquement :

1. Onglet **Actions** : attendre que « Construire l'APK » soit vert (environ 5 minutes).
2. Onglet **Releases** (à droite de la page du dépôt) : télécharger `mon-petit-journal.apk`.
3. Sur le téléphone : ouvrir le fichier, autoriser « Installer des applications inconnues » si Android le demande.

Les mises à jour s'installent par-dessus l'ancienne version sans perdre les données
(elles sont toutes signées avec la même clé, dans `android/keystore/`).

## Structure

- `www/index.html` : toute l'application (HTML, CSS, JavaScript), polices incluses dans `www/fonts/`.
- `android/` : projet Android généré par Capacitor 6.
- `capacitor.config.json` : nom, identifiant, écran de démarrage, icône des notifications.

## Compiler sur son ordinateur (facultatif)

Node 20 et Android Studio (JDK 17) installés :

```
npm ci
npx cap sync android
cd android && ./gradlew assembleRelease
```

L'APK se trouve dans `android/app/build/outputs/apk/release/`.

## Sécurité

La clé de signature est dans le dépôt pour simplifier : **garde ce dépôt privé**.
Les données du journal restent uniquement sur le téléphone. Pense à exporter une sauvegarde
(Réglages → Sauvegarde) de temps en temps.
