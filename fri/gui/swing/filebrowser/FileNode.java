package fri.gui.swing.filebrowser;

import java.awt.EventQueue;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import java.text.*;
import fri.util.FileUtil;
import fri.gui.swing.undo.*;
import fri.util.file.*;
import fri.util.NumberUtil;
import fri.util.os.OS;
import fri.util.sort.NaturalSortComparator4;

/**
	Ziel:<br>
		Eine Knoten-Klasse fuer Dateien und Verzeichnisse eines Filesystems.
		Saemtliche aud das Filesystem anwendbare Methoden sind hier implementiert.
		Benachrichtigungen an das GUI sind mit "invokeAndWait()" gekapselt.
*/

public class FileNode implements NetNode
{
	private static FileNode root;
	private static String rootDirectory;
	private static FileNode wastebasket = null;
	public static DateFormat dateFormater = DateFormat.getDateTimeInstance(
				DateFormat.SHORT,
				DateFormat.SHORT);	
	private final static String WASTEBASKET_NAME = "DELETED";
	
	// Windows specials
	private static String [] WINDOWS_DRIVES = {
		"A:", "B:", "C:", "D:", "E:", "F:", "G:", "H:", "I:", "J:", "K:", "L:", "M:",
		"N:", "O:", "P:", "Q:", "R:", "S:", "T:", "U:", "V:", "W:", "X:", "Y:", "Z:"
	};
	private static final char WINDOWS_DRIVE_CHAR = ':';
	private static String [] drivelist;
	private static boolean isWindows;

	private static DoListener doListener;

	// default transaction states when no observer is present
	private static boolean moveIsRemove;	// recursively moving to wastebasket
	private static boolean copyIsMove;	// move delegates ONCE to copy
	private static boolean noSubEdits;	// recursive commands are undone and redone in one edit

	// transaction filter when no observer is present
	private static String filterText;	// temporary applyable filter for list()
	private static boolean doInclude;
	private static boolean showFiles, showHidden;
	
	private static Vector drivesShowingDialog = new Vector();
	
	private static NaturalSortComparator4 naturalSortComparator = new NaturalSortComparator4();
	
	
	private boolean inFilteredFolderMove;	// non-static, move delegates to copy, only folders

	private TransactionObserver observer;
	
	protected Vector listeners;
	private int sortFlag = NetNode.SORT_DEFAULT;		
	private boolean isComputer;	// markiert Knoten "Windows-Arbeitsplatz"
	private boolean movePending;
	private String tooltip;
	private boolean isLink;
	protected File file;
	protected String label;
	private String error;
	private int errorCode;
	protected FileNode parent;
	protected Vector children;
	private boolean isLeaf;
	private boolean hidden;
	private boolean isFile;	// platform dependent file definition
	protected long size, modified;
	protected long recursiveSize = -1;
	private String filetype = "", readwrite = "";
	
	private boolean expanded;
	
	
	static	{
		isWindows = OS.isWindows;
		rootDirectory = getRootDirectory();
	}
	
	
	/**
		Construct a file-node from a string that is the display-name.
		Used by constructRoot().
	*/
	private FileNode(Object node)	{
		label = (String)node;
		setFile(new File(label));
		init();
	}

	/**
		Construct a file-node from the display-name and its parent.
		Used by FileNode to allocate new children.
	*/
	private FileNode(FileNode parent, Object node)	{
		this.parent = parent;
		if (parent.isComputer())	{	// WINDOWS only
			setFile(new File(((String) node)+File.separator));	// drive name
		}
		else	{
			setFile(new File(parent.getFile(), (String) node));
		}
		init();
	}

	/** Construct a file-node from parent and an existing File */
	private FileNode(FileNode parent, File file)	{
		this.parent = parent;
		setFile(file);
		init();
	}

	// Construct a file-node from a existing File,
	// do not initialize (temporary node). Internal use
	// for drag and drop - locating a File.
	private FileNode(File file, boolean init)	{
		setFile(file);
		if (init)
			init();
	}


	// Die Eigenschaften des Knotens festlegen
	public void init()	{
		//System.err.println("initing "+file.toString());
		// set display-label from Java-File to be consistent
		File f = getFile();
		String path = f.getPath();   // was toString(), but Java 11 returns different from toString() for drives
		if (path.equals(rootDirectory))	{
			label = path;
			if (rootDirectory.equals(ARTIFICIAL_ROOT))	{
				isComputer = true;
			}
			this.isLeaf = false;
		}
		else	{
			if (path.length() > 1 && path.endsWith(File.separator))
				path = path.substring(0, path.length() - 1);
			
			this.label = path;	// invariant for isDrive()
			if (isDrive())	{
				this.isLeaf = false;
			}
			else	{
				//System.err.println("initing "+file);
				this.label = f.getName();
				this.isLeaf = ! f.isDirectory();
				if (isLeaf)
					isFile = f.isFile();
				this.hidden = f.isHidden();
		
				// Is it a logical link?
				// This test is necessary here as the node could be created
				// as a consequence of recursive copy action.
				try	{
					if (f.getCanonicalPath().equals(f.getAbsolutePath()) == false)	{   // was toString(), but Java 11 returns different from toString() for drives
						//System.err.println("file is link >"+file+"<");
						this.isLink =  true;
					}
				}
				catch (IOException e)	{
					// Bei Windows-Netzlaufwerken gibt es diese Exception
				}		
			}
		}

		// init further informations for sorting
		this.size = f.length();
		this.modified = f.lastModified();
		
		// reset indicator for refreshing file information		
		tooltip = null;
		// if folder, this is not calculated
		recursiveSize = (long)-1;	// will be updated on demand
	}



	private File getFile()	{
		return this.file;
	}

	private void setFile(File file)	{
		this.file = file;
	}
	
	
	private boolean isDrive(String s)	{
		if (drivelist == null)
			return false;
		for (int i = 0; i < drivelist.length; i++)
			if (s.equals(drivelist[i]))
				return true;
		if (isFloppy())
			return true;
		return false;
	}
	
	private boolean isDrive()	{
		// compare whole path as "C:" could be a file on UNIX
		if (drivelist == null)
			return false;
		if (label.indexOf(WINDOWS_DRIVE_CHAR) <= 0)
			return false;			
		String s = (label == null) ? getFile().getPath() : label;
		return isDrive(s);
	}
	
	private int driveIndex(String s)	{
		if (drivelist == null)
			return -1;
		for (int i = 0; i < WINDOWS_DRIVES.length; i++)
			if (s.equals(WINDOWS_DRIVES[i]))	{
				//System.err.println("drive "+i+" is "+s);
				return i;
			}
		return -1;
	}


