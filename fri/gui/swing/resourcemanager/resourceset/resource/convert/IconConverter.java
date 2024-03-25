package fri.gui.swing.resourcemanager.resourceset.resource.convert;

import java.net.URL;
import java.net.MalformedURLException;
import java.awt.MediaTracker;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import fri.util.Equals;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.*;

/**
	Encapsulates methods to convert (String - Object) a icon resource.
*/

public class IconConverter extends AbstractConverter
{
	public static class IconAndUrl
	{
		public final String url;
		public final Icon icon;
		
		public IconAndUrl(Icon icon, String url)	{
			this.icon = icon;
			this.url = url;
		}
		
		public boolean equals(Object o)	{
			IconAndUrl other = (IconAndUrl)o;
			return Equals.equals(other.icon, icon) && Equals.equals(other.url, url);
		}
	}
	
	
	/** Turn the IconAndUrl into a persistence string. */
	public String objectToString(Object icon)	{
		if (icon != null)	{
			if (icon instanceof Icon)	{
				if (icon instanceof ImageIcon)
					return ((ImageIcon)icon).getDescription();
			}
			else	{
				IconAndUrl iu = (IconAndUrl) icon;
				return iu.url;
			}
		}
		return null;
	}

	/** Turn a persistence string into a IconAndUrl object. */
	public Object stringToObject(String spec)	{
		if (spec != null)	{
			ImageIcon icon = null;
			URL url = getClass().getResource(spec);	// necessary if file is in a .jar
			if (url == null)	{
				try	{ url = new URL(spec); }
				catch (MalformedURLException e)	{}
			}
			
			if (url != null)
				icon = new ImageIcon(url);
			else
				icon = new ImageIcon(spec);
	
			if (icon.getImageLoadStatus() != MediaTracker.COMPLETE)
				System.err.println("ERROR: Could not load icon: "+spec+" URL is "+url);
	
			return new IconAndUrl(icon, spec);
		}
		return null;
	}


	/** Returns the Icon from passed value, or null. */
	public Object toGuiValue(Object value, Object component)	{
		IconAndUrl iu = (IconAndUrl) value;
		return (iu == null) ? null : iu.icon;
	}

	public Class getGuiValueClass(Object component)	{
		return Icon.class;
	}

}
