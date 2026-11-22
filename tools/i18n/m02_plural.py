# Module 2 : le petit assistant plural(n, mot) passe par des clés de pluriel (règles de chaque langue).
W = {"capsule": "capsule", "faite": "faite", "fleur": "fleur", "jour": "jour", "lettre": "lettre", "page": "page",
     "prochain": "prochain", "tâche": "tâche", "vœu": "vœu", "mot": "mot", "tampon": "tampon", "pétale": "pétale"}
KEYS = {}
for w in W:
    KEYS["noun." + w] = "{n, plural, one {# %s} other {# %ss}}" % (w, w)
REPL = [
  ('const plural=(n,w)=>n+" "+w+(n>1?"s":"");',
   'const plural=(n,w)=>I18N.has("noun."+w)?_t("noun."+w,{n}):n+" "+w+(n>1?"s":"");', "both"),
]
