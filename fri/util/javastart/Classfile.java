package fri.util.javastart;

import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.StringTokenizer;

/**
	Define a Class from a .class file. The main method
	getClass() tries to load it from current CLLASPATH by iterating its path
	until one the package name is matched.
	If this fails, each call to Classfile.getClass() will create a new ClassLoader,
	so that there can be more than one instances of the file class in JVM
	(which identifies classes by their name and their ClassLoader).
	<p>
	This class supports a ClassDependencyRenderer interface, that can be
	installed to listen for loading of dependent classes (calls to loadClass()
	from native defineClass()).
*/

public class Classfile extends ClassLoader
{
	/**
		Error message if class could not be loaded.
	*/
	public static String error = null;

	private String filePath = null;

	/**
		Flag for jars, that have no manifest.
		The file-basename is returned then, assuming that the
		contained Main-class is the same as the JAR package.
	*/
	public static boolean notSure = false;

	// buffer variables
	private static String currCnFilename = "";
	private static String currCpFilename = "";
	private static String currClassName = null;
	private static String currClassPath = null;
	private static Class currClass = null;
	private static String classPath = "";



	public Classfile()	{
		super();
	}

	public Classfile(ClassLoader parent)	{
		super(parent);
	}

	
	protected Class loadClass(String name, boolean resolve)
		throws ClassNotFoundException
	{
		//System.err.println("Classfile "+hashCode()+" loadClass >"+name+"<");
		Class cl = null;

		// First try to load system classes like java.awt.Frame
		try {
			cl = super.loadClass(name, resolve);
		}
		catch (ClassNotFoundException e) {
			//System.err.println("super.loadClass, not found >"+name+"<");
		}
		
		if (cl == null)	{	// not in classpath, try to load class from file
			// name is "my.pkg.MyClass"
			String relPath = name.replace('.', File.separatorChar)+".class";

			// we do not yet know the base classpath of top file, as defineClass is working
			File f = null;
			String path = filePath;
			boolean exists;
		
			do	{
				f = new File(path, relPath);
				exists = f.isFile();
			}
			while (exists == false && (path = new File(path).getParent()) != null);

			if (exists)	{
				cl = loadFromFile(f.getAbsolutePath(), resolve);
				if (cl != null)
					filePath = path;
			}

			if (cl != null && resolve)
				resolveClass(cl);
		}
		
		return cl;
	}


	private Class loadFromFile(String fName, boolean resolve)	{
		if (error != null)	// eine Klasse konnte nicht geladen werden
			return null;
		
		File f = new File(fName);

		//System.err.println("Classfile "+hashCode()+" loadFromFile "+f);
		String filename = f.getPath();
		if (filePath == null)
			filePath = filename.substring(0, filename.lastIndexOf(File.separator));
		
		// Make sure the filename ends with .class
		if (filename.endsWith(".class") == false) {
			filename += ".class";
		}

		try {
			// Read in the byte codes.
			File file = new File(filename);
			if (file.exists() == false)
				return null;
				
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			byte buf[] = new byte[(int)file.length()];
			is.read(buf, 0, buf.length);
			is.close();
			
			// Define the class.
			Class c;

			try	{
				c = defineClass(null, buf, 0, buf.length);

				if (c != null)	{
					//System.err.println("defineClass succeeded: "+c.getName());
					if (filePath == null)
						filePath = getFileClassPath(c.getName(), filename);

					if (resolve)	{
						super.resolveClass(c);
					}
				}
			}
			catch (NoClassDefFoundError e)	{
				e.printStackTrace();
				if (error == null)
					error = e.toString();
				return null;
			}
			catch (ClassFormatError e)	{
				e.printStackTrace();
				if (error == null)
					error = e.toString();
				return null;
			}
			
			return c;
		}
		catch (Exception e) {
			e.printStackTrace();
			if (error == null)
				error = e.toString();
		}

		return null;
	}


	// Returns a URL containing the location of the named resource.
	public URL getResource(String name)	{
		try {
			String absPath = getFileClassPath();
			if (absPath.endsWith(File.separator) == false)
				absPath = absPath+File.separator;
			absPath = absPath+name;
			absPath = absPath.replace(File.separatorChar, '/');

			return new URL("file:///"+absPath);
		}
		catch (MalformedURLException e) {
			//e.printStackTrace();
		}
		return null;
	}

