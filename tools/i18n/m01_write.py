# Module 1 : écran Écrire, en-tête, barre d'onglets, humeurs, question du jour, choix de la langue.
# Chaque remplacement : (ancien, nouveau, où) avec où = "both" | "app" (www/index.html) | "web" (nikki.html).

PROMPTS = [
  "Qu'est-ce qui t'a fait sourire aujourd'hui ?","Trois petites choses pour lesquelles tu es reconnaissant·e ?",
  "Quel moment de la journée aimerais-tu revivre ?","Qu'as-tu appris de nouveau aujourd'hui ?",
  "Qu'est-ce qui t'a donné de l'énergie ? Et qu'est-ce qui t'en a pris ?","Décris ton repas préféré de la journée.",
  "À qui as-tu pensé aujourd'hui, et pourquoi ?","Qu'est-ce qui te trotte dans la tête en ce moment ?",
  "Si ta journée était une couleur, laquelle serait-elle ?","Une petite victoire d'aujourd'hui, même minuscule ?",
  "Qu'est-ce que tu aimerais dire à toi dans un an ?","Quelle chanson irait bien avec ta journée ?",
  "Qu'est-ce qui t'a surpris·e aujourd'hui ?","Qu'aimerais-tu faire demain pour te faire plaisir ?",
  "Un endroit où tu aimerais être là, tout de suite ?","Qu'est-ce qui t'a rendu·e fier·e de toi récemment ?",
  "Quelle conversation t'a marqué·e aujourd'hui ?","Qu'est-ce qui te stresse, et que pourrais-tu lâcher ?",
  "Décris un petit détail joli que tu as remarqué.","Qu'est-ce que tu ferais d'une journée entièrement libre ?",
  "Quel conseil donnerais-tu à ton toi d'hier ?","Qu'est-ce qui te réconforte quand ça ne va pas ?",
  "Quel rêve as-tu en ce moment, petit ou grand ?","Écris une lettre de trois lignes à quelqu'un que tu aimes."]
STARTERS = ["Aujourd'hui, j'ai souri quand…","Je suis reconnaissant·e pour…","Ce qui m'a pesé, c'est…","Demain, j'aimerais…"]

KEYS = {
  "app.name": "Mon petit journal",
  "app.tagline": "にっき · rien qu'à toi",
  "app.tagline_web": "にっき · un moment rien qu'à toi",
  "app.flame": "Ma flamme",
  "app.settings": "Réglages",
  "app.lock": "Verrouiller le journal",
  "app.lock_short": "Verrouiller",
  "tab.sections": "Sections",
  "tab.write": "Écrire",
  "tab.deco": "Déco",
  "tab.gacha": "Gacha",
  "tab.journal": "Journal",
  "tab.todo": "To-do",
  "tab.friends": "Amis",
  "view.deco": "Coin déco",
  "view.rewards": "Récompenses",
  "view.journal": "Mon journal",
  "write.back": "← Revenir à aujourd'hui",
  "write.mood_q": "Comment tu te sens ? · きもち",
  "write.prompt_label": "Question du jour · きょうのしつもん",
  "write.prompt_other": "Une autre ↻",
  "write.starters_label": "Débuts de phrase",
  "write.doux": "☾ Pas d'énergie ce soir ? Écris en 30 secondes",
  "write.page_label": "Ma page",
  "write.placeholder": "Écris ici… même trois lignes, c'est déjà très bien ✿",
  "write.goal.unit": "/ 50 mots",
  "write.goal.idle": "Petit objectif du jour",
  "write.goal.left": "Encore {n, plural, one {# mot} other {# mots}}",
  "write.goal.done": "Objectif atteint ✿ すごい！",
  "write.saved_initial": "Enregistré sur ton téléphone",
  "write.private": "Tes pages restent privées",
  "write.saved": "Enregistré sur ton téléphone ✓",
  "write.quota_full": "Ton carnet en ligne est plein : tes pages restent sur cet appareil.",
  "write.date_today": "Aujourd'hui, {date}",
  "write.say.hello": "Coucou ! Comment s'est passée ta journée ?",
  "write.say.morning": "おはよう ! Une petite page pour bien commencer la journée ?",
  "write.say.afternoon": "Coucou ! Une pause pour écrire un peu ?",
  "write.say.evening": "こんばんは ~ Comment s'est passée ta journée ?",
  "write.say.start": "C'est parti ! がんばって ✿",
  "write.say.keep": "Continue comme ça, j'adore te lire ~",
  "write.say.goal": "Objectif atteint ! Tu peux continuer autant que tu veux ♡",
  "write.say.old_page": "Tu relis une ancienne page. Tu peux la compléter si tu veux ~",
  "write.say.blank_page": "Cette page est restée blanche. Tu te souviens de ce jour-là ?",
  "write.tags.tip": "Astuce : écris #école, #amis ou #famille pour retrouver tes pages facilement.",
  "write.tags.label": "Tags :",
  "write.tags.see": "Voir toutes les pages {tag}",
  "mood.super": "Super",
  "mood.bien": "Bien",
  "mood.bof": "Bof",
  "mood.triste": "Triste",
  "mood.fatigue": "Fatigué·e",
  "mood.reply.super": "Yay ! Raconte-moi ce qui t'a rendu·e si heureux·se ✿",
  "mood.reply.bien": "Une bonne journée, ça se garde précieusement ~",
  "mood.reply.bof": "Les journées « bof » méritent aussi leur page. Écris ce qui te passe par la tête.",
  "mood.reply.triste": "Je suis là. Pose les mots ici, ça allège souvent un peu le cœur.",
  "mood.reply.fatigue": "Pas besoin d'écrire beaucoup ce soir. Trois lignes, et au lit ! おやすみ",
  "settings.tab.lang": "Langue · Language",
  "lang.title": "Langue",
  "lang.note": "Choisis la langue de l'appli. « Automatique » suit la langue de ton téléphone. Ton journal, lui, reste écrit comme tu l'as écrit.",
  "lang.auto": "Automatique",
  "lang.beta": "bêta",
  "lang.beta_note": "Les langues marquées « bêta » sont en cours de traduction : certains écrans restent en français pour l'instant.",
}
for i, p in enumerate(PROMPTS): KEYS["prompt.%d" % i] = p
for i, p in enumerate(STARTERS): KEYS["write.starter.%d" % i] = p

