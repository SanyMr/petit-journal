#!/usr/bin/env python3
"""Prépare les textes des widgets Android à partir des packs de contenu du journal.
Pour chaque www/i18n/<code>.content.js qui a un pack « wisdom », écrit
android/app/src/main/assets/wisdom/proverbs_<code>.json ({id: [traduction, explication, question]}),
lu par WisdomData.java pour la sagesse du jour. Lancé par la compilation ; à relancer après avoir
modifié un pack (tools/i18n_check.py signale un fichier pas à jour)."""
import glob, json, os, re, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
I18N = os.path.join(ROOT, "www", "i18n")
OUT = os.path.join(ROOT, "android", "app", "src", "main", "assets", "wisdom")

def packs(path):
    code = os.path.basename(path).split(".")[0]
    txt = open(path, encoding="utf-8").read()
    return code, dict((m.group(1), json.loads(m.group(2))) for m in re.finditer(r'I18N\.pack\("%s","(\w+)",(.*?)\);\n' % code, txt, re.S))

def expected():
    """{chemin: contenu attendu} pour chaque langue."""
    out = {}
    for path in sorted(glob.glob(os.path.join(I18N, "*.content.js"))):
        code, p = packs(path)
        if "wisdom" in p:
            out[os.path.join(OUT, "proverbs_%s.json" % code)] = json.dumps(p["wisdom"], ensure_ascii=False, separators=(",", ":"), sort_keys=True) + "\n"
    return out

def main():
    for f, s in expected().items():
        open(f, "w", encoding="utf-8").write(s)
        print("écrit", os.path.relpath(f, ROOT))

if __name__ == "__main__": main()