	// Returns an input stream to the named resource.
	public InputStream getResourceAsStream(String name)	{
		try {
			String absPath = getFileClassPath();
			if (absPath.endsWith(File.separator) == false)
				absPath = absPath+File.separator;
			absPath = absPath+name;
			absPath = absPath.replace('/', File.separatorChar);

			return new FileInputStream(absPath);
		}
		catch (FileNotFoundException e) {
			//e.printStackTrace();
		}
		return null;
	}



	/**
		Returns the file path minus package path of the loaded class.
	*/
	private String getFileClassPath()	{
		return filePath;
	}


	/**
		Returns the part of absolute path that does not include the package path.
		This is public only because it is dataless.
	*/
	private static String getFileClassPath(String className, String file) {
		File f = new File(file);
		String absolutePath = f.getAbsolutePath().toString();
		String relativePath = className.replace('.', File.separatorChar);
		
		int endIdx = absolutePath.indexOf(relativePath) - 1;
		if (endIdx <= 0)	// misplaced package name
			return null;
			
		String cpath = absolutePath.substring(0, endIdx);

		return cpath;
	}




	/**
		Tries to load the file by Class.forName() by testing all possible
		parent pathes, until the package path matches the class-name.
		Returns loaded class or null if not within current CLASSPATH.
		This method MUST not be combined with loadFromFile(), as their
		returned Classes will not work together!
	*/
	private static Class loadFileWithinClasspath(String name)	{
		// try all combinations of filename to find classname within classpath
		String filename = name;
		if (filename.toLowerCase().endsWith(".class"))
			filename = filename.substring(0, filename.length() - ".class".length());
			
		File f = new File(filename);
		String path, clsName = null;
		
		while ((path = f.getParent()) != null)	{
			clsName = f.getName()+(clsName != null ? "."+clsName : "");
			f = new File(path);

			try	{
				//System.err.println("Try to load "+clsName+" from "+path);
				Class cls = Class.forName(clsName);
				if (cls != null)
					return cls;
			}
			catch (ClassNotFoundException e)	{
			}
			catch (Throwable e)	{
				if (error == null)	{	// report global error
					String err = e.toString();
					// severe only if not "(wrong name)"
					if (err.indexOf("(wrong name:") < 0)	{
						e.printStackTrace();
						error = err;
					}
				}
				return null;	// NoClassDefFoundError means sub-class not found
			}
		}
		
		return null;
	}
	


	// static service methods

	private static Class load(String filename) {
		System.err.println("Classfile load "+filename);

		currClass = Classfile.loadFileWithinClasspath(filename);
		
		// if error is not null, file was in CLASSPATH but dependencies not
		if (error == null && currClass == null)	{
			// continue if file was not in CLASSPATH
			Classfile cf = new Classfile();
			currClass = cf.loadFromFile(filename, false);
			
			if (cf.getFileClassPath() != null)
				classPath = cf.getFileClassPath();
		}
			
		return currClass;
	}


	/**
		Returns the (defined) Class from the passed class file.
		If the class is not loadable from CLASSPATH,
		this call does not return any buffered Class, it loads
		the class newly by defining a new ClassLoader every time
		it is called.
	*/
	public static Class getClass(String filename)	{
		clear();
		
		if (filename.toLowerCase().endsWith(".class") == false)	{
			throw new IllegalArgumentException("filename of class does not end with .class");
		}

		return currClass = load(filename);
	}


	/** Retrieve main class name from manifest of jar. */
	private static String getJarMainName(String filename)	{
		JarFile jar;
		try	{
			jar = new JarFile(filename);
		}
		catch (IOException e)	{
			System.err.println(e);
			return null;
		}
		Manifest manif;
		try	{
			manif = jar.getManifest();
		}
		catch (IOException e)	{
			System.err.println(e);
			return null;
		}
		if (manif != null)	{
			Attributes attr = manif.getMainAttributes();
			if (attr != null)	{
				String s = attr.getValue(Attributes.Name.MAIN_CLASS);
				if (s != null)	{
					System.err.println("jar main attribute >"+s+"<");
					return s;
				}
				else	{
					error = "jar main attribute empty";
					System.err.println("FEHLER: "+error);
				}
			}
			else	{
				error = "no main attribute in manifest";
				System.err.println("FEHLER: "+error);
			}
		}
		else	{
			error = "no manifest in jar";
			System.err.println("FEHLER: "+error);
		}

		return null;
	}


	/**
		Return the classpath for the passed class in a file.
		This method loads the class and retrieves its name,
		then it changes its (absolute) path part and appends it
		to <code>System.getProperty("java.class.path")</code>.
		@param filename name of class file
		@return new classpath consisting of the one for this file 
			appended to the existing classpath.
	*/
	public static String getClassPath(String filename)	{
		return getClassPath(filename, null);
	}
	

