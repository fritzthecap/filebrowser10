package fri.util.concordance.textfiles;

import java.io.File;
import fri.util.concordance.text.LineWrapper;

/**
	Additionally wraps the file of a text line when lines of more
	than one file get tested for concordances.
*/

public class FileLineWrapper extends LineWrapper
{
	public final File file;
	
	public FileLineWrapper(String line, int lineNumber, File file)	{
		super(line, lineNumber);
		this.file = file;
	}
	
}
