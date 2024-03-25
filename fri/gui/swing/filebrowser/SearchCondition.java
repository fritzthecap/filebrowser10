package fri.gui.swing.filebrowser;

import java.io.*;
import java.util.*;
import fri.util.FileUtil;
import fri.util.observer.CancelProgressObserver;
import fri.util.os.OS;
import fri.util.file.*;
import fri.util.file.archive.*;

/**
	Mehrere verschiedene Bedingungen in einer Liste halten und
	mittels einer Methode "match(File)" abfragbar machen.
*/

public class SearchCondition extends Vector
{
	private boolean searchArchives;
	private CancelProgressObserver observer;
	private Vector extractRootFolders;
	
	
	/**
		Create a SearchCondition with a SearchFilePattern.
	*/
	public SearchCondition(
		SearchFrame dlg,
		String filePattern,
		boolean ignoreCase,
		boolean include,
		boolean searchArchives)
	throws
		gnu.regexp.REException,
		ArrayIndexOutOfBoundsException
	{
		this.observer = dlg;
		this.searchArchives = searchArchives;
		
		addElement(new SearchFilePattern(
				filePattern,
				ignoreCase,
				include));
	}
	

	/**
		Create a SearchCondition with a SearchFilePattern and a SearchContentPattern.
	*/
	public SearchCondition(
		SearchFrame dlg,
		String filePattern,
		boolean ignoreCase,
		boolean include,
		boolean positive,
		String contentPattern,
		boolean contIgnoreCase,
		String regExpSyntax,
		boolean wordMatch,
		boolean showFoundLines,
		boolean searchArchives)
	throws
		gnu.regexp.REException,
		ArrayIndexOutOfBoundsException
	{
		this.observer = dlg;
		this.searchArchives = searchArchives;
		
		addElement(new SearchFilePattern(
				filePattern,
				ignoreCase,
				include));
				
		addElement(new SearchContentPattern(
				contentPattern,
				contIgnoreCase,
				regExpSyntax,
				positive,
				wordMatch,
				showFoundLines,
				dlg));
	}


	/**
		Match a File against all contained search criteria (SearchPattern).
		Loop an archive file if archive option was given.
		Store the archive temporary extract root to a local list for clean().
	*/
	public Vector match(File f)	{
		Vector v = new Vector();
		
		// identify and do archive
		if (searchArchives && ArchiveFactory.isArchive(f))	{
			Archive archive = null;
			try	{
				archive = ArchiveFactory.newArchive(f, true);
				
				for (Enumeration e = archive.archiveEntries(); e.hasMoreElements(); )	{
					observer.progress(0L);
					
					ArchiveEntry entry = (ArchiveEntry)e.nextElement();
					
					SearchFile sf = new SearchFile(entry, archive);
					File file;
					
					if ((file = match(sf)) != null)	{
						v.add(file);
					}
				}
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
			finally	{
				if (archive != null)	{
					if (archive.getExtractRootFolder() != null)	{
						if (extractRootFolders == null)
							extractRootFolders = new Vector();
							
						extractRootFolders.add(archive.getExtractRootFolder());
					}
					archive.close();
				}
			}
		}
		else	{
			// do normal files and folders
			File file = match(new SearchFile(f));
			if (file != null)
				v.add(file);
		}

		return v.size() > 0 ? v : null;
	}


	// match exactly one file
	private File match(SearchFile sf)	{
		for (int i = 0; i < size(); i++)	{
			SearchPattern patt = (SearchPattern)elementAt(i);
			//System.err.println(" ... matching "+patt);
			if (patt.match(sf) == false)
				return null;
		}

		try	{
			return sf.getFile();	// files called by getFile() will deleteOnExit, but not on archive.close()
		}
		catch (IOException e)	{
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
		Delete all files left by temporary archive extractions.
	*/
	public void clean(BufferedTreeNode root)	{
		if (extractRootFolders != null)	{
			for (Enumeration e = extractRootFolders.elements(); e.hasMoreElements(); )	{
				File folder = (File)e.nextElement();
				new DeleteFile(folder);
				
				String pnt = folder.getParent();
				if (pnt == null)
					continue;
					
				NetNode n = NodeLocate.fileToNetNode(
						(NetNode)root.getUserObject(),
						FileUtil.getPathComponents(new File(pnt), OS.isWindows));

				n.list(true);
			}
		}
	}
	
}