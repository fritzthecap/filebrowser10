package fri.util.concordance.textfile;

import java.util.*;
import java.io.*;
import fri.util.concordance.*;
import fri.util.concordance.text.*;

/**
	Concordance search within a single textfile.
*/

public class TextfileConcordance extends TextConcordance
{
	protected TextfileConcordance()	{
	}
	
	public TextfileConcordance(File file, ValidityFilter textlineFilter)
		throws IOException
	{
		this(file, textlineFilter, 0, 0);
	}

	public TextfileConcordance(File file, ValidityFilter textlineFilter, int breakAfterCount, int minimumLinesPerBlock)
		throws IOException
	{
		this.breakAfterCount = breakAfterCount;
		this.minimumLinesPerBlock = minimumLinesPerBlock;
		
		List l = fileToLineList(file);
		
		startSearch(l, textlineFilter);
	}

	/** Converts a file to a list of string text lines. */
	protected List fileToLineList(File file)
		throws IOException
	{
		ArrayList lineList = new ArrayList(Math.min(10, (int)file.length() / 72));

		BufferedReader in = null;
		try	{
			in = new BufferedReader(new FileReader(file));
			String line;
			for (int i = 0; (line = in.readLine()) != null; i++)	{
				lineList.add(createWrapper(line, i));	// wrap line as this method is used to pack multiple files
			}
		}
		finally	{
			try	{ in.close(); }	catch (Exception e)	{}
		}
		
		return lineList;
	}

	/** Implements a factory method for the text line wrapper. Creates LineWrappers if object is not already LineWrapper. */
	protected Object createWrapper(Object object, int index)	{
		if (object instanceof LineWrapper)
			return object;
		return new LineWrapper((String)object, index);
	}



	/** Test main. */
	public static void main(String [] args)
		throws IOException
	{
		if (args.length <= 0)	{
			System.err.println("SYNTAX: java "+TextfileConcordance.class.getName()+" textfile");
			System.exit(1);
		}
		Concordance search = new TextfileConcordance(new File(args[0]), new DefaultTextlineValidityFilter());
		List blockedList = search.getBlockedResult();
		for (int b = 0; b < blockedList.size(); b++)	{
			Concordance.Block block = (Concordance.Block)blockedList.get(b);
			for (int i = 0; i < block.getOccurencesCount(); i++)	{	// loop over all occurences of block
				if (i > 0)
					System.out.println("----------------");
				for (int j = 0; j < block.getPartCount(); j++)	{
					LineWrapper part = (LineWrapper)block.getPartObject(i, j);
					System.out.println((part.lineNumber + 1)+":	"+part.line);
				}
			}
			System.out.println("================");
		}
	}

}