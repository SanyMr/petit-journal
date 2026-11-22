#!/usr/bin/env python3
"""Extraction automatique des textes français vers des clés de langue.
Usage : i18n_extract.py fichier.html [fichier2.html ...]
- Code : chaque chaîne française devient _t("clé"). Dans les gabarits `...${x}...`, chaque morceau de texte
  (hors balises) et chaque attribut lisible (aria-label, title, placeholder, alt) devient ${_t("clé",{v0:(x)})}.
- HTML statique : data-i18n / data-i18n-attr.
- La clé dépend de la fonction englobante et du texte : les deux versions (appli, web) partagent les mêmes clés.
- En français, _t renvoie exactement le texte d'origine : le comportement ne change pas.
Les clés sont ajoutées à www/i18n/fr.js."""
import hashlib, json, os, re, sys, unicodedata
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FRJS = os.path.join(ROOT, "www", "i18n", "fr.js")
SKIP_LINES = ("const QDUO=", "var qrcode=", "var WIS_P=")      # contenus à part (packs) et bibliothèques
FRW = set("""le la les un une des du de au aux et ou à ton ta tes mon ma mes son sa ses ce cet cette ces pour avec sans dans sur sous par
est être ai sont pas plus encore déjà toujours jamais tu toi moi je il elle nous vous ils elles qui que quoi quand comment
pourquoi ici là jour jours page pages mot mots tampon tampons jardin amie amies ami amis écris écrire touche fois rien tout tous toute
bien très peu petit petite petits petites nouveau nouvelle nouveaux voir ouvrir fermer retour annuler choisir ajouter garder""".split())
ATTRS = ("aria-label", "title", "placeholder", "alt")
AFTER_SKIP = re.compile(r"(\$\(|getElementById\(|querySelector(All)?\(|closest\(|matches\(|getItem\(|setItem\(|removeItem\(|===\s*|!==\s*|==\s*|!=\s*|case\s+|Sfx\.play\(|classList\.\w+\(|fs(Set|Get|List|Del)\(|reference:\s*|indexOf\(|includes\(|startsWith\(|endsWith\(|split\(|replace\(|replaceAll\(|RegExp\(|console\.\w+\(|addEventListener\(|dispatchEvent\(new Event\(|createElement(NS)?\(|getAttribute\(|setAttribute\(|hasAttribute\(|removeAttribute\(|setProperty\(|getPropertyValue\(|PL_\(\)\.\w+\.\w+\(|_t\(|in\s+)$")

def load_fr():
    if not os.path.exists(FRJS): return {}
    s = open(FRJS, encoding="utf-8").read(); return json.loads(s[s.index("{"):s.rindex("}") + 1])

def save_fr(d):
    body = ",\n".join("%s:%s" % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)) for k, v in d.items())
    open(FRJS, "w", encoding="utf-8").write('/* Mon petit journal · textes en fr (généré puis édité à la main) */\nI18N.add("fr",{\n%s\n});\n' % body)

def french(text):
    if text.strip() in ("»", "«", "« »") and " " in text: return True          # guillemets français à part
    t = re.sub(r"\{v\d+\}", " ", text).strip()
    if "<" in t and ">" in t: return False   # les chaînes avec balises passent par split_html
    if not re.search(r"[A-Za-zÀ-ÿ]{2,}", t): return False
    if "{" in t or "}" in t: return False
    if re.search(r"(rgba?\(|var\(--|https?:|\bpx\b|\d+px|#[0-9a-fA-F]{3,8}\b|\.(js|png|jpe?g|svg|json|css|mp3|xhtml|html)\b|^\w+[/:]\w)", t): return False
    if re.fullmatch(r"[A-Z0-9_ ]+", t): return False                      # constantes
    if re.fullmatch(r"[a-zà-ÿœ0-9_\-.]+", t) and not (text[:1] == " " or text[-1:] == " "): return False   # un mot seul collé : souvent un identifiant
    if t in ("Enter", "Escape", "Backspace", "Delete", "Tab", "Arrow", "ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown", "Space"): return False
    if re.fullmatch(r"[a-z0-9_\-.:/ ]+", t) and not set(re.findall(r"[a-zà-ÿ']+", t)) & FRW: return False   # classes css, identifiants
    if re.search(r"[À-ÿœŒ’«»…]", t): return True
    words = re.findall(r"[A-Za-zÀ-ÿ']+", t.lower())
    if set(words) & FRW: return True
    if re.fullmatch(r"[A-Z][a-zà-ÿ\-]+( [a-zà-ÿ\-]+)*[ !.?✿]*", t): return True   # mot ou nom propre affiché
    return False

