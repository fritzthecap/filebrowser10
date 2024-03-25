package fri.gui.swing.resourcemanager.persistence;

import fri.gui.awt.resourcemanager.persistence.*;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

/**
	Derived to create JResourceFile (listening for Look And Feel changes).
*/

public class JResourceFileFactory extends ResourceFileFactory
{
	/** Creates an JResourceFile. */
	protected AbstractResourceFile createResourceFile(String fileName, ResourceFactory resourceFactory, AbstractResourceFile componentTypeResourceFile)	{
		return new JResourceFile(fileName, resourceFactory, componentTypeResourceFile);
	}

}