	private static Vector testWinDrives(boolean testFloppy)	{
		Vector v = new Vector();
		if (isWindows() == false)
			return v;

		/*
		//System.err.println("before listing roots ...");
		File [] roots = File.listRoots();
		for (int i = 0; i < roots.length; i++)	{
			String s = roots[i].getPath();
			if (s.endsWith("\\"))
				s = s.substring(0, s.length() - 1);
			v.add(s);
			//System.err.println("... listing root "+roots[i]);
		}
		//System.err.println("after listing roots: "+v);

		return v;
		*/

		int i = testFloppy ? 0 : 2;	// leave out floppy drives "A:" and "B:"
		if (testFloppy)	// do test floppy and other drives showing a "Insert Disk" dialog
			drivesShowingDialog = new Vector();

		for (; i < WINDOWS_DRIVES.length; i++)	{
			if (drivesShowingDialog.indexOf(WINDOWS_DRIVES[i]) >= 0)
				continue;	// lasts too long, shows dialog
			
			//System.err.println("before listing roots: "+WINDOWS_DRIVES[i]);
			long time1 = System.currentTimeMillis();
			if ((new File(WINDOWS_DRIVES[i]+File.separator)).exists())	{
				v.addElement(WINDOWS_DRIVES[i]);
			}
			long time2 = System.currentTimeMillis();
			//System.err.println("after listing roots ("+(time2 - time1)+" millis needed): "+WINDOWS_DRIVES[i]);
			
			if (WINDOWS_DRIVES[i].equals("C:") == false && time2 - time1 >= 3000)	{	// do not test this drive anymore as it seems to show a dialog
				System.err.println("WARNING: a drive seems to show a dialog, leaving it out until refresh was triggered: "+WINDOWS_DRIVES[i]);
				drivesShowingDialog.addElement(WINDOWS_DRIVES[i]);
			}
		}
		
		return v;
	}

	private static String [] readWinDrives(boolean testFloppy)	{
		Vector drv = testWinDrives(testFloppy);
		if (drv.size() > 0)	{
			drivelist = new String [drv.size()];
			drv.copyInto(drivelist);
		}
		return drivelist;
	}
	
	private boolean isComputer()	{
		return isComputer;
	}
	
	private static boolean isWindows()	{
		//return drivelist != null;
		return isWindows;
	}



	private static String getRootDirectory()	{
		Vector drv = testWinDrives(false);
		if (drv.size() > 0)	{
			return ARTIFICIAL_ROOT;
		}
		else	{
			return FileUtil.rootDirectory();
		}
	}


	/** Test if passed root is a valid filesystem root. Returns the root FileNode when valid. */
	public static FileNode testRoot(String rootName)	{
		if (rootName == null || rootName.length() <= 0)
		    rootName = "/";   // fri_2019-09-17 fixing WINDOWS error when no property file exists 
			
		// Java converts "/" to "\" and lists current drive
		if (isWindows && rootName.equals("/"))	{
			rootName = getRootDirectory();	// make stored properties suitable for double boot
			System.err.println("WINDOWS, root is "+rootName+", taking root "+rootName);
		}

		rootDirectory = rootName;
		System.err.println("root directory is now "+rootName);
		
		FileNode fn = new FileNode(rootName);

		if (fn.listSilent() == null || fn.children.size() <= 0)	{
			System.err.println("bad root from persistence: "+rootName);
			return null;
		}

		return fn;
	}


	/** Factory method to build a root-node for a tree. Root can be null. */
	public static FileNode constructRoot(String rootName)	{
		return FileNode.constructRoot(rootName, null);
	}


	/** Factory method to build a root-node for a tree with a wastebasket. */
	public static FileNode constructRoot(String rootName, String [] waste)	{
		System.err.println("constructRoot "+rootName);
		
		FileNode fn;
		if ((fn = testRoot(rootName)) == null)	{
			throw new IllegalArgumentException("Could not construct root directory: "+rootName);
		}
		
		fn = constructRoot(fn);	
		
		createWastebasket(waste);
		System.err.println("wastebasket >"+(wastebasket == null ? null : wastebasket.getFile().getAbsolutePath())+"<");
		
		return fn;
	}


	/** Package-visible factory method to set an explicit root-node. */
	static FileNode constructRoot(FileNode fn)	{
		FileNode.readWinDrives(false);	// identify Windows for path building method
		
		//System.err.println("root >"+fn.getFile()+"<");
		root = fn;	// remember static root, necessary here for wastebasket construction
		
		return fn;
	}
	


	private FileNode explorePath(File f)	{
		String [] path = FileUtil.getPathComponents(f, isWindows());
		return (FileNode)NodeLocate.locate(root, path);
	}
	
	
	/** construct a mirror of data-net as wastebasket. */
	private static FileNode createWastebasket(String [] waste)	{
		if (waste == null)	{
			FileNode home = exploreHome();
			if (home != null)	{
				System.err.println("home >"+home.getFile()+"<");
				return wastebasket = (FileNode)home.createContainer(WASTEBASKET_NAME, true, false);
			}
			System.err.println("no home found");
		}
		else	{
			return wastebasket = (FileNode)NodeLocate.locate(root, waste);
		}
		return wastebasket = (FileNode)root.createContainer(WASTEBASKET_NAME, true, false);
	}

	public NetNode getWastebasket()	{
		return wastebasket;
	}
	
	
	private static FileNode exploreHome()	{
		File home = new File(FileUtil.homeDirectory());
		if (home.equals(root.getFile()))
			return root;
		return root.explorePath(home);
	}


	// interface NetNode

	/**
		Factory method to build a node for a tree from a drag and drop object.
		@param o File object to construct
		@return node, but is not inited (does not know its label and if it is a leaf).
	*/
	public NetNode construct(Object o)	{
		return new FileNode((File)o, false);
	}

	/** Drag and Drop needs a transferable object.
			@return File object of this node. For process-intern transfers
				this can be the node itself. In this case it is the File object.
	*/
	public Object getObject()	{
		return getFile();
	}


	public NetNode getParent()	{
		return parent;
	}


	public NetNode getRoot()	{
		return root;
	}


	/** Rename a node. Does not overwrite an existing sibling. */
	public NetNode rename(String newname)	{
		System.err.println("rename "+getFile()+" to "+newname);
		
		if (newname.equals(label))
			return null;
			
		// LINUX performs a move, WINDOWS does rename, so check anyway if a sibling with same name exists
		FileNode pnt = (FileNode)getParent();
		for (int i = 0; i < pnt.children.size(); i++)	{
			NetNode child = (NetNode)pnt.children.get(i);
			String name1 = OS.supportsCaseSensitiveFiles() ? child.getLabel() : child.getLabel().toLowerCase();
			String name2 = OS.supportsCaseSensitiveFiles() ? newname : newname.toLowerCase();
			
			if (child != this && name1.equals(name2))	{
				errorCode = NetNode.EXISTS;
				error = "The file \""+child.getLabel()+"\" already exists in that path.";
				return null;
			}
		}

		File newfile = new File(getFile().getParent(), newname);

		String [] oldname = new String [] { label };	// prepare command pattern

		if (renameTo(getFile(), newfile) == false)	{
			return null;
		}
		
		setFile(newfile);	// membervar file neu belegen
		label = getFile().getName();
		
		// if this is wastebasket, it stays in this folder and keeps its properties, no action necessary

		renameAllBufferedChildren();

		fireNodeRenamed();

		addEdit(new RenameCommand(root, oldname, getPathComponents()));

		return this;
	}


