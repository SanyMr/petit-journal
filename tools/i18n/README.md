# Langues de Mon petit journal

## Principe
- Le code n'écrit plus de texte en dur : il appelle `_t("clé")` ou `_t("clé", {valeur})`.
- Les textes sont dans `www/i18n/<langue>.js`. Le français (`fr.js`) est la source et le repli.
- HTML statique : `data-i18n="clé"` (texte) et `data-i18n-attr="aria-label:clé;placeholder:clé"`.
- Dates et nombres : `_date(d, options)` et `_num(n)` suivent la langue choisie.
- Pluriels : `{n, plural, one {# mot} other {# mots}}`, selon les règles de chaque langue.

## Ajouter une langue
1. Copier `www/i18n/en.js` en `www/i18n/<code>.js` et traduire les valeurs (jamais les clés ni les `{valeurs}`).
2. Ajouter la langue dans `LANGS` de `www/i18n/i18n.js` avec `ready:false`.
3. `python3 tools/i18n_check.py` donne la couverture. Passer `ready:true` quand tout est traduit et relu.

## Extraction automatique
`python3 tools/i18n_extract.py www/index.html <nikki.html>` repère les textes français restants (chaînes, gabarits, HTML)
et les remplace par des clés (espace de noms = fonction englobante). Les identifiants (mots seuls en minuscules,
classes, noms de base de données) sont ignorés. En français, l'affichage reste identique au caractère près.

## Migrer un module
1. Écrire `tools/i18n/mNN_<module>.py` : `KEYS` (clé → texte français) et `REPL` (ancien code → nouveau code, `both`/`app`/`web`).
2. `python3 tools/i18n_migrate.py <chemin de nikki.html>` applique la migration aux deux versions (relançable sans risque).
3. Ajouter les traductions dans chaque `<langue>.js`, puis `python3 tools/i18n_check.py`.
4. Vérifier qu'en français rien n'a changé à l'écran (test avant / après).

## Contrôle automatique (avant chaque compilation)
`tools/i18n_check.py` bloque si une clé utilisée manque en français, si une traduction n'a pas les mêmes `{valeurs}`,
si une clé traduite n'existe pas en français, ou si une langue marquée prête est incomplète.

## Widgets et notifications Android

- Les textes natifs vivent dans `android/app/src/main/res/values/strings.xml` (français, repli) et
  `values-<code>/strings.xml` (une langue par dossier, mêmes noms ; `plurals` pour les nombres,
  `string-array` pour les listes comme les saisons ou les petits mots).
- Les widgets suivent la langue choisie dans l'appli : le journal l'envoie avec les données des
  widgets (`lang`), `Tr.java` charge les textes de cette langue (`Tr.s`, `Tr.p`, `Tr.arr`).
- Proverbes de la sagesse du jour : `tools/i18n_android.py` écrit `assets/wisdom/proverbs_<code>.json`
  depuis le pack `wisdom` de `www/i18n/<code>.content.js` (lancé aussi par la compilation).
- `tools/i18n_check.py` contrôle aussi ces fichiers : textes inconnus, `%1$s` différents, listes de
  mauvaise longueur, langue prête sans ses textes Android, proverbes pas à jour.

Ajouter une langue : `www/i18n/<code>.js`, `<code>.content.js`, `res/values-<code>/strings.xml`,
une ligne dans `LANGS` (i18n.js), puis `ready:true` quand tout est traduit.
