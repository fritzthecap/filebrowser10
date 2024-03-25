Author: Fritz Ritzberger, Vienna
1999 - 2024

A platform-independent Java/Swing file explorer.

This project is absolutely free.
It is licensed under the GNU LIBRARY GENERAL PUBLIC LICENSE V2, see License.html.
I accept no responsibility for incorrect use.

Compile the project with ANT (this is not a Maven project):

	cd filebrowser10
	java -cp ../ant/ant.jar org.apache.tools.ant.Main

All open source libraries are present as .class files in various sub-directories.
The newly compiled application will be in filebrowser10 root directory.
Just modify the source and then run ANT, it will build a new FileBrowser10.jar.
You can run it then via

	java -jar FileBrowser10.jar
	
The application will create a (hidden) .friware directory in your user HOME where it persists runtime properties.
Main class is 

	fri.gui.swing.filebrowser.FileBrowser.

The source code has been started in 1999 and is nearly completely backward-compatible to Java 1.4.
