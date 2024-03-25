package fri.gui.swing.searchdialog;

import java.io.File;
import java.util.Vector;
import fri.gui.swing.combo.history.*;

public class SearchHistoryCombo extends MultilineHistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();	// alle haengen zusammen
	private static File globalFile;

	public SearchHistoryCombo()	{
		this(new File(HistConfig.dir()+"Search.list"));
	}
	public SearchHistoryCombo(File f)	{
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