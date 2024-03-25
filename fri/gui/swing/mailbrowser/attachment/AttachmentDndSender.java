package fri.gui.swing.mailbrowser.attachment;

import java.io.*;
import java.util.Vector;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import javax.activation.DataHandler;

import fri.util.activation.StreamToTempFile;
import fri.util.error.Err;
import fri.gui.mvc.controller.swing.dnd.AbstractDndPerformer;
import fri.gui.swing.dnd.JavaFileList;
import fri.gui.swing.mailbrowser.viewers.PartView;

/**
	Sending attachments by creating a temporary file from the attachment
	and sending the File. This can be used to drag attachments everywhere.
*/

public class AttachmentDndSender extends AbstractDndPerformer
{
	private PartView attachButton;
	
	public AttachmentDndSender(PartView attachButton)	{
		super(attachButton.getSensorComponent());
		this.attachButton = attachButton;
	}
	
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)
			if (flavors[i].equals(DataFlavor.javaFileListFlavor))
				return flavors[i];
		return null;
	}
	
	public Transferable sendTransferable()	{
		InputStream is = null;
		OutputStream os = null;
		try	{
			DataHandler dh = attachButton.getDataHandler();
			String name = dh.getName();
			String tmpFile = null;
			if (name != null)
				tmpFile = new File(System.getProperty("java.io.tempdir"), new File(name).getName()).getPath();
			
			File file = StreamToTempFile.create(dh.getInputStream(), tmpFile, dh.getContentType());
			Vector list = new Vector();
			list.add(file);
			
			return new JavaFileList(list);
		}
		catch (IOException e)	{
			Err.error(e);
		}
		finally	{
			try	{ is.close(); }	catch (Exception e)	{}
			try	{ os.close(); }	catch (Exception e)	{}
		}
		return null;
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		return false;
	}

}