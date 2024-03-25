package fri.gui.swing.xmleditor.controller;

import java.util.List;
import java.util.Vector;
import java.awt.Point;
import fri.gui.mvc.view.swing.SelectionDnd;
import fri.gui.swing.treetable.JTreeTable;
import fri.gui.swing.treetable.TreeTableSelection;
import fri.gui.swing.xmleditor.model.MutableXmlNode;
import fri.gui.swing.xmleditor.model.ControllerModelItem;

/**
	Selection implementation that wraps selected items into ControllerModelItems.

	@author  Ritzberger Fritz
*/

public class XmlTreeTableSelection extends TreeTableSelection implements
	SelectionDnd
{
	/**
		Create a selection requester and setter for a treetable.
	*/
	public XmlTreeTableSelection(JTreeTable treetable)	{
		super(treetable);
	}


	/**
		Get selection (ControllerModelItem or list of) from the view.
		This returns null if nothing is selected.
	*/
	public Object getSelectedObject()	{
		if (treetable == null)
			return null;

		Vector selected = getSelectedNodes();
		if (selected == null || selected.size() <= 0)
			return null;

		Vector selection = new Vector(selected.size());

		for (int i = 0; i < selected.size(); i++)	{
			MutableXmlNode node = (MutableXmlNode)selected.get(i);
			ControllerModelItem cmi = new ControllerModelItem(node);
			selection.add(cmi);
		}

		return selection;
	}
	
	/**
		Set selection (ControllerModelItem or list of) in the view.
		If o is null the selection is cleared.
	*/
	public void setSelectedObject(Object o)	{
		if (treetable == null)
			return;

		if (o == null || o instanceof List && ((List)o).size() <= 0)	{
			clearSelection();
		}
		else	{
			if (o instanceof ControllerModelItem)	{
				ControllerModelItem cmi = (ControllerModelItem)o;
				MutableXmlNode node = cmi.getXmlNode();
				setSelection(node);
			}
			else	{	// must be List
				List l = (List)o;
				Vector v = new Vector(l.size());	// clone the list
				v.addAll(l);
				v.remove(0);	// remove first item, as it is selected here

				// select first item
				ControllerModelItem cmi = (ControllerModelItem)l.get(0);
				MutableXmlNode node = cmi.getXmlNode();
				setSelection(node);

				// select rest
				addSelectedObject(v);
			}
		}
	}

	/**
		Add a ControllerModelItem or a list to selected items in the view.
	*/
	public void addSelectedObject(Object o)	{
		if (treetable == null)
			return;

		List l;

		if (o instanceof ControllerModelItem)	{
			l = new Vector();
			l.add(o);
		}
		else	{	// must be List
			l = (List)o;
		}

		for (int i = 0; i < l.size(); i++)	{
			ControllerModelItem cmi = (ControllerModelItem)l.get(i);
			MutableXmlNode node = cmi.getXmlNode();
			addSelection(node);
		}
	}


	/** Implement SelectionDnd: returns a ControllerModelItem from the node at specified Point. */
	public Object getObjectFromPoint(Point p)	{
		MutableXmlNode node = (MutableXmlNode)getNodeFromPoint(p);

		if (node != null)	{
			ControllerModelItem cmi = new ControllerModelItem(node);
			Vector selection = new Vector(1);
			selection.add(cmi);
			return selection;
		}

		return null;
	}

}