# Google-Play-Eintrag — Deutsch

## App-Name (max. 30 Zeichen)

```
Chessomnia
```

## Kurzbeschreibung (max. 80 Zeichen)

```
Schach zu zweit oder gegen das Gerät. Gratis, ohne Werbung, Konto und Tracking.
```

## Vollständige Beschreibung (max. 4000 Zeichen)

```
Chessomnia macht aus deinem Tablet oder Handy ein Schachbrett. Zwei Spieler, ein Gerät, einander gegenüber — genau wie an einem echten Brett. Und wenn niemand da ist, spielt das Gerät mit — vier Stärken, von kinderleicht bis Vereinsspieler.


KOSTENLOS. KEINE WERBUNG. KEIN KONTO. KEIN TRACKING.

Auf diese vier kommt es an, und keiner davon hat ein Sternchen.

Kostenlos heißt kostenlos — keine Testphase, nichts zum Freischalten, keine In-App-Käufe. Keine Werbung, keine Analyse. Kein Konto, keine Anmeldung, keine E-Mail-Adresse: installieren und spielen.

Und das mit dem Tracking ist kein Versprechen, sondern eine Eigenschaft. Die App hält keine Android-Berechtigung, die ihr irgendetwas erlaubt — nicht einmal Internetzugriff. Sie kann technisch gar nichts irgendwohin senden. Deine Partien verlassen das Gerät nie. Nachprüfbar: Chessomnia ist quelloffen.


FUNKTIONIERT VOLLSTÄNDIG OFFLINE

Es gibt nichts, womit sich die App verbinden könnte. Flugzeug, Keller, Zelt — kein Unterschied. Es existiert keine Serverseite, und der Gegner rechnet auf deinem Gerät.


EIN ERSATZ FÜRS BRETT

Es geht um die zwei Menschen am Tisch. Chessomnia übernimmt die Aufgaben, die ein Holzbrett nicht kann:

• Es kennt jede Regel, samt Rochade, en passant, Umwandlung, 50-Züge-Regel, dreifacher Wiederholung und totem Material.
• Es lässt keinen unerlaubten Zug durch und übersieht kein Matt.
• Es lässt die Figuren stehen, wo sie stehen, wenn das Gerät einschläft.


EIN GEGNER, WENN NIEMAND DA IST

Gegen das Gerät spielen statt gegen einen Menschen, in der Farbe deiner Wahl. Vier Stärken: die schwächste ist für Kinder von neun bis elf — sie hält ihre Stellung zusammen, übersieht aber das meiste, was du hängen lässt. Die stärkste ist ein Vereinsspieler, mit Absicht nicht so stark, wie sie sein könnte. Ein Gegner, den niemand schlagen kann, ist kein Feature.

Nimmst du gegen das Gerät einen Zug zurück, bist du wieder am Zug und nicht es.


LERNHILFE, KEIN LEHRER

Figur antippen — und jedes Feld, auf das sie ziehen darf, wird markiert. Rochade und en passant eigens hervorgehoben, denn genau die übersehen Anfänger. Abschaltbar, sobald du sie nicht mehr brauchst.

Was es nie geben wird: einen Vorschlag, welchen Zug du spielen sollst, oder einen Balken, der dir sagt, wer besser steht. Der Gegner zieht und sagt kein Wort über deine Stellung. Das herauszufinden ist das Spiel.


GEBAUT FÜR EIN GERÄT, DAS FLACH AUF DEM TISCH LIEGT

Am schönsten auf einem Tablet. Auf dem Handy genauso — das Brett wird kleiner, sonst ändert sich nichts.

Alles folgt aus dieser einen Idee:

• Uhr, Status und Knöpfe gibt es zweimal, je an einer Tischkante — niemand liest verkehrt herum.
• Die Figuren des Gegenübers werden gedreht gezeichnet — am Holzbrett erledigt das die Form der Figur.
• „Brett drehen" dreht es an Ort und Stelle, statt dass jemand das Gerät hochheben muss.
• Rückfragen und der Umwandlungsdialog drehen sich zu dem, der sie ausgelöst hat.
• Am Partieende verdeckt nichts das Brett. Nach einem Matt willst du sehen, warum — der markierte König und der hervorgehobene letzte Zug sind die Antwort.


EINE UHR, DIE INFORMIERT STATT ZU RICHTEN

Die Uhr zählt aufwärts: sie zeigt, wie lange jeder insgesamt nachgedacht hat. Sie läuft nie ab und beendet nie eine Partie — an einem Heimbrett ist das der Zweck einer Uhr. Wer sie nicht mag, schaltet sie ab.

Beim Zurücknehmen kommt auch die Bedenkzeit zurück — so, wie ein Takeback unter Freunden tatsächlich abläuft.


NACHPRÜFBAR KORREKT

Der Regelkern ist gegen 12,4 Millionen Stellungen der Standard-Perft-Tests abgesichert und über 4.576 Stellungen mit einer fremden Implementierung abgeglichen — dicht an Matt und Patt.

Sollte doch etwas falsch sein: „Fehler melden" erzeugt den Text, mit dem sich die Stellung nachstellen lässt — inklusive aller dort erlaubten Züge.


QUELLOFFEN

Apache-2.0. Nachlesen, selbst bauen, weiterentwickeln:
https://github.com/alramlechner/Chessomnia
```

## Hinweis zur Pflege

Die deutsche Fassung ist eine eigenständige Übersetzung, keine wörtliche. Wird
`listing-en.md` inhaltlich geändert, muss diese Datei mitgezogen werden — der
Play-Eintrag zeigt beide Sprachen nebeneinander, und ein Auseinanderlaufen fällt
Nutzern auf.

⚠️ **Play zählt Zeichen, und die Grenzen sind hart:** 30 für den Namen, 80 für
die Kurz-, 4000 für die Vollbeschreibung. Die deutsche Vollbeschreibung ist die
knappere der beiden — deutsche Sätze sind länger. Vor dem Einfügen in die
Console nachzählen:

```bash
python3 - <<'EOF'
import re, pathlib
for f in ("store/listing-en.md", "store/listing-de.md", "store/listing-es.md"):
    b = re.findall(r"```\n(.*?)\n```", pathlib.Path(f).read_text(), re.S)
    print(f, "kurz", len(b[1]), "voll", len(b[2]))
EOF
```