def slug(text):
    t = unicodedata.normalize("NFKD", re.sub(r"\{v\d+\}", " ", text)).encode("ascii", "ignore").decode().lower()
    w = re.findall(r"[a-z0-9]+", t)[:4]
    return "_".join(w) or "txt"

def key_for(ns, text):
    return "%s.%s_%s" % (ns, slug(text), hashlib.md5(text.encode()).hexdigest()[:4])

def unescape(raw, quote):
    out = []; i = 0
    while i < len(raw):
        c = raw[i]
        if c == "\\" and i + 1 < len(raw):
            n = raw[i + 1]
            if n == "n": out.append("\n"); i += 2
            elif n == "t": out.append("\t"); i += 2
            elif n == "r": out.append("\r"); i += 2
            elif n == "u" and raw[i + 2:i + 3] == "{":
                j = raw.index("}", i); out.append(chr(int(raw[i + 3:j], 16))); i = j + 1
            elif n == "u": out.append(chr(int(raw[i + 2:i + 6], 16))); i += 6
            elif n == "x": out.append(chr(int(raw[i + 2:i + 4], 16))); i += 4
            elif n == "\n": i += 2
            else: out.append(n); i += 2
        else: out.append(c); i += 1
    return "".join(out)

class X:
    def __init__(self, src, fr):
        self.s = src; self.fr = fr; self.new = {}; self.count = 0
        # espace de noms : dernière déclaration de premier niveau
        self.decl = [(m.start(), m.group(1) or m.group(2)) for m in re.finditer(r"^(?:async\s+)?function\s+(\w+)|^(?:const|let|var)\s+(\w+)", src, re.M)]
    def ns(self, pos):
        lo, hi, best = 0, len(self.decl) - 1, "app"
        while lo <= hi:
            mid = (lo + hi) // 2
            if self.decl[mid][0] <= pos: best = self.decl[mid][1]; lo = mid + 1
            else: hi = mid - 1
        return best[:24]
    def key(self, pos, text):
        k = key_for(self.ns(pos), text)
        if k in self.fr and self.fr[k] != text: k += "x"
        if k not in self.fr: self.fr[k] = text; self.new[k] = text
        self.count += 1
        return k
    # ---------- code JavaScript ----------
    def js(self, i, end, stop_brace=False):
        """transforme le code de i à end ; si stop_brace, s'arrête à l'accolade fermante de ${...}. Renvoie (texte, position)."""
        s = self.s; out = []; depth = 0; prev = "("
        while i < end:
            c = s[i]
            if c == "/" and s.startswith("//", i):
                j = s.find("\n", i); j = end if j < 0 else j; out.append(s[i:j]); i = j; continue
            if c == "/" and s.startswith("/*", i):
                j = s.index("*/", i) + 2; out.append(s[i:j]); i = j; continue
            if c == "/" and (prev in "(,=:[!&|?{};+-*%<>~^" or re.search(r"\b(return|typeof|case|in|of)\s*$", "".join(out[-3:]))):
                j = i + 1; cls = False
                while j < end:
                    if s[j] == "\\": j += 2; continue
                    if s[j] == "[": cls = True
                    elif s[j] == "]": cls = False
                    elif s[j] == "/" and not cls: break
                    elif s[j] == "\n": break
                    j += 1
                j += 1
                while j < end and s[j].isalpha(): j += 1
                out.append(s[i:j]); i = j; prev = "x"; continue
            if c in "\"'":
                j = i + 1
                while s[j] != c:
                    if s[j] == "\\": j += 1
                    j += 1
                raw = s[i + 1:j]; val = unescape(raw, c)
                before = "".join(out)[-40:]
                after = s[j + 1:j + 3]
                ok_ctx = not AFTER_SKIP.search(before) and not (re.match(r"\s*:", after) and re.search(r"[{,]\s*$", before)) and "\n" not in val
                if ok_ctx and "<" in val and ">" in val and not val.lstrip().startswith(("<path", "<rect", "<circle", "<ellipse", "<g", "<polygon", "<line")):
                    html = self.split_html(i, val)
                    out.append(html if html else s[i:j + 1])
                elif ok_ctx and french(val):
                    out.append('_t("%s")' % self.key(i, val))
                else: out.append(s[i:j + 1])
                i = j + 1; prev = "x"; continue
            if c == "`":
                t, i = self.template(i + 1, end); out.append(t); prev = "x"; continue
            if stop_brace:
                if c == "{": depth += 1
                elif c == "}":
                    if depth == 0: return "".join(out), i
                    depth -= 1
            out.append(c)
            if not c.isspace(): prev = c
            i += 1
        return "".join(out), i
    def split_html(self, pos, val):
        """chaîne simple avec balises → concaténation "<b>"+_t("clé")+"</b>" ; None si rien à traduire"""
        pieces = []; changed = False
        for m in re.finditer(r"<[^>]*>|[^<]+", val):
            g = m.group(0)
            if g.startswith("<"):
                # attributs lisibles dans la balise
                parts = []; last = 0
                for am in re.finditer(r'(%s)="([^"]*)"' % "|".join(ATTRS), g):
                    v = am.group(2)
                    if v and french(v):
                        parts.append(("s", g[last:am.start(2)])); parts.append(("k", self.key(pos, v))); last = am.end(2); changed = True
                parts.append(("s", g[last:])); pieces += parts; continue
            core = g.strip()
            if core and french(core):
                lead = g[:len(g) - len(g.lstrip())]; trail = g[len(g.rstrip()):]
                pieces += [("s", lead), ("k", self.key(pos, core)), ("s", trail)]; changed = True
            else: pieces.append(("s", g))
        if not changed: return None
        out = []
        for t, v in pieces:
            if t == "k": out.append('_t("%s")' % v)
            elif v: out.append(json.dumps(v, ensure_ascii=False))
        return "(" + "+".join(out) + ")"
    def template(self, i, end):
        """i : juste après l'accent grave ; renvoie (gabarit transformé, position après l'accent grave fermant)"""
        s = self.s; parts = []  # ("t", texte brut, pos) ou ("e", code transformé)
        buf = []; start = i
        while True:
            c = s[i]
            if c == "\\": buf.append(s[i:i + 2]); i += 2; continue
            if c == "`": parts.append(("t", "".join(buf), start)); i += 1; break
            if c == "$" and s[i + 1] == "{":
                parts.append(("t", "".join(buf), start)); buf = []
                code, j = self.js(i + 2, end, stop_brace=True)
                parts.append(("e", code)); i = j + 1; start = i; continue
            buf.append(c); i += 1
        return "`" + self.segments(parts) + "`", i
    def segments(self, parts):
        # parcourt le gabarit en flux de caractères/expressions et repère texte hors balise et valeurs d'attributs lisibles
        flat = []  # (type, valeur, pos)
        for p in parts:
            if p[0] == "t":
                for k, ch in enumerate(p[1]): flat.append(("c", ch, p[2] + k))
            else: flat.append(("e", p[1], None))
        out = []; n = len(flat); i = 0
        def emit_seg(seg):
            # seg : liste d'éléments ; renvoie le code du segment (traduit si français)
            vals = []; msg = []; raw = []
            for typ, v, pos in seg:
                raw.append(v if typ == "c" else "${" + v + "}")
                if typ == "c": msg.append(v)
                else: msg.append("{v%d}" % len(vals)); vals.append(v)
            rawtxt = "".join(raw); text = "".join(msg)
            lead = len(text) - len(text.lstrip()); trail = len(text) - len(text.rstrip())
            core = text.strip()
            try: core_val = unescape(core, "`")
            except Exception: return rawtxt
            if not french(core_val) or "\\" in core and "\\n" not in core and "\\u" not in core and "\\'" not in core: return rawtxt
            if re.search(r"[<>]", core_val): return rawtxt
            pos = next((p for t, v, p in seg if t == "c"), 0)
            k = self.key(pos, core_val)
            args = ",".join("v%d:(%s)" % (m, v) for m, v in enumerate(vals) if "{v%d}" % m in core)
            call = '_t("%s"%s)' % (k, ",{" + args + "}" if args else "")
            # garder les espaces de bord à l'extérieur (seulement des caractères, jamais une expression)
            lead_s = text[:lead]; trail_s = text[len(text) - trail:] if trail else ""
            return lead_s + "${" + call + "}" + trail_s
        while i < n:
            typ, v, pos = flat[i]
            if typ == "c" and v == "<":
                # balise : copier jusqu'à '>' en traitant les attributs lisibles
                out.append("<"); i += 1
                while i < n and not (flat[i][0] == "c" and flat[i][1] == ">"):
                    # attribut lisible ?
                    m = None
                    if flat[i][0] == "c":
                        ahead = "".join(x[1] if x[0] == "c" else "\0" for x in flat[i:i + 16])
                        m = re.match(r"(%s)=\"" % "|".join(ATTRS), ahead)
                    if m:
                        out.append(m.group(0)); i += len(m.group(0)); seg = []
                        while i < n and not (flat[i][0] == "c" and flat[i][1] == '"'): seg.append(flat[i]); i += 1
                        out.append(emit_seg(seg)); continue
                    out.append(flat[i][1] if flat[i][0] == "c" else "${" + flat[i][1] + "}"); i += 1
                if i < n: out.append(">"); i += 1
                continue
            seg = []
            while i < n and not (flat[i][0] == "c" and flat[i][1] == "<"): seg.append(flat[i]); i += 1
            out.append(emit_seg(seg))
        return "".join(out)
    # ---------- HTML statique ----------
    def html(self, frag, base):
        def attr_fix(tag):
            if "data-i18n-attr" in tag: return tag
            adds = []
            for a in ATTRS:
                m = re.search(r'\s%s="([^"]*)"' % a, tag)
                if m and french(m.group(1)) and "${" not in m.group(1): adds.append("%s:%s" % (a, self.key(base, m.group(1).replace("\\'", "'"))))
            if not adds: return tag
            return tag[:-1].rstrip("/") + ' data-i18n-attr="%s"' % ";".join(adds) + (" />" if tag.endswith("/>") else ">")
        out = []; pos = 0
        for m in re.finditer(r"<svg\b.*?</svg>|<!--.*?-->|<[^>]+>|[^<]+", frag, re.S):
            g = m.group(0)
            if g.startswith("<svg") or g.startswith("<!--"): out.append(g); continue
            if g.startswith("</"): out.append(g); continue
            if g.startswith("<"): out.append(attr_fix(g)); continue
            txt = g; core = txt.strip()
            if core and french(core) and "data-i18n=" not in "".join(out[-1:]):
                k = self.key(base, core.replace("&#39;", "'"))
                lead = txt[:len(txt) - len(txt.lstrip())]; trail = txt[len(txt.rstrip()):]
                # élément ne contenant que ce texte : on annote l'élément ; sinon on enveloppe le texte
                nxt = frag[m.end():m.end() + 2]
                prevtag = out[-1] if out else ""
                if prevtag.startswith("<") and not prevtag.startswith("</") and nxt == "</" and not lead and not trail:
                    out[-1] = prevtag[:-1] + ' data-i18n="%s">' % k; out.append(txt)
                else: out.append(lead + '<span data-i18n="%s">%s</span>' % (k, core) + trail)
            else: out.append(txt)
        return "".join(out)

