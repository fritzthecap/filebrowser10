package fri.util.diff;

import fri.util.text.Trim;

/**
	Wrapper for String objects when ignoring spaces.
	Overrides equals() and hashCode() to get space ignoring diff results,
	overrides toString() to render real String passed at construction.
*/

public class SpaceIgnoringString
{
	private String line;
	private String trimmed;
	
	public SpaceIgnoringString(String line, boolean exceptLeading)	{
		this.line = line;
		trimmed = Trim.removeSpaceAmounts(line, exceptLeading);
	}
	
	/** Return the trimmed line. */
	public String getTrimmed()	{
		return trimmed;
	}
	
	/** Return the real line. */
	public String toString()	{
		return line;
	}
	
	/** Return the trimmed line hashcode. */
	public int hashCode()	{
		return trimmed.hashCode();
	}

	/** Compare the trimmed lines. */
	public boolean equals(Object o)	{
		return trimmed.equals(((SpaceIgnoringString)o).trimmed);
	}

}