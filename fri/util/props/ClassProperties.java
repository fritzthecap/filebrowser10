package fri.util.props;

import java.util.*;
import java.io.*;

/**
 <UL>
 <LI><B>Background:</B><BR>
	Verwaltung verschiedener zu Java-Klassen gehoerenden (und auch dort residierenden)
	Property-Files mit String-Properties (nichts anderes als String!).
	Die Files werden mit Hilfe des Packagenamens der Class der anfordenden
	Applikation gefunden, indem sie mittels Class.getResourceAsStream()
	mit dem "Basis-Namen" ("MyClass" von "my.pack.age.MyClass")
	des Package geladen werden (heisst also "MyClass.properties").
 </LI><P>
 <LI><B>Responsibilities:</B><BR>
 	Laden des Property-Files vom Dateisystem beim ersten get() Aufruf.<br>
 	Speichern des Property-Files auf das Dateisystem.<br>
 	Fuer einen Namen einen globalen Wert liefern und fuer eine Klasse mit
 	einem Namen einen Wert spezifisch fuer diese Klasse liefern.
 </LI><P>
 <LI><B>Behaviour:</B><BR>
 	Wird kein klassen-spezifischer Wert gefunden, wird NICHT in den globalen
 	Properties gesucht!<br>
 	Der Name des Property-Files NICHT waehlbar, er ist gleich dem Basis-Namen
 	der angeforderten Klasse und liegt im gleichen Pfad.<br>
 	Wird die Applikation in einem jar ausgeliefert, werden die Files nur aus
 	dem jar gelesen, wenn sie im Dateisystem in den parallelen Pfaden nicht
 	gefunden werden. Dorthin werden sie auch geschrieben, wenn sie jemals
 	geschrieben werden.
 </LI><P>
 <LI><B>Usage:</B><BR>
 <PRE>
 	String value = ClassProperties.get("MyProperty");
 	// returns global property
 	
 	String value = ClassProperties.get(this.getClass(), "MyProperty");
 	// returns package property for calling class
 </PRE>
 </LI><P>
 </UL>
 <P>
 <UL>
 @author  $Author: fr $ - Ritzberger Fritz<BR>
 </UL>
*/

public abstract class ClassProperties
{
	private static Properties globalProps = null;
	private static Hashtable table = new Hashtable();
	

	private ClassProperties()	{}


	/** Auslesen des Wertes zu "name" aus den globalen Properties */
	public static String get(String name)	{
		return get(null, name);
	}

	/** Auslesen des Wertes zu "name" aus den Properties der uebergebenen Klasse */
	public static String get(Class app, String name)	{
		Properties props = load(app);
		if (props != null)
			return (String)props.getProperty(name);
		return null;
	}
	
	/** Speichern eines Wertes in den globalen Properties */
	public static void put(String name, String value)	{
		if (globalProps == null)
			globalProps = new Properties();
			
		if (value != null)
			globalProps.put(name, value);
		else
			globalProps.remove(name);
	}

	/** Speichern eines Wertes in den Properties der uebergebenen Klasse */
	public static void put(Class c, String name, String value)	{
		Properties props = propsForClass(c);
		if (props == null)	{	// not yet created
			props = new Properties();	// create it
			table.put(keyForClass(c), props);	// store it
		}

		if (value != null)
			props.put(name, value);
		else
			props.remove(name);
	}
	
	/** Loeschen eines Wertes aus den globalen Properties */
	public static Object remove(String name)	{
		if (globalProps == null)
			return null;
		return globalProps.remove(name);
	}
	
	/** Loeschen eines Wertes aus klassenspezifischen Properties */
	public static Object remove(Class c, String name)	{
		Properties props = propsForClass(c);
		if (props == null)
			return null;
		return props.remove(name);
	}


	/** Liefert globale Properties */
	public static Properties getProperties()	{
		if (globalProps == null)
			load(null);
		return globalProps;
	}
	
	/** Liefert klassenspezifische Properties */
	public static Properties getProperties(Class c)	{
		Properties props = propsForClass(c);
		if (props == null)	{
			return load(c);
		}
		return props;
	}
	
	/** Setzen anderer Properties fuer eine Klasse */
	public static void setProperties(Class c, Properties props)	{
		String cls = keyForClass(c);
		table.put(cls, props);
	}

	/** Leifert true wenn die Properites fuer diese Klasse geladen wurden. */
	public static boolean isLoaded(Class c)	{
		return propsForClass(c) != null;
	}
	
	/** Loeschen aller Schluessel aus den globalen Properties */
	public static void clear()	{
		if (globalProps == null)
			return;
		globalProps.clear();
	}
	
	/** Loeschen eines Schluessels aus klassenspezifischen Properties */
	public static void clear(Class c)	{
		Properties props = propsForClass(c);
		if (props == null)
			return;
		props.clear();
	}	


	/**
		Loeschen der globalen Properties aus dem Cache.
		Dann werden sie beim naechsten Abruf aus der Datei neu eingelesen.
	*/
	public static void clearCache()	{
		globalProps = null;
	}
	
	/**
		Loeschen der Klassen-Properties aus dem Cache.
		Dann werden sie beim naechsten Abruf aus der Datei neu eingelesen.
	*/
	public static void clearCache(Class c)	{
		table.remove(keyForClass(c));
	}
	


