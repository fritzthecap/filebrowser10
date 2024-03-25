package fri.gui.awt.resourcemanager.resourceset.resource;

public class ResourceNotContainedException extends Exception
{
	public ResourceNotContainedException(String typeName)	{
		super("The resource could not be found within passed component: "+typeName);
	}
}
