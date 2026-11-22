# Module 3 : « il y a … » avec des phrases entières (l'ordre des mots change selon la langue).
KEYS = {
  "ago.days": "il y a {n, plural, one {# jour} other {# jours}}",
  "ago.months": "il y a {n} mois",
  "ago.years": "il y a {n} ans",
}
REPL = [
  ('return _t("agoTxt.il_y_a_188a")+plural(d,"jour");', 'return _t("ago.days",{n:d});', "both"),
  ('m>=18?_t("agoTxt.il_y_a_188a")+Math.round(m/12)+" ans":_t("agoTxt.il_y_a_188a")+m+" mois"}', 'm>=18?_t("ago.years",{n:Math.round(m/12)}):_t("ago.months",{n:m})}', "both"),
]
