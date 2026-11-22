#!/usr/bin/env python3
"""Contrôle des langues, lancé avant chaque compilation (bloque en cas d'erreur).
Erreurs : clé utilisée dans le code mais absente du français ; fichier de langue illisible ;
clé traduite qui n'existe pas en français ; {valeurs} différentes entre le français et une traduction ;
langue marquée « prête » à laquelle il manque des clés ; widgets Android (res/values-<code>/strings.xml) :
texte inconnu, %1$s différents, liste de mauvaise longueur, langue prête incomplète ;
proverbes des widgets (assets/wisdom/proverbs_<code>.json) pas à jour.
Information : couverture de chaque langue et nombre de textes français restant à migrer."""
import glob, json, os, re, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
I18N = os.path.join(ROOT, "www", "i18n")
SRC = [os.path.join(ROOT, "www", "index.html")] + sys.argv[1:]

def load(path):
    s = open(path, encoding="utf-8").read()
    return json.loads(s[s.index("{"):s.rindex("}") + 1])

def vars_of(msg):
    out = set(re.findall(r"\{\s*(\w+)\s*[,}]", msg))
    return out

RES = os.path.join(ROOT, "android", "app", "src", "main", "res")

def res_strings(path):
    """{nom: (type, valeur)} d'un strings.xml ; valeur = texte, {quantité: texte} ou [textes]."""
    import xml.etree.ElementTree as ET
    out = {}
    for e in ET.parse(path).getroot():
        if e.get("translatable") == "false": continue
        if e.tag == "string": out[e.get("name")] = ("string", e.text or "")
        elif e.tag == "plurals": out[e.get("name")] = ("plurals", {i.get("quantity"): i.text or "" for i in e})
        elif e.tag == "string-array": out[e.get("name")] = ("array", [i.text or "" for i in e])
    return out

def fmt_args(t):
    return sorted(re.findall(r"%(?:\d+\$)?[sd]", t))

def android(ready, err):
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import i18n_android
    for f, want in i18n_android.expected().items():
        if not os.path.exists(f) or open(f, encoding="utf-8").read() != want:
            err.append("%s pas à jour : lance python3 tools/i18n_android.py" % os.path.relpath(f, ROOT))
    base = os.path.join(RES, "values", "strings.xml")
    if not os.path.exists(base): return
    fr = res_strings(base)
    for path in sorted(glob.glob(os.path.join(RES, "values-*", "strings.xml"))):
        code = os.path.basename(os.path.dirname(path))[7:]
        try: d = res_strings(path)
        except Exception as e: err.append("android %s illisible : %s" % (code, e)); continue
        for k, (typ, v) in d.items():
            if k not in fr: err.append("android %s : texte inconnu en français : %s" % (code, k)); continue
            ft, fv = fr[k]
            if typ != ft: err.append("android %s : %s n'est pas du même type" % (code, k)); continue
            if typ == "string" and fmt_args(v) != fmt_args(fv): err.append("android %s : %s a %s au lieu de %s" % (code, k, fmt_args(v), fmt_args(fv)))
            if typ == "plurals":
                if "other" not in v: err.append("android %s : %s sans « other »" % (code, k))
                for q, t in v.items():
                    if fmt_args(t) != fmt_args(fv.get("other", "")): err.append("android %s : %s/%s a %s au lieu de %s" % (code, k, q, fmt_args(t), fmt_args(fv.get("other", ""))))
            if typ == "array" and len(v) != len(fv): err.append("android %s : liste %s de %d éléments au lieu de %d" % (code, k, len(v), len(fv)))
        miss = [k for k in fr if k not in d]
        print("android %s : %d textes de widgets traduits sur %d" % (code, len(fr) - len(miss), len(fr)))
        if ready.get(code) == "true" and miss: err.append("android %s : langue prête mais il manque %s" % (code, miss[:5]))
    for code, r in ready.items():
        if r == "true" and code != "fr" and not os.path.exists(os.path.join(RES, "values-" + code, "strings.xml")):
            err.append("%s est prête mais n'a pas de res/values-%s/strings.xml (widgets)" % (code, code))