	private static String keyForClass(Class c)	{
		return c.getName();
	}

	private static Properties propsForClass(Class c)	{
		String cls = keyForClass(c);
		Properties props = (Properties)table.get(cls);
		return props;
	}
	

	private static Properties load(Class app)	{
		if (app == null)	{
			if (globalProps == null)	{
				globalProps = loadProperties(ClassProperties.class);
			}
			return globalProps;
		}
		else	{
			Properties props = propsForClass(app);
			if (props == null)	{
				props = loadProperties(app);
				table.put(keyForClass(app), props);
			}
			return props;
		}
	}
	
	private static Properties loadProperties(Class c)	{
		Properties props = new Properties();
		// first try to read properties outside of packed jar, as they are newer
		InputStream in = null;
		String fileName = keyForClass(c);

		try	{
			String s = getWriteFileName(fileName);
			in = new FileInputStream(s);
			props.load(in);
			System.err.println("ClassProperties.loadProperties from "+s+", standing in "+System.getProperty("user.dir"));
		}
		catch (Exception e)	{
			//System.err.println("loading from filesystem, error is: "+e.getMessage());
		}
		finally	{
			try	{ in.close(); }	catch (Exception e)	{}
		}

		if (in == null)	{	// no file found, try class loader
			try	{
				// binds to classname.properties, loaded by class, even from a jar
				String s = getReadFileName(fileName);
				in = c.getResourceAsStream(s);
				props.load(in);
				System.err.println("ClassProperties.loadProperties as stream ("+c+") from "+s);
			}
			catch (Exception e)	{
				//System.err.println("loding as resource, error is: "+e.getMessage());
			}
			finally	{
				try	{ in.close(); }	catch (Exception e)	{}
			}
		}

		return props;
	}

	private static String getReadFileName(String name)	{	// @param classname
		return getBaseFileName(name);	// gets loaded by "Class.getResource()"
	}
	
	private static String getBaseFileName(String name)	{	// @param my.pkg.cls
		int i;	// take only last name of "my.pkg.cls"
		if ((i = name.lastIndexOf(".")) > 0)
			name = name.substring(i + 1);	// "cls"
		name = name+".properties";	// "cls.properties"
		return name;
	}
	
	
	// seek directory (outside of a packed jar) to write files to.
	private static String getWriteFileName(String name)	{	// @param my.pkg.cls
		String rdf = getBaseFileName(name);	// "cls.properties"	

		int idx = name.lastIndexOf(".");	// retrieve package path
		if (idx > 0)	{	// make package path
			name = name.substring(0, idx);	// "my.pkg"
			name = name.replace('.', File.separatorChar);	// "my/pkg"
			name = name+File.separator+rdf;	// "my/pkg/cls.properties"
		}
		else	{
			name = rdf;	// "cls.properties"
		}

		return ConfigDir.dir()+name;
	}


	/** Speichern aller Properties aller Klassen und globaler. (Siehe Hinweis unten). */
	public static boolean store()	{
		boolean ret = true;
		for (Enumeration e = table.keys(); e.hasMoreElements(); )	{
			String name = (String)e.nextElement();
			ret = store(name) && ret;	// store local properties
		}
		ret = store((String)null) && ret;	// store global properties
		return ret;
	}

	private static boolean store(String c)	{
		// store package properties estimated by name, not by class
		Properties props = null;
		if (c != null)	{
			props = (Properties)table.get(c);
		}
		else	{
			props = globalProps;	// class is null, means global properties
			c = keyForClass(ClassProperties.class);
		}

		if (props == null)	{	// no props stored for given class
			return false;
		}
		
		// write file even if props are empty
		FileOutputStream out = null;
		try	{
			String s = getWriteFileName(c);
			if (props.size() <= 0)	{
				new File(s).delete();
			}
			else	{
				ensureDirectory(s);
				out = new FileOutputStream(s);
				System.err.println("ClassProperties.storeProperties to "+s+", standing in "+System.getProperty("user.dir"));
				props.store(out, "");
				return true;
			}
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		finally	{
			try	{ out.close(); }	catch (Exception e)	{}
		}
		return false;
	}
	
	/**
		Speichern der Properties der uebergebenen Klasse.<br><br>
		Die Datei liegt im Package-Verzeichnis der Klasse.
		D.h. der Aufenthaltsort der Dateien ist relativ zum Start-Verzeichnis
		der Java-Applikation! (Dies kann zu unerwarteten Ergebnissen fuehren,
		wenn das Entwicklungs-Verzeichnis ein anderes ist als das
		Installations-Verzeichnis!)<br>
		Abhilfe: "class.path" durchsuchen und absoluten Pfad bilden. FIXME
	*/
	public static boolean store(Class c)	{
		return store(c == null ? null : keyForClass(c));
	}
	
	/** Ensure that the directory for the passed filename exists */
	public static boolean ensureDirectory(String name)	{
		File f = new File(name);
		File dir = new File(f.getParent());
		if (dir.exists() == false && dir.mkdirs() == false)
			return false;
		return true;
	}


	/** Mix a new Property object with passed ones and given defaults. */
	public static Properties mix(Properties props, Properties defaults)	{
		Properties p = new Properties(defaults);
		for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )	{
			String s = (String)e.nextElement();
			p.setProperty(s, props.getProperty(s));
		}
		return p;
	}
	
}