	// tell the children File objects about their new path
	private void renameAllBufferedChildren()	{
		if (isLeaf() == false)	{
			for (int i = 0; children != null && i < children.size(); i++)	{
				FileNode fn = (FileNode)children.elementAt(i);
				fn.setFile(new File(getFile(), fn.getFile().getName()));
				//fn.label = fn.file.getName();
				fn.renameAllBufferedChildren();
			}
		}
	}


	private boolean renameTo(File file, File newfile)	{
		//System.err.println("renameTo "+file+" to "+newfile);
		String oldpath = file.getAbsolutePath();   // was toString(), but Java 11 returns different from toString() for drives
		try	{
			file.renameTo(newfile);
		}
		catch (SecurityException e)	{
			error = e.getMessage();
			return false;
		}
		
		// check if new file exists
		if (newfile.exists() == false)	{	// move across drives fails
			error = "Could not rename. File in use?";
			return false;
		}
		
		// check if old file still exists
		File oldfile = new File(oldpath);
		
		if (oldfile.exists())	{	// this seems to happen only on WINDOWS
			// first check if rename is not a move
			String oldParent = oldfile.getParent();
			String newParent = newfile.getParent();
			
			if ((oldParent == null && newParent == null || 
					oldParent != null && newParent != null && newParent.equals(oldParent)))
			{
				// both are in same path
				if (OS.supportsCaseSensitiveFiles() ||	// if either files are case sensitive or lower names are different
						oldfile.getName().toLowerCase().equals(newfile.getName().toLowerCase()) == false)
				{
					error = "\""+newfile.getName()+"\" already exists in that path.";
					errorCode = NetNode.EXISTS;
					return false;
				}
				// else old file AND new file exist as they are the same
			}
			else	{
				error = "Could not rename, \""+oldfile.getName()+"\" still exists.";
				return false;
			}
		}
		
		//System.err.println("renameTo "+file+" -> "+newfile+" succeeded");
		return true;
	}



	/** Move this node to the given destination. */
	public NetNode move(NetNode dest)
		throws Exception
	{
		if (moveAllowed() == false)
			return null;
			
		System.err.println("move "+getFile()+" to "+dest.getFullText());

		canceled();	// throws exception

		// check if something is in the way
		NetNode nn;
		if ((nn = overwriteCheck(dest, false)) != null)	{	// not overwritten
			setMovePending(false);
			return nn;
		}
		
		// now as overwrite conflict is solved it could be a copy or a rename 

		// do copy instead of move if it is a filtered container-move
		if (isFilteredRecursiveFolder())	{
			inFilteredFolderMove = true;
			
			NetNode n = copy(dest);
			
			inFilteredFolderMove = false;
			return n;	// Folder still exists
		}


		// do the move
		progress(1L);	// give a little progress for dialog to show
		
		NetNode n;
		File newfile = getNewFile(dest);
		FileNode wastesave = wastebasket;	// save if wastebasket is moved

		// if move as rename fails
		if (renameTo(getFile(), newfile) == false)	{	// move across drives fails on Windows
			// do move as copy+delete
			FileNode.copyIsMove = true;
			try	{
				n = copy(dest);	// delegate to copy and delete
				if (n != null)	// copy was successful
					delete();	// delete absolutely as this folder was copied
			}
			finally	{
				FileNode.copyIsMove = false;
			}
		}
		else	{	// if rename succeeded
			progress(size - 1L);	// the 1L from before
			
			FileNode parent = (FileNode)(dest.isLeaf() ? dest.getParent() : dest);
			n = new FileNode(parent, newfile);	// this node's new identity
			
			// notify listeners
			parent.fireNodeInserted(n);
			fireNodeDeleted();

			// store action to undo manager
			if (FileNode.moveIsRemove)
				addEdit(new RemoveCommand(root, getPathComponents(), dest.getPathComponents()));
			else
				addEdit(new MoveCommand(root, getPathComponents(), dest.getPathComponents()));
		}

		if (wastesave != null && this.equals(wastesave))	{	// this was wastebasket
			System.err.println("wastebasket has been moved to "+n.getFullText());
			wastebasket = (FileNode)n;	// set wastebasket to new value
		}

		return n;
	}


	// checks if a node would be overwritten, if so, asks the user for overwrite.
	// Returns not null if action should end.
	private NetNode overwriteCheck(NetNode dest, boolean isCopy)
		throws Exception
	{
		if (getNewFile(dest).exists() == false)
			return null;	// no overwrite conflict to solve
			
		// get overwrite candidate NetNode
		NetNode overwriteCandidate = dest.isLeaf() ? dest : (NetNode)NodeLocate.search(dest, getFile().getName());
		if (overwriteCandidate == null)	{	// must be folder, child must be there
			dest.list(true);	// force new listing. FRi TODO: why?
			overwriteCandidate = (NetNode)NodeLocate.search(dest, getFile().getName());
		}

		// consider overwrite
		boolean doOverwrite = FileNode.moveIsRemove ? true	// silent remove to wastebasket
				: observer == null ? false	// default to false if no observer present
				: observer.askOverwrite(	// show a dialog
						getFullText(),
						getToolTipText(),
						overwriteCandidate.getFullText(),
						overwriteCandidate.getToolTipText());

		if (doOverwrite)	{	// move existing node to wastebasket
			overwriteCandidate.remove();	// observer is not set, so no progress, but undo manager gets it
			return null;	// continue, nothing is in the way
			// FRi: TODO what if folder could not be removed, but contained files have been removed?
		}
		else	{	// do not overwrite
			if (isLeaf() || overwriteCandidate.isLeaf())	{	// one of both is a leaf
				error = "Can not overwrite file by folder or folder by file: "+overwriteCandidate;
				errorCode = NetNode.EXISTS;
				progress(size);	// else might never reach length and close
				return overwriteCandidate;	// do not overwrite a file by a folder or vice versa
			}
			else	{	// try to copy children one by one if not overwriting
				Vector v = (Vector)listFiltered(true).clone();

				for (int i = 0; i < v.size(); i++)	{	// rekursives Kommando!
					NetNode n = (NetNode)v.get(i);
					
					n.setObserver(observer, overwriteCandidate.getFullText());
					try	{
						if (isCopy)
							n.copy(overwriteCandidate);
						else
							n.move(overwriteCandidate);
					}
					finally{
						n.unsetObserver();
					}
				}
				// return not overwritten overwrite candidate
				return overwriteCandidate;
			}
		}
	}

	

