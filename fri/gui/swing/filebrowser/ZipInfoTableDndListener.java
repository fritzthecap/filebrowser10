package fri.gui.swing.filebrowser;

import javax.swing.*;
import java.awt.datatransfer.*;
import java.awt.Point;
import java.util.*;
import java.io.File;
import fri.gui.swing.dnd.JavaFileList;
import fri.gui.swing.dnd.*;
import fri.gui.swing.table.sorter.*;

/**
 <UL>
 <LI><B>Background:</B><BR>
 	Drag and Drop from ZIP archives InfoDialog to tree view.
 </LI><P>
 <LI><B>Responsibilities:</B><BR>
 	Extract entries to temporary path and submit file list
 </LI><P>
 <LI><B>Behaviour:</B><BR>
 	implements DndListener
 </LI><P>
 </UL>
 <P>
 <UL>
 @author  $Author: fr $ - Ritzberger Fritz<BR>
 </UL>
*/

public class ZipInfoTableDndListener implements
	DndPerformer
{
	private JTable table;
	private TableSorter sorter;
	private ZipInfoData data;
	private Object [] intransfer = null;

	
	/**
		@param table JTable to watch
		@param sorter the table model
	*/
	public ZipInfoTableDndListener(
		JTable table,
		TableSorter sorter,
		FileTableData data)
	{
		this.table = table;
		this.sorter = sorter;
		this.data = (ZipInfoData)data;
		new DndListener(this, table, DndPerformer.COPY_OR_MOVE);
	}

		
	// interface DndPerformer

	/** implements DndPerformer */
	public Transferable sendTransferable()	{
		Vector v = extractSelectedToTempPath();
		if (v == null)
			return null;
			
		intransfer = new Object[v.size()];
		v.copyInto(intransfer);
		System.err.println("setting drop menu to false");
		
		TreeMouseListenerJDK12.globalUseDropMenu = false;
		
		return new JavaFileList(Arrays.asList(intransfer));
	}


	public int [] getSelectedConvertedRowIndexes()	{
		int [] viewRows = table.getSelectedRows();
		
		if (viewRows == null || viewRows.length <= 0)
			return null;
			
		for (int i = 0; i < viewRows.length; i++)	{
			viewRows[i] = sorter.convertRowToModel(viewRows[i]);
		}
		return viewRows;
	}
	
	
	public Vector extractSelectedToTempPath()	{
		int [] iarr = getSelectedConvertedRowIndexes();
		if (iarr == null || iarr.length <= 0)
			return null;

		Vector v = null;
		try	{
			v = data.extractEntries(iarr, ZipInfoData.tempPath, null);
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		
		if (v != null)	{
			for (Enumeration e = v.elements(); e.hasMoreElements(); )	{
				((File)e.nextElement()).deleteOnExit();
			}
		}

		return v != null && v.size() > 0 ? v : null;
	}
	

	/** implements DndPerformer */
	public boolean dragOver(Point p)	{
		return false;
	}

	/** implements DndPerformer */
	public boolean receiveTransferable(Object data, int action, Point p)	{
		return false;
	}
	
	/** implements DndPerformer */
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		return DataFlavor.javaFileListFlavor;
	}


	public void actionCanceled()	{
		System.err.println("setting drop menu to true");
		TreeMouseListenerJDK12.globalUseDropMenu = true;
	}
	

	public void dataCopied()	{
		//actionCanceled();
	}
	public void dataMoved()	{
		//actionCanceled();
	}
	public void startAutoscrolling()	{}
	public void stopAutoscrolling()	{}

}