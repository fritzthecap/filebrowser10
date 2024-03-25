package fri.gui.swing.filebrowser;

import java.io.File;
import java.util.*;
import fri.gui.swing.combo.history.*;

/**
	Suchen nach einem Dateinamens-Muster.
*/

public class SearchPathComboBox extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();	// alle haengen zusammen
	private static File globalFile = null;


	public SearchPathComboBox()	{
		this(new File(HistConfig.dir()+"SearchPathes.list"));
	}
	
	/** Anlegen einer SearchComboBox.
		@param f file aus dem die Datei-Patterns zu lesen sind. */
	public SearchPathComboBox (File f)	{
		super();
		manageTypedHistory(this, f);
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