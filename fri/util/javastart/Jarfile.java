package fri.util.javastart;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.util.jar.Attributes;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.io.IOException;
import java.io.File;
import fri.util.FileUtil;
import fri.util.NetUtil;
import fri.util.ArrayUtil;
import fri.util.props.ClassProperties;

/**
 * A class loader for loading jar files, both local and remote.
 * Additional JARs are searched from subdirectories from
 * class-property-file "Jarfile.properties".
 */

public class Jarfile extends URLClassLoader
{
	/**
	* Creates a new JarClassLoader for the specified urls.
	* @param url the url of the jar file
	*/
	public Jarfile(URL [] urls) {
		super(urls, null);	// DO NOT allow System-ClassLoader as parent, classes would not work together!
		System.err.println("Jarfile URL's are: "+ArrayUtil.print(urls));
	}
	

	public static Class getClass(String jarFilename) throws
		IOException,
		MalformedURLException,
		ClassNotFoundException
	{
		// get the path part from JAR
		File dir = new File(FileUtil.separatePath(jarFilename));
		
		// provide a list for all JAR files
		Vector v = new Vector();
		v.add(jarFilename);	// add to first position

		// Get the application's main class name
		// try to load additional jars from Class-Path entry in MANIFEST
		URL u = NetUtil.makeJarUrl(NetUtil.makeURL(jarFilename));
		JarURLConnection uc = (JarURLConnection)u.openConnection();
		String classname = null;
		Attributes attr = uc.getMainAttributes();

		if (attr != null)	{
			classname = attr.getValue(Attributes.Name.MAIN_CLASS);
			String classpath = attr.getValue(Attributes.Name.CLASS_PATH);
			
			if (classpath != null)	{
				for (StringTokenizer stok = new StringTokenizer(classpath); stok.hasMoreTokens(); )	{
					String jarName = stok.nextToken();
					v.add(new File(dir, jarName).getAbsolutePath());
				}
			}
		}
		else {
			throw new IOException("Specified jar file does not contain a 'Main-Class' manifest attribute");
		}

		// try to load additional jars from directories listed in property file
		ClassProperties.clearCache(Jarfile.class);	// read file every time
		Properties props = ClassProperties.getProperties(Jarfile.class);
		
		for (Enumeration e = props.elements(); e.hasMoreElements(); )	{
			String s = (String)e.nextElement();
			
			File libDir;
			if (s.equals("."))
				libDir = dir;
			else
				libDir = new File(dir, s);
				
			getJarsFromDirectory(libDir, v);
		}

		// make ready all URL's for URLClassLoader constructor
		URL [] urls = new URL [v.size()];
		for (int i = 0; i < v.size(); i++)	{
			urls[i] = NetUtil.makeURL((String)v.get(i));
		}
		
		// Create the class loader for the application jar file and additional JAR's
		Jarfile cl = new Jarfile(urls);
		
		return cl.loadClass(classname);
	}
	
	
	
	private static Vector getJarsFromDirectory(File libDir, Vector v)	{
		if (libDir.exists())	{
			String [] list = libDir.list();
			
			for (int i = 0; i < list.length; i++)	{
				if (list[i].toLowerCase().endsWith(".jar"))	{
					String jar = new File(libDir, list[i]).getAbsolutePath();

					if (v.contains(jar) == false)
						v.add(jar);
				}
			}
		}

		return v;
	}


	// test main
	public static void main(String [] args)	{
		try	{
			//Class c = getClass("E:\\jEdit-1.5.1\\JarExtract\\jedit.jar");
			Class c = getClass("E:\\jEdit\\jedit.jar");
			System.err.println(c);
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
	}
	
}