package fri.gui.swing.ftpbrowser;

import java.util.*;
import java.util.Calendar;
import javax.swing.tree.*;
import fri.util.ftp.*;
import fri.util.NumberUtil;

/**
	The FTP server tree node.
	Contains FTP specific child list logic and retrieval of propeties (size, time).
	
	@author Fritz Ritzberger
*/

public class FtpServerTreeNode extends AbstractTreeNode
{
	public static boolean doSlowButSafeListing = false;
	public static boolean activeFtp = false;
	
	private ObservableFtpClient ftpClient;
	private String system;
	private Vector unsortedList;
	private Vector longList;
	private long bufferedSize = -1L;
	private String bufferedDate;


	/** File root constructor. */
	public FtpServerTreeNode(ObservableFtpClient ftpClient)	{
		super("/");
		
		this.ftpClient = ftpClient;
		
		try	{
			this.system = ftpClient.system();
		}
		catch (Exception e)	{
			ProgressAndErrorReporter.error(e);
		}
	}

	/** File child constructor. */
	private FtpServerTreeNode(String child)	{
		super(child);
	}
	
	
	public fri.gui.mvc.model.swing.AbstractTreeNode createTreeNode(Object name)	{
		return new FtpServerTreeNode((String)name);
	}
	

	public ObservableFtpClient getFtpClient()	{
		return ((FtpServerTreeNode)getRoot()).ftpClient;
	}
	
	private String getSystem()	{
		return ((FtpServerTreeNode)getRoot()).system;
	}

		
	/** Overridden to set is-directory property when new node gets inserted. */
	public void insert(MutableTreeNode newChild, int childIndex)	{
		try	{	// need to know if new node is a directory, but it is not in longList
			longList = null;
			FtpServerTreeNode n = (FtpServerTreeNode)newChild;
			n.isDirectory = Boolean.valueOf(getFtpClient().isDirectory(getAbsolutePath()+"/"+n.toString()));
			// do it the hard way as the parent listing will not contain the new node
		}
		catch (Exception e)	{
			ProgressAndErrorReporter.error(e);
		}
		
		super.insert(newChild, childIndex);
	}
	

	private boolean isVMS()	{
		return getSystem() != null && getSystem().startsWith("VMS");
	}
	
	protected boolean isDirectory()	{
		if (doSlowButSafeListing)	{
			try	{
				return getFtpClient().isDirectory(getAbsolutePath());
			}
			catch (Exception e)	{
				ProgressAndErrorReporter.error(e);
			}
		}
		else
		if (isVMS())	{	// VMS VAX server
			return getUserObject().toString().toLowerCase().endsWith(".dir");
		}
		else	{	// UNIX, Windows
			FtpServerTreeNode pnt = (FtpServerTreeNode)getParent();
			Vector shortList = pnt.unsortedList;
			Vector longList = pnt.getLongListing();
			
			int i = shortList != null ? shortList.indexOf(getUserObject()) : -1;
			if (i >= 0 && i < longList.size())	{
				return longList.get(i).toString().toLowerCase().startsWith("d");
			}
			else	{
				getFtpClient().getLog().println("Could not locate child name in parent list: "+getUserObject()+": "+longList);
			}
		}
		return true;	// newly created directory
	}


	private Vector getLongListing()	{
		if (longList == null)	{
			String listing = "";
			
			try	{
				getFtpClient().chdir(getAbsolutePath());
				listing = getFtpClient().listFiles();
				//System.err.println("long directory listing of "+getUserObject()+" is: \n"+listing);
				//System.err.println("unsorted child list is: "+unsortedList);
				getFtpClient().chdir(getRootName());
			}
			catch (Exception e)	{
			  System.err.println("getLongListing() in "+getAbsolutePath()+" threw "+e);
			}
			
			longList = FtpStringUtil.getLongListAsLines(listing);
		}
		return longList;
	}
		