	private boolean moveAllowed()	{
		if (!isManipulable())	{
			error = "Can not move a drive";
			return false;
		}
		return copyAllowed();
	}
	
	
	private boolean copyAllowed()	{
		if (isLink)	{	// copied link is a empty file that does not mean anything
			error = "Can not copy or move a link";	// possible if createLink() is available
			System.err.println("FEHLER: "+error);
			return false;
		}		
		if (isLeaf() && isFile == false)	{
			error = "Not a normal file";
			return false;
		}
		return true;
	}
	
	
	/** Copy this node to a default copy name.*/
	public NetNode saveCopy()
		throws Exception
	{
		if (copyAllowed() == false)
			return null;
			
		// search for a nonexisting name for new file
		System.err.println("saveCopy "+getFile());
		File newfile;
		int i = 1;
		do	{
			newfile = new File(getFile().getParent(), "Copy"+i+"_"+label);
			i++;
		}
		while (newfile.exists());

		NetNode n;
		
		if (isLeaf() == false)	{
			noSubEdits = true;
			try	{
				n = copyContainer((FileNode)getParent(), newfile.getName());
			}
			finally	{
				noSubEdits = false;
			}
		}
		else	{
			try	{
				new CopyFile(getFile(), newfile, observer).copy();	
			}
			catch (Exception e)	{
				try	{ newfile.delete(); }	catch (Exception ex)	{}
				throw e;
			}
			
			n = new FileNode((FileNode)getParent(), newfile);
			
			((FileNode)getParent()).fireNodeInserted(n);
		}
		
		if (n != null)
			addEdit(new SaveCopyCommand(root, getPathComponents(), n.getPathComponents()));
			
		return n;
	}


	
	/** Copy this node to the given destination. */
	public NetNode copy(NetNode dest)
		throws Exception
	{
		if (copyAllowed() == false)
			return null;
			
		System.err.println("copy "+getFile()+" to "+dest.getFullText());

		// check if something is in the way
		NetNode nn;
		if ((nn = overwriteCheck(dest, true)) != null)	{	// not overwritten
			return nn;
		}

		// nothing is in the way

		// copy directory
		if (isLeaf() == false)	{
			NetNode n = copyContainer((FileNode)dest, getFile().getName());
			return n;
		}

		File newfile = getNewFile(dest);
		
		// copy file
		try	{
			new CopyFile(getFile(), newfile, observer).copy();
		}
		catch (Exception e)	{
			error = e.toString();
			try	{ newfile.delete(); }	catch (Exception ex)	{}
			throw e;
		}
		
		FileNode parent = (FileNode)(dest.isLeaf() ? dest.getParent() : dest);
		FileNode n = new FileNode(parent, newfile);
		
		//System.err.println("copy file, fireNodeInserted "+this);
		parent.fireNodeInserted(n);

		addCopyEdit(n, parent, FileNode.copyIsMove);
		
		return n;
	}


	private NetNode copyContainer(FileNode dest, String name)
		throws Exception
	{
		if (dest.isLeaf())	{
			error = "Copy directory "+label+" can not overwrite file "+dest.toString();
			return null;
		}

		canceled();	// throws exception

		FileNode newcont = (FileNode)dest.createContainer(name, false, inFilteredFolderMove);
		// will be null if already existing
		
		progress(size);
		
		if (newcont != null)	{
			// add a undoable edit command
			addCopyEdit(newcont, dest, inFilteredFolderMove || FileNode.copyIsMove, inFilteredFolderMove);

			// copy all subitems
			if (isLink == false)	{	// symbolische Links nicht rekursiv kopieren: Endlos-Schleife!
				Vector v = listFiltered(true);
				
				for (int i = v.size() - 1; i >= 0; i--)	{	// rekursives Kommando!
					FileNode fn = (FileNode)v.elementAt(i);
					
					if (inFilteredFolderMove)	{
						fn.move(newcont);
					}
					else	{	// no sub-edits may happen as this is recursive. Errors at redo!
						boolean isCopyOrigin = (noSubEdits == false);
						noSubEdits = true;
						fn.setObserver(observer, newcont.getFullText());
						try	{
							fn.copy(newcont);	// recursive call
						}
						catch (Exception e)	{
							newcont.delete();
							throw e;
						}
						finally	{
							fn.unsetObserver();
							if (isCopyOrigin)
								noSubEdits = false;
						}
					}

					canceled();	// throws exception
				}
			}
		}
		
		return newcont;
	}

	
	// Copy command can mean different things as move fails across drives
	// and filtered moves use copy to create their peer folder.
	// A copy that is a move must set the file-time of the created file.
	private void addCopyEdit(
		FileNode dest,
		FileNode destParent,
		boolean setFileTime)
	{
		addCopyEdit(dest, destParent, setFileTime, false);
	}
	
	
	private void addCopyEdit(
		FileNode dest,
		FileNode destParent,
		boolean setFileTime,
		boolean filteredFolderMove)
	{
		if (setFileTime)
			setFileTimeAndAccessTo(dest);
		
		if (noSubEdits == false)	{
			if (FileNode.copyIsMove)	{
				if (FileNode.moveIsRemove)
					addEdit(new RemoveCommand(root, getPathComponents(), destParent.getPathComponents()));
				else
					addEdit(new MoveCommand(root, getPathComponents(), destParent.getPathComponents()));
			}
			else	{
				if (!filteredFolderMove)	// folder is created, not copied!
					addEdit(new CopyCommand(root, getPathComponents(), destParent.getPathComponents()));
			}
		}
	}
	
	
	// set the modification time to that of original
	private void setFileTimeAndAccessTo(FileNode newNode)	{
		if (isReadOnly())
			newNode.getFile().setReadOnly();

		newNode.getFile().setLastModified(getFile().lastModified());
	}
	

	private File getNewFile(NetNode dest)	{
		File newfile;
		if (dest.isLeaf())	{	// is a existing file
			newfile = ((FileNode)dest).getFile();
		}
		else	{	// is a directory
			newfile = new File(((FileNode)dest).getFile(), getFile().getName());
		}
		return newfile;
	}
	


	private File createDefaultFile(String name)	{
		File f;
		int i = 1;
		do	{
			f = new File(getFile(), name+i);
			i++;
		}	while (f.exists());
		return f;
	}
	
	/** create a new node with default name */
	public NetNode createNode()	{
		File f = createDefaultFile("newFile");
		try	{
			if (CreateFile.createFile(f))	{
				NetNode n = new FileNode(this, f);
				
				fireNodeInserted(n);
		
				addEdit(new InsertCommand(root, n.getPathComponents()));
		
				return n;
			}
		}
		catch (Exception e)	{
			error = "Create file: "+e.getMessage();
		}
		return null;
	}

	

	/** create a new container-node with default name */
	public NetNode createContainer()	{
		File f = createDefaultFile("newFolder");
		return createContainer(f, true);
	}
	
	public NetNode createContainer(String name)	{
		return createContainer(name, false);
	}
	
	private NetNode createContainer(String name, boolean takeExisting)	{
		return createContainer(name, takeExisting, true);
	}
	
	private NetNode createContainer(String name, boolean takeExisting, boolean addEdit)	{
		File f = new File(getFile(), name);
		
		if (takeExisting)	{
			FileNode fn = explorePath(f);
			
			if (fn != null)	{
				//System.err.println("taking existing container "+fn.getFullText());
				return fn;
			}
		}
		
		return createContainer(f, addEdit);
	}
	

