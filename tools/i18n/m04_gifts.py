# Module 4 : message des pétales offerts par les amies, en phrase entière.
KEYS = {"gifts.petals": "{who} {many, plural, one {t'a} other {t'ont}} laissé {n, plural, one {# pétale} other {# pétales}} ✿"}
REPL = [
  ('jdToast(names.slice(0,2).join(_t("jdLoadGifts.et_2738"))+(names.length>2?_t("jdLoadGifts.et_d_autres_c638"):"")+" t\'"+(got>1?"ont":"a")+_t("jdLoadGifts.laisse_a8aa")+got+_t("jdLoadGifts.petale_4ca6")+(got>1?"s":"")+" ✿");',
   'jdToast(_t("gifts.petals",{who:names.slice(0,2).join(_t("jdLoadGifts.et_2738"))+(names.length>2?_t("jdLoadGifts.et_d_autres_c638"):""),many:got>1?2:1,n:got}));', "both"),
]
