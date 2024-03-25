package fri.util.concordance.textfiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import fri.util.concordance.ValidityFilter;
import fri.util.concordance.textfile.TextfileConcordance;
import fri.util.observer.CancelProgressObserver;

/**
	Concordance search within multiple textfiles. No directories can be processed!
*/
public class TextfilesConcordance extends TextfileConcordance
{
	private File currentFile;	// helper variable for line wrapper factory method
	
	public TextfilesConcordance(File [] files, ValidityFilter textlineFilter)
		throws IOException
	{
		this(files, textlineFilter, null, 0, 0);
	}
	
	public TextfilesConcordance(
			File [] files,
			ValidityFilter textlineFilter,
			CancelProgressObserver observer,
			int breakAfterCount,
			int minimumLinesPerBlock)
		throws IOException
	{
		this.breakAfterCount = breakAfterCount;
		this.minimumLinesPerBlock = minimumLinesPerBlock;
		
		ArrayList lineList = new ArrayList();
		
		for (int i = 0; i < files.length; i++)	{
			if (observer != null)
				if (observer.canceled())
					return;
				else
					observer.setNote(files[i].getName());
				
			currentFile = files[i];
			List list = fileToLineList(currentFile);
			lineList.addAll(list);
		}

		startSearch(lineList, textlineFilter, observer);
	}
	
	/** Implements a factory method for the text line wrapper. Creates FileLineWrappers if object is not already FileLineWrapper. */
	protected Object createWrapper(Object object, int index)	{
		if (object instanceof FileLineWrapper)
			return object;
		return new FileLineWrapper((String)object, index, currentFile);
	}

}
