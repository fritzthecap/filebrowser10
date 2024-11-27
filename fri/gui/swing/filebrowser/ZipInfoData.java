package fri.gui.swing.filebrowser;

import java.io.*;
import java.util.*;
import javax.swing.*;
import fri.util.NumberUtil;
import fri.util.FileUtil;
import fri.util.os.OS;
import fri.util.file.archive.*;
import fri.gui.CursorUtil;
import fri.gui.swing.progressdialog.*;
import fri.gui.swing.yestoalldialog.*;

/**
	ZIP info-data and ZIP extract methods. Constructor makes a valid TableModel Vector.
*/

public class ZipInfoData extends FileTableData
{
	public String error = null;
	private Archive zip = null;
	private NetNode node;
	private final JFrame frame;
	private CancelProgressDialog observer = null;
	final static String tempPath = System.getProperty("java.io.tmpdir")+File.separator+"fri";
	
	static	{	// create an own directory in TEMP
		File f = new File(tempPath);
		if (f.exists() && f.isDirectory() == false)
			f.delete();	// no problem in TEMP
		if (f.exists() == false)
			f.mkdirs();
		System.err.println("Temporary path for extracting entries is: "+tempPath+", created: "+f.exists());
	}


	public ZipInfoData(
		JFrame frame,
		NetNode node,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
	{
		super();
		
		this.frame = frame;
		this.node = node;
		
		Enumeration e = null;
		
		try	{
			zip = ArchiveFactory.newArchive((File)node.getObject());
			System.err.println("InfoData: zip openeded");
			e = zip.archiveEntries();
			System.err.println("InfoData: zip enumerated");

			// zip entries get listed like in file, no sorting
			for (; e.hasMoreElements(); )	{
				ArchiveEntry z = (ArchiveEntry)e.nextElement();
				Vector row = buildRow(
					z,
					z.isDirectory(),
					z.isDirectory() ? "" : getName(z.getName()),
					z.isDirectory() ? "Folder" : "File",
					z.isDirectory() ? z.getName() : getPath(z.getName()),
					Long.valueOf(z.getSize()),
					Long.valueOf(z.getTime()),
					z.getInfo());
				addElement(row);
			}
		}
		catch (Exception ex)	{
			ex.printStackTrace();
			error(frame, ex.toString());
			return;
		}
		
		if (zip != null && zip.getError() != null)	{
			error(frame, zip.getError());
			return;
		}
	}
	
	
	private void error(JFrame f, String msg)	{
		error = msg;
		JOptionPane.showMessageDialog(
				f,
				msg,
				"Error",
				JOptionPane.ERROR_MESSAGE);
	}
	
	
	/** Close zip file */
	public void close()	{
		if (zip != null)
			zip.close();
	}	
	
	
	public ArchiveEntry getZipAtRow(int row)	{
		return (ArchiveEntry)files.elementAt(row);
	}

	
	/** Global size is ready calculated, set sizes of contained folders */
	public long calculateZipFolderSize(int row)	{
		long size = 0L;
		String name = getZipAtRow(row).getName();	// full qualified path
		String sep = "/";
		if (name.endsWith(sep) == false)
			name = name+sep;
		for (int i = 0; i < size(); i++)	{
			ArchiveEntry z = getZipAtRow(i);
			String s = z.getName();
			if (s.startsWith(name))
				size += z.getSize();
		}
		return size;
	}

	

	/**
		Extract a given archive file, in calling Thread.
		If the passed OverwriteDialog is not null, it will be activated
		for all existing files that would be overwritten.
		@param rows line numbers of JTable
		@param target String describing the target directory
		@param withPath true if the relative path from archive should be build
		@param yestoall dialog for overwriting existing files
	*/
	public Vector extractEntries(
		int [] rows,
		String target,
		OverwriteLauncher yestoall)
		throws Exception
	{
		Vector v = new Vector();
		int ignoredSizes = 0;

		for (int i = 0; i < rows.length; i++)	{
			ArchiveEntry z = getZipAtRow(rows[i]);

			// figure out the resulting file
			File f = new File(target, z.getName());

			// check if file exists
			if (yestoall != null && observer != null && f.exists() && f.isDirectory() == false)	{
				// do not change focus when drag&drop is working!
				int erg = yestoall.show(
						observer.getDialog(),
						z.getName(),
						NumberUtil.getFileSizeString(z.getSize()),
						f.toString(),
						NumberUtil.getFileSizeString(f.length()));

				if (erg == YesToAllDialog.NO)	{	// NO_TO_ALL is the same
					ignoredSizes += z.getSize();
					continue;	// do not add entry to list
				}
			}

			// now add entry to list
			v.add(z);
			//System.err.println("  will extract entry at "+rows[i]+", name = "+z.getName());
		}

		ArchiveEntry [] entries = null;
		boolean doAll = (v.size() == size());	// extract all if selection count is entry count
		
		if (doAll == false)	{	// if extract only selected entries, name them
			entries = new ArchiveEntry[v.size()];
			v.copyInto(entries);
		}
		// else pass null to extract method to extract all entries

		if (observer != null && ignoredSizes > 0)	{
			observer.progress(ignoredSizes);
		}

		Hashtable h = zip.extractEntries(new File(target), entries, observer);
		
		if (zip.getError() != null)	{
			errorFromThread("Warning", zip.getError(), JOptionPane.ERROR_MESSAGE);
		}
		
		if (h == null)
			return null;
		
		v = new Vector(h.size());
		for (Enumeration e = h.elements(); e.hasMoreElements(); )	{
			v.add(e.nextElement());
		}
		
		return v;
	}


	/** Extract all archive files in a background Thread. */
	public void extractAll(String target)	{
		int [] iarr = new int [size()];
		for (int i = 0; i < iarr.length; i++)
			iarr[i] = i;
			
		extractEntries(iarr, target);
	}


	/** Extract selected archive files in a background Thread. */
	public void extractEntries(final int [] iarr, final String target)	{
		long size = 0L;
		for (int i = 0; i < iarr.length; i++)	{
			ArchiveEntry z = getZipAtRow(iarr[i]);
			size += z.getSize();
		}
		
		Runnable runnable = new Runnable()	{
			public void run()	{
				OverwriteLauncher yestoall = new OverwriteLauncher();
				try	{
					CursorUtil.setWaitCursor(frame);
					extractEntries(iarr, target, yestoall);
				}
				catch (UserCancelException e)	{
					errorFromThread("Information", "Nothing extracted.", JOptionPane.INFORMATION_MESSAGE);
				}
				catch (final Exception e)	{
					e.printStackTrace();
					errorFromThread("Error", e.toString(), JOptionPane.ERROR_MESSAGE);
				}
				finally	{
					CursorUtil.resetWaitCursor(frame);
					observer.endDialog();
				}
			}
		};
		
		Runnable finish = new Runnable()	{
			public void run()	{
				// refresh target folder
				NetNode n = (NetNode)NodeLocate.locate(node.getRoot(), FileUtil.getPathComponents(new File(target), OS.isWindows));
				n.list(true);
			}
		};
		
		observer = new CancelProgressDialog(
				frame,
				"Extract",
				runnable,
				finish,
				size);
	}



	private void errorFromThread(final String title, final String msg, final int type)	{
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				JOptionPane.showMessageDialog(frame, msg, title, type);
			}
		});
	}
	
}