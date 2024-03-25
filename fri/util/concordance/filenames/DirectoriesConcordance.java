package fri.util.concordance.filenames;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import fri.util.concordance.Concordance;
import fri.util.concordance.ValidityFilter;
import fri.util.file.SortedFileCollectVisitor;
import fri.util.observer.CancelProgressObserver;

/**
	Concordance search for files with same names within directories, recursive.
	The passed ValidityFilter filters files before concordance search is started.
*/
public class DirectoriesConcordance extends Concordance
{
	public DirectoriesConcordance(File [] directories, ValidityFilter fileFilter)
		throws IOException
	{
		this(directories, fileFilter, null);
	}
	
	public DirectoriesConcordance(File [] directories, ValidityFilter fileFilter, CancelProgressObserver observer)
		throws IOException
	{
		ArrayList fileList = new ArrayList();
		
		for (int i = 0; i < directories.length; i++)	{
			if (observer != null && observer.canceled())
				return;
				
			new DirectoryVisitor(directories[i], fileList, fileFilter, observer);
		}

		startSearch(fileList, null, observer);	// filter was already applied
	}


	
	private static class DirectoryVisitor extends SortedFileCollectVisitor
	{
		private ValidityFilter fileFilter;
		private CancelProgressObserver observer;
		
		DirectoryVisitor(File dir, List list, ValidityFilter fileFilter, CancelProgressObserver observer)	{
			super(list);
			
			this.fileFilter = fileFilter;
			this.observer = observer;
			
			try	{
				loop(dir);
			}
			catch (RuntimeException e)	{
				if (e.getMessage() == null || e.getMessage().equals("mine") == false)
					throw e;
			}
		}

		protected void visit(File f)	{
			if (observer != null)
				if (observer.canceled())
					throw new RuntimeException("mine");
				else
					observer.setNote(f.getName());
				
			Object key = fileFilter.isValid(f);
			if (key != null)
				list.add(new FileWrapper(f, key));
		}

	}


	/** Test main. */
	public static void main(String [] args)
		throws IOException
	{
		if (args.length <= 0)	{
			System.err.println("SYNTAX: java "+DirectoriesConcordance.class.getName()+" directory directory ...");
			System.exit(1);
		}
		
		File [] farr = new File[args.length];
		for (int i = 0; i < args.length; i++)
			farr[i] = new File(args[i]);
			
		Concordance search = new DirectoriesConcordance(farr, new DefaultFilenameValidityFilter());
		
		List blockedList = search.getBlockedResult();
		for (int b = 0; b < blockedList.size(); b++)	{
			Concordance.Block block = (Concordance.Block)blockedList.get(b);
			for (int i = 0; i < block.getOccurencesCount(); i++)	{	// loop over all occurences of block
				if (i > 0)
					System.out.println("----------------");
				for (int j = 0; j < block.getPartCount(); j++)
					System.out.println(block.getPartObject(i, j));
			}
			System.out.println("================");
		}
	}

}
