package fri.gui.swing.filebrowser;

import java.io.File;

public class FilterableFile extends File implements
	Filterable
{
	public FilterableFile(File parent, String file)	{
		super(parent, file);
	}
	
	public boolean isLeaf()	{
		return isDirectory() == false;
	}
	
	public boolean isHiddenNode()	{
		return isHidden();
	}
	
	public String toString()	{
		return getName();
	}
}