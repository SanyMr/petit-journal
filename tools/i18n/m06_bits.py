KEYS = {"garden.at": " à ", "settings.petals": "pétales", "backup.restore_label": "Restaurer une sauvegarde (fichier .json) :"}
REPL = [
  ('+JD_NEXT[st]+" à "+nx+', '+JD_NEXT[st]+_t("garden.at")+nx+', "both"),
  ('<b>${petalBal()}</b> pétales</span>', '<b>${petalBal()}</b> ${_t("settings.petals")}</span>', "both"),
]
REPL.append(('lab.textContent="Restaurer une sauvegarde (fichier .json) :";', 'lab.textContent=_t("backup.restore_label");', "app"))
