package fri.gui.awt.resourcemanager.resourceset.resource.convert;

import java.lang.reflect.*;
import java.util.StringTokenizer;
import java.awt.Font;

/**
	Encapsulates methods to convert (String - Object) a font resource.
*/

public class FontConverter extends AbstractConverter
{
	public Class getGuiValueClass(Object component)	{
		return Font.class;
	}

	/** Turn the font into a persistence string. */
	public String objectToString(Object font)	{
		if (font == null)
			return null;
			
		Font f = (Font)font;
		String style = "plain";
		if ((f.getStyle() & Font.BOLD) != 0)
			style = "bold";
		if ((f.getStyle() & Font.ITALIC) != 0)
			style = style+"italic";
			
		return f.getName()+"-"+style+"-"+f.getSize();
	}

	/** Turn a persistence string into a font. */
	public Object stringToObject(String spec)	{
		String name = "Dialog";	// defaults
		int size = 12, style = Font.PLAIN, i = 0;
		
		if (spec != null)	{
			for (StringTokenizer stok = new StringTokenizer(spec, "-"); stok.hasMoreTokens(); i++)	{
				String s = stok.nextToken().trim();
				try	{
					switch (i)	{
						case 0:	// font name
							name = s;
							break;
						case 1:		// font style
							s = s.toLowerCase();
							if (s.indexOf("bold") >= 0)
								style = Font.BOLD;
							if (s.indexOf("italic") >= 0)
								style |= Font.ITALIC;
							break;
						case 2:	// font size
							size = Integer.parseInt(s);
							break;
					}
				}
				catch (NumberFormatException e)	{
					System.err.println("ERROR: invalid number \""+s+"\" in font size: "+spec);
				}
			}	// end for
		}
		return new Font(name, style, size);
	}



	static	{	// workaround for (old?) LINUX systems: initialize graphics environment, without ClassNotFoundException
		try	{
			Class c = Class.forName("java.awt.GraphicsEnvironment");
			Method m = c.getMethod("getLocalGraphicsEnvironment", new Class [0]);
			Object o = m.invoke(null, (Object []) new Class[0]);
			c = o.getClass();
			m = c.getMethod("getAllFonts", new Class [0]);
			o = m.invoke(o, (Object []) new Class[0]);
		}
		catch (Throwable e)	{
			System.err.println("WARNUNG: "+e);
		}
	}

}