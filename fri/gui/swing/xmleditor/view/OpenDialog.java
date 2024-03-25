package fri.gui.swing.xmleditor.view;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

import fri.gui.swing.filechooser.*;

/**
 * Open XML URIs or files.
 * 
 * @author Fritz Ritzberger
 * Created on 13.03.2006
 */
public class OpenDialog
{
	private Component parent;
	private Class chooserPersistenceClass;
	private String [] chosen;
	
	public OpenDialog(Component parent, Class chooserPersistenceClass)
		throws CancelException
	{
		this.parent = parent;
		this.chooserPersistenceClass = chooserPersistenceClass;
		chooseFiles();
	}
	
	public String [] getURIsToOpen()	{
		return chosen;
	}

	private void chooseFiles()
		throws CancelException
	{
		File [] f = DefaultFileChooser.openDialog(
				parent,
				chooserPersistenceClass,
				new String [] { ".xml", ".dtd", ".xsd", ".xsl", ".xmi" });
		
		ArrayList list = new ArrayList(f == null ? 0 : f.length);
		
		for (int i = 0; f != null && i < f.length; i++)	{
			if (f[i].exists() == false)	{	// an URL was entered
				String s = f[i].getPath();
				// do some dirty work to get the URL from JFileChooser return
				// which is "/home/fri/abc.xml/file:/www.home.com/rss.xml"
				int idx = s.lastIndexOf(":/");
				if (idx > 0)	{
					String s2 = s.substring(idx);
					int j = idx - 1;
					for (; j > 0 && Character.isLetter(s.charAt(j)); j--)	// skip back to protocol: "file:/"
						;
					String s1 = s.substring(j + 1, idx);
					if (s2.startsWith("://") == false)	// filechooser converted "//" to "/"
						s2 = "://" + s2.substring(":/".length());
					list.add(s1 + s2);
				}
			}
			else	{
				list.add(f[i].getPath());
			}
		}
		
		this.chosen = (String []) list.toArray(new String[list.size()]);
	}
	
}
