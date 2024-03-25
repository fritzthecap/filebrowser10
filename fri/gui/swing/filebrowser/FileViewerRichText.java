package fri.gui.swing.filebrowser;

import javax.swing.JFrame;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;
import fri.util.NetUtil;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.htmlbrowser.HTMLViewer;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.IconUtil;

/**
	A Textviewer for RTF and HTML, loading hyperlinks.
*/

public class FileViewerRichText extends JFrame
{
	public FileViewerRichText(Object o)	{
		super("HTML Browser");
		
		IconUtil.setFrameIcon(this, GuiApplication.getApplicationIconURL());

		File f;
		URL url = null;
		if (o == null)	{
			url = getClass().getResource("help/index.html");
		}
		else	{
			f = (File)o;
			if (f.exists())
				url = urlFromFile(f);
		}
		
		Container c = getContentPane();
		final HTMLViewer viewer = new HTMLViewer(url);
		c.add(viewer, BorderLayout.CENTER);
		
		new GeometryManager(this).show();
		
		addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				viewer.close();
			}
		});
	}


	private URL urlFromFile(File f)	{
		URL url = null;
		try	{
			url = NetUtil.makeURL(f);
		}
		catch (MalformedURLException mue) {
			System.err.println("FEHLER: ungueltige URL: "+f);
			return null;
		}
		return url;
	}

}