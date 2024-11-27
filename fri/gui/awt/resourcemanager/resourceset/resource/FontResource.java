package fri.gui.awt.resourcemanager.resourceset.resource;

import java.lang.reflect.Method;
import java.awt.*;
import fri.util.reflect.ReflectUtil;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.FontConverter;

/**
	Encapsulates methods to set and reset a Font resource.
*/

public class FontResource extends Resource
{
	/** Constructor with a persistence value retrieved from properties. */
	public FontResource(String spec)	{
		super(spec);
	}
	
	/** Constructor with a GUI-component. @exception ResourceNotContainedException when this resource is not gettable from passed component. */
	public FontResource(Object component)
		throws ResourceNotContainedException
	{
		super(component);
	}

	protected Converter createConverter()	{
		return new FontConverter();
	}
	
	public String getTypeName()	{
		return ResourceFactory.FONT;
	}


	
	/** Need to override the resource setter method to adjust the row height of JTable and JTree for the Font. */
	protected void visualizeOnComponent(Object component, Object guiValue)	{
		if (component instanceof Component && guiValue != null)
			setRowHeight((Component) component, (Font) guiValue);
		super.visualizeOnComponent(component, guiValue);
	}

	private void setRowHeight(Component component, Font font)	{
		Method setRowHeightMethod = ReflectUtil.getMethod(component, "setRowHeight", new Class[] { int.class });
		if (setRowHeightMethod != null)	{
			int h = calculateRowHeight(component, font);
			//System.err.println("Calculated new row height: "+h);
			if (h > 0)	{
				try	{
					setRowHeightMethod.invoke(component, new Object[] { Integer.valueOf(h) });
				}
				catch (Exception e)	{
					System.err.println("WARNING: Could not set row height: "+e.getMessage());
				}
			}
		}
	}

	private static int calculateRowHeight(Component component, Font font)	{
		FontMetrics fm = component.getFontMetrics(font);
		int newFontHeight = fm.getAscent() + fm.getDescent();
		int newRowHeight = newFontHeight;
		
		// try to adjust the FontMetrics calculation a little
		Integer oldRowHeight = (Integer) ReflectUtil.invoke(component, "getRowHeight");
		Font oldFont = (Font) ReflectUtil.invoke(component, "getFont");
		
		if (oldFont != null && oldRowHeight != null && oldRowHeight.intValue() > 0)	{
			FontMetrics fmOld = component.getFontMetrics(oldFont);
			int oldFontHeight = fmOld.getAscent() + fmOld.getDescent();
			
			// newRowHeight / newFontHeight == oldRowHeight / oldFontHeight
			newRowHeight = (int) Math.round((double) (newFontHeight * oldRowHeight.doubleValue()) / (double) oldFontHeight);
		}
		return newRowHeight;
	}

}
