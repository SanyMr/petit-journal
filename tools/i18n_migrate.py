#!/usr/bin/env python3
"""Applique les modules de migration tools/i18n/m*.py à l'appli (www/index.html) et à la version web (nikki.html).
- installe le système de langues (scripts i18n dans <head>, traduction du HTML avant le code principal) ;
- remplace chaque texte par sa clé ;
- ajoute les clés françaises dans www/i18n/fr.js (le français reste la source).
Relançable sans risque : un remplacement déjà fait est ignoré. Usage : i18n_migrate.py [chemin de nikki.html]"""
import glob, importlib.util, json, os, re, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "www", "index.html")
WEB = sys.argv[1] if len(sys.argv) > 1 else None
FR = os.path.join(ROOT, "www", "i18n", "fr.js")
HEAD = '<script src="i18n/i18n.js"></script><script src="i18n/fr.js"></script>'

def load_dict(path):
    if not os.path.exists(path): return {}
    s = open(path, encoding="utf-8").read()
    return json.loads(s[s.index("{"):s.rindex("}") + 1])

def save_dict(path, code, d):
    body = ",\n".join("%s:%s" % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)) for k, v in d.items())
    open(path, "w", encoding="utf-8").write('/* Mon petit journal · textes en %s (généré puis édité à la main) */\nI18N.add("%s",{\n%s\n});\n' % (code, code, body))

def install(src, kind):
    if HEAD not in src:
        src = src.replace('<meta charset="utf-8">', '<meta charset="utf-8">\n' + HEAD, 1)
    main = '<script type="module">' if kind == "app" else None
    if kind == "web":
        i = src.index("<body"); m = re.compile(r"<script>").search(src, i); main = None
        if "<script>I18N.apply()</script>" not in src: src = src[:m.start()] + "<script>I18N.apply()</script>\n" + src[m.start():]
    elif "<script>I18N.apply()</script>" not in src:
        src = src.replace(main, "<script>I18N.apply()</script>\n" + main, 1)
    return src

def main():
    files = {"app": APP}
    if WEB: files["web"] = WEB
    srcs = {k: install(open(p, encoding="utf-8").read(), k) for k, p in files.items()}
    fr = load_dict(FR); errors = []
    for path in sorted(glob.glob(os.path.join(ROOT, "tools", "i18n", "m*.py"))):
        spec = importlib.util.spec_from_file_location(os.path.basename(path)[:-3], path); mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
        for k, v in mod.KEYS.items():
            if k in fr and fr[k] != v: errors.append("%s : la clé %s existe déjà avec un autre texte" % (mod.__name__, k))
            fr.setdefault(k, v)
        for old, new, where in mod.REPL:
            for kind in (["app", "web"] if where == "both" else [where]):
                if kind not in srcs: continue
                s = srcs[kind]; o = old
                if old.startswith("@") and old.endswith("@"):
                    o = getattr(mod, old.strip("@").lower() + "_block")(s)
                    if o is None:
                        if new in s: continue
                        errors.append("%s [%s] : bloc %s introuvable" % (mod.__name__, kind, old)); continue
                if new in s: continue  # déjà appliqué
                n = s.count(o)
                if n == 1: srcs[kind] = s.replace(o, new)
                else: errors.append("%s [%s] : %d occurrence(s) de %r" % (mod.__name__, kind, n, o[:80]))
    if errors:
        print("\n".join(errors)); sys.exit(1)
    for k, p in files.items(): open(p, "w", encoding="utf-8").write(srcs[k])
    save_dict(FR, "fr", fr)
    print("ok :", len(fr), "clés en français")

if __name__ == "__main__": main()
