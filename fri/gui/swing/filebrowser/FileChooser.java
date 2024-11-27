package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.awt.BorderLayout;
import java.util.Hashtable;
import java.io.File;
import javax.swing.*;
import java.awt.event.*;
import fri.util.FileUtil;
import fri.util.file.ValidFilename;
import fri.util.os.OS;

/**
	FileChooser factory. 
	The arguments for <code>showDialog()</code> call are as follows:
	<ul>
		<li>appName - e.g. "Save", this appears on OK-Button. Can be null.
			This name is used to cache dialogs and reactivate them when called again
			with same appName.
			</li>
		<li>parent - Component contained in Window on which dialog should be shown.
			Not null.
			</li>
		<li>root - the root node for the treeview. Not null.
			</li>
		<li>suggestedFileName - If a non-existent file should be created, this is
			the suggestion name for it. Can be null.
			</li>
		<li>pathToSelect - The path or file that should be selected and opened.
			Not null. This can not be the file to create, but its path.
			</li>
		<li>forcePathToSelect - Should the passed path be selected even if a
			former state of dialog has another selection? Normally not.
			</li>
		<li>filter - File name filter, e.g. "*.jar". Can be null.
			</li>
		<li>chooseFiles - If false, it is a directory only chooser, if true a file only chooser,
			if null both can be chosen. The option "showFiles" will set to false for
			directory choosers. Mind that "filter" filters even directories if showFiles
			is false!
			</li>
		<li>singleSelect - Just one file or directory can be selected and chosen.
			</li>
	</ul>
	If singleSelect is true, the selected file is element(0) of returned array.
	If the return value is null, the dialog was canceled.
*/

public abstract class FileChooser
{
	private static Hashtable cache = new Hashtable();

	public static File [] showDirectoryDialog(
		String appName,
		Component parent,
		NetNode root,
		File pathToSelect,
		boolean singleSelect)
	{
		return showDialog(
				appName,
				parent,
				root,
				null,	// suggested filename
				pathToSelect,	// path to select
				false,	// force path
				null,	// filter
				Boolean.FALSE,	// chooseFiles
				singleSelect);
	}

	public static File [] showFileDialog(
		String appName,
		Component parent,
		NetNode root,
		File pathToSelect,
		boolean singleSelect)
	{
		return showDialog(appName,
				parent,
				root,
				null,
				pathToSelect,
				false,
				null,
				Boolean.TRUE,
				singleSelect);
	}

	public static File [] showFileDialog(
		String appName,
		Component parent,
		NetNode root,
		String suggestedFileName,
		File pathToSelect,
		boolean singleSelect)
	{
		return showFileDialog(
				appName,
				parent,
				root,
				suggestedFileName,
				pathToSelect,
				null,	// filter
				singleSelect);
	}

	public static File [] showFileDialog(
		String appName,
		Component parent,
		NetNode root,
		String suggestedFileName,
		File pathToSelect,
		String filter,
		boolean singleSelect)
	{
		return showDialog(appName,
				parent,
				root,
				suggestedFileName,
				pathToSelect,
				true,
				filter, 
				Boolean.TRUE,
				singleSelect);
	}
	
