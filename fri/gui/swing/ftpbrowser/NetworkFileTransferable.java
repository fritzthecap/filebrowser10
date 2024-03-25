package fri.gui.swing.ftpbrowser;

import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
	Implementation of interface Transferable for Drag 'n Drop between FTP windows.
	The contained clas NetworkFile can be a filesystem- or FTP-file. It recognizes
	its role in constructor by instanceof tests.

	@author Ritzberger Fritz
*/

public class NetworkFileTransferable implements
	Transferable,
	ClipboardOwner
{
	public static final DataFlavor networkFileFlavor = new DataFlavor(NetworkFile.class, "NetworkFile");  		
	public static final DataFlavor[] flavors = {
		networkFileFlavor,
	};
	private static final List flavorList = Arrays.asList(flavors);
	private List data;


	public NetworkFileTransferable(List data) {
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
		if (flavor.equals(networkFileFlavor))	{
			return this.data;
		}
		else	{
			throw new UnsupportedFlavorException (flavor);
		}
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}
	
	public String toString() {
		return "NetworkFileTransferable";
	}




	/**
		Class to transfer one local or FTP file. If this represents a file from
		local filesystem, the host membervariable is null.
	*/
	public static class NetworkFile implements
		Serializable
	{
		public final String host;
		public final int port;
		public final String user;
		public final byte [] password;
		public final String absolutePath;

		public NetworkFile(FtpServerTreeNode ftpn)	{
			absolutePath = ftpn.getAbsolutePath();
			host = ftpn.getFtpClient().getHost();
			port = ftpn.getFtpClient().getPort();
			user = ftpn.getFtpClient().getUser();
			password = ftpn.getFtpClient().getPassword();
		}
		
		public String toString()	{
			return "ftp://"+host+":"+port+"/"+absolutePath;
		}
	}

}