def main():
    err = []
    try: fr = load(os.path.join(I18N, "fr.js"))
    except Exception as e: print("ERREUR fr.js illisible :", e); sys.exit(1)
    core = open(os.path.join(I18N, "i18n.js"), encoding="utf-8").read()
    ready = dict(re.findall(r"(\w+):\{name:\"[^\"]*\",locale:\"[^\"]*\",ready:(true|false)[,}]", core))
    used = set()
    for p in SRC:
        s = open(p, encoding="utf-8").read()
        used |= set(re.findall(r"_t\(\"([\w.]+)\"", s))
        used |= set(re.findall(r"data-i18n=\"([\w.]+)\"", s))
        used |= set(k for a in re.findall(r"data-i18n-attr=\"([^\"]+)\"", s) for k in re.findall(r":([\w.]+)", a))
        # clés construites : _t("prompt."+i) → toutes les clés prompt.N doivent exister
        for pre in re.findall(r"_t\(\"([\w.]+\.)\"\+", s):
            if not any(k.startswith(pre) for k in fr): err.append("préfixe %s utilisé mais aucune clé" % pre)
    used = {k for k in used if not k.endswith(".")}
    for k in sorted(used - set(fr)): err.append("clé absente du français : " + k)
    for path in sorted(glob.glob(os.path.join(I18N, "*.js"))):
        code = os.path.basename(path)[:-3]
        if code in ("i18n", "fr") or path.endswith(".content.js"): continue
        try: d = load(path)
        except Exception as e: err.append("%s.js illisible : %s" % (code, e)); continue
        for k in d:
            if k not in fr: err.append("%s : clé inconnue en français : %s" % (code, k))
            elif not vars_of(d[k]) <= vars_of(fr[k]): err.append("%s : valeur inconnue dans %s (%s, français : %s)" % (code, k, sorted(vars_of(d[k]) - vars_of(fr[k])), sorted(vars_of(fr[k]))))
            elif vars_of(fr[k]) - vars_of(d[k]) and not all(re.fullmatch(r"v\d+", v) for v in vars_of(fr[k]) - vars_of(d[k])): err.append("%s : valeur oubliée dans %s (%s)" % (code, k, sorted(vars_of(fr[k]) - vars_of(d[k]))))
        miss = [k for k in fr if k not in d]
        cov = 100 * (len(fr) - len(miss)) // max(1, len(fr))
        print("%s : %d %% des clés traduites (%d manquantes)%s" % (code, cov, len(miss), "" if ready.get(code) != "true" else " — langue prête"))
        if ready.get(code) == "true" and miss: err.append("%s est marquée prête mais il manque %d clés" % (code, len(miss)))
    # packs de contenu : mêmes identifiants (proverbes) et même nombre d'éléments (questions) que le français
    app = open(SRC[0], encoding="utf-8").read()
    for path in sorted(glob.glob(os.path.join(I18N, "*.content.js"))):
        code = os.path.basename(path).split(".")[0]; txt = open(path, encoding="utf-8").read()
        packs = dict((m.group(1), json.loads(m.group(2))) for m in re.finditer(r'I18N\.pack\("%s","(\w+)",(.*?)\);\n' % code, txt, re.S))
        if "wisdom" in packs:
            m = re.search(r"var WIS_P=(\[.*?\]);\n", app, re.S); ids = [e[0] for e in json.loads(m.group(1))]
            miss = [i for i in ids if i not in packs["wisdom"]]; bad = [i for i, v in packs["wisdom"].items() if len(v) != 3]
            if miss or bad: err.append("%s.content : proverbes manquants %s / mal formés %s" % (code, miss[:5], bad[:5]))
            print("%s : %d proverbes" % (code, len(packs["wisdom"])))
        if "duo" in packs:
            m = re.search(r'const QDUO=I18N\.list\("duo",(\[.*?\])\);', app, re.S)
            if m and len(json.loads(m.group(1))) != len(packs["duo"]): err.append("%s.content : %d questions à deux au lieu de %d" % (code, len(packs["duo"]), len(json.loads(m.group(1)))))
            print("%s : %d questions à deux" % (code, len(packs["duo"])))
    android(ready, err)
    # textes français encore écrits en dur dans le code (progression de la migration)
    s = open(SRC[0], encoding="utf-8").read()
    left = len(re.findall(r"\"[^\"\n<>{}]*\b(?:tu|ton|ta|tes|le|la|les|une|des|pour|avec)\b[^\"\n<>{}]*\"", s))
    print("français encore en dur (estimation) :", left)
    if err:
        print("\n".join("ERREUR " + e for e in err)); sys.exit(1)
    print("langues : ok (%d clés)" % len(fr))

if __name__ == "__main__": main()
