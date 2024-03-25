package fri.gui.swing.mailbrowser.addressbook;

import java.io.File;
import java.util.*;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.*;
import fri.util.file.FileString;
import fri.gui.mvc.view.swing.TableSelectionDnd;
import fri.gui.mvc.controller.swing.dnd.AbstractAutoScrollingDndPerformer;
import fri.gui.swing.scroll.ScrollPaneUtil;

/**
*/

public class AddressDndPerformer extends AbstractAutoScrollingDndPerformer
{
	private AddressController controller;
	
	public AddressDndPerformer(Component sensor, AddressController controller)	{
		super(sensor, ScrollPaneUtil.getScrollPane(sensor));
		this.controller = controller;
	}
	
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)
			if (flavors[i].equals(DataFlavor.stringFlavor) || flavors[i].equals(DataFlavor.javaFileListFlavor))
				return flavors[i];
		return null;
	}
	
	public Transferable sendTransferable()	{
		List sel = (List)controller.getSelection().getSelectedObject();
		AddressTableRow row = (AddressTableRow)sel.get(0);
		// build semicolon separated string from row
		String s = controller.packRow(row);
		return new StringSelection(s);
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		if (data instanceof List)	{
			List files = (List)data;
			if (files.size() <= 0)
				return false;
			
			for (int i = 0; i < files.size(); i++)
				receiveFile((File)files.get(i));
			
			return true;
		}
		else
		if (data instanceof String)	{
			AddressTableRow dropRow = (AddressTableRow) ((TableSelectionDnd)controller.getSelection()).getObjectFromPoint(p);
			receiveString((String)data, dropRow);
			return true;
		}

		return false;
	}


	private void receiveFile(File f)	{
		String s = FileString.get(f);
		boolean done = false;
		StringTokenizer stok = new StringTokenizer(s, "\r\n");	// parse by newline

		while (stok.hasMoreTokens())	{
			String line = stok.nextToken();
			Vector v = controller.parseRow(line);
			if (v != null)	{
				controller.mergeAddress(v, null);
				done = true;
			}
		}
		
		if (done)
			controller.setActionState();
	}

	private void receiveString(String s, AddressTableRow dropRow)	{
		Vector v = controller.parseRow(s);
		if (v != null)	{
			controller.mergeAddress(v, dropRow);
			controller.setActionState();
		}
	}

}
