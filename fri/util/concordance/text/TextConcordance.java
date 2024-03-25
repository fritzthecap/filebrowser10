package fri.util.concordance.text;

import java.util.List;
import fri.util.concordance.Concordance;
import fri.util.concordance.ValidityFilter;

/**
	Concordance search within a list of text lines.
*/
public class TextConcordance extends Concordance
{
	protected TextConcordance()	{
	}
	
	public TextConcordance(List lines, ValidityFilter textlineFilter)	{
		this(lines, textlineFilter, 0, 0);
	}
	
	public TextConcordance(List lines, ValidityFilter textlineFilter, int breakAfterCount, int minimumLinesPerBlock)	{
		super(lines, textlineFilter, breakAfterCount, minimumLinesPerBlock);
	}

	/** Implements a factory method for the text line wrapper. Creates LineWrappers. */
	protected Object createWrapper(Object object, int index)	{
		return new LineWrapper((String)object, index);
	}

}