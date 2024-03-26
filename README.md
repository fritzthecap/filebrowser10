Author: Fritz Ritzberger, Vienna
1999 - 2024

A platform-independent Java/Swing file explorer.

This project and its source is absolutely free.
It is licensed under the GNU LIBRARY GENERAL PUBLIC LICENSE V2.
I accept no responsibility for incorrect use.

The source code has been started in 1999 and is nearly completely backward-compatible to Java 1.4.
I dissociate myself from this source, I was inexperienced in OO when I wrote it.
Nevertheless, this tool has served me well for 25 years on all platforms.

----

This is not a Maven project.
After you have configured your ANT installation in build.sh, compile the project:

	cd filebrowser10
	build.sh

The newly compiled application will be in FileBrowser10.jar.
If you want all .java sources in the JAR, remove the excludes on lines 72 and 77 in build.xml.

You can optionally configure your JAVA installation in run.sh, then run the application:

	run.sh
	
The application will create a (hidden) .friware directory in your user HOME where it persists runtime properties.
To reset the application to its defaults (e.g. it appears on a non-existent screen),
terminate the app, remove that directory and restart the app.

Main application class is in META-INF/MANIFEST.MF:

	fri.gui.swing.filebrowser.FileBrowser

Pressing F7 on any focused GUI component lets you configure colors, borders, fonts etc. of that focused component.

----

Eclipse Integration:

- Package Explorer, context menu "Import"
- Choose "General" - "Projects from Folder or Archive"
- Select the filebrowser10 directory
- Click "Finish"
- Configure Build Path of the new Eclipse project
-- on "Source" tab, select "Included" and click "Edit", add "fri" directory as inclusion
-- on "Libraries" tab, add all *.jar from "lib" directory to "Classpath"
-- on "Module Dependencies" tab, remove javax.xml module
- Click "Apply and Close"

