package fri.gui.awt.resourcemanager.persistence;

import fri.util.text.Trim;

/**
	A hierarchical name is the unique identifier of a Component within its Component-tree.
	<p>
	A hierarchical name consists of a dotted name, in which the last part is
	the typename of the Resource (Font, Text, Color, ...), e.g. "frame0.panel1.button2.Font".
	Each part before in the hierarchical name is numbered by some digits at the end of
	the name: "frame0.panel1.button2". The last part of this name is the Component's typename
	(button, frame,  ...), which can bear "_" tags, made from component texts: "button_OK".
*/

public class HierarchicalName
{
	public static final String HIERARCHICAL_SEPARATOR = ".";
	public static final String COMPONENT_TAG_SEPARATOR = "-";

	/** E.g. "frame0.panel1.button0". */
	private final String hierarchicalName;

	/** E.g. "Font". */
	private final String resourceTypeName;

	/** E.g. "button". */
	private final String componentTypeName;

	/**
	 * Constructs from an Properties-Key and retrieves hierarchicalName, resouceTypeName and componentTypeName.
	 * @param propertiesKey e.g. "frame0.panel1.button0.Font"
	 */
	public HierarchicalName(String propertiesKey)	{
		int i = propertiesKey.lastIndexOf(HIERARCHICAL_SEPARATOR);
		this.hierarchicalName = (i > 0) ? propertiesKey.substring(0, i) : propertiesKey;
		this.resourceTypeName = (i > 0) ? propertiesKey.substring(i + 1) : null;
		
		i = hierarchicalName.lastIndexOf(HIERARCHICAL_SEPARATOR);
		String ctn = (i > 0) ? hierarchicalName.substring(i + 1) : propertiesKey;
		i = ctn.indexOf(COMPONENT_TAG_SEPARATOR);	// remove "-OK" from "button-OK" to get pure type name
		ctn = i > 0 ? ctn.substring(0, i) : ctn;
		this.componentTypeName = Trim.removeTrailingDigits(ctn);
	}

	/** Returns the hierarchical name used as resourceFile key, without trailing resourceTypeName. */
	public String getHierarchicalName()	{
		return hierarchicalName;
	}
	
	/** Returns "Font", Color", ... */
	public String getResourceTypeName()	{
		return resourceTypeName;
	}
	
	
	/** Returns the pure component type name: "button", not "button_OK". */
	public static String componentTypeNameFromHierarchicalName(String hierarchicalName)	{
		return new HierarchicalName(hierarchicalName+".XXX").componentTypeName;
	}
	
	/** Returns the real component name: "button_OK", not the type name "button". */
	public static String componentNameFromHierarchicalName(String hierarchicalName)	{
		return new HierarchicalName(hierarchicalName).resourceTypeName;
	}
	
	/** Catenizes the hierarchicalName and the resourceTypeName by "." */
	public static String propertiesKeyFromHierarchicalName(String hierarchicalName, String resourceTypeName)	{
		return hierarchicalName + HIERARCHICAL_SEPARATOR + resourceTypeName;
	}

	/** Catenizes the parent and the child by ".", if parent not null, else returns child. */
	public static String hierarchicalNameFromParentAndChild(String parent, String child)	{
		if (parent == null)
			return child;
		return propertiesKeyFromHierarchicalName(parent.toString(), child);
	}

}
