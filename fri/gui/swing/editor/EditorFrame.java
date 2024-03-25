package fri.gui.swing.editor;

import java.io.File;
import java.awt.Dimension;
import javax.swing.*;
import java.awt.BorderLayout;
import fri.util.os.OS;
import fri.gui.swing.application.*;
import fri.gui.swing.toolbar.ScrollablePopupToolbar;

/**
	Multiple-Document-Interface (MDI) Text Editor for files.
	Loading files into newly opened MDI window, closing all
	MDI windows when frame closes, bringing windows to front.
	<PRE>
		EditorFrame ed = EditorFrame.singleton(file);
		...
		ed.addWindow(file);
	// open file or set to front when already open
	</PRE>

	@author Ritzberger Fritz
*/

public class EditorFrame extends GuiApplication
{
	private static EditorFrame editor = null;
	private EditorMdiPane mdiPane;
	private EditController controller;
	private static final String version = "2.2";
	

	/**
		Returns a new or existing editor window and shows the editor on screen.
	*/
	public static EditorFrame singleton() {
		return singleton((File [])null);
	}

	/**
		Returns a new or existing editor window. Adds passed
		file in an internal frame for editing.
	*/
	public static EditorFrame singleton(File file) {
		return singleton(new File [] { file });
	}
	
	/**
		Returns a new or existing editor window. Adds passed
		files in internal frames for editing.
	*/
	public static EditorFrame singleton(File [] files) {
		if (editor == null)
			editor = new EditorFrame(files);
		else
			editor.addWindows(files);
		return editor;
	}


	/**
		Create a new editor window, override singleton mechanism.
	*/
	public EditorFrame() {
		this((File[])null);
	}

	public EditorFrame(File file) {
		this(new File[] { file });
	}

	public EditorFrame(File [] files) {
		super("Editor "+version);
		build();
		addWindows(files);
	}

	protected EditController createEditController()	{
		return new TextEditController(this);
	}
	
	protected EditorMdiPane createEditorMdiPane(EditController controller)	{
		return new TextEditorMdiPane((TextEditController)controller);
	}

	private void build()	{
		controller = createEditController();
		mdiPane = createEditorMdiPane(controller);
		controller.setMdiPane(mdiPane);

		JMenuBar mb = new JMenuBar();
		mdiPane.fillMenuBar(mb);
		
		//JToolBar tb = new JToolBar(JToolBar.HORIZONTAL);
		ScrollablePopupToolbar tb = new ScrollablePopupToolbar(mdiPane, true, SwingConstants.TOP);
		if (OS.isAboveJava13) tb.getToolbar().setRollover(true);
		mdiPane.fillToolBar(tb.getToolbar());
		
		JPopupMenu popup = new JPopupMenu();
		mdiPane.fillPopupMenu(popup);	// stores popup and adds it to every new editor
		
		setJMenuBar(mb);

		JPanel p = new JPanel(new BorderLayout());		
		p.add(mdiPane, BorderLayout.CENTER);
		//p.add(tb, BorderLayout.NORTH);

		getContentPane().add(p);

		mdiPane.setPreferredSize(new Dimension(700, 800));
		super.init(mdiPane.getCustomizeItem());
	}

	
	/** Open internal frames with the specified files. */
	public void addWindows(File [] files)	{
		for (int i = 0; files != null && i < files.length; i++)	{
			addWindow(files[i]);
		}		
	}


	/** Open an internal frame with the specified file. */
	public void addWindow(File file)	{
		if (activateWindow(file) == false)
			mdiPane.createMdiFrame(file);
	}
	
	// Brings an internal frame with the specified file to front if existing.
	private boolean activateWindow(File file)	{
		System.err.println("Editor activating or opening file: "+file);
		boolean open = mdiPane.isOpen(file);
		setVisible(true);
		return open;
	}
	
	/** Open a searchdialog with the given parameters */
	public void find(String pattern, String syntax, boolean ignoreCase, boolean wordMatch)	{
		controller.find(pattern, syntax, ignoreCase, wordMatch);
	}

	/** Close and dispose if no save dialog cancels. */
	public boolean close()	{
		if (mdiPane.close() == false)
			return false;

		setVisible(false);
		
		return true;
	}




	/** Application main
. Arguments are the files to open. */
	public static void main(String[] args) {
		if (args.length > 0) {
			if (args[0].indexOf("-help") >= 0 || args[0].equals("-h"))	{
				System.err.println("SYNTAX: "+EditorFrame.class.getName()+" [file file ...]");
				System.exit(1);
			}

			EditorFrame edi = null;
			
			for (int i = 0; i < args.length; i++) {
				if (edi == null)	{
					edi = new EditorFrame(new File(args[i]));
					//edi.find("fri.*", "AWK", true);
				}
				else	{
					edi.addWindow(new File(args[i]));
				}
			}
		}
		else {
			new EditorFrame();
		}
	}

}