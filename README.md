# Java File World

![Static Badge](https://img.shields.io/badge/Topic-File--Management-blue)
![Static Badge](https://img.shields.io/badge/Type-Desktop_App-blue)
![Static Badge](https://img.shields.io/badge/Language-Java_8-darkgreen)
![Static Badge](https://img.shields.io/badge/UI_System-Swing-darkgreen)
![Static Badge](https://img.shields.io/badge/Application_JAR-3.7_MB-darkgreen)

![Static Badge](https://img.shields.io/github/license/fritzthecap/filebrowser10?color=pink)
![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/fritzthecap/filebrowser10?color=pink)
![GitHub repo size](https://img.shields.io/github/repo-size/fritzthecap/filebrowser10?color=pink)
![GitHub last commit](https://img.shields.io/github/last-commit/fritzthecap/filebrowser10?color=pink)

A platform-independent Java/Swing file explorer with lots of tools like text-editor, hex-editor, xml-editor, mail-client, ftp-client, command-terminal, diff-views and many more. Pressing F7 on any focused GUI-component lets you configure its colors, border, font and even text.

**Hint:** To drag multiple files in a Swing tree, select the files (Ctrl-click, Shift-click), then press left mouse button somewhere **outside the selection** and start to drag. Release the mouse over the target folder, a context menu will appear where you can choose Copy, Move or Cancel.  

<img width="410" height="545" alt="Java-File-World_10 1 2" src="https://github.com/user-attachments/assets/487f3370-ac19-4f14-8d19-d11893c8c2d5" />

----

This project and its source is absolutely free.
I accept no responsibility for incorrect use.
Implementation started in 1999 and is widely backward-compatible to Java 1.4.

Personally I dissociate myself from this source code. 
I was inexperienced in OO when I started it, moreover it is unreadable and ugly:-)
Nevertheless, this tool has served me well for 25 years on all platforms!

----

This is **not a Maven project**.
After you have configured your ANT installation in build.sh, compile the project:

	cd filebrowser10
	build.sh

The newly compiled application will be in *FileBrowser10.jar*.
If you want all .java sources in the JAR, remove the excludes on lines 79 and 84 in build.xml.

You can optionally configure your JAVA installation in run.sh, then run the application:

	run.sh
	
The application will create a (hidden) *.friware* directory in your user HOME where it persists runtime properties.
To reset the application to its defaults (e.g. it appears on a non-existent screen),
terminate the app, remove that directory and restart the app.

Main application class (and latest version) is in *META-INF/MANIFEST.MF*:

	fri.gui.swing.filebrowser.FileBrowser


----

Eclipse Integration:

- Package Explorer, context menu "Import"
- Choose "General" - "Projects from Folder or Archive"
- Select the filebrowser10 directory
- Click "Finish"
- Configure Build Path of the new Eclipse project
		- on "Source" tab, select "Included" and click "Edit", add "fri", "META-INF" directory as inclusion
		- use "build" or "out" as output directory (for eclipse)
		- on "Libraries" tab, add all *.jar files in "lib" directory to "Classpath"
- Click "Apply and Close"

----

Author: Fritz Ritzberger 1999 - 2025

[current development issues: TODO.md](TODO.md)
