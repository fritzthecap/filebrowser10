package fri.gui.swing.searchdialog;

import java.io.File;
import java.util.Vector;
import fri.gui.swing.combo.history.*;

public class ReplaceHistoryCombo extends MultilineHistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();	// alle haengen zusammen
	private static File globalFile;

	public ReplaceHistoryCombo()	{
		this(new File(HistConfig.dir()+"Replace.list"));
	}
	public ReplaceHistoryCombo(File f)	{
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