	private NetNode createContainer(File f, boolean addEdit)	{
		errorCode = 0;
		
		if (f.exists())	{
			errorCode = NetNode.EXISTS;
			error = "Directory exists: "+f.getName();
			return null;
		}
		try	{
			if (CreateFile.createDirectory(f))	{
				NetNode n = new FileNode(this, f);
				
				fireNodeInserted(n);
		
				if (addEdit)	//!createIsCopy)
					addEdit(new InsertCommand(root, n.getPathComponents()));
		
				return n;
			}
			error = "Could not create directory "+f.getAbsolutePath();
		}
		catch (Exception e)	{
			error = "Create directory: "+e.getMessage();
		}

		return null;
	}



	/* begin implementation wastebasket */

	private boolean moveToWastebasket()
		throws Exception
	{
		if (isReadOnly() && askDeleteReadOnly() == false)	{
			progress(size);
			return false;
		}
		
		if (wastebasket == null)	{	// create one
			createWastebasket(null);
		}

		//System.err.println("moveToWastebasket "+getFile());

		FileNode fn = null;
		if ((fn = existsInWastebasket()) != null)	{
			System.err.println("wastebasket already contains "+label+", deleting");
			fn.delete();	// will not warn readonly under wastebasket
		}

		// create a folder under wastebasket according to this' path
		String [] path = getPathComponents();
		path[0] = maskDrive(path[0]);

		FileNode waste = wastebasket;
		for (int i = 0; waste != null && i < path.length - 1; i++)	{
			waste = (FileNode)waste.createContainer(path[i], true, false);
			if (waste == null)
				return false;
		}

		// move node to waste
		FileNode.moveIsRemove = true;
		try	{
			return move(waste) != null;
		}
		finally	{
			FileNode.moveIsRemove = false;
		}
	}


	private String maskDrive(String name)	{
		if (isWindows())
			return name.replace(WINDOWS_DRIVE_CHAR, '_');
		else
			return name;
	}

	
	/** @return true if this node is under the wastebasket:
			it can only be deleted, not removed. */
	public boolean underWastebasket()	{
		//System.err.println("comparing "+file.toString()+" with "+wastebasket.getFile().toString()+File.separator);
		if (wastebasket == null)
			return false;
		return getFullText().startsWith(wastebasket.getFullText());
	}


	private FileNode existsInWastebasket()	{
		String [] path = getPathComponents();
		path[0] = maskDrive(path[0]);
		return (FileNode)NodeLocate.locate(wastebasket, path);
	}

	/* end implementation wastebasket */



	/** Remove this node by moving it to wastebasket */
	public boolean remove()
		throws Exception
	{
		if (!isManipulable())	{
			error = "Can not remove a drive";
			return false;
		}
		
		//System.err.println("remove "+getFile());

		if (wastebasket != null && this.equals(wastebasket))	{
			// remove wastebasket himself
			boolean ret;
			if (ret = delete())
				wastebasket = null;

			return ret;
		}
		
		if (wastebasketIsUnderThis())	{
			System.err.println("creating new wastebasket as it is under this ...");
			createWastebasket(null);	// create default wastebasket
			
			if (wastebasket != null && wastebasketIsUnderThis())	{
				error = "Can not remove to wastebasket as wastebasket is under the removed folder.";
				return false;
			}
		}
		
		if (isLink || underWastebasket())	{
			System.err.println("is link or under wastebasket, deleting now (the link or wastebasket content)");
			return delete();
		}

		return moveToWastebasket();
	}


	private boolean wastebasketIsUnderThis()	{
		if (isLeaf() == false)	{
			if (wastebasket == null)
				return true;	// wastebasket could get created under this folder
			
			return wastebasket.getFullText().startsWith(getFullText());
		}
		return false;
	}


	/** Implements NetNode and returns true if file.canWrite() returns false. */
	public boolean isReadOnly()	{
		return getFile().canWrite() == false;
	}


	/** Delete this node from filesystem without moving it to wastebasket. */
	public boolean delete()
		throws Exception
	{
		//System.err.println("delete "+file);
		boolean ret = true;
		
		if (isReadOnly() &&
				underWastebasket() == false &&
				FileNode.copyIsMove == false &&
				askDeleteReadOnly() == false)
		{
			progress(size);
			return false;
		}
		
		if (isLeaf() == false && emptyContainer() == false)	{
			return false;
		}
		
		try	{
			getFile().delete();
			//System.err.println("deleted "+file);
		}
		catch (SecurityException e)	{
			error = "Delete: "+e.getMessage();
			System.err.println("FEHLER: "+error);
			return false;
		}
		
		if (underWastebasket() == false && FileNode.copyIsMove == false)
			progress(size);
		
		if (getFile().exists())	{
			error = "Not deleted. File In Use?";
			System.err.println("FEHLER: "+error);
			return false;
		}
		
		if (wastebasket != null && this.equals(wastebasket))
			wastebasket = null;
			
		fireNodeDeleted();
		
		return ret;
	}


	// default delete readonly nodes if there is no observer
	private boolean askDeleteReadOnly()
		throws Exception
	{
		return observer == null ||
				observer.askDeleteReadOnly(getFullText(), getToolTipText()) == true;
	}

		
	private boolean emptyContainer()
		throws Exception
	{
		if (isLink)	{
			error = "Can not empty link";
			System.err.println("FEHLER: "+error);
			return false;
		}

		boolean ret = true;

		// list node if never been opened
		listSilent();			

		for (int i = children.size() - 1; i >= 0; i--)	{
			NetNode fn = (NetNode)children.elementAt(i);
			
			fn.setObserver(observer);
			try	{
				ret = fn.delete() && ret;
			}
			finally	{
				fn.unsetObserver();
			}
		}

		setRecursiveSize(-1);
		return ret;
	}



	/** Empty this container recursive. */
	public boolean empty()
		throws Exception
	{
		if (isLeaf())
			return false;

		/*if (isLink)	{
			error = "Can not empty link";
			System.err.println("FEHLER: "+error);
			return false;
		}*/
		
		if (underWastebasket())
			return emptyContainer();

		System.err.println("empty "+file);

		boolean ret = true;
		
		// list node if never been opened
		Vector v = listFiltered(true);
		
		for (int i = v.size() - 1; i >= 0; i--)	{
			FileNode fn = (FileNode)v.elementAt(i);

			fn.setObserver(observer);
			try	{
				if (fn.isFilteredRecursiveFolder())		{
					ret = fn.empty() && ret;
				}
				else	{
					ret = fn.remove() && ret;
				}
			}
			finally	{
				fn.unsetObserver();
			}
		}

		setRecursiveSize(-1);
		return ret;
	}




	/** List all child nodes of filesystem, use buffered list */
	public Vector list()	{
		return listSilent(false, false);
	}

	/**
		List all child nodes of filesystem, refresh buffered list.
		@param rescan if false, the buffered list is used if existing, else the
			buffered list is refreshed or created.
 */
	public Vector list(boolean rescan)	{
		return list(rescan, false);
	}
	
