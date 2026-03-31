## Symlinks! 🎉

**Version 10.0.0** führt Symlinks ein. Mit Symlinks kannst du denselben Tracker, Graphen, dieselbe Funktion oder sogar Gruppe in mehreren Gruppen verwenden. Es ist kein Duplikat und keine Kopie, sondern ein Verweis auf dieselbe Komponente. Alle Änderungen, die du an einem vornimmst, werden in allen anderen übernommen. Um mit Symlinks zu beginnen, tippe einfach auf die +-Schaltfläche oben rechts in einer beliebigen Gruppe und wähle „Symlink".

## Neue Datenpunkt-Aktionen! 📝

Du kannst jetzt Datenpunkte direkt über den Verlaufsbildschirm zu einem Tracker hinzufügen (der sich öffnet, wenn du auf die Tracker-Karte tippst). Du findest die neue schwebende Aktionstaste unten rechts auf dem Bildschirm.

![Datenpunkt-Aktionen](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/changelogs/10.0.0/data_point_actions.jpg)

Es gibt auch einen neuen Mehrfachauswahlmodus im Verlaufsbildschirm, mit dem du mehrere Datenpunkte gleichzeitig verschieben, kopieren oder löschen kannst. Du kannst sogar Datenpunkte von einer Funktion in einen Tracker kopieren! Aktiviere den Mehrfachauswahlmodus durch langes Drücken auf einen Datenpunkt, wähle die gewünschten Datenpunkte aus und suche nach den neuen Aktionstasten unten rechts auf dem Bildschirm.

## Gesperrte Erfassung! 🔒

Eine neue Funktion im Dialog zum Hinzufügen von Datenpunkten ermöglicht es dir, mehrere Datenpunkte für einen Tracker hintereinander hinzuzufügen. Achte auf das neue Schloss-Symbol am Ende der Eingabefelder:

![Gesperrte Erfassung](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/changelogs/10.0.0/locked_tracking.jpg)

Wenn ein Schloss aktiviert ist, bleibt der Dialog nach dem Hinzufügen eines Datenpunkts geöffnet, und die gesperrten Felder werden automatisch mit demselben Wert wie beim vorherigen Datenpunkt ausgefüllt.

## Fehlerbehebungen und Verbesserungen

- Behoben: Graphen blieben in endloser Ladeschleife hängen (sorry dafür)
- Behoben: Bearbeiten einer Erinnerung nach dem Aktualisieren öffnete den Dialog nicht
- Behoben: fehlende Übersetzungen für Erinnerungen
- Behoben: eindeutige Work-Manager-Anfragen pro Erinnerung, um doppelte Erinnerungen zu vermeiden
- Kopierte Erinnerungen werden jetzt sofort geplant
- Behoben: kopierte Erinnerungen erschienen am falschen Ort
- Behoben: Notizen wurden nicht unter Graphen angezeigt
- Behoben: Standardabweichung gibt NaN bei Gleitkomma-Präzisionsfehlern zurück (in Funktionsknoten)
- Behoben: Anzeigeindizes wurden bei ID-Konflikten nicht korrekt aktualisiert
- Lua-Skript-Info-Button im Knotenauswahldialog mit der Entwickleranleitung verknüpft
- Verbesserte Zuverlässigkeit der Tracking-Widgets nach App-Updates
- Aktualisierung der Bibliotheksabhängigkeiten für verbesserte Leistung und Stabilität
- Jetzt auf Android API-Level 36 ausgerichtet
            