	public String getAbsolutePath()	{
		if (isRoot())
			return getUserObject().toString();
			
		TreeNode [] path = getPath();
		String root = getRootName();
		String p = root.equals("/") ? "" : root;
		
		for (int i = 1; i < path.length; i++)	{
			FtpServerTreeNode n = (FtpServerTreeNode)path[i];
			String part = n.getUserObject().toString();
			
			if (isVMS() && part.toLowerCase().endsWith(".dir"))
				part = part.substring(0, part.length() - ".dir".length());
				
			p = p+"/"+part;
		}
		
		return p;
	}
	
	protected void list()	{
		try	{
			String dir = getAbsolutePath();
			
			getFtpClient().chdir(dir);
			String [] sarr = getFtpClient().listNames();
			getFtpClient().chdir(getRootName());

			for (int i = 0; sarr != null && i < sarr.length; i++)	{
				if (unsortedList == null)
					unsortedList = new Vector(sarr.length);
				unsortedList.add(sarr[i]);
			}

			for (int i = 0; sarr != null && i < sarr.length; i++)	{
				add(createTreeNode(sarr[i]));
			}
			
			if (children != null)
				children = sortChildren(children);
		}
		catch (Exception e)	{
			ProgressAndErrorReporter.error(e);
		}
	}

	public String getRootName()	{
		return ((DefaultMutableTreeNode)getRoot()).getUserObject().toString();
	}


	/** Returns  displayable String about the file: size, modification date. */
	public String getFileInfo()	{
		return FileInfo.getFileInfo(NumberUtil.getFileSizeString(getBufferedLength()), getBufferedDate());
	}
						
	/** Always returns false. */
	public boolean isLink()	{
		return false;
	}

	/** Returns the recursive size (un-observed) of this node. */
	public long getRecursiveSize()	{
		try	{
			if (getAllowsChildren())
				return getFtpClient().getDownloadDirectorySize(getAbsolutePath(), true);
			else
				return getFtpClient().length(getAbsolutePath());
		}
		catch (Exception e)	{
			ProgressAndErrorReporter.error(e);
			return 0L;
		}
	}

	/** Returns 0 for a directory and the length for a file. Uses the buffered size if present. */
	private long getBufferedLength()	{
		if (bufferedSize < 0L)	{
			if (getAllowsChildren() == false)
				return bufferedSize = getRecursiveSize();
			return 0L;
		}
		return bufferedSize;
	}

	/** Returns the buffered modify date if present, else requests and buffers it. */
	private String getBufferedDate()	{
		if (bufferedDate == null)	{
			try	{
				System.err.println("retrieving date of: "+this);
				bufferedDate = getFtpClient().lastModified(getAbsolutePath());
				System.err.println("retrieved date is: "+bufferedDate);
				
				if (bufferedDate.length() >= 14)	{	// seems to be default FTP format
					try	{
						String year = bufferedDate.substring(0, 4);
						String month = bufferedDate.substring(4, 6);
						String day = bufferedDate.substring(6, 8);
						String hour = bufferedDate.substring(8, 10);
						String min = bufferedDate.substring(10, 12);
						String sec = bufferedDate.substring(12, 14);
						
						Calendar c = Calendar.getInstance();
						c.set(Calendar.YEAR, Integer.parseInt(year));
						c.set(Calendar.MONTH, Integer.parseInt(month) - 1);
						c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
						c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
						c.set(Calendar.MINUTE, Integer.parseInt(min));
						c.set(Calendar.SECOND, Integer.parseInt(sec));
						c.set(Calendar.MILLISECOND, 0);
						
						bufferedDate = FileInfo.dateFormater.format(c.getTime());
					}
					catch (Exception e)	{
					}
				}
			}
			catch (Exception e)	{
				//ProgressAndErrorReporter.error(e);	// VMS does not support "mdtm" FTP command
				bufferedDate = "";
			}
		}
		return bufferedDate;
	}

}
