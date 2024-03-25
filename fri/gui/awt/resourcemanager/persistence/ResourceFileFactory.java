package fri.gui.awt.resourcemanager.persistence;

import java.util.*;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

/**
	Factory pattern for creating and caching GUI-specific resource file instances.
*/

public class ResourceFileFactory
{
	private static Map cache = new Hashtable();

	/** Create a new AbstractResourceFile for passed Window type name, or return a cached one. */
	public AbstractResourceFile getResourceFile(String typeName, ResourceFactory resourceFactory)	{
		return getResourceFile(typeName, resourceFactory, false);
	}
	
	private AbstractResourceFile getResourceFile(String typeName, ResourceFactory resourceFactory, boolean componentTypeBound)	{
		String fileName = typeNameToPersistenceName(typeName);
		AbstractResourceFile resourceFile = (AbstractResourceFile) cache.get(fileName);
		if (resourceFile == null)	{
			AbstractResourceFile componentTypeResourceFile = null;
			if (componentTypeBound == false)
				componentTypeResourceFile = getResourceFile("ComponentTypeBound", resourceFactory, true);
			resourceFile = createResourceFile(fileName, resourceFactory, componentTypeResourceFile);
			cache.put(fileName, resourceFile);
		}
		return resourceFile;
	}

	/** Creates an AbstractResourceFile from passed arguments. To be overridden. */
	protected AbstractResourceFile createResourceFile(String fileName, ResourceFactory resourceFactory, AbstractResourceFile componentTypeResourceFile)	{
		return new ResourceFile(fileName, resourceFactory, componentTypeResourceFile);
	}

	/** Returns typeName".properties" for property file persistence. */
	protected String typeNameToPersistenceName(String typeName)	{
		return typeName+".properties";
	}

}
