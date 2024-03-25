package fri.gui.awt.geometrymanager;

import java.awt.*;
import java.io.*;
import java.util.*;
import fri.gui.GuiConfig;

/**
	Provides persistence for window type geometries.
	Associates a window type (key) with a file and a geometry template
	(managed by static maps). The geometry template will always set the
	geometry values of last saved window to a newly created window geometry.
*/
public class GeometryManager extends AbstractGeometryManager
{
	private static final String GEOMETRY_SUBDIR = "windowgeometry";
	private static final Hashtable fileCache = new Hashtable();
	
	/** Creates a GeometryManager that cascades windows. */
	public GeometryManager(Window window)	{
		super(window);
	}
	
	/** Creates a GeometryManager that cascades windows, and optionally does no sizing. */
	public GeometryManager(Window window, boolean doSize)	{
		super(window, doSize);
	}
	
	/** Creates a GeometryManager that cascades or tiles windows. @param style one of CASCADING or TILING. */
	public GeometryManager(Window window, int style)	{
		super(window, style);
	}
	
	/** Creates a new instance of WindowGeometry. Overridden for Properties persistence. */
	protected void load(String key, WindowGeometry geometry)	{
		File file = (File) fileCache.get(key);
		
		if (file == null)	{	// try to read in the properties file for this window type
			File parent = new File(GuiConfig.dir());
			File dir = new File(parent, GEOMETRY_SUBDIR);
			dir.mkdirs();
			
			file = new File(dir, key+".properties");
			fileCache.put(key, file);
			
			InputStream is = null;
			try	{	// load as read/writeable file
				load(is = new FileInputStream(file), geometry);
				System.err.println("Loaded GUI geometry from file "+file);
			}
			catch	(IOException e)	{
				try	{	// load as resource
					String classResource = "/"+parent.getName()+"/"+file.getName();
					load(is = getClass().getResourceAsStream(classResource), geometry);
					System.err.println("Loaded GUI geometry as stream from "+classResource);
				}
				catch	(Exception e2)	{
				}
			}
			finally	{
				try	{ is.close(); }	catch (Exception e)	{}
			}
		}
	}

	private void load(InputStream is, WindowGeometry geometry)	// reads the geometry from properties
		throws IOException
	{
		Properties p = new Properties();
		p.load(is);
		
		int height = getInt(p, "height");
		int width = getInt(p, "width");
		int x = getInt(p, "x");
		int y = getInt(p, "y");
		
		geometry.setPoint(new Point(x, y));
		geometry.setDimension(new Dimension(width, height));
	}

	private int getInt(Properties p, String key)	{
		String s = p.getProperty(key);
		return (s != null) ? Integer.parseInt(s) : -1;
	}



	/** Saves passed geometry. Overridden for Properties persistence. */
	protected void save(String key, WindowGeometry windowGeometry)	{
		File file = (File) fileCache.get(key);
		
		Properties p = new Properties();	// save to property persistence
		
		if (windowGeometry.getPoint() != null)	{
			p.setProperty("x", ""+windowGeometry.getPoint().x);
			p.setProperty("y", ""+windowGeometry.getPoint().y);
		}
		if (windowGeometry.getDimension() != null)	{
			p.setProperty("width", ""+windowGeometry.getDimension().width);
			p.setProperty("height", ""+windowGeometry.getDimension().height);
		}
		
		if (p.size() > 0)	{
			OutputStream os = null;
			try	{
				os = new FileOutputStream(file);
				p.store(os, "User: "+System.getProperty("user.name"));
				System.err.println("Saved GUI geometry to "+file);
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
