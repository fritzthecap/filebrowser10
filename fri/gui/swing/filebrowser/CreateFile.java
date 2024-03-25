package fri.gui.swing.filebrowser;

import java.io.*;

public abstract class CreateFile
{
	/**
	 * Physisches Erzeugen einer leeren Datei
	 * @param file Ziel-Datei
	 */
	public static boolean createFile(File file) throws Exception
	{
		/*
		FileOutputStream out = null;
		out = new FileOutputStream(file);
		out.close();
		return file.exists();
		*/
		return file.createNewFile();
	}

	/**
	 * Physisches Erzeugen eines Verzeichnisses.
	 * @param dir Ziel-Directory
	 */
	public static boolean createDirectory(File dir) throws Exception
	{
		return dir.mkdir();
	}
}