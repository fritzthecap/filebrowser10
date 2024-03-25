package fri.gui.swing.hexeditor;

import java.io.File;
import fri.gui.swing.editor.*;

/**
	Multiple-Document-Interface (MDI) Hex Editor for files.
	Loading files into newly opened MDI window, closing all
	MDI windows when frame closes, bringing windows to front.
	
	<PRE>
		HexEditorFrame ed = HexEditorFrame.singleton(file);
		...
		ed.addWindow(file);
	// open file or set to front when already open
	</PRE>

	@author Ritzberger Fritz
*/

public class HexEditorFrame extends EditorFrame
{
	private static HexEditorFrame editor = null;
	private static final String version = "1.0";
	

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
			editor = new HexEditorFrame(files);
		else
			editor.addWindows(files);
		return editor;
	}


	/**
		Create a new editor window, override singleton mechanism.
	*/
	public HexEditorFrame() {
		this((File[])null);
	}

	public HexEditorFrame(File file) {
		this(new File[] { file });
	}

	public HexEditorFrame(File [] files) {
		super(files);
		setTitle("Hex Editor "+version);
	}


	protected EditController createEditController()	{
		return new HexEditController(this);
	}
	
	protected EditorMdiPane createEditorMdiPane(EditController controller)	{
		return new HexEditorMdiPane((HexEditController)controller);
	}



	/** Application main
. Arguments are the files to open. */
	public static void main(String[] args) {
		if (args.length > 0) {
			if (args[0].indexOf("-help") >= 0 || args[0].equals("-h"))	{
				System.err.println("SYNTAX: "+HexEditorFrame.class.getName()+" [file file ...]");
				System.exit(1);
			}

			HexEditorFrame edi = null;
			
			for (int i = 0; i < args.length; i++) {
				if (edi == null)	{
					edi = new HexEditorFrame(new File(args[i]));
				}
				else	{
					edi.addWindow(new File(args[i]));
				}
			}
		}
		else {
			new HexEditorFrame();
		}
	}

}