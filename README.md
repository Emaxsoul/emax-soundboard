# Emax Soundboard

Prima build Android nativa (MVP) per smartphone in verticale.

## Funzioni MVP
- 8 banchi da 16 pad.
- Importazione audio da Android: tocca a lungo un pad per scegliere un file audio.
- Riproduzione simultanea di pad differenti.
- Stop generale.
- Controllo da tastiera Bluetooth HID: freccia destra/sinistra per cambiare banco; Invio/centrale/spazio per riprodurre il pad selezionato; Esc/Back per stop generale.
- Schermo verticale.

## APK automatico
Ogni push su `main` avvia GitHub Actions. Il file APK viene pubblicato come artifact della build:
Actions → Build Android APK → Artifacts → EmaxSoundboard-debug.

## Stato
Prototipo iniziale. Non è ancora collaudato con lo SKY Turner ST-1 né adatto a un concerto senza test prolungati. Il supporto HID dipende dalla modalità impostata sul pedale e dai keycode che esso invia.