def jsarr(a): return ",".join('"%s"' % x for x in a)

REPL = [
  # ---- HTML commun ----
  ('<button class="back" id="backBtn" hidden>← Revenir à aujourd\'hui</button>', '<button class="back" id="backBtn" hidden data-i18n="write.back">← Revenir à aujourd\'hui</button>', "both"),
  ('<div class="bubble" id="bubble" role="status">Coucou', '<div class="bubble" id="bubble" role="status" data-i18n="write.say.hello">Coucou', "both"),
  ('<p class="label">Comment tu te sens ? · きもち</p>', '<p class="label" data-i18n="write.mood_q">Comment tu te sens ? · きもち</p>', "both"),
  ('<span class="tagl">Question du jour · きょうのしつもん</span>', '<span class="tagl" data-i18n="write.prompt_label">Question du jour · きょうのしつもん</span>', "both"),
  ('<button class="ghost" id="newPrompt">Une autre ↻</button>', '<button class="ghost" id="newPrompt" data-i18n="write.prompt_other">Une autre ↻</button>', "both"),
  ('id="starters" aria-label="Débuts de phrase"', 'id="starters" aria-label="Débuts de phrase" data-i18n-attr="aria-label:write.starters_label"', "both"),
  ('id="douxBtn" type="button">☾', 'id="douxBtn" type="button" data-i18n="write.doux">☾', "both"),
  ('<label for="text" class="label" style="margin:0">Ma page</label>', '<label for="text" class="label" style="margin:0" data-i18n="write.page_label">Ma page</label>', "both"),
  ('<textarea id="text" placeholder="', '<textarea id="text" data-i18n-attr="placeholder:write.placeholder" placeholder="', "both"),
  ('<span class="count" id="words">0</span> / 50 mots<br>', '<span class="count" id="words">0</span> <span data-i18n="write.goal.unit">/ 50 mots</span><br>', "both"),
  # ---- HTML appli ----
  ('<h1 class="atitle">Mon petit journal<small>にっき · rien qu\'à toi</small></h1>', '<h1 class="atitle"><span data-i18n="app.name">Mon petit journal</span><small data-i18n="app.tagline">にっき · rien qu\'à toi</small></h1>', "app"),
  ('id="flameBtn" aria-label="Ma flamme"', 'id="flameBtn" aria-label="Ma flamme" data-i18n-attr="aria-label:app.flame"', "app"),
  ('id="setBtn" aria-label="Réglages"', 'id="setBtn" aria-label="Réglages" data-i18n-attr="aria-label:app.settings"', "app"),
  ('id="lockBtn" hidden aria-label="Verrouiller le journal"', 'id="lockBtn" hidden aria-label="Verrouiller le journal" data-i18n-attr="aria-label:app.lock"', "app"),
  ('<span class="status" id="status">Enregistré sur ton téléphone</span>', '<span class="status" id="status" data-i18n="write.saved_initial">Enregistré sur ton téléphone</span>', "app"),
  ('id="v-ecrire" aria-label="Écrire"', 'id="v-ecrire" aria-label="Écrire" data-i18n-attr="aria-label:tab.write"', "app"),
  ('id="v-deco" aria-label="Coin déco"', 'id="v-deco" aria-label="Coin déco" data-i18n-attr="aria-label:view.deco"', "app"),
  ('id="v-recomp" aria-label="Récompenses"', 'id="v-recomp" aria-label="Récompenses" data-i18n-attr="aria-label:view.rewards"', "app"),
  ('id="v-journal" aria-label="Mon journal"', 'id="v-journal" aria-label="Mon journal" data-i18n-attr="aria-label:view.journal"', "app"),
  ('id="v-todo" aria-label="To-do"', 'id="v-todo" aria-label="To-do" data-i18n-attr="aria-label:tab.todo"', "app"),
  ('id="v-amis" aria-label="Amis"', 'id="v-amis" aria-label="Amis" data-i18n-attr="aria-label:tab.friends"', "app"),
  ('role="tablist" aria-label="Sections"', 'role="tablist" aria-label="Sections" data-i18n-attr="aria-label:tab.sections"', "app"),
  ('<span>Écrire</span>', '<span data-i18n="tab.write">Écrire</span>', "app"),
  ('<span>Déco</span>', '<span data-i18n="tab.deco">Déco</span>', "app"),
  ('<span>Gacha</span>', '<span data-i18n="tab.gacha">Gacha</span>', "app"),
  ('<span>Journal</span>', '<span data-i18n="tab.journal">Journal</span>', "app"),
  ('<span>To-do</span>', '<span data-i18n="tab.todo">To-do</span>', "app"),
  ('<span>Amis</span>', '<span data-i18n="tab.friends">Amis</span>', "app"),
  # ---- HTML web ----
  ('<h1>Mon petit journal<small>にっき · un moment rien qu\'à toi</small></h1>', '<h1><span data-i18n="app.name">Mon petit journal</span><small data-i18n="app.tagline_web">にっき · un moment rien qu\'à toi</small></h1>', "web"),
  ('id="flameBtn" data-long="1" aria-label="Ma flamme"', 'id="flameBtn" data-long="1" aria-label="Ma flamme" data-i18n-attr="aria-label:app.flame"', "web"),
  ('</svg>Réglages</button>', '</svg><span data-i18n="app.settings">Réglages</span></button>', "web"),
  ('</svg>Verrouiller</button>', '</svg><span data-i18n="app.lock_short">Verrouiller</span></button>', "web"),
  ('<span class="status" id="status">Tes pages restent privées</span>', '<span class="status" id="status" data-i18n="write.private">Tes pages restent privées</span>', "web"),
  # ---- code : humeurs ----
  ('{id:"super",label:"Super",', '{id:"super",label:_t("mood.super"),', "both"),
  ('{id:"bien",label:"Bien",', '{id:"bien",label:_t("mood.bien"),', "both"),
  ('{id:"bof",label:"Bof",', '{id:"bof",label:_t("mood.bof"),', "both"),
  ('{id:"triste",label:"Triste",', '{id:"triste",label:_t("mood.triste"),', "both"),
  ('{id:"fatigue",label:"Fatigué·e",', '{id:"fatigue",label:_t("mood.fatigue"),', "both"),
  ('  super:"Yay ! Raconte-moi ce qui t\'a rendu·e si heureux·se ✿",', '  super:_t("mood.reply.super"),', "both"),
  ('  bien:"Une bonne journée, ça se garde précieusement ~",', '  bien:_t("mood.reply.bien"),', "both"),
  ('  bof:"Les journées « bof » méritent aussi leur page. Écris ce qui te passe par la tête.",', '  bof:_t("mood.reply.bof"),', "both"),
  ('  triste:"Je suis là. Pose les mots ici, ça allège souvent un peu le cœur.",', '  triste:_t("mood.reply.triste"),', "both"),
  ('  fatigue:"Pas besoin d\'écrire beaucoup ce soir. Trois lignes, et au lit ! おやすみ"\n', '  fatigue:_t("mood.reply.fatigue")\n', "both"),
  # ---- code : questions du jour et débuts de phrase ----
  ("@PROMPTS@", 'const PROMPTS=Array.from({length:%d},(_,i)=>_t("prompt."+i));' % len(PROMPTS), "both"),
  ('const STARTERS=[%s];' % jsarr(STARTERS), 'const STARTERS=Array.from({length:%d},(_,i)=>_t("write.starter."+i));' % len(STARTERS), "both"),
  # ---- code : écran Écrire ----
  ('function savedLabel(){return "Enregistré sur ton téléphone ✓"}', 'function savedLabel(){return _t("write.saved")}', "app"),
  ('setStatus("Ton carnet en ligne est plein : tes pages restent sur cet appareil.")', 'setStatus(_t("write.quota_full"))', "both"),
  ('const fr=d.toLocaleDateString("fr-FR",{weekday:"long",day:"numeric",month:"long"});\n  const t=$("dateTitle");t.textContent=(isToday?"Aujourd\'hui, ":"")+fr;',
   'const fr=_date(d,{weekday:"long",day:"numeric",month:"long"});\n  const t=$("dateTitle");t.textContent=isToday?_t("write.date_today",{date:fr}):fr;', "both"),
  ('say(written(current)?"Tu relis une ancienne page. Tu peux la compléter si tu veux ~":"Cette page est restée blanche. Tu te souviens de ce jour-là ?")',
   'say(written(current)?_t("write.say.old_page"):_t("write.say.blank_page"))', "both"),
  ('$("goalTxt").textContent=w>=GOAL?"Objectif atteint ✿ すごい！":w>0?"Encore "+(GOAL-w)+" mots":"Petit objectif du jour";',
   '$("goalTxt").textContent=w>=GOAL?_t("write.goal.done"):w>0?_t("write.goal.left",{n:GOAL-w}):_t("write.goal.idle");', "both"),
  ('if(w===0) say("Coucou ! Comment s\'est passée ta journée ?");', 'if(w===0) say(_t("write.say.hello"));', "both"),
  ('else if(w<15) say("C\'est parti ! がんばって ✿");', 'else if(w<15) say(_t("write.say.start"));', "both"),
  ('else if(w<GOAL) say("Continue comme ça, j\'adore te lire ~");', 'else if(w<GOAL) say(_t("write.say.keep"));', "both"),
  ('else say("Objectif atteint ! Tu peux continuer autant que tu veux ♡");', 'else say(_t("write.say.goal"));', "both"),
  ('box.textContent="Astuce : écris #école, #amis ou #famille pour retrouver tes pages facilement.";', 'box.textContent=_t("write.tags.tip");', "both"),
  ('l.textContent="Tags :";', 'l.textContent=_t("write.tags.label");', "both"),
  ('b.title="Voir toutes les pages "+t;', 'b.title=_t("write.tags.see",{tag:t});', "both"),
  ('say(h<11?"おはよう ! Une petite page pour bien commencer la journée ?":h<18?"Coucou ! Une pause pour écrire un peu ?":"こんばんは ~ Comment s\'est passée ta journée ?")',
   'say(h<11?_t("write.say.morning"):h<18?_t("write.say.afternoon"):_t("write.say.evening"))', "both"),
  # ---- réglages : onglet Langue ----
  ('["save","Sauvegarde"],["code","Code secret"]]', '["save","Sauvegarde"],["code","Code secret"],["lang",_t("settings.tab.lang")]]', "app"),
  ('["anniv","Anniversaire"],["code","Code secret"]]', '["anniv","Anniversaire"],["code","Code secret"],["lang",_t("settings.tab.lang")]]', "web"),
  ('  if(settingsTab==="sons")renderSoundTab(body);', '  if(settingsTab==="sons")renderSoundTab(body);\n  if(settingsTab==="lang")renderLangTab(body);', "both"),
  ('function renderSettings(msg){', '''function renderLangTab(body){
  const s=sec(_t("lang.title"),"ことば"),n=document.createElement("p");n.className="note";n.textContent=_t("lang.note");
  const g=document.createElement("div");g.className="seg";g.style.flexWrap="wrap";
  [["auto",_t("lang.auto")]].concat(Object.keys(I18N.langs).map(c=>[c,I18N.langs[c].name+(I18N.langs[c].ready?"":" · "+_t("lang.beta"))])).forEach(([c,l])=>{
    const b=document.createElement("button");b.type="button";b.textContent=l;b.setAttribute("aria-pressed",String(I18N.pref===c));b.onclick=()=>{if(I18N.pref!==c)I18N.set(c)};g.append(b)});
  s.append(n,g);if(Object.keys(I18N.langs).some(c=>!I18N.langs[c].ready)){const nb=document.createElement("p");nb.className="note";nb.textContent=_t("lang.beta_note");s.append(nb)}
  body.append(s)}
function renderSettings(msg){''', "both"),
]

def prompts_block(src):
    # le tableau PROMPTS d'origine, retrouvé tel quel dans le fichier
    i = src.find("const PROMPTS=[")
    if i < 0: return None
    j = src.index("];", i) + 2
    blk = src[i:j]
    found = __import__("re").findall(r'"((?:[^"\\]|\\.)*)"', blk)
    assert found == PROMPTS, "les questions du jour ont changé : mettre à jour PROMPTS dans le spec"
    return blk