	/**
		List all child nodes of filesystem.
		@param rescan if true, don't return buffered children, do rescan filesystem
		@param scanDrives if true, then test (slow) floppy drives.
	*/
	public Vector list(boolean rescan, boolean scanDrives)	{
		if (isLeaf())	{
			return null;
		}
		if (children == null || rescan == true)	{	// list needs refresh
			listSilent(rescan, scanDrives);
			fireChildrenRefreshed();	// notify all listeners, that net may have changed
		}				
		return children;
	}	


	/** List buffered child nodes without fire event */
	public Vector listSilent()	{
		// no fireChildrenRefreshed
		return listSilent(false);
	}

	/** Refresh and list child nodes without fire event */
	public Vector listSilent(boolean rescan)	{
		// no fireChildrenRefreshed
		return listSilent(rescan, false);
	}


	private Vector listSilent(boolean rescan, boolean scanDrives)	{
		// no fireChildrenRefreshed
		//System.err.println("listSilent() "+file);
		if (isLeaf())	{
			//System.err.println("  is leaf!");
			return null;
		}
		
		//changed = false;
		if (children == null || rescan == true)	{

			synchronized (this)	{

				System.err.println("rescanning "+(isComputer() ? getFile().getName() : getFile().getAbsolutePath()));
				// call medium to list nodes
				String [] names = listDisk(scanDrives);
				
				// delete not existing children
				if (children != null)	{
					for (int i = children.size() - 1; i >= 0; i--)	{
						FileNode fn = (FileNode)children.elementAt(i);
						boolean found = false;
						for (int j = 0; names != null && !found && j < names.length; j++)	{
							if (names[j].equals(fn.getLabel()))
								found = true;
						}
						if (found == false)	{
							fn.listeners = null;
							children.removeElement(fn);
							//changed = true;
						}
					}
				}
				else	{	// allocate array for children
					children = new Vector(names != null ? names.length : 0);
				}
		
				// insert new children
				for (int i = 0; names != null && i < names.length; i++)	{
					FileNode fn = null;
					for (int j = 0; fn == null && j < children.size(); j++)	{
						FileNode fn1 = (FileNode)children.elementAt(j);
						if (names[i].equals(fn1.getLabel()))
							fn = fn1;
					}
					if (fn == null)	{
						fn = new FileNode(this, names[i]);
						children.addElement(fn);
						//changed = true;
					}
					else	{
						//System.err.println("  initing "+fn.getFullText());
						fn.init();	// reset node as it can be changed in size or time ...
					}
				}
				
				setRecursiveSize(-1);	// folder size is not calculated
					
			}	// end synchronized
				
		}	// end if
		
		return children;
	}


	// apply filter to list if filterText is not null
	private Vector listFiltered()	{
		return listFiltered(false);
	}
	
	// apply filter to list if filterText is not null
	private Vector listFiltered(boolean rescan)	{
		listSilent(rescan);

		if (filterText != null && FileNode.moveIsRemove == false)	{
			if (showFiles)
				return NodeFilter.filter(filterText, children, doInclude, showFiles, showHidden);
			else
				return NodeFilter.filterFolders(filterText, children, doInclude, showHidden);
		}
		return children;
	}


	private boolean isFloppy()	{
		if (label.indexOf(WINDOWS_DRIVE_CHAR) <= 0)
			return false;
		int drive = driveIndex(label);
		return drive < 2 && drive >= 0;
	}

	/**
		Is refresh necessary? This method updates leafs automatically.
		@return NetNode.NEEDS_REFRESH, NetNode.NOT_LISTED, etc
	*/
	public int checkForDirty()	{
		if (isLeaf())	{
			init();
			return UP_TO_DATE;
		}
		// check if it is listed or expanded
		if (children == null || isFloppy())	{	// was never listed or do not watch
			return NOT_LISTED;
		}
		if (isExpanded() == false)	{	// currently not expanded
			return NOT_EXPANDED;
		}
		
		String [] list = listDisk(false);
		if (list == null && children.size() > 0)
			return NEEDS_REFRESH;
		
		// compare count of children
		if (list != null && children.size() != list.length)	{
			// check for just explored floppy drive on Windows
			if (isWindows && children.size() > list.length && isComputer())	{
				return checkListForDirty(list, true);
			}
			return NEEDS_REFRESH;
		}
		return checkListForDirty(list, false);
	}
	
		
	private int checkListForDirty(String [] list, boolean floppyTest)	{
		// compare all children by label
		for (int i = 0; i < children.size(); i++)	{
			FileNode fn = (FileNode)children.elementAt(i);
			if (floppyTest == false || fn.isFloppy() == false)	{
				boolean found = false;
				for (int j = 0; found == false && j < list.length; j++)
					if (list[j].equals(fn.getLabel()))
						found = true;
				if (found == false)
					return NEEDS_REFRESH;
			}
			/*
			else
			if (floppyTest)	{	// is floppy in root-node
				String [] floppyList = fn.listDisk(false);
				System.err.println("floppy list is "+floppyList);
				if (floppyList == null)	{
					return NEEDS_REFRESH;
					//System.err.println("	floppy list length = "+floppyList.length);
				}
			}*/
		}
		return UP_TO_DATE;
	}
	
	
	// medium scan
	private String [] listDisk(boolean scanDrives)	{
		String [] names;
		if (isComputer())	{
			names = readWinDrives(scanDrives);
		}
		else	{
			names = getFile().list();
		}
		return names;
	}
	


	/** is node a file (return true) or a directory (return false) */
	public boolean isLeaf()	{
		return isLeaf;
	}

	/** can node be renamed, removed, moved, copied */
	public boolean isManipulable()	{
		if (isComputer() || isDrive())
			return false;
		return true;
	}
	
	public boolean canCreateChildren()	{
		if (isComputer() || isLeaf())
			return false;
		return true;
	}

	/** returns display name in filesystem */
	public String toString()	{
		return label;
	}

	/** returns display name in filesystem */
	public String getLabel()	{
		return toString();
	}


	/** comparing the files of this node and the given */
	public boolean equals(NetNode n)	{
		return getFile().equals(((FileNode)n).getFile());
	}

	
    private int compareStrings(String a, String b)    {
        return naturalSortComparator.compare(a, b);
        //return a.toLowerCase().compareTo(b.toLowerCase());
    }
    
	private int compareNames(FileNode fa, FileNode fb)	{
		String sa = fa.label;
		String sb = fb.label;

		if (fa.isLeaf() != fb.isLeaf())   // one is file, one is directory
    		if (fa.isLeaf())	// files below
    			return 1;	
    		else if (fb.isLeaf())	// folders above
    			return -1;

		return compareStrings(sa, sb);
	}
	
