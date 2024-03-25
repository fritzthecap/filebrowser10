package fri.util.file;

import java.io.*;
import java.util.Vector;

import fri.util.observer.CancelProgressContinueConsole;
import fri.util.observer.CancelProgressContinueObserver;
import fri.util.sort.quick.*;
import fri.util.file.FileSplit;
import fri.util.io.CopyStream;

/**
	Joins files, created by FileSplit to, to the original file.
	The name for the created join file is taken from the first found
	split file in a default or in given directory.
	<p>
	A passed CancelProgressContinueObserver implementation 
	can tell progress and ask for next disk after each created file.
	<p>
	Usage:
	<pre>
		File joinedFile = new FileJoin(new File(splitDirectory)).join();
	</pre>
	A FileJoin can be done from 1-n mediums that contain 1-n files.
	After each group of split files from a medium a dialog for inserting
	a new medium is launched.
*/

public class FileJoin
{
	private String targetName = null;
	private File fromDir, toDir, targetFile;
	private CancelProgressContinueObserver dlg;
	private int nextIndex = 0, controlIndex = -1, arrayStartIndex;
	private boolean newMedium, cleanUp = false;
	
	private static Comparator joinFileSorter = new Comparator()	{
		public boolean equals(Object o)	{
			return false;
		}
		public int compare(Object o1, Object o2)	{
			String s1 = o1.toString();
			String s2 = o2.toString();
			s1 = getJoinFileNumber(s1);
			s2 = getJoinFileNumber(s2);
			return Integer.valueOf(s1).intValue() - Integer.valueOf(s2).intValue();
		}
	};


	/**
		Join a file from many others in a directory.
		@param fromDir source directory containing all files to join
	*/
	public FileJoin(File fromDir)
		throws IOException
	{
		this(fromDir, null);
	}
	
	/**
		Join a file from many others in a directory.
		@param fromDir source directory containing all files to join
		@param dlg Observer dialog for progress, cancel and continue comfirmation, can be null
	*/
	public FileJoin(File fromDir, CancelProgressContinueObserver dlg)
		throws IOException
	{
		this(fromDir, null, dlg);
	}
	
	/**
		Join a file from many others in a directory.
		@param fromDir source directory containing all files to join
		@param toDir target directory where the joined file will be in, can be null
		@param dlg Observer dialog for progress, cancel and continue comfirmation, can be null
	*/
	public FileJoin(File fromDir, File toDir, CancelProgressContinueObserver dlg)
		throws IOException
	{
		// check arguments
		if (fromDir.isFile())	{
			throw new IOException("Cannot join from file \""+fromDir+"\", need a directory.");
		}
		
		if (fromDir.isDirectory() == false)	{
			throw new IOException("Cannot find directory \""+fromDir);
		}

		if (toDir != null && toDir.exists() && toDir.isDirectory() == false)	{
			throw new IOException("Target directory is a not a directory: " +toDir);
		}

		if (toDir == null)	{
			toDir = getDefaultTargetDirectory();
		}
		else
		if (toDir.exists() == false)	{
			toDir.mkdirs();
		}

		if (toDir != null && toDir.canWrite() == false)	{
			throw new IOException("Can not write to target directory:" +toDir);
		}
		
		this.dlg = dlg;
		this.fromDir = fromDir;
		this.toDir = toDir;
	}


	/**
		Returns the default target directory where the file to create will be in.
		The returned directory must exist and be writeable.
	*/
	public static File getDefaultTargetDirectory()	{
		return new File(System.getProperty("user.home"));
	}
	

	/**
		Start to join files.
		@return created join file.
	*/
	public File join()
		throws IOException
	{
		return join(false);
	}

	/**
		Start to join files and optionally clean up after.
		@param cleanUp if true, all .split files in source directory will
			be deleted after join.
		@return created join file.
	*/
	public File join(boolean cleanUp)
		throws IOException
	{
		this.cleanUp = cleanUp;
		nextIndex = 0;
		controlIndex = -1;
		
		OutputStream out = null;
		try	{
			out = doJoin();
		}
		finally	{
			try	{ out.close(); } catch (Exception e) {}
		}
		
		return targetFile;
	}


	/**
		Sorted list of File parts in passed split directory.
		@return Vector of all File split parts in directory of this FileJoin object.
	*/
	public Vector list()
		throws IOException
	{
		nextIndex = 0;
		controlIndex = -1;

		File [] splitfiles = getSplitFiles();
		if (splitfiles == null)
			return null;
			
		Vector v = new Vector(splitfiles.length);
		for (int k = 0; k < splitfiles.length; k++)
			if (splitfiles[k] != null)
				v.addElement(splitfiles[k]);
				
		return v;
	}

	
	private File [] getSplitFiles()
		throws IOException
	{
		// seek all split files in source directory
		String [] list = fromDir.list();
		
		// filter file list for split extension
		Vector v = new Vector(list.length);
		for (int i = 0; i < list.length; i++)	{
			if (list[i].endsWith(FileSplit.EXTENSION))	{
				v.add(list[i]);
			}
		}
		
		if (v.size() <= 0)
			return null;
		
		list = new String [v.size()];
		v.copyInto(list);
		
		// sort by split number
		try	{
			new QSort(joinFileSorter).sort(list);
		}
		catch (NumberFormatException e)	{
			e.printStackTrace();
			return null;
		}
		
		File [] splitfiles = new File[list.length];
		boolean done = false;
		
		for (int i = 0; i < list.length; i++)	{
			String fileName = list[i];
			
			String num = getJoinFileNumber(fileName);
			nextIndex = Integer.valueOf(num).intValue();

			if (newMedium == false)	{	// check if next number follows controlindex
				if (nextIndex == controlIndex + 1)	{
					newMedium = true;	// found the right number
				}
				else
				if (controlIndex == -1)	{
					throw new IOException("Need split file 0 at start, found: "+nextIndex);
				}
				else	{
					return null;	// wrong disk inserted
				}
						
				arrayStartIndex = nextIndex;	// calculate offset for file array
			}

			if (targetName == null)	{	// we do not have the name of the target file
				targetName = fileName.substring(0, fileName.length() - num.length() - FileSplit.EXTENSION.length() - ".".length());	// "XXX"
			}

			splitfiles[nextIndex - arrayStartIndex] = new File(fromDir, list[i]);
			done = true;
		}
		
		return done ? splitfiles : null;
	}


