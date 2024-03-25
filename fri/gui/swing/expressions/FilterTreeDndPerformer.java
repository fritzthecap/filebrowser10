package fri.gui.swing.expressions;

import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.*;
import javax.swing.*;
import fri.gui.mvc.view.swing.SelectionDnd;
import fri.gui.mvc.model.MutableModel;
import fri.gui.mvc.view.swing.SwingView;
import fri.gui.mvc.model.swing.AbstractMutableTreeModel;
import fri.gui.mvc.controller.swing.dnd.AbstractAutoScrollingDndPerformer;
import fri.gui.swing.scroll.ScrollPaneUtil;

/**
*/

public class FilterTreeDndPerformer extends AbstractAutoScrollingDndPerformer
{
	private FilterTreeController controller;
	private static FilterTreeController sendingController;
	
	public FilterTreeDndPerformer(Component sensor, FilterTreeController controller)	{
		super(sensor, ScrollPaneUtil.getScrollPane(sensor));
		this.controller = controller;
	}
	
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)
			if (flavors[i].equals(TransferableExpression.expressionFlavor))
				return flavors[i];
		return null;
	}
	
	public Transferable sendTransferable()	{
		List sel = (List)controller.getSelection().getSelectedObject();
		if (sel == null || sel.size() <= 0)
			return null;
			
		List expressions = new ArrayList(sel.size());
		for (int i = 0; i < sel.size(); i++)	{
			FilterTreeNode n = (FilterTreeNode)sel.get(i);
			if (n.isDragable() == false)
				return null;
				
			Object expression = n.getUserObject();
			expressions.add(expression);
		}
		
		controller.setSourceModel((MutableModel)controller.getModel());
		sendingController = controller;
		
		return new TransferableExpression(expressions);
	}


	protected boolean receiveMove(Object data, Point p)	{
		return receive(data, p, false);
	}
	
	protected boolean receiveCopy(Object data, Point p)	{
		return receive(data, p, true);
	}

	// No data exchange between models is implemented, only transfer within one model.
	private boolean receive(Object data, Point p, boolean isCopy)	{
		Object dropTarget = ((SelectionDnd)controller.getSelection()).getObjectFromPoint(p);
		if (dropTarget == null || data instanceof List == false)
			return false;
		
		MutableModel sendingModel = controller.getSourceModel();	// remember the sending model as ...

		List list = (List)data;
		List dropped = new ArrayList(list.size());
		for (int i = 0; i < list.size(); i++)	{
			Object expression = list.get(i);
			Object n = ((AbstractMutableTreeModel)sendingModel).locate(expression);
			if (n == null)
				throw new IllegalArgumentException("Can not locate dragged node in source model: >"+expression+"<");

			dropped.add(n);
		}
		
		if (isCopy)
			sendingController.cb_Copy(dropped);	// ... this sets the sending model wrong ...
		else
			sendingController.cb_Cut(dropped);
			
		controller.setSourceModel(sendingModel);	// ... so restore the sending model before paste happens

		// paste the dropped nodes by opening popup choice
		controller.getSelection().setSelectedObject(dropTarget);
		controller.setPopupPoint(FilterTreeController.ACTION_PASTE, p);
		Action action = (Action)controller.get(FilterTreeController.ACTION_PASTE);	// retrieve the paste action
		action.actionPerformed(		// trigger the paste action
				new ActionEvent(((SwingView)controller.getView()).getSensorComponent(), ActionEvent.ACTION_PERFORMED, FilterTreeController.ACTION_PASTE)
		);
		
		return true;
	}

}
