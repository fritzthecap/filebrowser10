A platform-independent Java/Swing file explorer with lots of tools like text-editor, hex-editor, xml-editor, mail-client, ftp-client, command-terminal, diff-views etc.
Pressing F7 on any focused GUI-component lets you configure colors, border, font and even text of that focused component.

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

Author: Fritz Ritzberger, Vienna
1999 - 2024

[current development issues: TODO.md](TODO.md)
