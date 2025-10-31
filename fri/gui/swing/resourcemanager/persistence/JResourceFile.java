package fri.gui.swing.resourcemanager.persistence;

import java.util.Properties;
import fri.gui.awt.resourcemanager.persistence.*;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;
import fri.gui.awt.resourcemanager.resourceset.ResourceSet;
import fri.gui.swing.resourcemanager.resourceset.JResourceSet;
import fri.gui.swing.resourcemanager.LookAndFeel;

/**
	Derived to manage Swing Look And Feel setting.
*/

public class JResourceFile extends ResourceFile implements
	LookAndFeel.ChangeListener
{
	private static String lookAndFeel;
	
	protected JResourceFile(String fileName, ResourceFactory resourceFactory, AbstractResourceFile componentTypeResourceFile)	{
		super(fileName, resourceFactory, componentTypeResourceFile);
		LookAndFeel.addChangeListener(this);
	}

	protected boolean isSpecialKey(String key, String value)	{
		boolean ret = false;
		if (isComponentTypeBound() && key.equals("lookAndFeel"))	{	// restore look and feel
			if (lookAndFeel == null || lookAndFeel.equals(value) == false)
				LookAndFeel.setLookAndFeel(lookAndFeel = value);
			ret = true;
		}
		return ret ? ret : super.isSpecialKey(key, value);
	}

	/** Factory method to create a resource set. Returns a JResourceSet. */
	protected ResourceSet createResourceSet()	{
		return new JResourceSet();
	}
	
	protected Properties saveToMap()	{
		Properties p = super.saveToMap();
		if (isComponentTypeBound() && LookAndFeel.getLookAndFeel() != null)	// save current look and feel
			p.setProperty("lookAndFeel", LookAndFeel.getLookAndFeel());
		return p;
	}


	/** Implements LookAndFeel.ChangeListener to save new Look And Feel. */
	public void lookAndFeelChanged(String newlookAndFeel)	{
		if (lookAndFeel == null || lookAndFeel.equals(newlookAndFeel) == false)	{
			save();
			lookAndFeel = newlookAndFeel;
		}
	}

//	protected void finalize()	{
//		LookAndFeel.removeChangeListener(this);
//	}

}
