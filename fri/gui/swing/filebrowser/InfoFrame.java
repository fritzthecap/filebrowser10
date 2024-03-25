package fri.gui.swing.filebrowser;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.File;
import java.util.Hashtable;
import fri.util.javastart.*;
import fri.util.FileUtil;
import fri.util.NumberUtil;
import fri.util.os.OS;
import fri.util.file.archive.ArchiveFactory;
import fri.gui.CursorUtil;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.text.ClipableJTextField;
import fri.gui.swing.IconUtil;

/**
	Target: Show information about a node that can be file, folder,
		drive, zip or jar archive etc.<br>
	Responsibilities: Calculate folder sizes in background thread.
		Handle a possible filter for size calculations.
		Let ZIP files extract. one or more or all.<br>
	Behaviour: Renders container contents in a table.
*/

public class InfoFrame extends JFrame implements
	Runnable,
	KeyListener,
	InfoRenderer,
	NetNodeListener
{
	private static Icon img = UIManager.getIcon("OptionPane.informationIcon");
	private NetNode node = null;	// if one node is rendered
	private NetNode commonParentNode = null;
	private NetNode [] nodes = null;	// if more than one node is rendered
	private String filter = null;
	private boolean include;
	private boolean showfiles, showhidden;
	private JLabel lbSize, lbFolders, lbFiles;
	private long size = 0L, files = 0L, folders = 0L;
	private InfoTable table = null;
	private boolean interrupted = false;
	private Thread thread;		
	private String pack = null;	// optional package name for class files
	private TreeEditController tc;
	private JTextField nodeTextField;
	private boolean isArchive = false;
	private InfoTableDndListener dndLsnr;
	private boolean first = true;


	private InfoFrame(NetNode [] nodes, TreeEditController tc)	{
		setTitleBar(nodes);
		this.tc = tc;
		this.nodes = nodes;
		storeParent(nodes);
	}
	
	/** Info-Dialog without Filter */
	public InfoFrame(TreeEditController tc, NetNode [] nodes)	{
		this(nodes, tc);
		init();
	}
	
	/** Info-Dialog without Filter, force archive view. */
	public InfoFrame(TreeEditController tc, NetNode [] nodes, boolean forceArchive)	{
		this(nodes, tc);
		this.isArchive = forceArchive;
		init();
	}
	
	
	/** Info-Dialog with a Filter to be applied to folder-contents. */
	public InfoFrame(
		TreeEditController tc,
		NetNode [] nodes,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
	{
		setTitleBar(nodes);
		this.tc = tc;
		this.nodes = nodes;
		storeParent(nodes);
		this.filter = filter;
		this.include = include;
		this.showfiles = showfiles;
		this.showhidden = showhidden;
		init();
	}
	
	
	/** render new list of files in this window */
	public void setFiles(File [] files)	{
		NetNode [] nodes = new NetNode [files.length];

		for (int i = 0; i < files.length; i++)	{
			nodes[i] = NodeLocate.fileToNetNode(
					(NetNode)tc.getRoot().getUserObject(),
					FileUtil.getPathComponents(files[i], OS.isWindows));
		}
		
		setTitleBar(nodes);
		
		NetNode p = getValidParentNode();
		if (p != null)
			p.removeNetNodeListener(this);
			
		this.node = null;
		this.nodes = nodes;
		commonParentNode = null;
		storeParent(nodes);
		
		init();
	}
	
	
	private void setTitleBar(NetNode [] nodes)	{
		setTitle(nodes.length == 1 ? nodes[0].getLabel() : "File Information");
	}
	


	/** Return currently selected files, for Drag&Drop */
	public File [] getSelectedFileArray()	{
		return table.getSelectedFileArray();
	}
	

	/** receive dropped files in this folder */
	public boolean receiveDroppedFiles(File [] files, boolean dragCopy)	{
		//System.err.println("InfoFrame.receiveDroppedFiles");
		boolean externDrag = !tc.areThereDraggedNodes() && !dndLsnr.areThereDraggedNodes();
		DefaultMutableTreeNode [] draggedNodes = new DefaultMutableTreeNode [files.length];
		
		for (int i = 0; i < files.length; i++)	{
			BufferedTreeNode b = BufferedTreeNode.fileToBufferedTreeNode(files[i], tc.getRoot());
			draggedNodes[i] = b;
		}
		
		// check for impossible actions on a hierarchical tree: move to descendant
		DefaultMutableTreeNode dropNode = table.getFolder();
		
		for (int i = 0; i < draggedNodes.length; i++)	{
			if (draggedNodes[i].isNodeDescendant(dropNode) || tc.isIn(dropNode, draggedNodes[i]))	{
				draggedNodes = null;
				return false;
			}
		}
		
		if (tc.getUseDropMenu())	{ /*&& false JDK1.3beta Bug when other Component gets focus*/
			tc.showDragNDropPopup(
					new MouseEvent(table.getJTable(), 0, 0, 0, tc.getMousePoint().x, tc.getMousePoint().y, 0, false),
					table.getFolder(),
					draggedNodes,
					dragCopy);
		}
		else	{
			tc.finishDrag(dropNode, draggedNodes, dragCopy);
		}
			
		System.err.println("InfoFrame.receiveDroppedFiles return "+externDrag);
		return externDrag;
		//return true;
	}
	

	public void setMousePoint(Point p)	{
		tc.setMousePoint(p, table.getJTable());
	}

	

	private void storeParent(NetNode [] nodes)	{
		if (nodes.length == 1)	{
			this.node = nodes[0];
		}
	}
	
	
	private void init()	{
		System.err.println("InfoFrame.init");
		IconUtil.setFrameIcon(this, GuiApplication.getApplicationIconURL());

		CursorUtil.setWaitCursor(this);
		
		try	{
			//setDoSize(false);	// setting my own size, not persistent
	
			this.size = this.files = this.folders = 0L;
			NetNode p = getValidParentNode();
			if (p != null)
				p.removeNetNodeListener(this);
	
			//boolean tableRemoved = table != null;
			Container c = getContentPane();
			c.removeAll();
			if (table != null)	{
				table.close();
				table = null;
			}
			
			c.setLayout(new BorderLayout());
				
			String label = null;
			if (node != null)
				label = node.getLabel().toLowerCase();
			
			String [][] props;
	
			pack = null;
			if (label != null && FileExtensions.isJavaClass(label) != null)	{
				// show package name in title
				Classfile.clear();
				pack = Classfile.getPackageName(node.getFullText());
				if (Classfile.error != null)
					pack = Classfile.error;
				else
				if (Classfile.notSure)
					pack = null;	// maybe made of filename
			}
	
			isArchive = label != null && (isArchive || ArchiveFactory.isArchive((File)node.getObject()));
			
			if (node != null && node.isLeaf() && !isArchive)	{
				// show properties
				props = new String [][]	{
					{ "Path", node.getFullText() },
					{ "Size", "" },
					{ "Modified", node.getTime() },
					{ "Access", node.getReadWriteAccess() },
				};
				node.addNetNodeListener(this);
			}
			else	{	// folder
				//tableRemoved = false;
				if (isArchive)	{
					table = new ZipInfoTable(
							this,
							tc,
							node,
							filter,
							include,
							showfiles,
							showhidden);
				}
				else	{
					table = new InfoTable(
							this,
							tc,
							nodes,
							filter,
							include,
							showfiles,
							showhidden);
				}
				
				String sz = "Size";
				String flds = "Folders";
				String fils = "Files";
				if (filter != null)	{
					String flt = "'"+filter+"'";
					String inc = (include ? " incl. " : " excl. ");
					if (!showfiles)	{
						flds = flds+inc+flt;
					}
					else	{
						sz = sz+inc+flt;
						fils = fils+inc+flt;
					}
				}
				props = new String [][]	{
					{ "Path", getCommonPath() },
					{ sz, "0 (working ...)" },
					{ flds, "" },
					{ fils, "" },
					{ "Modified", getCommonTime() },
					{ "Access", getCommonAccess() },
				};
			}
			
			JPanel p0 = new JPanel(new BorderLayout());
			
			JLabel icon = new JLabel(img);
			icon.setVerticalAlignment(SwingConstants.CENTER);
			icon.setHorizontalAlignment(SwingConstants.CENTER);
			icon.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
	
			p0.add(icon, BorderLayout.WEST);
			JPanel panel = buildPanel(props, node != null ? node.getType() : "");
			p0.add(panel, BorderLayout.CENTER);
					
			// if table exists, make a split pane
			if (table != null)	{
				JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
				sp.setTopComponent(p0);
				p0.setMinimumSize(new Dimension());
				sp.setBottomComponent(table);
				c.add(sp, BorderLayout.CENTER);
			}
			else	{
				c.add(p0, BorderLayout.CENTER);
			}
			
			new InfoPanelDndListener(panel, this);
			new InfoPanelDndListener(nodeTextField, this);
			new InfoPanelDndListener(icon, this);
			new InfoPanelDndListener(this, this);
			
			if (table != null)
				dndLsnr = new InfoTableDndListener(table.getJTable(), this);
				
			if (dndLsnr != null && table != null)	{
				dndLsnr.setActive(table.getFolder() != null);
				((InfoDataTable)table.getJTable()).setDndListener(dndLsnr);
			}
			
			if (first)	{
				first = false;
				
				new GeometryManager(this, false).show();	// do no sizing as table would take all place (JDK 1.3)
				
				setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						close();
					}
					public void windowActivated(WindowEvent e) {
						if (table != null)
							table.setSelectedListLine();
					}
				});	
			}
			else	{
				pack();
			}
	
			thread = new Thread(this);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}
	
	
	private String getCommonPath()	{
		commonParentNode = getCommonPath(nodes);
		commonParentNode.addNetNodeListener(this);
		String s = commonParentNode.getFullText();
		if (commonParentNode == node)
			commonParentNode = null;	// is used as flag in NetNodeListener!
		return s;
	}
	
	
	public static final NetNode getCommonPath(NetNode [] nodes)	{
		if (nodes.length == 1)
			return nodes[0];
		
		Vector [] lists = new Vector [nodes.length];
		
		for (int i = 0; i < nodes.length; i++)	{
			Vector v = new Vector();
			NetNode pnt = nodes[i].getParent(), prevPnt = null;
			while (pnt != null && pnt != prevPnt)	{
				prevPnt = pnt;
				v.add(0, pnt);
				pnt = pnt.getParent();
			}
			lists[i] = v;
			//System.err.println("  NetNode["+i+"] path array = "+v);
		}
		
		boolean done = false;
		int level = 0;
		NetNode common = nodes[0];

		for (; !done; level++)	{
			String lbl1 = null;
			NetNode node = null;
			
			for (int i = 0; i < lists.length; i++)	{
				Vector v = lists[i];
				
				if (level >= v.size())	{
					done = true;
				}
				else	{
					node = (NetNode)v.get(level);
					String lbl2 = node.getLabel();
					if (lbl1 == null)	{	// init compare string with first coming path part
						lbl1 = lbl2;
					}
					else	{	// compare this to first path part
						if (lbl1.equals(lbl2) == false)
							done = true;
					}
				}
			}
			
			if (done == false)
				common = node;
		}
		
		return common;
	}

	
	private String getCommonTime()	{
		if (node != null)
			return node.getTime();
			
		long min = Long.MAX_VALUE, max = 0L;
		int minIndex = 0, maxIndex = 0;
		for (int i = 0; i < nodes.length; i++)	{
			long time = nodes[i].getModified();
			if (time > max)	{
				maxIndex = i;
				max = time;
			}
			else
			if (time < min)	{
				minIndex = i;
				min = time;
			}
		}
		return nodes[minIndex].getTime()+"  -  "+nodes[maxIndex].getTime();
	}
	
	
	private String getCommonAccess()	{
		if (node != null)
			return node.getReadWriteAccess();
		String access = nodes[0].getReadWriteAccess();
		for (int i = 0; i < nodes.length; i++)	{
			if (access.equals(nodes[i].getReadWriteAccess()) == false)
				return "";
		}
		return access;
	}


	private JPanel buildPanel(String [][] props, String type)	{
		int len = props.length +
				(node != null && node.isLink() ? 1 : 0) +
				(pack != null ? 1 : 0);
				
		// name panel
		JPanel p1 = new JPanel(new GridLayout(len, 1));
		p1.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 4));
		// value panel
		JPanel p2 = new JPanel(new GridLayout(len, 1));
		p2.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		
		// create GUI
		for (int i = 0; i < props.length; i++)	{
			p1.add(new JLabel(props[i][0]));
				
			if (props[i][0].startsWith("Path"))	{
				JTextField tf = new ClipableJTextField(props[i][1]);
				tf.setEditable(false);
				tf.addKeyListener(this);
				nodeTextField = tf;
				p2.add(tf);
				if (node != null && node.isLink())	{
					p1.add(new JLabel("Link To"));
					tf = new ClipableJTextField(node.getFullLinkText());
					tf.setEditable(false);
					p2.add(tf);
				}
				if (pack != null)	{	// .class or .jar
					if (node != null && node.getLabel().toLowerCase().endsWith(".jar"))	{
						p1.add(new JLabel("Main-Class"));	// jar
					}
					else	{	// class
						p1.add(new JLabel("Package"));
					}
					tf = new ClipableJTextField(pack);
					tf.setEditable(false);
					p2.add(tf);
				}
			}
			else	{
				JLabel l2;
				p2.add(l2 = new JLabel(props[i][1]));
				
				if (props[i][0].startsWith("Size"))
					lbSize = l2;
				else
				if (props[i][0].startsWith("Folders"))
					lbFolders = l2;
				else
				if (props[i][0].startsWith("Files"))
					lbFiles = l2;
			}
		}
		JPanel p = new JPanel(new BorderLayout());
		p.add(p1, BorderLayout.WEST);
		p.add(p2, BorderLayout.CENTER);
		p.setBorder(BorderFactory.createTitledBorder(type));
		return p;
	}


	private void close()	{
		System.err.println("InfoFrame.close");
		if (table != null)
			table.close();
			
		interrupted = true;
		
		NetNode p = getValidParentNode();
		if (p != null)
			p.removeNetNodeListener(this);
			
		//setVisible(false);
		dispose();
	}
	



	
	// interface Runnable
	
	public void run()	{
		System.err.println("run() starts");
		//Thread.dumpStack();
		Thread.yield();
		for (int i = 0; i < nodes.length; i++)	{
			NetNode node = nodes[i];
			System.err.println("calculating size of "+node);
			getRecursiveSize((File)node.getObject(), node);
		}
		setSize(true);
		System.err.println("run() ended");
	}



	private long getRecursiveSize(File f)	{
		return getRecursiveSize(f, null);
	}
	
	
	// do not use NetNodes: do not fill memory with unneeded FileNodes
	private long getRecursiveSize(File f, NetNode nonManipulable)	{
		//System.err.println("getRecursiveSize "+f);
		boolean leaf;
		if (nonManipulable != null && nonManipulable.isLeaf() == false)	// Windows root!
			leaf = false;
		else
			leaf = ! f.isDirectory();
			
		long size = (leaf || node != null && nonManipulable != node) ? f.length() : 0L;
		
		addSize(size, (leaf || node != null && nonManipulable == node) ? 0L : 1L, leaf ? 1L : 0L);
		
		if (leaf)	{
			//System.err.println("Leaf "+f+" returns size "+size);
			return size;
		}
			
		Vector v = new Vector();
		if (nonManipulable != null)	{	// first node or drive
			v = nonManipulable.listSilent();
		}
		else	{
			String [] sarr = f.list();
			for (int i = 0; sarr != null && i < sarr.length; i++)	{
				FilterableFile child = new FilterableFile(f, sarr[i]);
				v.addElement(child);
			}
		}
		
		// empty folder?
		if (v.size() <= 0)	{
			//System.err.println("Empty folder "+f+" returns size "+size);
			return size;
		}
		
		// filter the list if filter is defined
		if (filter != null)	{
			if (showfiles == false)
				v = NodeFilter.filterFolders(filter, v, include, showhidden);
			else
				v = NodeFilter.filter(filter, v, include, showfiles, showhidden);
		}
		
		// calculate size of all children
		for (int i = 0; i < v.size(); i++)	{
			if (interrupted)	{
				return size;
			}
			
			Filterable o = (Filterable)v.elementAt(i);
			//System.err.println("getting size of "+o);
			NetNode nn = null;
			long currSize = 0L;
			if (o instanceof File)	{
				currSize = getRecursiveSize((File)o);
				//System.err.println("File "+o+" returns size "+currSize);
			}
			else	{
				nn = (NetNode)o;
				if (nn.isManipulable() == false)	// list a drive by NetNode
					currSize = getRecursiveSize((File)nn.getObject(), nn);
				else	// list by File to avoid allocating NetNodes
					currSize = getRecursiveSize((File)nn.getObject());
				//System.err.println("NetNode "+nn+" returns size "+currSize);
			}

			size += currSize;
			
			// set the size of subfolders on first level to display
			if (nn != null && nn.isLeaf() == false)	{
				//System.err.println("setting size "+currSize+" to "+nn);
				setSubFolderSize(nn, currSize);
			}
		}
		
		if (node == null && nonManipulable != null)
			setSubFolderSize(nonManipulable, size);
					
		return size;
	}
	
	
	
	private int count = 0;
	
	private void addSize(long add, long folders, long files)	{
		this.size += add;

		this.folders += folders;
		this.files += files;
		if (add >= 1000000 || count > 100)	{
			setSize(false);
			count = 0;
		}
		else
			count++;
	}


	private void setSize(final boolean ready)	{
		if (interrupted)
			return;

		Hashtable hash = null;
		if (ready && isArchive)	{	// calculate in background thread
			hash = table.calculateZipFolderSizes();
		}
		final Hashtable fhash = hash;

		// this goes from thread to GUI, so invoke later
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				if (interrupted)
					return;

				String disp;
				boolean labelsDone = false;
				if (ready)	{
					if (node != null && filter == null)	// if unfiltered calculation
						node.setRecursiveSize(size);	// tell the file node how big he is
						
					String len = NumberUtil.getFileSizeString(size);
					String exact = NumberUtil.printNumber(size)+" Byte";
					if (exact.equals(len))
						disp = exact;
					else
						disp = exact+" = "+len;
						
					lbSize.setText(disp);
					
					if (fhash != null)
						labelsDone = table.setZipFolderSizes(fhash, lbFiles, lbFolders);
						// set sizes in foreground thread
				}
				else	{
					disp = NumberUtil.printNumber(size)+"  (working ...)";
					lbSize.setText(disp);
				}
				
				if (labelsDone == false)	{
					if (folders > 0 && lbFolders != null)
						lbFolders.setText(NumberUtil.printNumber(folders));
					if (files > 0 && lbFiles != null)
						lbFiles.setText(NumberUtil.printNumber(files));
				}
			}
		});
	}


	private void setSubFolderSize(final NetNode n, final long size)	{
		if (filter == null)
			n.setRecursiveSize(size);
		
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				table.setSubFolderSize(n, size);
			}
		});
	}




	// interface KeyListener: dispose dialog on escape
		
	public void keyPressed(KeyEvent e)	{
		switch(e.getKeyCode())	{
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_ESCAPE:
				System.err.println("key pressed: escape or return");
				close();
				break;
		}
	}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	
	
	// interface InfoRenderer
	
	public JFrame getFrame()	{
		return this;
	}	
	public JTable getTable()	{
		return table.getJTable();
	}	


	// interface NetNodeListener

	public void childrenRefreshed(Vector list)	{
		if (commonParentNode != null)
			return;
		init();
	}
		
	public void nodeRenamed()	{
		NetNode p = getValidParentNode();
		if (p != null)	{
			nodeTextField.setText(p.getFullText());
		}
	}

	public void nodeDeleted()	{
		close();
	}
	
	public void nodeInserted(Object child)	{
		if (commonParentNode != null)
			return;
		System.err.println("nodeInserted");
		NetNode n = (NetNode)child;
		if (table != null)	{
			try	{
				InfoTableModel model = (InfoTableModel)table.getModel();				
				model.addRow(n);
			}
			catch (ClassCastException e)	{
			}
		}
	}
	
	public void movePending()	{
		NetNode p = getValidParentNode();
		if (p != null)
			setEnabledRecursive(getContentPane(), !p.getMovePending());
	}

	private NetNode getValidParentNode()	{
		if (node != null)
			return node;
		if (commonParentNode != null)
			return commonParentNode;
		return null;
	}
	
	private void setEnabledRecursive(Container c, boolean enable)	{
		Component [] comps = c.getComponents();
		for (int i = 0; i < comps.length; i++)	{
			if (comps[i] instanceof JComponent)	{
				((JComponent)comps[i]).setEnabled(enable);
			}
			if (comps[i] instanceof Container)	{
				setEnabledRecursive((Container)comps[i], enable);
			}
		}
	}
	
}