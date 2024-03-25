package fri.gui.swing.commandmonitor;

import java.util.Vector;
import java.io.File;
import fri.gui.swing.combo.history.*;

/** Textfield for commands with history */

public class CommandComboBox extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();	// alle haengen zusammen
	private static File globalFile = null;

	/** Anlegen einer CommandComboBox. @param f file aus dem die Strings zu lesen sind. */
	public CommandComboBox(File f)	{
		super();
		manageTypedHistory(CommandComboBox.this, f);
	}

	// interface TypedHistoryHolder
	
	public void setTypedHistoryData(Vector v, File f)	{
		globalHist = v;
		globalFile = f;
	}
	
	public Vector getTypedHistory()	{
		return globalHist;
	}
	
	public File getHistoryFile()	{
		return globalFile;
	}

}