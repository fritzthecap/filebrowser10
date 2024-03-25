package fri.gui.swing.filechooser;

import java.io.File;
import java.util.Vector;
import java.util.StringTokenizer;
import javax.swing.filechooser.FileFilter;

/**
	A FileFilter dealing with extensions. Does accept all directories.
	Compares after converting to lowercase.
	<p>
	Parses lists of extensions from constructor string, separated by spaces.
*/

public class GenericExtensionFileFilter extends FileFilter
{
	private String description;
	private Vector extensions = new Vector();
	
	public GenericExtensionFileFilter(String extension)	{
		this.description = extension;

		StringTokenizer stok = new StringTokenizer(extension);
		while (stok.hasMoreTokens())	{
			String ext = stok.nextToken();

			if (ext.startsWith("*"))
				ext = ext.substring(1);

			if (ext.startsWith("."))
				ext = ext.substring(1);

			if (ext.length() > 0 && ext.equals("*") == false)
				extensions.add("."+ext.toLowerCase());
			else
				System.err.println("Discarding empty extension: >"+ext+"<");
		}
	}
	
	public boolean accept(File f)	{
		if (f == null || f.isDirectory())
			return true;
			
		String s = f.getName().toLowerCase();
		for (int i = 0; i < extensions.size(); i++)	{
			if (s.endsWith((String)extensions.get(i)))
				return true;
		}
		return false;
	}
	
	public String getDescription()	{
		return description;
	}
	
}