package fri.util.props;

import java.util.*;

/**
	Exchange of values between Properties.
*/

public class PropertiesCopy
{
	/**
		Copy properties from one list to another list when not empty.
		Remove empty source properties in target and source when deleteOnEmpty is true.
		@return target Properties object "to".
	*/
	public static Properties copyWhenNotEmptyDeleteOptionalOnEmpty(
		Properties from,
		Properties to,
		String [] propNames,
		boolean deleteOnEmpty)
	{
		for (int i = 0; i < propNames.length; i++)	{
			String p = from.getProperty(propNames[i]);
			
			if (p != null && p.equals("") == false)	{	// set property
				to.put(propNames[i], p);
			}
			else
			if (deleteOnEmpty)	{	// remove property
				from.remove(propNames[i]);
				to.remove(propNames[i]);
			}
		}
		return to;
	}
	
	/**
		Copy properties from one list to another list. Overwrite settings,
		even when empty (no delete). When a source property is null, an empty
		string is copied to target properties.
		@return target Properties object "to".
	*/
	public static Properties copyOverwrite(
		Properties from,
		Properties to,
		String [] propNames)
	{
		for (int i = 0; i < propNames.length; i++)	{
			String p = from.getProperty(propNames[i]);
			to.put(propNames[i], p != null ? p : "");
		}
		return to;
	}

}