	private static String getJoinFileNumber(String fileName)	{
		int idx = fileName.lastIndexOf(FileSplit.EXTENSION);	// found extension in "XXX.0.split"
		String s = fileName.substring(0, idx);	// "XXX.0"

		idx = s.lastIndexOf(".");	// found number
		String t = idx > 0 ? s.substring(idx + 1) : null;	// "0"
		
		return t;
	}
	
		
	/**
		Returns the pending target file name if <i>getSplitFiles()</i> has been called once.
		This name has no path: "myArchive.zip".
	*/
	public String getTargetName()	{
		return targetName;
	}
	
	
	private OutputStream doJoin()
		throws IOException
	{
		OutputStream out = null;
		
		while (askContinue(dlg, nextIndex, fromDir))	{
			newMedium = false;	// check file numbers
			
			File [] splitfiles = getSplitFiles();	// nextIndex gets set
			
			if (newMedium == false)	{
				if (dlg != null)
					dlg.setNote("Need "+(controlIndex + 1)+", found " +nextIndex);
				else
					throw new IOException("Found no split files in: " +fromDir);

				nextIndex = controlIndex + 1;
				continue;
			}
			
			if (targetName == null || splitfiles == null)	{
				if (dlg != null)
					dlg.setNote("Found no split files in: " +fromDir);
				else
					throw new IOException("Found no split files in: " +fromDir);

				continue;
			}
			
			// check if found files have correct numbers
			if (nextIndex <= controlIndex)	{
				if (dlg != null)
					dlg.setNote("Files up to number "+nextIndex+" already have been appended!");
				else
					throw new IOException("Files up to number "+nextIndex+" already have been appended!");

				nextIndex = controlIndex + 1;
				continue;
			}
			
			controlIndex = nextIndex;	// what was
			nextIndex++;	// what should be

			if (targetFile == null)	{
				targetFile = new File(toDir, targetName);
				
				if (targetFile.exists())	{
					if (dlg != null && dlg.askContinue("Overwrite \""+targetFile+"\"?") == false)	{
						dlg.endDialog();
						return out;
					}

				}
				
				if (dlg != null)
					dlg.setNote("Creating target file: "+targetFile);
					
				out = new BufferedOutputStream(new FileOutputStream(targetFile));
			}

			for (int i = 0; i < splitfiles.length; i++)	{
				File from = splitfiles[i];
				long length = from.length();
				CopyStream.bufsize = (int)length;
				
				if (from != null)	{
					InputStream in = new BufferedInputStream(new FileInputStream(from));
			
					if (dlg != null)
						dlg.setNote("Reading split file: "+from);
					
					new CopyStream(in, length, out, dlg, false, true).copy();
				}
			}

			if (cleanUp)	{
				new DeleteFile(fromDir, FileSplit.EXTENSION);
			}
		}

		if (dlg != null)
			dlg.endDialog();
			
		return out;
	}


	/**
		Ask the user if join should be continued when no more split file is found in
		source directory (insert new disk?).
	*/
	protected boolean askContinue(CancelProgressContinueObserver dlg, int i, File fromDir)	{
		if (dlg != null)	{
			String msg = i <= 0 ?
					"Search for file(s) in \""+fromDir+"\"?\n(Medium ready?)" :
					"Search for further file(s) to append in \""+fromDir+"\"?\n(Next medium ready?)";
			return dlg.askContinue(msg);
		}
		return true;
	}
	
	
	
	/**
		Application main
	*/
	public static void main(String [] args)	{
		if (args.length < 1 || args.length > 2)	{
			System.err.println("SYNTAX: java fri.util.file.FileJoin sourceDirectory [targetDirectory]");
			System.err.println("	Joins all .split files found in sourceDirectory to a new file in targetDirectory.");
			System.err.println("	If no targetDirectory is given, file is created in current directory \"user.home\": "+System.getProperty("user.home"));
			System.err.println("	Files must end with \"<number>.split\", e.g. \"xxx.0.split\".");
		}
		else	{
			String src = args[0];
			String tgt = null;
			if (args.length > 1)	{
				tgt = args[1];
			}
			
			CancelProgressContinueObserver dlg = new CancelProgressContinueConsole();
			File srcDir = new File(src);

			if ((srcDir.exists() == false || srcDir.isFile()) && FileSplit.isSplitDir(srcDir) == false)	{
				// write "fri.zip" (target file) instead of "fri.zip.split" (source diretory)
				srcDir = FileSplit.makeSplitDir(srcDir);
			}
			
			try	{
				FileJoin fj = (tgt == null) ?
						new FileJoin(srcDir, dlg) :
						new FileJoin(srcDir, new File(tgt), dlg);
				
				dlg.setNote(tgt != null ? "Target directory is: "+tgt : "");
				boolean del = dlg.askContinue("Delete .split files in \""+src+"\" after joining?");

				fj.join(del);
			}
			catch (IOException e)	{
				e.printStackTrace();
			}
		}
	}
	
}