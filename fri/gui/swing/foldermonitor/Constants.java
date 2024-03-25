package fri.gui.swing.foldermonitor;

import java.io.File;
import java.util.*;

abstract class Constants
{
	static String EVENT_CREATED = "Created";
	static String EVENT_DELETED = "Deleted";
	static String EVENT_MODIFIED = "Modified";

	static String TYPE_FOLDER = "folder";
	static String TYPE_FILE = "file";
	static String TYPE_UNKNOWN = "?";

	static String TIME = "Time";
	static String CHANGE = "Change";
	static String FILETYPE = "Type";
	static String NAME = "Name";
	static String PATH = "Path";
	static String SIZE = "Size";

	static final Vector columns;
	static	{
		Vector cols = new Vector(4);
		cols.add(TIME);
		cols.add(CHANGE);
		cols.add(FILETYPE);
		cols.add(NAME);
		cols.add(PATH);
		cols.add(SIZE);
		columns = cols;
	}

	static String toTypeString(File f)	{
		return f.isDirectory() ? TYPE_FOLDER : f.isFile() ? TYPE_FILE : TYPE_UNKNOWN;
	}

}