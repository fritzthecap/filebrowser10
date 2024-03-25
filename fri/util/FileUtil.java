package fri.util;

import java.io.*;
import java.util.*;

public abstract class FileUtil
{
	private static final Random random = new Random();
	
	/**
	 * Returns a unique filename in the temporary OS directory ("java.io.tmpdir").
	 * As this does not create the file, the caller must loop until a non-existing file was found.
	 */
	public static String getUniqueTempFileName()	{
		return System.getProperty("java.io.tmpdir")+File.separator+"friware"+System.currentTimeMillis()+"."+random.nextInt();
	}

	
	/** Das aktuelle Arbeitsverzeichnis liefern */
	public static String workingDirectory()	{
		return System.getProperty("user.dir");
	}

	/** Das aktuelle Arbeitsverzeichnis liefern */
	public static String rootDirectory()	{
		File f = new File(workingDirectory());
		String s;
		while ((s = f.getParent()) != null)	{
			f = new File(s);
		}
		return f.getPath();
	}

	/** Das aktuelle Home-Verzeichnis liefern */
	public static String homeDirectory()	{
		return System.getProperty("user.home");
	}


	/** @return File aus allen uebergebenen components.
		Die root muss enthalten sein! */
	public static String makePath(String [] comps)	{
		if (comps == null)
			return null;
			
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < comps.length; i++)	{
			sb.append(comps[i]);
			if (i < comps.length - 1 &&
					(i > 0 || comps[i].endsWith(File.separator) == false))
				sb.append(File.separator);
		}
		return sb.toString();
	}
	
	
	/** Alle Pfad-Teile als Strings ohne (File-Separatoren) zurueckliefern.
		Die root ist nicht enthalten, auch wenn sie einen verwertbaren Namen hat.
	*/
	public static String [] getPathComponents(File f)	{
		return getPathComponents(f, false);
	}

	/** Alle Pfad-Teile als Strings ohne (File-Separatoren) zurueckliefern.
		Die root ist enthalten, wenn withRoot true ist.
	*/
	public static String [] getPathComponents(File f, boolean withRoot)	{
		return getPathComponents(f, withRoot, true);
	}

	public static String [] getPathComponents(
		File f,
		boolean withRoot,
		boolean canonical)
	{
		if (f == null)
			return null;

		if (canonical)	{	// ACHTUNG: kanonisieren des Pfades macht grossen Anfangsbuchstaben unter WINDOWS!
			try	{	// echten Pfad bestimmen, nicht Link
				f = new File(f.getCanonicalPath());
			}
			catch (IOException e)	{
			}
		}

		Vector v = new Vector(10);
		String s;
		while ((s = f.getParent()) != null)	{
			v.insertElementAt(f.getName(), 0);		
			f = new File(s);
		}

		if (withRoot)	{
			String s1 = f.toString();
			if (s1.length() > 1 && s1.endsWith(File.separator))
				s1 = s1.substring(0, s1.indexOf(File.separator));
			v.insertElementAt(s1, 0);
		}
		String [] ret = new String [v.size()];
		for (int i = 0; i < v.size(); i++)	{
			ret[i] = new String((String)v.elementAt(i));
		}
		return ret;
	}
	
	/**
		Entfernen der Datei-Extension
		@return name ohne die letzte ".extension", falls eine gefunden wurde.
	*/
	public static String cutExtension(String name)	{
		// Zum Einreihen der aus Datei gelesenen Angaben.
		// Aus name muss ".font" am Ende entfernt werden.
		int i = 0;
		if ((i = name.lastIndexOf(".")) > 0)
			return name.substring(0, i);
		else
			return name;
	}

	/** Die Endung des Dateinames zurueckliefern.
		@return Die (letzte) Extension ohne Punkt, falls eine gefunden wurde,
		sonst Leerstring.
	*/
	public static String getExtension(String name)	{
		int i = name.lastIndexOf(".");
		if (i > 0)
			return name.substring(i + 1);
		else
			return "";
	}

	/** Einen Pfadnamen vom Dateinamen trennen und
			als absoluten zurueckliefern.
	*/
	public static String separatePath(String filename)	{
		return separatePath(filename, true);
	}

	/** Einen Pfadnamen vom Dateinamen trennen und zurueckliefern.
			Wenn absolute true, wird absoluter Pfad geliefert,
			sonst der relative.
			@return den Pfad mit angehaengtem File.separator.
	*/
	public static String separatePath(String filename, boolean absolute)	{
		if (filename == null)	{
			System.err.println("FEHLER: FileUtil.separatePath(): Parameter ist null");
			return "";
		}
		File f = new File (filename);
		String parent = f.getParent();

		if (parent != null)	{	// hat Pfadname
			if (absolute)	{
				File p;
				if (parent.equals("."))
					p = new File("");
				else
					p = new File(parent);

				String path = p.getAbsolutePath();
				if (path.endsWith(File.separator))
					return path;
				else
					return path+File.separator;
			}
			else	{
				if (parent.equals("."))
					return "."+File.separator;
				if (parent.equals(File.separator+"."+File.separator))
					return File.separator;

				File p = new File(parent);
				String path = p.getPath();

				if (path.endsWith(File.separator))
					return path;
				else
					return path+File.separator;
			}
		}
		else	{
			if (absolute)	{
				File p = new File(f.getAbsolutePath());
				String path = p.getParent();
				if (path != null)
					if (path.endsWith(File.separator))
						return path;
					else
						return path+File.separator;
				else
					return File.separator;
			}
			else	{
				if (filename.equals(f.getPath()) && filename.indexOf(File.separator) >= 0)
					return filename;
				return "."+File.separator;
			}
		}
	}


	/** Returns true if null or "." or "./", or "."+File.seaparator. */
	public static boolean isCurrentDir(String dir)	{
		return dir == null || dir.equals(".") || dir.equals("."+File.separator) || dir.equals("./");
	}
	
	

	/** Einen Dateinamen vom Pfadnamen trennen und zurueckliefern.
			Endet der uebergebene String mit einem File-Separator,
			wird null geliefert.
	*/
	public static String separateFile(String filename)	{
		File f = new File (filename);
		String ret = f.getName();
		if (ret.equals(".") || ret.equals(""))
			return null;
		return ret;
	}



	/** Einen Pfad- und einen Dateinamen zusammenfuegen.
			Es wird sichergestellt, dass die beiden durch einen
			File-Separator getrennt sind. Nulls und empty Strings
			werden adaequat behandelt.
	*/
	public static String joinPathFile(String path, String file)	{
		if (path == null && file == null)
			return "";
		if (path == null || path.equals(""))
			return file;
		if (file == null)
			return path;
		if (path.endsWith(File.separator) == false)
			return path+File.separator+file;
		else
			return path+file;
	}



	/** Den Pfad "relativieren". Der Pfad wird beschnitten, wenn er ein
			Sub-Directory des aktuellen Pfades ist. Ist er es nicht,
			wird er unveraendert zurueckgeliefert.
	*/
	public static String makeRelativePath(String path)	{
		String parent = workingDirectory();
		return makeRelativePath(parent, path);
	}

	/** Den Pfad "relativieren". Der Pfad wird beschnitten, wenn er ein
			Sub-Directory des parent Pfades ist. Ist er es nicht,
			wird er unveraendert zurueckgeliefert.
	*/
	public static String makeRelativePath(String parent, String path)	{
		if (path != null && parent != null)	{
			if (isSubdir(parent, path))	{
				// Bug, unter Windows ist Pfad kuerzer: wird Doppelpunkt vergessen? JDK 1.1
				//int plus = File.separator.equals("\\") ? 1 : 0;
				//return path.substring(parent.length() + plus);
				if (parent.endsWith(File.separator) == false)
					parent = parent+File.separator;
				return path.substring(Math.min(parent.length(), path.length()));
			}
		}
		else
		if (parent == null && path != null)	{
			int i = path.indexOf(File.separator);
			if (i >= 0)
				return path.substring(i + 1);
		}
		return path;
	}


	/** @return Anzahl der File.separators im String, wobei
		sichergestellt wird, dass hinten dran einer haengt. */
	public static int pathLevel(String path)	{
		if (path == null)
			return -1;
		if (path.endsWith(File.separator) == false)
			path = path+File.separator;
		int h = 0;
		for (int i = 0; i < path.length(); i++)	{
			char c = path.charAt(i);
			if (c == File.separatorChar)
				h++;
		}
		return h;
	}


	/** @return true if path is a subdir of parent or equal to parent */
	public static boolean isSubdir(String parent, String path)	{
		if (equalPathes(parent, path))
			return false;
		if (parent.endsWith(File.separator) == false)
			parent = parent+File.separator;
		return path.startsWith(parent);
	}


	/** @return true if pathes are equal ensuring that a
		File.separator is appended to both */
	public static boolean equalPathes(String p1, String p2)	{
		boolean ok1 = p1.endsWith(File.separator);
		boolean ok2 = p2.endsWith(File.separator);
		if (ok1 == ok2 && p1.equals(p2))
			return true;
		if (!ok1)
			p1 = p1+File.separator;
		if (!ok2)
			p2 = p2+File.separator;
		return p1.equals(p2);
	}


	/**
		@return first name in path. If path starts with separator character,
			it is skipped to avoid returning an empty string. No separator is appended
			to returned string.
	*/
	public static String getFirstPathPart(String path)	{
		if (path == null)
			return "";
		while (path.startsWith(File.separator))
			path = path.substring(1);
		int i = path.indexOf(File.separator);
		if (i <= 0)
			return path; 
		else
			return path.substring(0, i);
	}


	/** Removes leading "./" and turns any leading "../" into an absolute path. */
	public static String resolveLeadingDots(String path)	{
		String orig = path;
		
		while (path.startsWith("./"))
			path = path.substring(2);
		
		int higher = 0;
		while (path.startsWith("../"))	{
			higher++;
			path = path.substring(3);
		}
		
		String [] absolutePart = null;
		if (higher > 0)	{
			if (absolutePart == null)
				absolutePart = getPathComponents(new File(workingDirectory()), true, false);
				
			if (absolutePart.length <= higher)
				throw new IllegalArgumentException("Can not change to such a directory or drive: "+orig);
				
			String [] newAbsolute = new String[absolutePart.length - higher];
			System.arraycopy(absolutePart, 0, newAbsolute, 0, newAbsolute.length);
			File f = new File(makePath(newAbsolute), path);
			path = f.getAbsolutePath();
		}
		
		return path;
	}

	
	private FileUtil()	{}

	
	/** test main

	public static void main(String[] args)	{
		String [] tests = {

			// plattformunabhaengig

			File.separator+"a"+File.separator+"b"+File.separator,
			File.separator+"a"+File.separator+"b",
			"a"+File.separator+"b"+File.separator,
			"a"+File.separator+"b",
			File.separator+"c"+File.separator,
			File.separator+"c",
			"c"+File.separator,
			"c",
			File.separator+"."+File.separator,
			File.separator+".",
			"."+File.separator,
			".",
			File.separator,

			"c:"+File.separator+"a"+File.separator+"b"+File.separator,
			"c:"+File.separator+"a"+File.separator+"b",
			"c:"+File.separator+"c"+File.separator,
			"c:"+File.separator+"c",
			"c:"+File.separator+"."+File.separator,
			"c:"+File.separator+".",
			"c:"+File.separator,
			"c:",

			// plattformabhaengig

			"/"+"a"+"/"+"b"+"/",
			"/"+"a"+"/"+"b",
			"/"+"c"+"/",
			"/"+"c",
			"/"+"."+"/",
			"/"+".",
			"/",

			"c:"+"\\"+"a"+"\\"+"b"+"\\",
			"c:"+"\\"+"a"+"\\"+"b",
			"c:"+"\\"+"c"+"\\",
			"c:"+"\\"+"c",
			"c:"+"\\"+"."+"\\",
			"c:"+"\\"+".",
			"c:"+"\\",
		};

		System.out.println("workingDirectory = "+workingDirectory());

		for (int i = 0; i < tests.length; i++)	{
			System.out.println(
				"separateFile(\""+tests[i]+"\") = "+
				separateFile(tests[i]));
		}
		for (int i = 0; i < tests.length; i++)	{
			System.out.println(
				"separatePath(\""+tests[i]+"\") = "+
				separatePath(tests[i]));
		}
		for (int i = 0; i < tests.length; i++)	{
			System.out.println(
				"separatePath(\""+tests[i]+"\", false) = "+
				separatePath(tests[i], false));
		}
	}
	
	*/

}