def process(path, fr):
    s = open(path, encoding="utf-8").read(); x = X(s, fr); out = []; i = 0
    for m in re.finditer(r"(<script(?: type=\"module\")?>)(.*?)(</script>)", s, re.S):
        a, b = m.start(2), m.end(2)
        bi = s.find("<body")
        if 0 <= bi < a:
            cut = max(i, bi); out.append(s[i:cut] + x.html(s[cut:a], cut))
        else: out.append(s[i:a])
        code = s[a:b]
        if "I18N" in code and len(code) < 200: out.append(code)
        else:
            # lignes ignorées (contenus, bibliothèques) : on les recopie telles quelles
            pieces = []; p = a
            for lm in re.finditer(r"^(%s).*$" % "|".join(re.escape(z) for z in SKIP_LINES), code, re.M):
                st = a + lm.start(); en = a + lm.end()
                pieces.append(x.js(p, st)[0]); pieces.append(s[st:en]); p = en
            pieces.append(x.js(p, b)[0]); out.append("".join(pieces))
        i = b
    out.append(s[i:])
    open(path, "w", encoding="utf-8").write("".join(out))
    return x

def main():
    fr = load_fr(); tot = 0
    for p in sys.argv[1:]:
        x = process(p, fr); tot += x.count; print(os.path.basename(p), ":", x.count, "textes,", len(x.new), "nouvelles clés")
    save_fr(fr); print("fr.js :", len(fr), "clés")

if __name__ == "__main__": main()
