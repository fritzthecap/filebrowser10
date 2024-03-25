package fri.gui.swing.htmlbrowser;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.net.*;
import java.util.List;
import fri.util.NetUtil;
import fri.gui.swing.dnd.*;

/**
	This is not bound to TreeTable. It is a DND-listener that
	sends StringSelection of hyperlink URL's and receives files
	or URL-Strings.
*/

public class TreeTableDndListener implements
	DndPerformer
{
	private HtmlStructureRenderer renderer;
	private boolean activated = true;
	
	
	public TreeTableDndListener(Component comp, HtmlStructureRenderer renderer)	{
		new DndListener(this, comp);
		this.renderer = renderer;
	}


	/** Workaround for get focus bug, that starts drag&drop. */
	public void setActivated(boolean activated)	{
		this.activated = activated;
	}


	// interface DndPerformer
	
	public Transferable sendTransferable()	{
		if (activated == false)
			return null;

		String url = renderer.getSelectedURL();
		if (url == null)
			return null;
					
		return new StringSelection(url);
	}


	public boolean receiveTransferable(Object data, int action, Point p)	{
		System.err.println("TreeTableDndListener.receiveTransferable");
		try	{
			String url = (String)data;
			renderer.loadURL(url);
			return true;
		}
		catch (ClassCastException e)	{
		}
		
		try	{
			List list = (List)data;
			File file = (File)list.get(0);	// only one file
			String url = urlFromFile(file);
			if (url != null)
				renderer.loadURL(url);			
		}
		catch (ClassCastException e)	{
		}
		catch (Exception e)	{
			System.err.println("FEHLER: drop class: "+e.toString());
		}
		
		System.err.println("dropped class: "+data.getClass());
		return false;
	}
	
	private String urlFromFile(File f)	{
		URL url = null;
		try	{
			url = NetUtil.makeURL(f);
		}
		catch (MalformedURLException mue) {
			System.err.println("FEHLER: ungueltige URL: "+f);
			return null;
		}
		return url.toExternalForm();
	}

	
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)	{
			//System.err.println("   ... flavor "+flavors[i].getMimeType());
			if (flavors[i].equals(DataFlavor.stringFlavor))
				return DataFlavor.stringFlavor;
				
			if (flavors[i].equals(DataFlavor.javaFileListFlavor))
				return DataFlavor.javaFileListFlavor;
		}
		return null;
	}


	public void dataMoved()	{}	
	public void dataCopied()	{}
	public void actionCanceled()	{}

	public boolean dragOver(Point p)	{
		return true;
	}
	public void startAutoscrolling()	{}
	public void stopAutoscrolling()	{}

}