	public static File [] showDialog(
		String appName,
		Component parent,
		NetNode root,
		String suggestedFileName,
		File pathToSelect,
		boolean forcePathToSelect,
		String filter,
		Boolean chooseFiles,
		final boolean singleSelect)
	{
		//System.err.println("showing FileChooser on Component "+parent);
		final boolean chooseOnlyFiles = chooseFiles == null ? false : chooseFiles.booleanValue();
		final boolean chooseOnlyDirs  = chooseFiles == null ? false : ! chooseFiles.booleanValue();
		boolean showFiles = chooseOnlyDirs == false;
			
		// set a default application name
		if (appName == null)
			appName = "OK";

		// try to get a buffered TreePanel
		TreePanel tp = getBufferedTreePanel(appName);

		String [] pathNames = FileUtil.getPathComponents(pathToSelect, OS.isWindows);
		
		// if not there, build a new one
		if (tp == null)	{
			PathPersistent pp = null;
			// filename for persistent pathes
			String propfile = FileBrowser.configDir+File.separator+ValidFilename.correctFilename("FileChooser_"+appName+".properties");
			
			if (forcePathToSelect == false && new File(propfile).canRead())	{	// pathToSelect was only a initializer
				pp = new PathPersistent(propfile);
			}
			
			// ensure that there is a object
			if (pp == null)	{	// construct a PathPersistent from scratch when null
				pp = new PathPersistent(propfile, pathNames, showFiles);
			}

			// construct a TreePanel
			tp = new TreePanel(root, pp, filter);

			if (singleSelect)
				tp.setSingleSelect();
			
			// make the path listen to tree opens and selection	
			pp.setTree(tp.getTree());

			cache.put(appName, tp);
		}
		else	{	// set arguments
			// set selected path
			if (forcePathToSelect)	{
				String [][] sarr = new String [1][];
				sarr[0] = pathNames;
				tp.expandiere(sarr, null);
			}
			
			// set filter
			if (filter != null)	{
				tp.setFilter(filter);
				tp.getEditController().refilter();
			}
		}
		
		// set the suggested filename, that might not yet exist
		if (suggestedFileName != null)	{
			tp.setSuggestedFilename(suggestedFileName);
		}

		// construct a JOptionPane dialog (OK/Cancel) and trigger
		// the OK-Callback to check results.
		
		final String [] opts = new String [2];
		opts[0] = appName;
		opts[1] = "Cancel";
		
		final TreePanel ftp = tp;

		JOptionPane pane = new JOptionPane(
				tp,
				JOptionPane.PLAIN_MESSAGE,	// message
				JOptionPane.OK_CANCEL_OPTION,	// button method
				null,	//icon
				opts,
				opts[0])
		{
			// ensure that either directories or files are chosen
			public void setValue(Object value)	{
				if (value != null && value.equals(opts[0]) &&
						(chooseOnlyFiles || chooseOnlyDirs))
				{	// OK pressed
					File [] files = getChosenFiles(singleSelect, ftp);
					
					for (int i = 0; i < files.length; i++)	{
						if (files[i] == null ||
								files[i].isDirectory() && chooseOnlyFiles ||
								!files[i].isDirectory() && chooseOnlyDirs)
						{
							System.err.println("  not accepted because of >"+files[i]+"< is directory = "+(files[i] != null ? Boolean.valueOf(files[i].isDirectory()) : null));
							return;
						}
					}
				}
				super.setValue(value);
			}
		};

		pane.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 15));	// shows asymetric!

		JDialog dlg = pane.createDialog(parent, chooseOnlyDirs ? "Directory Chooser" : "File Chooser");

		// show dialog
		dlg.setResizable(true);
		dlg.setVisible(true);
		
		// if OK was pressed, return Selection from TreePanel.
		Object o = pane.getValue();
		if (o != null && o.equals(opts[0]))	{
			File [] files = getChosenFiles(singleSelect, tp);
			tp.save();
			return files;
		}
		
		return null;
	}
	

	private static File [] getChosenFiles(boolean singleSelect, TreePanel tp)	{
		File [] files;
		if (singleSelect)	{
			File f = tp.getSelectedFile();
			files = new File [1];
			files[0] = f;
		}
		else	{
			files = tp.getSelectedFiles();
		}
		return files;
	}
	
		
	private static TreePanel getBufferedTreePanel(String name)	{
		Object o;
		if (name != null && (o = cache.get(name)) != null)	{
			return (TreePanel)o;
		}
		return null;
	}
	
	
	
	
	// test main
	public static void main(String [] args)	{
		JFrame f = new JFrame("FileChooser Test");
		final JLabel lbl = new JLabel("D:\\Projekte\\fri", SwingConstants.CENTER);
		f.getContentPane().add(lbl, BorderLayout.CENTER);
		final JButton btn = new JButton("Choose File");
		f.getContentPane().add(btn, BorderLayout.SOUTH);

		btn.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				String s = lbl.getText();
				File [] files = FileChooser.showDialog(
						"Uncompress",
						btn,
						FileNode.constructRoot((String)null),
						"SuggestedName.txt",
						new File(s),
						false,
						"*.jar",	// filter
						Boolean.TRUE,	// showFiles
						true);	// singleSelect
				lbl.setText(files != null ? files[0].getAbsolutePath() : "");
			}
		});
		f.setSize(200, 200);
		f.setVisible(true);
	}
	
}