	private int compareExtensions(FileNode fa, FileNode fb)	{
		String sa = fa.label;
		String sb = fb.label;
		int apos = fa.isLeaf() ? sa.lastIndexOf(".") : -1;
		int bpos = fb.isLeaf() ? sb.lastIndexOf(".") : -1;
		if (apos > 0 && bpos > 0)	{	// both have extension, both are files
			String aext = sa.substring(apos + 1);
			String bext = sb.substring(bpos + 1);
            int result = compareStrings(aext, bext);
			if (result == 0)
				return compareNames(fa, fb);
			else
				return result;
		}
		
		if (apos > 0)	// a contains extension, extensions below
			return 1;	
		if (bpos > 0)	// b contains extension
			return -1;

		if (fa.isLeaf() && fb.isLeaf())
			return compareNames(fa, fb);
			
		if (fa.isLeaf())	// one is file: files below
			return 1;	
		if (fb.isLeaf())	// one is file: folders above
			return -1;

		return compareNames(fa, fb);
	}
	
	/** compare nodes according to previously set sort criterium */
	public int compare(Object a, Object b)	{
		//System.err.println("comparing "+a+" with "+b);
		FileNode fa = (FileNode)a;
		FileNode fb = (FileNode)b;
		switch (sortFlag)
		{
			case SORT_BY_NAME: return compareNames(fa, fb);
			case SORT_BY_EXTENSION: return compareExtensions(fa, fb);
			case SORT_BY_TIME:
				long diff = fb.modified - fa.modified;	// newest above
				return (diff > 0L) ? 1 : (diff < 0L) ? -1 : compareNames(fa, fb);
			case SORT_BY_SIZE:
				// if folders never have been calculated, assume maximum
				long asize = fa.isLeaf() ? fa.size : fa.recursiveSize >= 0 ? fa.recursiveSize : Integer.MAX_VALUE;
				long bsize = fb.isLeaf() ? fb.size : fb.recursiveSize >= 0 ? fb.recursiveSize : Integer.MAX_VALUE;
				if (asize != bsize)
					return (int)(bsize - asize);

				if (fa.isLeaf() && fb.isLeaf() || !fa.isLeaf() && !fb.isLeaf())	// same type
					return compareNames(fa, fb);

				if (fa.isLeaf())	// a is file: files below
					return 1;
				if (fb.isLeaf())	// b is file: folders above
					return -1;
				break;
		}
		System.err.println("ERROR: sort flag not valid");
		return 0;
	}


	public void setSortFlag(int sortFlag)	{
		//System.err.println("setSortFlag "+sortFlag);
		this.sortFlag = sortFlag;
	}
	


	/** @return the last happened error. */
	public String getError()	{
		return error;
	}

	/** @return the error code of the last happened error.
			Use NetNode-Constants to identify */
	public int getErrorCode()	{
		return errorCode;
	}



	public boolean getMovePending()	{
		return movePending;
	}

	public void setMovePending(boolean value)	{
		movePending = value;
		fireMovePending();
	}



	public String getInfoText()	{
		return getToolTipText();
	}
	
	public String getFullText()	{
		if (isComputer)
			return "";
		String s = getFile().getAbsolutePath();   // was toString(), but Java 11 returns different from toString() for drives
		if (isLeaf() == false)
			if (s.endsWith(File.separator) == false)
				s = s+File.separator;
		return s;
	}

	public String getFullLinkText()	{
		if (isComputer)
			return "";
		try	{
			return getFile().getCanonicalPath();
		}
		catch (Exception e)	{
			return "error at requesting canonical path";
		}
	}


	/** Returns all path components begining from root as String-array. */
	public String [] getPathComponents()	{
		return FileUtil.getPathComponents(getFile(), isWindows());
	}
	
	public String [] getHomePathComponents()	{
		String s = FileUtil.homeDirectory();
		if (s == null)
			return null;
		return FileUtil.getPathComponents(new File(s), isWindows());
	}
	
	public String [] getWorkPathComponents()	{
		String s = FileUtil.workingDirectory();
		if (s == null)
			return null;
		return FileUtil.getPathComponents(new File(s), isWindows());
	}
	

	// tooltip service function

	/**
		The member-variables modified, size and file must be set before
		calling this method. This is done by init().
		@return a string representaion of essential file information.
	*/
	public String getToolTipText()	{
		if (isComputer)
			return null;

		if (tooltip == null)	{
			//System.err.println("getToolTipText refresh");			
			filetype = isLink ? "Linked " : "";
			filetype += getFile().isHidden() ? "Hidden " : "";
			filetype += isFile ? "File" : isLeaf() ? "Node" : isDrive() ? "Drive" : "Folder";

			readwrite = (getFile().canRead() ? "Read" : "");
			if (readwrite.equals(""))
				readwrite = (getFile().canWrite() ? "Write" : "-");
			else
				readwrite += (getFile().canWrite() ? "/Write" : "Only");

			long size;
			if (recursiveSize >= 0)
				size = recursiveSize;	// if calculated, render recursive size
			else
				size = this.size;
			
			String len = NumberUtil.getFileSizeString(size, isLeaf() || recursiveSizeReady());

			String date = getTime();

			tooltip =
					filetype+"  |  "+
					(len.equals("") ? "" : len+"  |  ")+
					date+"  |  "+
					readwrite;
		}
		return tooltip;
	}


	/**
		Create a undo listener and associate passed button actions with an Undo-Manager.
	*/
	public void createDoListener(DoAction undo, DoAction redo)	{
		if (FileNode.doListener == null)
			FileNode.doListener = new DoListener(undo, redo);
		else
			throw new IllegalStateException("Undo listener already created!");
	}

	/**
		Pass the DoListener to start a transaction.
	*/
	public DoListener getDoListener()	{
		return FileNode.doListener;
	}




	/**
		Called by TreeNode's to get fired events.
	*/
	public void addNetNodeListener(NetNodeListener l)	{
		if (listeners == null)
			listeners = new Vector();
		listeners.addElement(l);
		//System.err.println("  listener "+listeners.size()+" on "+getFullText());
	}

	public void removeNetNodeListener(NetNodeListener l)	{
		if (listeners != null)
			listeners.removeElement(l);
	}

	public Vector getNetNodeListeners()	{
		if (listeners != null && listeners.size() > 0)
			return listeners;
		return null;
	}
	
	

	// fire events to NetNodeListeners

	private void fireNetNodeEvent(Runnable runnable)	{
	    if (EventQueue.isDispatchThread() == false)	{
			try	{
				EventQueue.invokeAndWait(runnable);
			}
			catch (Exception e)	{
				e.printStackTrace();
				System.err.println("FEHLER: invokeAndWait in fireNetNodeEvent");
			}
	    }
	    else	{
			//System.err.println("running fire event in dispatch thread");
			runnable.run();
	    }
	}


	private void fireChildrenRefreshed()	{	// called by the container-node == parent
		if (listeners == null)
			return;

		//System.err.println("fireChildrenRefreshed in "+file);		
		final Vector lsnr = listeners;
		final Vector chldr = children;
		Runnable runnable = new Runnable()	{
			public void run()	{
				for (int i = lsnr.size() - 1; i >= 0; i--)	{
					NetNodeListener l = (NetNodeListener)lsnr.elementAt(i);
					l.childrenRefreshed(chldr);
				}
			}
		};
		fireNetNodeEvent(runnable);
	}
	
