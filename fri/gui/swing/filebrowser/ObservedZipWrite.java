package fri.gui.swing.filebrowser;

import java.awt.Component;
import javax.swing.*;
import java.io.*;
import java.util.Vector;
import fri.util.FileUtil;
import fri.util.observer.CancelProgressObserver;
import fri.util.os.OS;
import fri.util.zip.*;
import fri.util.io.CopyStream;
import fri.gui.CursorUtil;
import fri.gui.swing.progressdialog.*;

/**
	GUI controlled recursive zipping.
	Filtering is added for folder contents.
*/

public class ObservedZipWrite extends ZipWrite
{
	protected Component parent;
	protected CancelProgressObserver dlg;
	protected String filter = null;
	protected boolean include, showfiles, showhidden;
	private NetNode [] nodes;
	
	protected ObservedZipWrite()	{
	}
	
	public ObservedZipWrite(
		Component parent,
		NetNode [] n,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
		throws Exception
	{
		this.filter = filter;
		this.include = include;
		this.showfiles = showfiles;
		this.showhidden = showhidden;
		
		init(parent, n);
	}

	
	public ObservedZipWrite(Component parent, NetNode [] n)
		throws Exception
	{
		init(parent, n);
	}


	protected void init(Component parent, NetNode [] n)
		throws Exception
	{
		this.parent = parent;
		this.nodes = n;
		
		String [] filenames = new String [n.length];
		for (int i = 0; i < n.length; i++)	{
			filenames[i] = n[i].getFullText();
		}
		
		init(filenames);
		
		File defDest = getDefaultArchive();
		String fileFilter = "*."+FileUtil.getExtension(defDest.getName());
		
		File [] files = FileChooser.showFileDialog(
				"Compress To",
				parent,
				n[0].getRoot(),
				defDest.getName(),	// suggested file name
				new File(defDest.getParent()),
				fileFilter,	// filter
				true);	// single select

		if (files != null) {
			File tgt = files[0];
			
			if (tgt.exists())	{
				int ret = JOptionPane.showConfirmDialog(
					parent,
					"Overwrite "+tgt.getName()+"?",
					"Existing Archive",
					JOptionPane.YES_NO_OPTION);

	     if (ret != JOptionPane.YES_OPTION)
	     	return;
			}
			
			System.err.println("compressing to "+tgt);
			zipNodesTo(tgt);
		}
	}
	

	/** overriding method to make it observable */
	protected void doCopy(InputStream in, long size, OutputStream out)
		throws IOException
	{
		// Copy without closing out-stream, as next entry gets written to it.
		// Do not close in-stream, as CopyStream does this.
		new CopyStream(in, size, out, dlg, false).copy();
	}
	

	
	/** Run zipFilesTo() in background */
	private void zipNodesTo(final File archive)
		throws Exception
	{
		Runnable runnable = new Runnable()	{
			public void run()	{
				try	{
					zipFilesTo(archive);
				}
				catch (Exception e)	{
					e.printStackTrace();
				}
				finally	{
					synchronized(ObservedZipWrite.this)	{
						// synchronize, as thread could finish before variable "dlg" was set
						dlg.endDialog();	// close progress dialog
					}
				}
			}
		};

		Runnable finish = new Runnable()	{
			public void run()	{
				if (dlg.canceled())	{	// delete archive if canceled
					archive.delete();
				}
				NetNode n = (NetNode)NodeLocate.locate(nodes[0].getRoot(), FileUtil.getPathComponents(new File(archive.getParent()), OS.isWindows));
				n.list(true);	// refresh target folder
			}
		};

		startProgressDialog(runnable, finish, getRecursiveSize(), archive);
	}


	protected synchronized void startProgressDialog(Runnable runnable, Runnable finish, long size, File target)	{
		dlg = new CancelProgressDialog(
				parent,
				getProgressDialogTitle(),
				runnable,
				finish,
				size);
	}


	protected String getProgressDialogTitle()	{
		return "Zip";
	}


	/** Set wait cursor while counting bytes. */
	public long getRecursiveSize()	{
		CursorUtil.setWaitCursor(parent);
		try	{
			return super.getRecursiveSize();
		}
		finally	{
			CursorUtil.resetWaitCursor(parent);
		}
	}
	
	
	/** overriding method to filter files */
	protected File [] getChildren(File parent, String [] list)	{
		if (filter == null)
			return super.getChildren(parent, list);
			
		if (list == null)
			return null;
		
		Vector v = new Vector(list.length);
		for (int i = 0; i < list.length; i++)	{
			FilterableFile f = new FilterableFile(parent, list[i]);
			v.addElement(f);
		}
		v = NodeFilter.filter(filter, v, include, showfiles, showhidden);
			
		File [] files = new File [v.size()];
		for (int i = 0; i < v.size(); i++)
			files[i] = (File)v.elementAt(i);
			
		return files;
	}

}