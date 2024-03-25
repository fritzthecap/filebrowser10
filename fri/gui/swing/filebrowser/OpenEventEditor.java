package fri.gui.swing.filebrowser;

import javax.swing.*;

import java.awt.event.*;
import java.awt.*;
import java.util.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.IconUtil;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.textviewer.TextViewer;

/**
	Ziel: Editieren von Kommandos zum Oeffnen (Doppelklick) von Dateien.<BR>
	Verantwortlichkeiten: OK- und Cancel-Buttons behandeln, eine einzige Instanz
		zulasen.<BR>
	Verhalten: Da es nur einen Editor fuer alle Instanzen geben soll,
		wird der Editor ueber die construct() Factory-Methode allokiert (Singleton).
*/

public class OpenEventEditor extends JFrame implements
	ActionListener
{
	private static OpenEventEditor singleton = null;
	private JButton ok, can, help;
	//private OpenEventTable table;
	private OpenCommandList commands;
	private OpenCommandList oldcommands;
	private OpenEventTable table;
	private static final String helptext = 
		"Define your open event for selected file(s) by a file pattern and a commandline.\n"+
		"\n"+
		"Semantics:\n"+
		"----------\n"+
		"\n"+
		"Name: Arbitrary name for command, appears in popup-menu.\n"+
		"Pattern: Wildcard \"*?[]\"), to identify 1-n commandlines for $FILE.\n"+
		"Command: Commandline to launch.\n"+
		"  Runtime substituted keywords are:\n"+
		"  (Example: $FILE = \"/a/b/c/d.java\")\n"+
		"    $FILE      -> /a/b/c/d.java   (fully qualified path)\n"+
		"    $DIR       -> /a/b/c/         (directory name)\n"+
		"    $BASEEXT   -> d.java          (file with extension)\n"+
		"    $BASE      -> d               (file without extension, dotted classname for class- or jar-file)\n"+
		"    $FILEBASE  -> /a/b/c/d        (path and file without extension)\n"+
		"    $EXT       -> .java           (extension)\n"+
		"    $CLASSPATH -> /a/:$CLASSPATH  (if class is \"b.c.d\", CLASSPATH to file)\n"+
		"  Internal command keywords are:\n"+
		"    VIEW    $FILE - internal Java viewer\n"+
		"    EDIT    $FILE - internal Java editor\n"+
		"    HTML    $FILE - internal Java HTML viewer\n"+
		"    XML     $FILE - internal Java XML editor\n"+
		"    ARCHIVE $FILE - internal Java ZIP/JAR/TAR tool\n"+
		"    IMAGE   $FILE - internal Java image viewer\n"+
		"    JAVA    $FILE - internal launch of Java class or JAR archive\n"+
		"Path: Where to execute the launched application.\n"+
		"\n"+
		"Options:\n"+
		"--------\n"+
		"\n"+
		"Monitor: Start a window for in- and output to the started process.\n"+
		"Loop: Make as much commandline-calls as there are filenames.\n"+
		"Type: Makes command applyable to files only, folders only or both.\n"+
		"Precondition: Makes the explorer wait for successful command termination.\n"+
		"    If command fails (exit not 0), a folder is not opened.\n"+
		"    The error will be shown in a dialog. Useful for mounting or locking folders.\n"+
		"\n"+
		"Restrictions:\n"+
		"-------------\n"+
		"\n"+
		"You can not not use \"Monitor\" when \"Precondition\" is checked.\n"+
		"You can not not use \"Monitor\" when internal command keywords are used.\n"+
		"\n"+
		"Configurations:\n"+
		"---------------\n"+
		"\n"+
		"Setting \"java -D filebrowser.actions.builtIn=false ...\" switches off\n"+
		"all built-in Java-Actions for file types (VIEW, EDIT, HTML, ARCHIVE, IMAGE, JAVA).\n"+
		"\n"+
		"Setting \"java -D filebrowser.actions.JAF=false ...\" switches off\n"+
		"all JAF-Actions for file types.\n"+
		"\n"+
		"Setting \"java -D filebrowser.actions.JAF_Platform=false ...\" switches off\n"+
		"all platform specific JAF-Actions for file types.\n"+
		"\n"+
		"Setting \"java -D filebrowser.actions.JAF_Java=false ...\" switches off\n"+
		"all Java implemented JAF-Actions given in $HOME/.mailcap for file types.\n"+
		"\n"+
		"Setting \"java -D filebrowser.actions.forceMailcap=true ...\" forces\n"+
		"JAF to use $HOME/.mailcap and $HOME/.mime.types instead of platform implementation.\n"+
		"This implementation executes even Non-Java commands given in $HOME/.mailcap.\n"+
		""
		;
	
	
	private OpenEventEditor(
		OpenCommandList commandList,
		String [] pattern,
		boolean [] isLeaf)
	{
		setTitle("Configure Open Commands (Double Click Actions)");
		IconUtil.setFrameIcon(this, GuiApplication.getApplicationIconURL());
		
		this.commands = new OpenCommandList((Vector)commandList.clone());	// do not touch settings before save
		this.oldcommands = commandList;
		
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		
		ok = new JButton("Ok");
		ok.addActionListener(this);
		can = new JButton("Cancel");
		can.addActionListener(this);
		help = new JButton("Help");
		help.addActionListener(this);
		
		JPanel p1 = new JPanel();
		p1.add(ok);
		p1.add(can);
		p1.add(help);
		
		table = new OpenEventTable(this, commands);
		c.add(table, BorderLayout.CENTER);
		c.add(p1, BorderLayout.SOUTH);
	
		KeyStroke k = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		can.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(k, "close_openevent_editor");
		can.getActionMap().put("close_openevent_editor", new AbstractAction()	{
			public void actionPerformed(ActionEvent e) {
				close(false);
			}
		});
		
		if (pattern != null)
			setNewPattern(pattern, isLeaf);

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				close(false);
			}
		});
		
		new GeometryManager(this).show();
	}
	
	/**
		Factory-Methode um den Editor zu oeffnen. Falls keiner offen ist,
		wird er neu erzeugt, sonst wird der vorhandene in den Vordergrund geholt.
		@param commands Liste der Kommandos
	*/
	public static OpenEventEditor construct(OpenCommandList commands)	{
		return OpenEventEditor.construct(commands, null, null);
	}

	/**
		Factory-Methode um den Editor zu oeffnen und ein neues Pattern zu erfassen.
		@param commands Liste der Kommandos
		@param pattern neu zu erfassendes Datei-Pattern
	*/
	public static OpenEventEditor construct(
		OpenCommandList commands,
		String [] pattern,
		boolean [] isLeaf)
	{
		if (singleton == null)	{
			singleton = new OpenEventEditor(commands, pattern, isLeaf);
		}
		else	{
			singleton.requestFocus();
			if (pattern != null)
				singleton.setNewPattern(pattern, isLeaf);
		}
		return singleton;
	}
		
		
		
	private void setNewPattern(String [] pattern, boolean [] isLeaf)	{
		for (int i = 0; i < pattern.length; i++)
			table.setNewPattern(pattern[i], isLeaf[i]);
	}
	
	
	// interface ActionListener
	
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == ok)	{
			close(true);
		}
		else
		if (e.getSource() == can)	{
			close(false);
		}
		else
		if (e.getSource() == help)	{
			TextViewer.singleton("Open Event Editor Help", helptext);
		}
	}
	
	private void close(boolean dosave)	{
		if (dosave)	{	// restore old state
			//commands.setContent(oldcommands);
			table.commit();
			oldcommands.setContent(commands);
			oldcommands.save();
		}
		dispose();
		singleton = null;
	}
}