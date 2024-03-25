package fri.gui.awt.resourcemanager.persistence;

import java.io.*;
import java.util.*;
import fri.gui.GuiConfig;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

/**
	Creates, saves and caches property resource files.
	This is the property file persistence implementation of AbstractResourceFile.
	<p>
	Resource files are searched
	<ul>
		<li>in "$HOME/guiresources" and</li>
		<li>with <i>getResourceAsStream()</i> in "/guiresources"
				(when in same JAR archive as this class)</li>
	</ul>
*/

public class ResourceFile extends AbstractResourceFile
{
	private static final String RESOURCE_SUBDIR = "guiresources";
	private File file;

	/** Loads from file persistence, using Properties. First tries file, then class resource loader. */
	protected ResourceFile(String fileName, ResourceFactory resourceFactory, AbstractResourceFile componentTypeResourceFile)	{
		super(componentTypeResourceFile);
		
		File parent = new File(GuiConfig.dir());
		File dir = new File(parent, RESOURCE_SUBDIR);
		dir.mkdirs();
		this.file = new File(dir, fileName);
		
		InputStream is = null;
		try	{	// load as read/writeable file
			load(is = new FileInputStream(file), resourceFactory);
			System.err.println("Loaded GUI resources from file "+file);
		}
		catch	(IOException e)	{
			try	{	// load as resource
				String classResource = "/"+parent.getName()+"/"+RESOURCE_SUBDIR+"/"+file.getName();
				load(is = getClass().getResourceAsStream(classResource), resourceFactory);
				System.err.println("Loaded GUI resources as stream from "+classResource);
			}
			catch	(Exception e2)	{
			}
		}
		finally	{
			try	{ is.close(); }	catch (Exception e)	{}
		}
	}

	private void load(InputStream is, ResourceFactory resourceFactory)
		throws IOException
	{
		Properties p = new Properties();
		p.load(is);
		loadFromMap(p, resourceFactory);
	}

	/** Stores to file persistence where this ResourceFile comes from. */
	public void save()	{
		Properties p = saveToMap();
		
		if (p.size() > 0)	{
			OutputStream os = null;
			try	{
				os = new FileOutputStream(file);
				p.store(os, "User: "+System.getProperty("user.name"));
				System.err.println("Saved GUI resources to "+file);
			}
			catch (IOException e)	{
				e.printStackTrace();
			}
			finally	{
				try	{ os.close(); }	catch (Exception e)	{}
			}
		}
		else	{
			file.delete();
		}
	}

}
