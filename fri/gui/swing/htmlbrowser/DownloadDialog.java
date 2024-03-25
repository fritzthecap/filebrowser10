package fri.gui.swing.htmlbrowser;

import java.awt.*;
import javax.swing.*;
import java.net.*;
import java.io.*;
import fri.util.props.*;
import fri.util.html.*;
import fri.gui.swing.progressdialog.*;

/**
	Ein Dialog zur directory-Auswahl, der eine einzelne URL herunterlaedt.
*/

public class DownloadDialog
{
	public static String downloadDir;

	static	{
		downloadDir = ClassProperties.get(DownloadDialog.class, "downloadDir");
		if (downloadDir == null)	{
			downloadDir = new File("downloads").getAbsolutePath();	// in current working directory
		}
	}
	
	/**
		Download einer einzelnen URL, die nicht HTML ist.
		Es wird kein Verzeichnis fuer die Datei angelegt!
	*/
	public DownloadDialog(Frame frame, URL url)	{
		String name = url.toString();
		Object o = Util.openURLConnection(url);
		if (o instanceof URLConnection == false)	{
			error(o != null ? o.toString() : "is null!");
			return;
		}
		int len = ((URLConnection)o).getContentLength();
		
		FileDialog dlg = new FileDialog(frame, "Download "+name, FileDialog.SAVE);
		dlg.setDirectory(downloadDir);
		String fileName = Util.getLastName(name);
		dlg.setFile(fileName);
		dlg.show();
		if (dlg.getDirectory() == null || dlg.getDirectory().equals("") ||
				dlg.getFile() == null || dlg.getFile().equals(""))	{
			// Dialog wurde abgebrochen
			return;
		}

		String target = dlg.getDirectory()+dlg.getFile();
		storeDownloadDir(dlg.getDirectory());

		new File(downloadDir).mkdirs();

		CancelProgressDialog observer = new CancelProgressDialog(
				frame,
				"Download "+name,
				len);

		Runnable download = makeRunnable(
				new String [] { target },
				new URL [] { url },
				observer);

		observer.start(download);
	}
	
	public static void storeDownloadDir(String dir)	{
		downloadDir = dir;
		ClassProperties.put(DownloadDialog.class, "downloadDir", dir);
		ClassProperties.store(DownloadDialog.class);
	}


	private static void error(final String msg)	{
		Runnable dlg = new Runnable()	{
			public void run()	{
				JOptionPane.showMessageDialog(
						null,
						msg,
						"Error",
						JOptionPane.ERROR_MESSAGE);
			}
		};
		SwingUtilities.invokeLater(dlg);
	}


	private static Runnable makeRunnable(
		final String [] targets,
		final URL [] urls,
		final CancelProgressDialog observer)
	{
		Runnable download = new Runnable()	{
			public void run()	{
				for (int i = 0; i < targets.length; i++)	{
					try	{
						System.err.println("starting download "+urls[i]);
						Util.urlContentsToFile(targets[i], urls[i], observer);
						System.err.println("ended download "+urls[i]);
					}
					catch (IOException e)	{
						error(e.getMessage());
					}
					
					if (observer.canceled())
						break;
				}
				observer.endDialog();
			}
		};
		return download;
	}

}