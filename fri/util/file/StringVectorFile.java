package fri.util.file;

import java.io.*;
import java.util.*;

/**
	Methoden zum Sichern und Laden einer String-Liste in und aus einer Datei.
	Es wird ein Reader/Writer (Character Konvertierung) zum verwendet.
	Die Verzeichnisse im Datei-Pfad werden angelegt, wenn nicht vorhanden.
*/

public abstract class StringVectorFile
{
	/**
		Sichern eines Vectors von Strings auf das Dateisystem. Ist er leer,
		wird die Datei geloescht. Die Strings duerfen keine Newlines
		enthalten, da dies der record-separator ist.
		@param file Datei, auf die geschrieben werden soll.
		@param v String-Liste, die geschrieben werden soll.
		@return false wenn Datei nicht geoeffnet werden konnte.
	*/
	public static boolean save(File file, Vector v)  {
		String [] sarr = null;
		if (v != null && v.size() > 0)	{
			sarr = new String [v.size()];
			v.copyInto(sarr);
		}
		return saveArray(file, sarr);
	}
	
	/**
		Sichern eines String-Arrays auf das Dateisystem. Ist es leer,
		wird die Datei geloescht. Die Strings duerfen keine Newlines
		enthalten, da dies der record-separator ist.
		@param file Datei, auf die geschrieben werden soll.
		@param sarr String-Array, das geschrieben werden soll.
		@return false wenn Datei nicht geoeffnet werden konnte.
	*/
	public static boolean saveArray(File file, String [] sarr)  {
		// delete file if array is empty
		if (sarr == null || sarr.length <= 0)	{
			try	{
				file.delete();
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
			return true;
		}
		
		// create directories if not existent
		String pnt = file.getParent();
		if (pnt != null)	{
			File dir = new File(pnt);
			if (dir.exists() == false && dir.mkdirs() == false)
				return false;
		}

		boolean ret = true;

		// write to file
		System.err.println("saving "+sarr.length+" strings to "+file);
		BufferedWriter out = null;
		try	{
			out = new BufferedWriter(new FileWriter(file));
			for (int i = 0; i < sarr.length; i++)	{
				out.write(sarr[i]);
				out.newLine();
			}
		}
		catch (IOException e)	{
			e.printStackTrace();
			ret = false;
		}
		finally	{
			try	{ out.close(); }	catch (Exception e)	{}
		}

		return ret;
	}

	/**
		Laden eines Vectors vom Dateisystem.
		@param f Datei, aus der der Vector gelesen werden soll, newline-separated.
		@param v Liste, die mit Strings aus der Datei gefuellt wird.
		@return true wenn Datei vorhanden war, wenn nicht oder bei Lesefehler false.
	*/
	public static boolean load(File file, Vector v)	{
		System.err.println("loading Strings from "+file);
		BufferedReader in = null;
		try	{
			in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null)	{
				v.addElement(line);
			}
		}
		catch (FileNotFoundException e1)	{
			// muss nicht existieren, weil vielleicht nie gesichert wurde
			return false;
		}
		catch (Exception e)	{
			e.printStackTrace();
			return false;
		}
		finally	{
			try	{ in.close(); }	catch (Exception e)	{}
		}
		return true;
	}

	/**
		Laden eines String-Arrays vom Dateisystem.
		@param f Datei, aus der der Vector gelesen werden soll, newline-separated.
		@return Vector von String aus der Datei.
	*/
	public static String [] loadArray(File file)	{
		Vector v = new Vector();

		boolean success = load(file, v);
		if (success == false)
			return null;

		String [] sarr = new String [v.size()];
		v.copyInto(sarr);

		return sarr;
	}
	
	private StringVectorFile()	{}
}
