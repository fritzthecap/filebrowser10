TODO
====

Rudimental Version handling
---------------------------

FileBrowser Version is stored at one single place: The MANIFEST.MF
Version in showed on commandline and as frame title, also within TreeEdit (About).
 
implemented with version 10.1.0
 
Drag and drop improvements
--------------------------
 
Drag'n drop should be improved allowing drag and drop single and multi file selections between FileBrowser windows.
Communication with other system applications should work im best case.
Move/Copy option should be choosen with using [ctrl] key (not with a little drag down menu). Default is move.

- fri_2025-04-13: Mit dem Entfernen des drop-down menu bei drag&drop bin ich nicht einverstanden! Dieses Verhalten laesst sich ohnehin im oberen toolbar ueber die checkbox "Drop Menu" konfigurieren, einfach ausschalten und das drop-down menu wird nicht mehr erscheinen!
- Multi-file selections: das sollte ohnehin programmiert sein, ist ein bug wenn es nicht funktioniert. Achtung, in Swing wird die selection geloescht und neu gesetzt, wenn man in die selection selbst hineinklickt, daher drag&drop immer mit einem Klick ausserhalb der selection starten!