	/**
		Return the classpath for the passed class in a file.
		This method loads the class and retrieves its name,
		then it changes its (absolute) path part and appends it
		to the passed classpath or, if null, to
		<code>System.getProperty("java.class.path")</code>.
		@param filename name of class file
		@param oldClassPath some classpath to append to,
			"java.class.path" is taken when null
		@return new classpath consisting of the one for this file 
			appended to the passed classpath or, if null, the system classpath.
	*/
	public static String getClassPath(String filename, String oldClassPath)	{
		if (currCpFilename.equals(filename))
			return currClassPath;

		clear();
		currCpFilename = filename;
		
		String cp = "";
		if (filename.toLowerCase().endsWith(".class"))	{
			currClass = load(filename);
			cp = classPath;
		}
		else
		if (filename.toLowerCase().endsWith(".jar"))	{
			File f = new File(filename);
			cp = f.getAbsolutePath().toString();
		}
		
		if (oldClassPath == null)	{
			oldClassPath =
				//System.getProperty("sun.boot.class.path")+File.pathSeparator+
				System.getProperty("java.class.path");
		}
		
		boolean found = false;
		StringTokenizer stok = new StringTokenizer(oldClassPath, File.pathSeparator);
		while (cp.length() > 0 && !found && stok.hasMoreTokens())	{
			if (stok.nextToken().equals(cp))
				found = true;
		}

		if (cp.length() > 0 && !found)	{	// not contained in system classpath
			cp = oldClassPath+
					(oldClassPath.length() <= 0 || oldClassPath.endsWith(File.pathSeparator) ?
							"" :
							File.pathSeparator)+
					cp;
		}
		else	{
			cp = oldClassPath;
		}
			
		//System.err.println("Classpath >"+cp+"<");
		return currClassPath = cp;
	}


	/** Retrieve the classname from a class- or jar-file.
		@param filename name of file to retrieve its classname from
		@return full package-name of the class in the classfile 
	*/
	public static String getClassName(String filename)	{
		if (currCnFilename.equals(filename))
			return currClassName;

		clear();
		currCnFilename = filename;
		
		if (filename.toLowerCase().endsWith(".jar"))	{
			String s = getJarMainName(filename);
			if (s != null)
				return currClassName = s;
				
			notSure = true;
		}
			
		if (filename.toLowerCase().endsWith(".class"))	{
			currClass = load(filename);
			if (currClass != null)
				return currClassName = currClass.getName();

			notSure = true;
		}
		
		File f = new File(filename);
		String s = f.getName();	
		int i;
		if ((i = s.indexOf(".")) > 0)
			return currClassName = s.substring(0, i);
			
		return currClassName = s;
	}
	
	
	/** Returns the package name without class-name for class-files
		and the Main-Class (if manifest existent) for jar-files. */
	public static String getPackageName(String filename)	{
		String cn = getClassName(filename);
		if (filename.toLowerCase().endsWith(".jar"))
			return cn;
		int i = cn.lastIndexOf(".");
		return cn.substring(0, (i < 0 ? 0 : i));
	}
	
	
	

	/** Call this to clear error to be sure all is inited! */
	public static void clear()	{
		currCnFilename = "";
		currCpFilename = "";
		error = null;
		notSure = false;
	}
	
	
	
	// test main
	/*
	public static void main(String [] args)	{
		// create a frame with a textfield
		java.awt.Frame f = new java.awt.Frame("File Class Loading Test");
		final java.awt.TextField app = new java.awt.TextField("D:\\Projekte\\fri\\gui\\swing\\filebrowser\\FileBrowser.class");
		f.add(app);
		// create an action listener that launches the java app
		app.addActionListener(new java.awt.event.ActionListener()	{
			public void actionPerformed(java.awt.event.ActionEvent e)	{
				String cn = app.getText();
				if (!cn.toLowerCase().endsWith(".class") && !cn.toLowerCase().endsWith(".jar"))	{
					throw new IllegalArgumentException("name is not .jar or .class");
				}

				try	{
					Class c = cn.toLowerCase().endsWith(".class") ?
						Classfile.getClass(cn) :
						Jarfile.getClass(cn);

					java.lang.reflect.Method main = c.getMethod("main", new Class [] { String[].class });
					main.setAccessible(true);
					main.invoke(null, new Object [] { new String[0] });
				}
				catch (Exception ex)	{
					//ex.printStackTrace();
					System.err.println(ex.getMessage());
				}
			}
		});
		f.pack();
		f.show();
	}
	*/

}
