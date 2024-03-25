package fri.gui.swing.resourcemanager.resourceset.resource;

import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceNotContainedException;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.Converter;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.MnemonicConverter;

/**
	Encapsulates methods to set and reset and convert (String - Object) a mnemonic resource.
*/

public class MnemonicResource extends Resource
{
	/** Constructor with a persistence value retrieved from properties. */
	public MnemonicResource(String spec)	{
		super(spec);
	}
	
	/** Constructor with a original value retrieved from some GUI-component. */
	public MnemonicResource(Object component)
		throws ResourceNotContainedException
	{
		super(component);
	}

	protected Converter createConverter()	{
		return new MnemonicConverter();
	}

	public String getTypeName()	{
		return JResourceFactory.MNEMONIC;
	}

}
