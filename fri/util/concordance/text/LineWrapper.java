package fri.util.concordance.text;

/**
	Wraps a text line and its line-number.
*/
public class LineWrapper
{
	public final String line;
	public final int lineNumber;
	
	public LineWrapper(String line, int lineNumber)	{
		this.line = line;
		this.lineNumber = lineNumber;
	}
	
	/** Returns the line. Do not add line-number, as this is used to hash lines!!! */
	public String toString()	{
		return line;
	}

}
