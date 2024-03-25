package fri.gui.swing.resourcemanager.resourceset.resource;

import javax.swing.JTextArea;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceNotContainedException;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.IntegerConverter;

/**
	Encapsulates methods to set and reset a tab-size resources.
	The tab-size will be adjusted by:
	<pre>
		textarea.setTabSize(size);
		boolean b = textarea.getLineWrap();	// make visible
		textarea.setLineWrap(!b);
		textarea.setLineWrap(b);
	</pre>
*/

public class TabSizeResource extends Resource
{
	/** Constructor with a persistence value retrieved from properties. */
	public TabSizeResource(String spec)	{
		super(spec);
	}

	/** Constructor with a original value retrieved from some GUI-component. */
	public TabSizeResource(Object component)
		throws ResourceNotContainedException
	{
		super(component);
	}

	public String getTypeName()	{
		return JResourceFactory.TABSIZE;
	}

	protected Converter createConverter()	{
		return new IntegerConverter();
	}

	protected void visualizeOnComponent(Object component, Object guiValue)	{
		super.visualizeOnComponent(component, guiValue);
		
		if (guiValue != null && component instanceof JTextArea)	{
			JTextArea ta = (JTextArea) component;
			boolean wrap = ta.getLineWrap();
			ta.setLineWrap(! wrap);
			ta.setLineWrap(wrap);
		}
	}

}
