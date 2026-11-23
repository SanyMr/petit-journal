/* Mon petit journal : système de langues 言語
   - Les textes vivent dans i18n/<langue>.js sous forme de clés ("write.goal.left").
   - Le français est la langue source et le repli : une clé absente d'une langue s'affiche en français.
   - Messages : {nom} pour une valeur, {n, plural, one {# mot} other {# mots}} pour les pluriels
     (règles de pluriel de chaque langue via Intl.PluralRules).
   - HTML statique : data-i18n="clé" (texte) et data-i18n-attr="aria-label:clé;placeholder:clé".
   - Ajouter une langue = ajouter i18n/<code>.js et une ligne dans I18N.langs. */
(function(){
  var LANGS={
    fr:{name:"Français",locale:"fr-FR",ready:true},
    en:{name:"English",locale:"en-GB",ready:true,content:true},
    es:{name:"Español",locale:"es-ES",ready:true,content:true},
    pt:{name:"Português",locale:"pt-BR",ready:true,content:true},
    de:{name:"Deutsch",locale:"de-DE",ready:true,content:true},
    ar:{name:"العربية",locale:"ar-u-nu-latn",ready:true,content:true,dir:"rtl"}
  };
  var K="nikki.lang",D={},pref="auto",lang="fr";
  try{pref=localStorage.getItem(K)||"auto"}catch(e){}
  function pick(){
    if(pref!=="auto"&&LANGS[pref])return pref;
    var nav=[].concat(navigator.languages||[],navigator.language||[]);
    for(var i=0;i<nav.length;i++){var c=String(nav[i]).slice(0,2).toLowerCase();if(c==="fr")return"fr";if(LANGS[c]&&LANGS[c].ready)return c}
    return"fr"}
  lang=pick();
  // sens d'écriture (arabe : de droite à gauche), posé tout de suite pour éviter un saut à l'affichage
  try{document.documentElement.dir=LANGS[lang].dir||"ltr";document.documentElement.lang=lang}catch(e){}
  var PR={};
  function plural(n){var l=LANGS[lang].locale;try{return(PR[l]||(PR[l]=new Intl.PluralRules(l))).select(n)}catch(e){return n===1?"one":"other"}}
  // remplace {x} et {n, plural, one {...} other {...}} (un niveau d'imbrication)
  function fmt(s,v){
    if(!v)return s;var out="",i=0;
    while(i<s.length){var c=s[i];
      if(c!=="{"){out+=c;i++;continue}
      var d=0,j=i;for(;j<s.length;j++){if(s[j]==="{")d++;else if(s[j]==="}"){d--;if(!d)break}}
      var body=s.slice(i+1,j),m=/^\s*(\w+)\s*,\s*plural\s*,(.*)$/.exec(body);
      if(m){var n=+v[m[1]],cat=plural(n),opts={},re=/\s*(=?\w+)\s*\{/g,r,rest=m[2];
        while((r=re.exec(rest))){var k=r[1],st=re.lastIndex,dd=1,e=st;for(;e<rest.length&&dd;e++){if(rest[e]==="{")dd++;else if(rest[e]==="}")dd--}opts[k]=rest.slice(st,e-1);re.lastIndex=e}
        var pick=opts["="+n]!=null?opts["="+n]:opts[cat]!=null?opts[cat]:opts.other||"";
        out+=fmt(pick.replace(/#/g,String(n)),v)}
      else{var key=body.trim();out+=v[key]!=null?v[key]:"{"+key+"}"}
      i=j+1}
    return out}
  function t(key,v){var s=(D[lang]&&D[lang][key]);if(s==null)s=D.fr&&D.fr[key];if(s==null){if(I18N.missing)I18N.missing[key]=1;return key}return fmt(s,v)}
  // traduit le HTML statique ; un texte déjà modifié par l'appli (différent du français d'origine) n'est pas touché
  function apply(root){
    root=root||document;if(root===document){document.documentElement.lang=lang;document.documentElement.dir=LANGS[lang].dir||"ltr"}
    if(lang==="fr")return;var fr=D.fr||{};
    var a=root.querySelectorAll("[data-i18n]");for(var i=0;i<a.length;i++){var k=a[i].getAttribute("data-i18n");if(fr[k]==null||a[i].textContent.trim()===fr[k].trim())a[i].textContent=t(k)}
    var b=root.querySelectorAll("[data-i18n-attr]");for(var j=0;j<b.length;j++){var parts=b[j].getAttribute("data-i18n-attr").split(";");
      for(var p=0;p<parts.length;p++){var kv=parts[p].split(":");if(kv.length!==2)continue;var at=kv[0].trim(),kk=kv[1].trim(),cur=b[j].getAttribute(at);
        if(fr[kk]==null||cur==null||cur===fr[kk])b[j].setAttribute(at,t(kk))}}}
  // packs de contenu (proverbes, questions…) : chargés seulement pour la langue choisie
  var P={};
  function pack(name){return P[lang]&&P[lang][name]}
  var I18N=window.I18N={
    pack:function(code,name,data){(P[code]=P[code]||{})[name]=data},
    // liste traduite si le pack a exactement le même nombre d'éléments (les index servent d'identifiants)
    list:function(name,arr){var p=pack(name);return Array.isArray(p)&&p.length===arr.length?p:arr},
    // tableau d'entrées identifiées par e[idx] : fn(entrée, traduction) pour chaque entrée traduite
    patch:function(name,arr,fn,idx){var p=pack(name);if(!p)return arr;idx=idx||0;return arr.map(function(e){var t=p[e[idx]];return t?fn(e,t):e})},
    langs:LANGS,missing:null,
    get lang(){return lang},get pref(){return pref},
    locale:function(){return LANGS[lang].locale},
    get dir(){return LANGS[lang].dir||"ltr"},
    add:function(code,dict){D[code]=Object.assign(D[code]||{},dict)},
    has:function(key){return!!((D[lang]&&D[lang][key]!=null)||(D.fr&&D.fr[key]!=null))},
    keys:function(code){return Object.keys(D[code]||{})},
    set:function(code){try{localStorage.setItem(K,code)}catch(e){}location.reload()},
    apply:apply,t:t,fmt:fmt
  };
  window._t=t;
  // droite à gauche : « 3 / 7 » s'afficherait « 7 / 3 » ; on isole ces fractions (LRI…PDI) et on retourne
  // les flèches (‹ › ← →) dans tout texte affiché. Chaque nœud n'est traité qu'une fois par valeur.
  if(LANGS[lang].dir==="rtl"&&window.MutationObserver){
    var FR=/(^|[^\d.,\u2066])(\d+(?:[.,]\d+)*) \/ (\d+(?:[.,]\d+)*)/g,AR=/[‹›←→]/g,SW={"‹":"›","›":"‹","←":"→","→":"←"},SKIP={TEXTAREA:1,INPUT:1,SCRIPT:1,STYLE:1},DONE=new WeakMap();
    var fix=function(n){if(n.nodeType===3){var v=n.nodeValue;if(!v||DONE.get(n)===v)return;var w=v;
        if(w.indexOf(" / ")>=0)w=w.replace(FR,"$1\u2066$2 / $3\u2069");
        if(AR.test(w)){AR.lastIndex=0;w=w.replace(AR,function(c){return SW[c]})}AR.lastIndex=0;
        DONE.set(n,w);if(w!==v)n.nodeValue=w;return}
      if(n.nodeType!==1||SKIP[n.nodeName]||n.isContentEditable)return;var c=n.firstChild;while(c){fix(c);c=c.nextSibling}};
    var start=function(){fix(document.body);new MutationObserver(function(ms){for(var i=0;i<ms.length;i++){var m=ms[i];if(m.type==="characterData")fix(m.target);else for(var j=0;j<m.addedNodes.length;j++)fix(m.addedNodes[j])}}).observe(document.body,{childList:true,subtree:true,characterData:true})};
    if(document.body)start();else document.addEventListener("DOMContentLoaded",start)}
  // dates et nombres dans la langue choisie
  window._date=function(d,o){return d.toLocaleDateString(LANGS[lang].locale,o)};
  window._num=function(n,o){return Number(n).toLocaleString(LANGS[lang].locale,o)};
  // charge la langue choisie avant le reste de l'appli (le français est déjà inclus)
  if(lang!=="fr"){var src=(document.currentScript&&document.currentScript.src||"").replace(/i18n\.js(\?.*)?$/,lang+".js");if(src){document.write('<script src="'+src+'"><\/script>');if(LANGS[lang].content)document.write('<script src="'+src.replace(/\.js$/,".content.js")+'"><\/script>')}}
})();
