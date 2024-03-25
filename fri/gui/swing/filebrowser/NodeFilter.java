package fri.gui.swing.filebrowser;

import java.util.Vector;
import fri.util.regexp.*;
import fri.util.os.OS;

public abstract class NodeFilter
{
	private static Vector filterShowFiles(
		Vector filteredList,
		Vector origList,
		boolean showFiles,
		boolean showHidden)
	{
		if (filteredList == null)
			return null;

		// allocate a new list as original list must not be touched
		Vector newlist = (Vector)filteredList.clone();
		
		if (showFiles)	{	// add folders as they might have been filtered away
			for (int i = 0; i < origList.size(); i++)	{
				Filterable fn = (Filterable)origList.elementAt(i);
				if (!fn.isLeaf() && newlist.indexOf(fn) < 0)	// if not contained
					newlist.addElement(fn);
			}
		}
		else	{	// remove all files from list, filter is to be applied to folders
			for (int i = newlist.size() - 1; i >= 0; i--)	{
				Filterable fn = (Filterable)newlist.elementAt(i);
				if (fn.isLeaf())
					newlist.removeElementAt(i);
			}
		}
		
		return filterHidden(newlist, showHidden);
	}
	
		
	private static Vector filterHidden(Vector newlist, boolean showHidden)	{
		// remove hidden files/folders if necessary
		if (showHidden == false)	{
			for (int i = newlist.size() - 1; i >= 0; i--)	{
				Filterable fn = (Filterable)newlist.elementAt(i);
				if (fn.isHiddenNode())
					newlist.removeElementAt(i);
			}
		}

		/*for (int i = 0; i < newlist.size(); i++)
			System.err.print(" "+newlist.elementAt(i));
		System.err.println();*/

		return newlist;
	}


	/**
		Filter all objects in list by toString() method according to passed filter.
		@param filterText Filter to be applied
		@param list Vector to be filtered
		@param include filter is applied including if true, else excluding
		@param showfiles files will be removed if true
		@return same list if filter is null, "" or "*", else a new allocated list.
	*/
	public static Vector filter(
		String filterText,
		Vector list,
		boolean doInclude,
		boolean showFiles,
		boolean showHidden)
	{
		/*System.err.println("NodeFilter.filter "+filterText+", include "+doInclude+", showfiles "+showFiles);
		for (int i = 0; i < list.size(); i++)
			System.err.print(" "+list.elementAt(i));
		System.err.println();*/

		// Filter list with the given pattern
		Vector filteredList = RegExpUtil.getFilteredAlternation(filterText, list, doInclude, OS.supportsCaseSensitiveFiles());
		// Check if files are shown, add folders again if so, else remove all files
		list = filterShowFiles(filteredList, list, showFiles, showHidden);

		/*System.err.println("NodeFilter.filter "+filterText+", include "+doInclude+", showfiles "+showFiles);
		for (int i = 0; i < list.size(); i++)
			System.err.print(" "+list.elementAt(i));
		System.err.println();*/
		
		return list;
	}


	/**
		Filter the folder-objects in list by toString() method according to passed filter.
		It must be ensured that the filter is valid by calling isFilterValid() before this,
		else the original passed list will be changed!
		@param filterText Filter to be applied
		@param list Vector to be filtered
		@param include filter is applied including if true, else excluding
		@return same list if filter is null, "" or "*", else a new allocated list.
	*/
	public static Vector filterFolders(
		String filterText,
		Vector list,
		boolean doInclude,
		boolean showHidden)
	{
		Vector filteredList = RegExpUtil.getFilteredAlternation(filterText, list, doInclude, OS.supportsCaseSensitiveFiles());
		// filter must be valid, so no clone of list is necessary
		for (int i = 0; i < list.size(); i++)	{
			Filterable fn = (Filterable)list.elementAt(i);
			if (fn.isLeaf() && filteredList.indexOf(fn) < 0)	// all files are included in list
				filteredList.addElement(fn);
		}
		return filterHidden(filteredList, showHidden);
	}

	/**
		Defines a valid filter for nodes.
		@return false if filter is "*" or "" 
	*/
	public static boolean isFilterValid(
		String filterText,
		boolean doInclude)
	{
		if (filterText.equals("*") || filterText.equals(""))
			return false;
		return true;
	}

}
