package fri.util.concordance.filenames;

import java.io.File;
import fri.util.concordance.ValidityFilter;

/** Filter that returns the file name as key. */

public class DefaultFilenameValidityFilter implements ValidityFilter
{
	/** Returns the name of the passed file. */
	public Object isValid(Object o)	{
		return ((File)o).getName();
	}

}