	private void fireNodeRenamed()	{	// called by the node
		//System.err.println("fireNodeRenamed "+file);
		final Vector lsnr = listeners;
		Runnable runnable = new Runnable()	{
			public void run()	{
				for (int i = lsnr.size() - 1; i >= 0; i--)	{
					NetNodeListener l = (NetNodeListener)lsnr.elementAt(i);
					l.nodeRenamed();
				}
			}
		};
		fireNetNodeEvent(runnable);
	}

	private void fireNodeDeleted()	{	// called by the node
		((FileNode)getParent()).children.removeElement(this);
		//System.err.println("removeElement "+this+" from "+parent.getFullText()+" = "+ret);

		if (listeners == null)
			return;	// temporary allocated node

		//System.err.println("fireNodeDeleted "+file);
		final Vector lsnr = listeners;
		Runnable runnable = new Runnable()	{
			public void run()	{
				for (int i = lsnr.size() - 1; i >= 0; i--)	{
					NetNodeListener l = (NetNodeListener)lsnr.elementAt(i);
					l.nodeDeleted();
				}
			}
		};
		fireNetNodeEvent(runnable);
	}

	private void fireNodeInserted(NetNode newnode)	{	// called by the parent
		if (children == null)
			children = new Vector();
		children.addElement(newnode);
				
		if (listeners == null)
			return;	// temporary allocated node

		//System.err.println("fireNodeInserted in "+file+" "+newnode.getFullText());
		final Vector lsnr = listeners;
		final NetNode nn = newnode;
		Runnable runnable = new Runnable()	{
			public void run()	{
				for (int i = lsnr.size() - 1; i >= 0; i--)	{
					NetNodeListener l = (NetNodeListener)lsnr.elementAt(i);
					l.nodeInserted(nn);
				}
			}
		};
		fireNetNodeEvent(runnable);
	}

	private void fireMovePending()	{
		if (listeners == null)
			return;	// temporary allocated node

		final Vector lsnr = listeners;
		Runnable runnable = new Runnable()	{
			public void run()	{
				for (int i = lsnr.size() - 1; i >= 0; i--)	{
					NetNodeListener l = (NetNodeListener)lsnr.elementAt(i);
					l.movePending();
				}
			}
		};
		fireNetNodeEvent(runnable);
	}
	
	

	// build command pattern for undoable edit commands
	
	private void addEdit(FileCommand edit)	{
		if (getDoListener() != null)
			getDoListener().addEdit(edit);
	}
	
	
	/**
		Apply filter to list() to execute recursively filtering commands
		As all instances have references to system of FileNode, reset filter
		immediately after command execution!
	*/
	public void setListFiltered(
		String filterText,
		boolean doInclude,
		boolean showFiles,
		boolean showHidden)
	{
		//Thread.dumpStack();
		FileNode.filterText = filterText;
		FileNode.doInclude = doInclude;
		FileNode.showFiles = showFiles;
		FileNode.showHidden = showHidden;
	}

	/**
		As the filter is global (static), it must be reset,
		else other instances would be affected.
	*/
	public void resetListFiltered()	{
		//Thread.dumpStack();
		FileNode.filterText = null;
	}

	private boolean isFilteredRecursiveFolder()	{
		return FileNode.filterText != null &&
			!isLeaf() &&
			FileNode.showFiles &&
			FileNode.moveIsRemove == false;
	}



	/**
		To watch long lasting transactions, a controller is passed
	*/
	public void setObserver(TransactionObserver observer)	{
		setObserver(observer, null);
	}

	/**
		To watch long lasting transactions, a controller is passed
		@param observer object receiving progress information
		@param target copy- or move-target as a string to be used in setNote().
	*/
	public void setObserver(TransactionObserver observer, String target)	{
		this.observer = observer;
		
		if (observer != null)	{
			if (target == null)	{
				observer.setNote(getFullText());
			}
			else	{
				observer.setNote(getLabel()+"  to  "+target);
			}
		}
	}


	/** Enable garbage collecting of observers. */
	public void unsetObserver()	{
		this.observer = null;
	}
	
	
	
	private void canceled()
		throws Exception
	{
		if (observer != null && observer.canceled())
			throw new Exception("user canceled");
	}
	
	private void progress(long size)	{
		if (observer != null)	{
			//Thread.dumpStack();
			//System.err.println("FileNode progress = "+size+" in "+getFullText());
			observer.progress(size);
		}
	}
	
	
	/**
		Calculate the size of a folder recursively for the observer, that
		needs to know the length of the transaction to init his progress bar.
	*/
	public long getRecursiveSize()	{
		long size = this.size;
		
		if (isLeaf() == false)	{
			if (recursiveSizeReady())	// do not calculate every time
				return recursiveSize;
			
			Vector v = listFiltered();
			
			for (int i = 0; v != null && i < v.size(); i++)	{
				NetNode n = (NetNode)v.elementAt(i);
				size += n.getRecursiveSize();
			}
			
			if (FileNode.filterText == null)	// if there was set no filter
				setRecursiveSize(size);	// buffer recursive size
		}
		
		return size;
	}

	/**
		Extern objects could loop recursively through FileNodes and
		calculate the size by themselves. By this method they can set
		the calculated size or reset it to -1.
	*/
	public void setRecursiveSize(long recursiveSize)	{
		this.recursiveSize = recursiveSize;
		tooltip = null;	// force new rendering
	}
	
	/**
		Do not calculate a second time, if the recursive size has been
		calculated and stored in this node.
	*/
	public boolean recursiveSizeReady()	{
		return recursiveSize >= 0;
	}


	/** Returns size of this node. If folder, it is 0 on Windows. */
	public long getSize()	{
		return size;
	}


	/** Returns time of last modification as a formated string. */
	public String getTime()	{
		return dateFormater.format(new Date(modified));
	}
	public long getModified()	{
		return modified;
	}
	
	
	/** Set a new file date */
	public Long setModified(String time)	{
		try	{
			Date d = dateFormater.parse(time);
			long t = d.getTime();
			if (getFile().setLastModified(t))
				return new Long(t);
		}
		catch (ParseException e)	{
			//e.printStackTrace();
		}
		return null;
	}

	/** Return access rights string representation. */
	public String getReadWriteAccess()	{
		if (tooltip == null)
			getToolTipText();
		return readwrite;
	}

	/** Returns "folder", "hidden file", "linked file" etc. */
	public String getType()	{
		if (tooltip == null)
			getToolTipText();
		return filetype;
	}

	/** Returns true if the path of this file does not match the canconical path. */
	public boolean isLink()	{
		return isLink;
	}


	/** Returns true if this file is platform-hidden. */
	public boolean isHiddenNode()	{
		return hidden;
	}


	/** set the node expanded or collapsed. This brings performance for update-thread. */
	public void setExpanded(boolean expanded)	{
		//System.err.println("set expanded "+expanded+" "+getFullText());
		this.expanded = expanded;
	}
	
	public boolean isExpanded()	{
		return expanded;
	}

}
