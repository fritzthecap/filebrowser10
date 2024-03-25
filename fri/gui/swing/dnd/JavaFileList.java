package fri.gui.swing.dnd;

import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
	Implementation of interface Transferable for File-Lists,
	needed for Drag 'n Drop.
*/	 
public class JavaFileList implements
	Transferable,
	ClipboardOwner
{
	public static final DataFlavor[] flavors = {
		DataFlavor.javaFileListFlavor,
	};
	private static final List flavorList = Arrays.asList(flavors);
	protected List data;


	public JavaFileList(List data) {
		this.data = data;
	}
		 
	public synchronized DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}
		 
	public boolean isDataFlavorSupported(DataFlavor flavor ) {
		return flavorList.contains(flavor);
	}
	
	public synchronized Object getTransferData(DataFlavor flavor)
		throws UnsupportedFlavorException, IOException
	{
		//dumpFlavor(flavor);    
		if (flavor.equals(DataFlavor.javaFileListFlavor))	{
			return this.data;
		}
		else	{
			throw new UnsupportedFlavorException (flavor);
		}
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		System.out.println("JavaFileList lost ownership of "+clipboard.getName());
		System.out.println("data: "+contents);    
	}
	
	public String toString() {
		return "JavaFileList";
	}


	// dump utility function
	private static void dumpFlavors(DropTargetDropEvent e)	{
		System.err.println("stringDropFlavor, DataFlavor not recognized!");
		DataFlavor [] dfs = e.getCurrentDataFlavors();
		for (int i = 0; i< dfs.length; i++)
			dumpFlavor(dfs[i]);
	}


	// dump utility function
	private static void dumpFlavor(DataFlavor flavor) {
		System.err.println("dumping DataFlavor:");
		System.err.println(" - getMimeType "+flavor.getMimeType());
		/*
		System.err.println(" - getHumanPresentableName "+flavor.getHumanPresentableName());
		System.err.println(" - getRepresentationClass "+flavor.getRepresentationClass().getName());
		System.err.println(" - isMimeTypeSerializedObject "+flavor.isMimeTypeSerializedObject());
		System.err.println(" - isRepresentationClassInputStream "+flavor.isRepresentationClassInputStream());
		System.err.println(" - isRepresentationClassSerializable "+flavor.isRepresentationClassSerializable());
		System.err.println(" - isRepresentationClassRemote "+flavor.isRepresentationClassRemote());
		System.err.println(" - isFlavorSerializedObjectType "+flavor.isFlavorSerializedObjectType());
		System.err.println(" - isFlavorRemoteObjectType "+flavor.isFlavorRemoteObjectType());
		System.err.println(" - isFlavorJavaFileListType "+flavor.isFlavorJavaFileListType());
		*/
	}

/*
	else
	if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
	{
		dropTargetDropEvent.acceptDrop (DnDConstants.ACTION_COPY_OR_MOVE);
		List fileList = (List)tr.getTransferData(DataFlavor.javaFileListFlavor);
		Iterator iterator = fileList.iterator();
		while (iterator.hasNext()) {
			File file = (File)iterator.next();
			Hashtable hashtable = new Hashtable();
			hashtable.put("name", file.getName());
			hashtable.put("url", file.toURL().toString());
			((DefaultListModel)getModel()).addElement(hashtable);
		}
		dropTargetDropEvent.getDropTargetContext().dropComplete(true);
	}
*/
}




/*
	utility functions for FileList transfer

	public static DataFlavor getStringFlavor(DropTargetDropEvent e) {
		if (e.isLocalTransfer() == true &&
			e.isDataFlavorSupported(JavaFileList.localStringFlavor))
			return JavaFileList.localStringFlavor;
		else
		if (e.isDataFlavorSupported(JavaFileList.plainTextFlavor))
			return JavaFileList.filelistFlavor;
		else
		if (e.isDataFlavorSupported(StringTransferable.localStringFlavor))
			return StringTransferable.localStringFlavor;	
		else
		if (e.isDataFlavorSupported(DataFlavor.stringFlavor))
			return DataFlavor.stringFlavor;
		else
		if (e.isDataFlavorSupported(DataFlavor.plainTextFlavor))
			return DataFlavor.plainTextFlavor;	

		dumpFlavors(e);
		return null;
	}


	public static String stringDrop(Object data)	{
		if (data instanceof String ) {
			System.err.println("string comes as instance ...");
			return (String) data;
		}
		else
		if (data instanceof InputStream)	{
			InputStream input = (InputStream)data;
			InputStreamReader isr = null;
			try {
				isr = new InputStreamReader(input, "Unicode");
				System.err.println("string comes by input-stream, Unicode ...");
			} catch (UnsupportedEncodingException uee)	{
				isr = new InputStreamReader(input);	  	  
				System.err.println("string comes by input-stream ...");
			}

			StringBuffer str = new StringBuffer();
			int in = -1;
			try {
				while((in = isr.read()) >= 0 ) {
					if (in != 0)
						str.append((char)in);
				}
				return str.toString();

			}
			catch (IOException e)	{
				System.err.println("FEHLER: "+e.getMessage());
			}
		}
		else	{
			System.err.println("FEHLER: transferierte Daten nicht erkannt: class "+data.getClass().getName());
		}
		return null;
	}
*/