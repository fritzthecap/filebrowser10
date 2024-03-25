package fri.gui.swing.filebrowser;

import java.awt.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.io.File;
import fri.gui.swing.dnd.*;
import fri.util.FileUtil;
import fri.util.os.OS;
import fri.gui.swing.dnd.JavaFileList;


	// interface DndPerformer, Drag and Drop jdk1.2

public class TreeMouseListenerJDK12 implements
	DndPerformer,
	MouseListener,
	MouseMotionListener,
	ActionListener	// Timer
{
	public final static int AUTOSCROLL_PERCENT = 10;
	private static Object [] intransfer = null;
	private static DefaultMutableTreeNode [] draggedNodes;
	private DefaultMutableTreeNode dropNode;
	private JTree tree;
	private TreeEditController tc;
	private boolean dragCopy;
	private boolean useDropMenu = true;
	public static boolean globalUseDropMenu = true;
	
	private Point currPoint;
	private Timer timer = null;
	private JViewport port;
	private int incX, incY;
	private int direction;	// scroll direction
	private int percent = AUTOSCROLL_PERCENT;		// scroll space = percent of viewport height

	private DefaultTreeModel model;
	
	
	
	public TreeMouseListenerJDK12(
		JTree tree,
		DefaultTreeModel model,
		JViewport port,
		TreeEditController tc)
	{
		this.tree = tree;
		this.model = model;
		this.tc = tc;
		this.port = port;
		new DndListener(this, tree, DndPerformer.COPY_OR_MOVE);
		tree.addMouseListener(this);
		tree.addMouseMotionListener(this);
		timer = new Timer(50, this);	// for autoscrolling
	}


	public void setAutoscrollSpeed(int value)	{
		// percent value 1->30, timer interval 100->3
		percent = value;
		timer.stop();
		int i = 1000 / (percent * 10);
		//System.err.println("autoscroll timer interval "+i+", page scroll percent "+percent);
		timer = new Timer(i, this);	// for autoscrolling
	}
	

	public boolean areThereDraggedNodes()	{
		//System.err.println("areThereDraggedNodes: "+(draggedNodes != null));
		return draggedNodes != null;
	}
	
	public void setUseDropMenu(boolean useit)	{
		useDropMenu = useit;
	}
		
	public boolean getUseDropMenu()	{
		return useDropMenu && globalUseDropMenu;
	}
	
		
	// interface DndPerformer, Drag and Drop jdk1.2

	public Transferable sendTransferable()	{
		intransfer = null;
		TreePath [] tp = tree.getSelectionPaths();
		if (tp == null)
			return null;
			
		if (tp.length > 0)	{
			intransfer = new Object [tp.length];
			draggedNodes = new DefaultMutableTreeNode [tp.length];
			for (int i = 0; i < tp.length; i++)	{
				draggedNodes[i] = (DefaultMutableTreeNode)tp[i].getLastPathComponent();
				if (draggedNodes[i].isRoot())	{
					System.err.println("root cannot be dragged");
					draggedNodes = null;
					return null;
				}
				NetNode n = (NetNode)draggedNodes[i].getUserObject();
				if (n.isManipulable() == false)	{
					System.err.println("node not editable, cannot be dragged");
					draggedNodes = null;
					return null;
				}
				intransfer[i] = n.getObject();
			}
		}
		if (intransfer == null)	{
			System.err.println("FEHLER: begin drag, no selection");
			draggedNodes = null;
			return null;
		}
		
		tree.setEditable(false);	// gegen staendiges Editieren ...

		return new JavaFileList(Arrays.asList(intransfer));
	}


	public boolean dragOver(Point p)	{
		//System.err.println("drag over "+p);
		currPoint = p;
		tc.setMousePoint(p, tree);
		return true;	// do not reject any location
	}


	public boolean receiveTransferable(Object data, int action, Point p)	{
		stopAutoscrolling();
		
		System.err.println("receiveTransferable, drop target is "+dropNode);

		if (action == DndPerformer.COPY)
			dragCopy = true;
		else
			dragCopy = false;
		
		boolean externDrag = false;
		if (draggedNodes == null)	{	// must be extern drag, as draggedNodes is static
			externDrag = true;	
			List fileList;
			try	{
				fileList = (List)data;
			}
			catch (ClassCastException e)	{
				return handleAliens(data);
			}
			Iterator iterator = fileList.iterator();
			draggedNodes = new DefaultMutableTreeNode [fileList.size()];
			for (int i = 0; iterator.hasNext(); i++) {
				File f;
				try	{
					f = (File)iterator.next();
				}
				catch (ClassCastException e)	{
					return handleUnlocalizable(fileList);
				}
				
				String [] path = FileUtil.getPathComponents(f, OS.isWindows);
				// localize file in tree and add to draggedNodes
				DefaultMutableTreeNode d = tc.getRoot().localizeNode(path);
				if (d == null)	{	// file was not found
					return handleUnlocalizable(fileList);
				}
				draggedNodes[i] = d;
			}
		}
		
		// check for impossible actions on a hierarchical tree: move to descendant
		for (int i = 0; i < draggedNodes.length; i++)	{
			if (draggedNodes[i].isNodeDescendant(dropNode) ||
					tc.isIn(dropNode, draggedNodes[i]))	{
				draggedNodes = null;
				return false;
			}
		}
	
		if (getUseDropMenu())	{
			showDragNDropPopup(p, dropNode);
		}
		else	{
			finishDrag(dropNode);
		}
			
		System.err.println("receiveTransferable returns "+externDrag);
		return externDrag;	// intern move does everything, extern must receive true
		//return true;
		//return false;
	}


	private boolean handleAliens(Object data)	{
		System.err.println("FEHLER: handleAliens, data is "+data);
		intransfer = null;
		draggedNodes = null;
		return false;
	}


	private boolean handleUnlocalizable(List list)	{
		System.err.println("FEHLER: handleUnlocalizable, list "+list);
		intransfer = null;
		draggedNodes = null;
		return false;
	}


	
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		// gets called at drop() before receiveTransferable()
		dropNode = getTreeContainerFromPoint(p);
		if (dropNode == null)
			return null;
		return DataFlavor.javaFileListFlavor;
	}


	public void actionCanceled()	{
		//System.err.println("action canceled");
		dataCopied();
	}
	
	public void dataCopied()	{
		//System.err.println("data copied");
		draggedNodes = null;
		intransfer = null;
	}
	
	public void dataMoved()	{
		System.err.println("dataMoved called ...");
		if (false && draggedNodes != null)	{
			System.err.println("data moved, remove objects ...");
			DefaultMutableTreeNode d = null;
			for (int i = 0; i < draggedNodes.length; i++)	{
				System.err.println("   object "+draggedNodes[i]);
				d = tc.selectionAfterRemove(draggedNodes[i]);
				model.removeNodeFromParent(draggedNodes[i]);
			}
			if (d != null)
				tree.setSelectionPath(new TreePath(d.getPath()));
				
			draggedNodes = null;
		}
		intransfer = null;
		System.err.println("dataMoved ended ...");
	}



	// DnD jdk1.2 helpers
	
	private void finishDrag(final DefaultMutableTreeNode d)	{
		tc.finishDrag(d, draggedNodes, dragCopy);
		intransfer = null;
		draggedNodes = null;
	}

	private void showDragNDropPopup(Point p, DefaultMutableTreeNode d)	{
		tc.showDragNDropPopup(
				new MouseEvent(tree, 0, 0, 0, p.x, p.y, 0, false),
				d,
				draggedNodes,
				dragCopy);
		intransfer = null;
		draggedNodes = null;
	}

	
	private DefaultMutableTreeNode getTreeContainerFromPoint(Point p)	{
		if (tree.getRowForLocation(p.x, p.y) < 0)
			return null;
		TreePath curPath = tree.getPathForLocation(p.x, p.y);
		DefaultMutableTreeNode d = (DefaultMutableTreeNode)curPath.getLastPathComponent();
		NetNode n = (NetNode)d.getUserObject();	// only node knows that it is a container
		if (n.isLeaf())	{	// search container of leaf
			d = (DefaultMutableTreeNode)d.getParent();
		}
		return d;
	}


	public void startAutoscrolling()	{
		if (currPoint == null)
			return;
			
		//System.err.println("startAutoscrolling at "+currPoint);
		Rectangle vr = port.getViewRect();
		//System.err.println("view rect     "+vr);
		Point pos = port.getViewPosition();
		currPoint.x -= pos.x;
		currPoint.y -= pos.y;
		//System.err.println("relative           at "+currPoint);
		
		incX = vr.width  * percent / 100;
		incY = vr.height * percent / 100;
		
		direction = 0;

		if (currPoint.y >= vr.height - 5)
			direction = 4;	// down
		else
		if (currPoint.y < 5)
			direction = 2;	// up
		else
		if (currPoint.x > vr.width - 5)
			direction = 1;	// right
		else
		if (currPoint.x < 5)
			direction = 3;	// left

		//System.err.println("startAutoscrolling direction = "+direction);
		timer.start();
	}


	public void stopAutoscrolling()	{
		//System.err.println("stopAutoscrolling");
		timer.stop();
	}



	// interface ActionListener
	
	public void actionPerformed(ActionEvent e)	{
		//System.err.println("timer scrolls ...");
		Rectangle vr = port.getViewRect();
		Rectangle tb = tree.getBounds();
		Point pos = port.getViewPosition();
		//System.err.println("  position        "+pos);
		//System.err.println("  view rect       "+vr);
				
		if (direction == 1)	{	// right
			int diff = tb.width - vr.width;
			if (pos.x >= diff)	{
				stopAutoscrolling();
				return;
			}
			pos.x += Math.min(incX, diff);
		}
		else
		if (direction == 2)	{	// up
			if (pos.y <= 0)	{
				stopAutoscrolling();
				return;
			}
			int diff = tb.height - vr.height;
			pos.y -= Math.min(incY, diff);
		}
		else
		if (direction == 3)	{	// left
			if (pos.x <= 0)	{
				stopAutoscrolling();
				return;
			}
			int diff = tb.width - vr.width;
			pos.x -= Math.min(incX, diff);
		}
		else
		if (direction == 4)	{	// down
			int diff = tb.height - vr.height;
			if (pos.y >= diff)	{
				stopAutoscrolling();
				return;
			}
			pos.y += Math.min(incY, diff);
		}
		else	{
			stopAutoscrolling();
			return;	// to fast dragged
		}

		//System.err.println("setViewPosition "+pos);
		port.setViewPosition(pos);
	}



	// interface MouseListener

	/** Manage mouse press: set selection if within a node. */
	public void mousePressed(MouseEvent e)	{
		//System.err.println("mousePressed");
		if (SwingUtilities.isRightMouseButton(e))	{
			tree.setEditable(false);	// gegen staendiges Editieren ...
			TreePath tp = tc.getTreePathFromMouseEvent(e);
			if (tp != null)
				tree.setSelectionPath(tp);
		}
		timer.stop();
	}
	/** Manage double click: open node. */
	public void mouseClicked(MouseEvent e)	{
		if (e.getClickCount() >= 2)	{
			tc.openNode(e);
		}
	}
	/** Show popup-menu */
	public void mouseReleased(MouseEvent e)	{
		//System.err.println("mouseReleased");
		tree.setEditable(true);	//wegen staendigem Editieren
		if (SwingUtilities.isRightMouseButton(e))	{
			tc.showActionPopup(e, tree);
		}
		timer.stop();
	}
	public void mouseEntered(MouseEvent e)	{
	}
	public void mouseExited(MouseEvent e)	{
	}
	
	
	
	/** Implementing MouseMotionListener: set popup point in treeview. */
	public void mouseMoved(MouseEvent e)	{
		tc.setMousePoint(e.getPoint(), tree);
	}
	public void mouseDragged(MouseEvent e)	{
	}
	
}