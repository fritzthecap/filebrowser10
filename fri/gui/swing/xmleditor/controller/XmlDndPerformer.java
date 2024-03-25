package fri.gui.swing.xmleditor.controller;

import java.util.*;
import java.awt.Point;
import java.awt.Component;
import java.awt.datatransfer.*;
import javax.swing.JScrollPane;
import org.w3c.dom.Node;

import fri.gui.CursorUtil;
import fri.gui.mvc.controller.swing.dnd.AbstractAutoScrollingDndPerformer;
import fri.gui.swing.treetable.*;
import fri.gui.swing.xmleditor.view.XmlTreeTable;
import fri.gui.swing.xmleditor.model.*;

/**
	Drag&Drop handler that autoscrolls the treetable. It performs
	copy and move actions within one or between two Documents.

	@author  Ritzberger Fritz
*/

public class XmlDndPerformer extends AbstractAutoScrollingDndPerformer
{
	private XmlEditController controller;
	private FileDndPerformer fileDndPerformer;


	/**
		Create a autoscrolling drag and drop handler for W3C Nodes and Files.
	*/
	public XmlDndPerformer(
		XmlTreeTable treetable,
		JScrollPane scrollPane,
		XmlEditController controller,
		FileDndPerformer fileDndPerformer)
	{
		super(treetable, scrollPane);

		this.controller = controller;
		this.fileDndPerformer = fileDndPerformer;
	}


	private JTreeTable getTreeTable()	{
		return (JTreeTable)sensor;
	}


	/** Overridden to switch controller to the view the mouse is over. */
	public boolean dragOver(Point p)	{
		boolean ret = super.dragOver(p);
		controller.setSelectedEditor(getTreeTable());
		return ret;
	}


	/** Checks for types this handler is supporting: W3C Nodes and Files. */
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)	{
			if (flavors[i].equals(XmlNodeTransferable.xmlNodeFlavor))	{
				return XmlNodeTransferable.xmlNodeFlavor;
			}

			if (flavors[i].equals(DataFlavor.javaFileListFlavor))	{
				return DataFlavor.javaFileListFlavor;
			}

			//if (flavors[i].equals(DataFlavor.stringFlavor))
			//	return DataFlavor.stringFlavor;
		}
		return null;
	}


	// send methods

	/**
		Implements DndPerformer: sending selected items.
	*/	
	public Transferable sendTransferable()	{
		if (activated == false || getTreeTable().isEditing())
			return null;
		
		// get selected nodes
		List intransfer = new Vector();
		List selection = (List)controller.getSelection().getSelectedObject();

		if (selection == null || selection.size() <= 0)	{
			return null;
		}

		// check for dragability
		for (Iterator it = selection.iterator(); it.hasNext(); )	{
			ControllerModelItem cmi = (ControllerModelItem)it.next();
			MutableXmlNode treeNode = cmi.getXmlNode();

			if (treeNode.isManipulable() == false)	{
				System.err.println("XmlDndPerformer: can not drag "+treeNode);
				return null;
			}

			// copy node and subtrees
			Node n = new XmlNodeTransferable.SerializableNode(treeNode.getW3CNode());
			intransfer.add(n);
		}
		
		controller.getClipboard().clear();	// clear clipboard to avoid ambiguities
		controller.getClipboard().setSourceComponent(getTreeTable());

		System.err.println("XmlDndPerformer sending data: "+intransfer);

		return new XmlNodeTransferable(intransfer);
	}


	// receive methods

	/** Receive a move command. */
	protected boolean receiveMove(Object data, Point p)	{
		return receive(p, (List)data, false);
	}

	/** Receive a copy command. */
	protected boolean receiveCopy(Object data, Point p)	{
		return receive(p, (List)data, true);
	}

	private boolean receive(Point p, List data, boolean isCopy)	{
		//System.err.println("XmlDndPerformer receiving data "+data);

		// look if we have received Files
		if (fileDndPerformer != null && fileDndPerformer.receiveCopy(data, p))
			return true;

		Component c = getTreeTable();
		CursorUtil.setWaitCursor(c);
		try	{
			// controller must have been switched to drop target frame
			List dropNode = (List)controller.getSelectionHolderDnd().getObjectFromPoint(p);
	
			// look if data came from an editor window
			Component srcComp = controller.getClipboard().getSourceComponent();
			if (srcComp != null)	{	// internal drag&drop
				JTreeTable srcTable = (JTreeTable)srcComp;
				MutableXmlTreeTableModel model = (MutableXmlTreeTableModel)srcTable.getTreeTableModel();
				data = localizeData(data, model);
			}
			else	{	// external data
				MutableXmlTreeTableModel model = (MutableXmlTreeTableModel)getTreeTable().getTreeTableModel();
				data = convertData(data, model);
			}
	
			System.err.println("receiving drop "+data+", is copy: "+isCopy+" at drop node "+dropNode);
	
			if (isCopy)
				controller.getClipboard().copy(data);	// clipboard.setSourceComponent MUST NOT be called!
			else	// isMove
				controller.getClipboard().cut(data);	// clipboard.setSourceComponent MUST NOT be called!
	
			// set selection to drop node, set paste flags for drop node
			// if popup choice is shown, fallback-action-listener works on current selection
			controller.getSelection().setSelectedObject(dropNode);
	
			// unfortunately setSelection is deferred in treetable, so set flags explicitely
			ControllerModelItem cmi = (ControllerModelItem)dropNode.get(0);
			controller.fillActionPASTE(cmi.getXmlNode(), true);
	
			// where to open an optional popup
			controller.setRecentPoint(p);
	
			// actual paste of node
			controller.cb_Paste(dropNode);
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
		return true;
	}


	private List localizeData(List list, MutableXmlTreeTableModel model)	{
		// seek every received node in the local document,
		// if found, take the local node, else create a new one
		Vector v = new Vector(list.size());
		for (int i = 0; i < list.size(); i++)	{
			Node node = (Node)list.get(i);
			MutableXmlNode local = model.findNode(node);
			v.add(new ControllerModelItem(local));
		}
		return v;
	}

	private List convertData(List list, MutableXmlTreeTableModel model)	{
		Vector v = new Vector(list.size());
		for (int i = 0; i < list.size(); i++)	{
			Node node = (Node)list.get(i);
			MutableXmlNode local = model.createMutableXmlNode(node);
			v.add(new ControllerModelItem(local));
		}
		return v;
	}

}