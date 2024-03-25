package fri.gui.swing.filebrowser;

import java.util.Vector;
import java.io.File;

/**
	Festlegung der Spalten fuer Datei-Information.
	Methoden, um Zeilen einzufuegen und Object an 
	bestimmten Indizes zurueckzuliefern.
*/

public class FileTableData extends Vector
{
	protected static final int COLUMNS = 7;
	protected Vector cols = new Vector(COLUMNS);
	protected Vector files = null;
	

	/** construct the header of the table */
	public FileTableData()	{
		cols.addElement("Type");
		cols.addElement("Path");
		cols.addElement("Name");
		cols.addElement("Tag");
		cols.addElement("Size");
		cols.addElement("Time");
		cols.addElement("Access");
	}
	
	/** @return the column header of the table */
	public Vector getColumns()	{
		return cols;
	}
	
	public int getTypeColumn()	{
		return 0;
	}
	public int getPathColumn()	{
		return 1;
	}
	public int getNameColumn()	{
		return 2;
	}
	public int getExtensionColumn()	{
		return 3;
	}
	public int getSizeColumn()	{
		return 4;
	}
	public int getTimeColumn()	{
		return 5;
	}
	public int getAccessColumn()	{
		return 6;
	}
	

	/** @return a filtered list of nodes */
	public Vector filter(
		Vector v,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
	{	
		// filter the list of nodes
		if (filter != null)	{
			System.err.println("InfoData, filter is "+filter);
			if (showfiles == false)
				v = NodeFilter.filterFolders(filter, v, include, showhidden);
			else
				v = NodeFilter.filter(filter, v, include, showfiles, showhidden);
		}
		return v;
	}

	/** rebuild the list */
	public void init()	{
		removeAllElements();
		if (files != null)
			files.removeAllElements();
	}
	
	
	/** building rows for adding by model */
	public Vector buildRow(
		Object o,
		boolean isDirectory,
		String name,
		String type,
		String path,
		Long size,
		Long time, 
		String other)
	{
		//System.err.println("FileTableData.buildRow name="+name+", path="+path);
		if (files == null)
			files = new Vector();
			
		files.addElement(o);
		
		Vector row = new Vector(COLUMNS);
		
		row.addElement(type);
		if (path != null)
			row.addElement(path);
		row.addElement(name);
		row.addElement(isDirectory ? "" : getFileExtension(name));
		row.addElement(size);
		row.addElement(time);
		row.addElement(other == null ? "" : other);
		
		return row;
	}


	protected String getFileExtension(String name)	{
		int last = name.lastIndexOf(".");
		String extension = (last > 0) ? name.substring(last + 1) : "";
		return extension.toLowerCase();
	}
		
	
	protected String getName(String name)	{
		File f = new File(name);
		return f.getName();
	}
	
	protected String getPath(String name)	{
		File f = new File(name);
		String p = f.getParent();
		if (p == null)
			return "";
		return p;
//		if (p.endsWith(File.separator) || p.endsWith("/"))
//			return p;
//		return p+(p.indexOf(File.separator) >= 0 ? File.separator : "/";
	}
	
	
	/** remove row after model.remove() */
	public void removeRow(int i)	{
		files.remove(i);
	}


	/** @return index of passed object in list */
	public int getIndexOf(Object node)	{
		return files.indexOf(node);
	}

	/** set new object at passed index in list */
	public void setObjectAt(int i, Object node)	{
		if (files != null)
			files.setElementAt(node, i);
	}
	
	/** @return object of passed index in list */
	public Object getObjectAt(int i)	{
		if (files != null && files.size() > i)
			return files.elementAt(i);
		return null;
	}

	/** @return typed file object of passed index in list */
	public File getFileAt(int i)	{
		Object o = getObjectAt(i);
		if (o instanceof NetNode)
			return (File)((NetNode)o).getObject();
		else
		if (o instanceof File)
			return (File)o;
		return null;
	}


	/** close the table */
	public void close()	{
	}	

	/** @return always null */
	public NetNode getParentNode()	{
		return null;
	}

}